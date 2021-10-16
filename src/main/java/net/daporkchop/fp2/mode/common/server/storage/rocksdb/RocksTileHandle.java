/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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
 *
 */

package net.daporkchop.fp2.mode.common.server.storage.rocksdb;

import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.mode.api.tile.ITileSnapshot;
import net.daporkchop.fp2.mode.api.tile.TileSnapshot;
import org.rocksdb.RocksDBException;
import org.rocksdb.Transaction;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.fp2.mode.common.server.storage.rocksdb.RocksStorage.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class RocksTileHandle<POS extends IFarPos, T extends IFarTile> implements ITileHandle<POS, T> {
    @Getter
    @NonNull
    protected final POS pos;
    @NonNull
    protected final RocksStorage<POS, T> storage;

    @Override
    @SneakyThrows(RocksDBException.class)
    public long timestamp() {
        byte[] timestampBytes = this.storage.db.get(this.storage.cfTileTimestamp, this.pos.toBytes());
        return timestampBytes != null
                ? readLongLE(timestampBytes) //timestamp for this tile exists, extract it from the byte array
                : TIMESTAMP_BLANK;
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public ITileSnapshot<POS, T> snapshot() {
        byte[] keyBytes = this.pos.toBytes();

        //read timestamp and tile bytes using multiGet to ensure coherency
        List<byte[]> valueBytes = this.storage.db.multiGetAsList(
                ImmutableList.of(this.storage.cfTileTimestamp, this.storage.cfTileData),
                ImmutableList.of(keyBytes, keyBytes));

        byte[] timestampBytes = valueBytes.get(0);
        byte[] tileBytes = valueBytes.get(1);

        return timestampBytes != null
                ? new TileSnapshot<>(this.pos, readLongLE(timestampBytes), tileBytes)
                : null;
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public boolean set(@NonNull ITileMetadata metadata, @NonNull T tile) {
        try (Transaction txn = this.storage.db.beginTransaction(WRITE_OPTIONS)) {
            byte[] keyBytes = this.pos.toBytes();

            //obtain an exclusive lock on both timestamp keys to ensure coherency
            byte[][] get = txn.multiGetForUpdate(READ_OPTIONS,
                    ImmutableList.of(this.storage.cfTileTimestamp, this.storage.cfTileDirtyTimestamp),
                    new byte[][]{ keyBytes, keyBytes });

            byte[] timestampBytes = get[0];
            long timestamp = timestampBytes != null
                    ? readLongLE(timestampBytes) //timestamp for this tile exists, extract it from the byte array
                    : TIMESTAMP_BLANK;

            byte[] dirtyTimestampBytes = get[1];
            long dirtyTimestamp = dirtyTimestampBytes != null
                    ? readLongLE(dirtyTimestampBytes) //dirty timestamp for this tile exists, extract it from the byte array
                    : TIMESTAMP_BLANK;

            if (metadata.timestamp() <= timestamp) { //the new timestamp isn't newer than the existing one, so we can't replace it
                //exit without committing the transaction
                return false;
            }

            //store new timestamp in db
            txn.put(this.storage.cfTileTimestamp, keyBytes, writeLongLE(metadata.timestamp()));

            //clear dirty timestamp if needed
            if (metadata.timestamp() >= dirtyTimestamp) {
                txn.delete(this.storage.cfTileDirtyTimestamp, keyBytes);
            }

            //encode tile and store it in db
            ByteBuf buf = ByteBufAllocator.DEFAULT.heapBuffer();
            try {
                if (tile.write(buf)) { //the tile was empty, remove it from the db!
                    txn.delete(this.storage.cfTileData, keyBytes);
                } else { //the tile was non-empty, store it in the db
                    txn.put(this.storage.cfTileData, keyBytes, Arrays.copyOfRange(buf.array(), buf.arrayOffset(), buf.arrayOffset() + buf.writerIndex()));
                }
            } finally {
                buf.release();
            }

            //commit transaction and report that a change was made
            txn.commit();

            this.storage.listeners.forEach(listener -> listener.tilesChanged(Stream.of(this.pos)));
            return true;
        }
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public long dirtyTimestamp() {
        byte[] dirtyTimestampBytes = this.storage.db.get(this.storage.cfTileDirtyTimestamp, this.pos.toBytes());
        return dirtyTimestampBytes != null
                ? Unpooled.wrappedBuffer(dirtyTimestampBytes).readLongLE() //dirty timestamp for this tile exists, extract it from the byte array
                : TIMESTAMP_BLANK;
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public boolean markDirty(long dirtyTimestamp) {
        try (Transaction txn = this.storage.db.beginTransaction(WRITE_OPTIONS)) {
            byte[] keyBytes = this.pos.toBytes();

            //obtain an exclusive lock on both timestamp keys to ensure coherency
            byte[][] get = txn.multiGetForUpdate(READ_OPTIONS,
                    ImmutableList.of(this.storage.cfTileTimestamp, this.storage.cfTileDirtyTimestamp),
                    new byte[][]{ keyBytes, keyBytes });

            byte[] timestampBytes = get[0];
            long timestamp = timestampBytes != null
                    ? Unpooled.wrappedBuffer(timestampBytes).readLongLE() //timestamp for this tile exists, extract it from the byte array
                    : TIMESTAMP_BLANK;

            byte[] dirtyTimestampBytes = get[1];
            long existingDirtyTimestamp = dirtyTimestampBytes != null
                    ? Unpooled.wrappedBuffer(dirtyTimestampBytes).readLongLE() //dirty timestamp for this tile exists, extract it from the byte array
                    : TIMESTAMP_BLANK;

            if (timestamp == TIMESTAMP_BLANK //the tile doesn't exist, so we can't mark it as dirty
                    || dirtyTimestamp <= timestamp || dirtyTimestamp <= existingDirtyTimestamp) { //the new dirty timestamp isn't newer than the existing one, so we can't replace it
                //exit without committing the transaction
                return false;
            }

            //store new dirty timestamp in db
            txn.put(this.storage.cfTileDirtyTimestamp, keyBytes, UnpooledByteBufAllocator.DEFAULT.heapBuffer(Long.BYTES, Long.BYTES).writeLongLE(dirtyTimestamp).array());

            //commit transaction and report that a change was made
            txn.commit();

            this.storage.listeners.forEach(listener -> listener.tilesDirty(Stream.of(this.pos)));
            return true;
        }
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public boolean clearDirty() {
        try (Transaction txn = this.storage.db.beginTransaction(WRITE_OPTIONS)) {
            byte[] keyBytes = this.pos.toBytes();

            if (txn.getForUpdate(READ_OPTIONS, this.storage.cfTileDirtyTimestamp, keyBytes, true) == null) { //the tile isn't dirty
                //exit without committing the transaction
                return false;
            }

            //store new dirty timestamp in db
            txn.delete(this.storage.cfTileDirtyTimestamp, keyBytes);

            //commit transaction and report that a change was made
            txn.commit();
            return true;
        }
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public boolean anyVanillaExists() {
        return this.storage.db.get(this.storage.cfAnyVanillaExists, this.pos.toBytes()) != null;
    }
}
