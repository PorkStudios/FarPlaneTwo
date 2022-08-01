/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.daporkchop.fp2.core.mode.common.server.storage;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.core.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.core.mode.api.tile.ITileSnapshot;
import net.daporkchop.fp2.core.mode.api.tile.TileSnapshot;
import net.daporkchop.fp2.core.util.recycler.Recycler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.fp2.core.mode.common.server.storage.DefaultTileStorage.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DefaultTileHandle<POS extends IFarPos, T extends IFarTile> implements ITileHandle<POS, T> {
    @Getter
    @NonNull
    protected final POS pos;
    @NonNull
    protected final DefaultTileStorage<POS, T> storage;

    @Override
    @SneakyThrows(FStorageException.class)
    public long timestamp() {
        byte[] timestampBytes = this.storage.storageInternal.readGet(access -> access.get(this.storage.columnTimestamp, this.pos.toBytes()));
        return timestampBytes != null
                ? readLongLE(timestampBytes) //timestamp for this tile exists, extract it from the byte array
                : TIMESTAMP_BLANK;
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public ITileSnapshot<POS, T> snapshot() {
        return this.storage.storageInternal.readGet(access -> {
            Recycler<byte[]> posBufferRecycler = this.storage.posBufferRecyclerCache.get();

            //allocate temporary buffer for tile position
            byte[] keyBytes = posBufferRecycler.allocate();
            try {
                //serialize position
                this.storage.posCodec.store(this.pos, keyBytes, 0);

                //read timestamp and tile bytes using multiGet to ensure coherency
                List<byte[]> valueBytes = access.multiGet(
                        ImmutableList.of(this.storage.columnTimestamp, this.storage.columnData),
                        ImmutableList.of(keyBytes, keyBytes));

                byte[] timestampBytes = valueBytes.get(0);
                byte[] tileBytes = valueBytes.get(1);

                return timestampBytes != null
                        ? new TileSnapshot<>(this.pos, readLongLE(timestampBytes), tileBytes)
                        : null;
            } finally {
                posBufferRecycler.release(keyBytes);
            }
        }, ITileSnapshot::release);
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public boolean set(@NonNull ITileMetadata metadata, @NonNull T tile) {
        boolean result = this.storage.storageInternal.transactAtomicGet(access -> {
            byte[] keyBytes = this.pos.toBytes();

            //obtain an exclusive lock on both timestamp keys to ensure coherency
            List<byte[]> get = access.multiGet(
                    ImmutableList.of(this.storage.columnTimestamp, this.storage.columnDirtyTimestamp),
                    ImmutableList.of(keyBytes, keyBytes));

            byte[] timestampBytes = get.get(0);
            long timestamp = timestampBytes != null
                    ? readLongLE(timestampBytes) //timestamp for this tile exists, extract it from the byte array
                    : TIMESTAMP_BLANK;

            byte[] dirtyTimestampBytes = get.get(1);
            long dirtyTimestamp = dirtyTimestampBytes != null
                    ? readLongLE(dirtyTimestampBytes) //dirty timestamp for this tile exists, extract it from the byte array
                    : TIMESTAMP_BLANK;

            if (metadata.timestamp() <= timestamp) { //the new timestamp isn't newer than the existing one, so we can't replace it
                //exit without making any changes
                return false;
            }

            //store new timestamp in db
            access.put(this.storage.columnTimestamp, keyBytes, writeLongLE(metadata.timestamp()));

            //clear dirty timestamp if needed
            if (metadata.timestamp() >= dirtyTimestamp) {
                access.delete(this.storage.columnDirtyTimestamp, keyBytes);
            }

            //encode tile and store it in db
            ByteBuf buf = ByteBufAllocator.DEFAULT.heapBuffer();
            try {
                if (tile.write(buf)) { //the tile was empty, remove it from the db!
                    access.delete(this.storage.columnData, keyBytes);
                } else { //the tile was non-empty, store it in the db
                    access.put(this.storage.columnData, keyBytes, Arrays.copyOfRange(buf.array(), buf.arrayOffset(), buf.arrayOffset() + buf.writerIndex()));
                }
            } finally {
                buf.release();
            }

            return true;
        });

        if (result) { //the tile was modified, notify the listeners
            this.storage.listeners.forEach(listener -> listener.tilesChanged(Stream.of(this.pos)));
        }
        return result;
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public long dirtyTimestamp() {
        byte[] dirtyTimestampBytes = this.storage.storageInternal.readGet(access -> access.get(this.storage.columnDirtyTimestamp, this.pos.toBytes()));
        return dirtyTimestampBytes != null
                ? Unpooled.wrappedBuffer(dirtyTimestampBytes).readLongLE() //dirty timestamp for this tile exists, extract it from the byte array
                : TIMESTAMP_BLANK;
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public boolean markDirty(long dirtyTimestamp) {
        boolean result = this.storage.storageInternal.transactAtomicGet(access -> {
            byte[] keyBytes = this.pos.toBytes();

            //obtain an exclusive lock on both timestamp keys to ensure coherency
            List<byte[]> get = access.multiGet(
                    ImmutableList.of(this.storage.columnTimestamp, this.storage.columnDirtyTimestamp),
                    ImmutableList.of(keyBytes, keyBytes));

            byte[] timestampBytes = get.get(0);
            long timestamp = timestampBytes != null
                    ? Unpooled.wrappedBuffer(timestampBytes).readLongLE() //timestamp for this tile exists, extract it from the byte array
                    : TIMESTAMP_BLANK;

            byte[] dirtyTimestampBytes = get.get(1);
            long existingDirtyTimestamp = dirtyTimestampBytes != null
                    ? Unpooled.wrappedBuffer(dirtyTimestampBytes).readLongLE() //dirty timestamp for this tile exists, extract it from the byte array
                    : TIMESTAMP_BLANK;

            if (timestamp == TIMESTAMP_BLANK
                //the tile doesn't exist, so we can't mark it as dirty
                || dirtyTimestamp <= timestamp
                || dirtyTimestamp <= existingDirtyTimestamp) { //the new dirty timestamp isn't newer than the existing one, so we can't replace it
                //exit without making any changes
                return false;
            }

            //store new dirty timestamp in db
            access.put(this.storage.columnDirtyTimestamp, keyBytes, UnpooledByteBufAllocator.DEFAULT.heapBuffer(Long.BYTES, Long.BYTES).writeLongLE(dirtyTimestamp).array());

            return true;
        });

        if (result) { //the tile's dirty timestamp was modified, notify the listeners
            this.storage.listeners.forEach(listener -> listener.tilesDirty(Stream.of(this.pos)));
        }
        return result;
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public boolean clearDirty() {
        return this.storage.storageInternal.transactAtomicGet(access -> {
            byte[] keyBytes = this.pos.toBytes();

            if (access.get(this.storage.columnDirtyTimestamp, keyBytes) == null) { //the tile isn't dirty
                //exit without making any changes
                return false;
            }

            //clear dirty timestamp in db
            access.delete(this.storage.columnDirtyTimestamp, keyBytes);

            return true;
        });
    }
}
