/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.server.storage;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.TilePosCodec;
import net.daporkchop.fp2.core.engine.tile.ITileHandle;
import net.daporkchop.fp2.core.engine.tile.ITileMetadata;
import net.daporkchop.fp2.core.engine.tile.ITileSnapshot;
import net.daporkchop.fp2.core.engine.tile.TileSnapshot;
import net.daporkchop.fp2.core.util.datastructure.java.list.ListUtils;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.core.util.GlobalAllocators.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DefaultTileHandle implements ITileHandle {
    @Getter
    @NonNull
    protected final TilePos pos;
    @NonNull
    protected final DefaultTileStorage storage;

    @Override
    @SneakyThrows(FStorageException.class)
    public long timestamp() {
        return this.storage.storageInternal.readGet(access -> {
            /*
             * struct buffer {
             *     POS pos; //db key
             *     int64_t timestamp; //load target for db value
             * };
             */

            final long bufferKeyOffset = 0L;
            final int bufferKeySize = toIntExact(TilePosCodec.size());

            final long bufferTimestampOffset = bufferKeyOffset + bufferKeySize;
            final int bufferTimestampSize = LONG_SIZE;

            final long bufferSize = bufferTimestampOffset + bufferTimestampSize;

            long buffer = PUnsafe.allocateMemory(bufferSize);
            try {
                //serialize position
                TilePosCodec.store(this.pos, buffer + bufferKeyOffset);

                int size;

                { //db operations
                    //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);
                    ByteBuffer timestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferTimestampOffset, bufferTimestampSize);

                    //actually get timestamp from db
                    size = access.get(this.storage.columnTimestamp, keyBuffer, timestampBuffer);

                    //release ByteBuffers again
                    byteBufferRecycler.release(timestampBuffer);
                    byteBufferRecycler.release(keyBuffer);
                }

                return size >= 0
                        ? PUnsafe.getUnalignedLongLE(buffer + bufferTimestampOffset) //we assume that the timestamp, if it exists, is always exactly 8 bytes long
                        : TIMESTAMP_BLANK; //key isn't present in db -> the tile doesn't exist
            } finally {
                PUnsafe.freeMemory(buffer);
            }
        });
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public ITileSnapshot snapshot() {
        return this.storage.storageInternal.readGet(access -> {
            /*
             * struct buffer {
             *     POS pos; //db key
             *     int64_t timestamp; //load target for db value (timestamp)
             *     uint8_t data[this.storage.tileCodec.maxSize()]; //load target for db value (tile data)
             * };
             */

            final long bufferKeyOffset = 0L;
            final int bufferKeySize = toIntExact(TilePosCodec.size());

            final long bufferTimestampOffset = bufferKeyOffset + bufferKeySize;
            final int bufferTimestampSize = LONG_SIZE;

            final long bufferTileOffset = bufferTimestampOffset + bufferTimestampSize;
            final int bufferTileSize = toIntExact(Tile.CODEC.maxSize());

            final long bufferSize = bufferTileOffset + bufferTileSize;

            long buffer = PUnsafe.allocateMemory(bufferSize);
            try {
                //serialize position
                TilePosCodec.store(this.pos, buffer + bufferKeyOffset);

                int sizeTimestamp;
                int sizeTile;

                { //db operations
                    //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);
                    ByteBuffer timestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferTimestampOffset, bufferTimestampSize);
                    ByteBuffer tileBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferTileOffset, bufferTileSize);

                    //temporarily allocate int[] for sizes array
                    ArrayAllocator<int[]> intArrayAlloc = ALLOC_INT.get();
                    int[] sizes = intArrayAlloc.atLeast(2);

                    //actually get timestamp and tile data from db
                    boolean success = access.multiGet(
                            this.storage.listColumns_Timestamp_Data,
                            ListUtils.repeat(keyBuffer, 2),
                            ListUtils.immutableListOf(timestampBuffer, tileBuffer), //this allocates a single two-element list referencing the two buffers
                            sizes);

                    sizeTimestamp = sizes[0];
                    sizeTile = sizes[1];

                    //release int[] again
                    intArrayAlloc.release(sizes);

                    //release ByteBuffers again
                    byteBufferRecycler.release(tileBuffer);
                    byteBufferRecycler.release(timestampBuffer);
                    byteBufferRecycler.release(keyBuffer);

                    checkState(success, "insufficient buffer space???");
                }

                if (sizeTimestamp < 0) { //timestamp wasn't present in the db, meaning the tile doesn't exist
                    return null;
                } else { //the tile exists
                    return TileSnapshot.of(
                            this.pos,
                            PUnsafe.getUnalignedLongLE(buffer + bufferTimestampOffset), //we assume that the timestamp, if it exists, is always exactly 8 bytes long
                            buffer + bufferTileOffset, sizeTile);
                }
            } finally {
                PUnsafe.freeMemory(buffer);
            }
        });
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public boolean set(@NonNull ITileMetadata metadata, @NonNull Tile tile) {
        boolean result = this.storage.storageInternal.transactAtomicGet(access -> {
            /*
             * struct buffer {
             *     POS pos; //db key
             *     int64_t timestamp; //load target for db value (timestamp)
             *     int64_t dirtyTimestamp; //load target for db value (dirtyTimestamp)
             *     uint8_t data[this.storage.tileCodec.maxSize()]; //destination region to serialize tile data in
             * };
             */

            final long bufferKeyOffset = 0L;
            final int bufferKeySize = toIntExact(TilePosCodec.size());

            final long bufferTimestampOffset = bufferKeyOffset + bufferKeySize;
            final int bufferTimestampSize = LONG_SIZE;

            final long bufferDirtyTimestampOffset = bufferTimestampOffset + bufferTimestampSize;
            final int bufferDirtyTimestampSize = LONG_SIZE;

            final long bufferTileOffset = bufferDirtyTimestampOffset + bufferDirtyTimestampSize;
            final int bufferTileSize = toIntExact(Tile.CODEC.maxSize());

            final long bufferSize = bufferTileOffset + bufferTileSize;

            long buffer = PUnsafe.allocateMemory(bufferSize);
            try {
                //serialize position
                TilePosCodec.store(this.pos, buffer + bufferKeyOffset);

                int sizeTimestamp;
                int sizeDirtyTimestamp;

                { //db operations: load timestamp and dirtyTimestamp from db
                    //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);
                    ByteBuffer timestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferTimestampOffset, bufferTimestampSize);
                    ByteBuffer dirtyTimestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(),
                            buffer + bufferDirtyTimestampOffset, bufferDirtyTimestampSize);

                    //temporarily allocate int[] for sizes array
                    ArrayAllocator<int[]> intArrayAlloc = ALLOC_INT.get();
                    int[] sizes = intArrayAlloc.atLeast(2);

                    //actually get timestamp and dirtyTimestamp from db
                    boolean success = access.multiGet(
                            this.storage.listColumns_Timestamp_DirtyTimestamp,
                            ListUtils.repeat(keyBuffer, 2),
                            ListUtils.immutableListOf(timestampBuffer, dirtyTimestampBuffer), //this allocates a single two-element list referencing the two buffers
                            sizes);

                    sizeTimestamp = sizes[0];
                    sizeDirtyTimestamp = sizes[1];

                    //release int[] again
                    intArrayAlloc.release(sizes);

                    //release ByteBuffers again
                    byteBufferRecycler.release(dirtyTimestampBuffer);
                    byteBufferRecycler.release(timestampBuffer);
                    byteBufferRecycler.release(keyBuffer);

                    checkState(success, "insufficient buffer space???");
                }

                long timestamp = sizeTimestamp < 0
                        ? TIMESTAMP_BLANK
                        : PUnsafe.getUnalignedLongLE(buffer + bufferTimestampOffset); //we assume that the timestamp, if it exists, is always exactly 8 bytes long

                long dirtyTimestamp = sizeDirtyTimestamp < 0
                        ? TIMESTAMP_BLANK
                        : PUnsafe.getUnalignedLongLE(buffer + bufferDirtyTimestampOffset); //we assume that the timestamp, if it exists, is always exactly 8 bytes long

                if (metadata.timestamp() <= timestamp) { //the new timestamp isn't newer than the existing one, so we can't replace it
                    //exit without making any changes
                    return false;
                }

                { //db operations: put new timestamp into db
                    //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);
                    ByteBuffer timestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferTimestampOffset, bufferTimestampSize);

                    //store new timestamp in buffer
                    PUnsafe.putUnalignedLongLE(buffer + bufferTimestampOffset, metadata.timestamp());

                    //actually put new timestamp into db
                    access.put(this.storage.columnTimestamp, keyBuffer, timestampBuffer);

                    //release ByteBuffers again
                    byteBufferRecycler.release(timestampBuffer);
                    byteBufferRecycler.release(keyBuffer);
                }

                //clear dirty timestamp if needed
                if (metadata.timestamp() >= dirtyTimestamp) {
                    { //db operations: delete old dirtyTimestamp
                        //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                        Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                        ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);

                        //actually delete old dirtyTimestamp
                        access.delete(this.storage.columnDirtyTimestamp, keyBuffer);

                        //release ByteBuffers again
                        byteBufferRecycler.release(keyBuffer);
                    }
                }

                { //db operations: encode tile and store it in db
                    //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);
                    ByteBuffer tileBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferTileOffset, bufferTileSize);

                    if (tile.isEmpty()) { //the tile is empty, so it has no contents for us to bother keeping around
                        //actually delete tile data from db
                        access.delete(this.storage.columnData, keyBuffer);
                    } else { //the tile isn't empty
                        //actually encode tile
                        try (DataOut out = DataOut.wrap(tileBuffer)) {
                            Tile.CODEC.store(tile, out);
                        } catch (IOException e) {
                            throw PUnsafe.throwException(e); //sneaky rethrow
                        }
                        tileBuffer.flip();

                        //actually put tile data into db
                        access.put(this.storage.columnData, keyBuffer, tileBuffer);
                    }

                    //release ByteBuffers again
                    byteBufferRecycler.release(tileBuffer);
                    byteBufferRecycler.release(keyBuffer);
                }

                return true;
            } finally {
                PUnsafe.freeMemory(buffer);
            }
        });

        if (result) { //the tile was modified, notify the listeners
            this.storage.listeners.forEach(listener -> listener.tilesChanged(Stream.of(this.pos)));
        }
        return result;
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public long dirtyTimestamp() {
        return this.storage.storageInternal.readGet(access -> {
            /*
             * struct buffer {
             *     POS pos; //db key
             *     int64_t dirtyTimestamp; //load target for db value
             * };
             */

            final long bufferKeyOffset = 0L;
            final int bufferKeySize = toIntExact(TilePosCodec.size());

            final long bufferDirtyTimestampOffset = bufferKeyOffset + bufferKeySize;
            final int bufferDirtyTimestampSize = LONG_SIZE;

            final long bufferSize = bufferDirtyTimestampOffset + bufferDirtyTimestampSize;

            long buffer = PUnsafe.allocateMemory(bufferSize);
            try {
                //serialize position
                TilePosCodec.store(this.pos, buffer + bufferKeyOffset);

                int size;

                { //db operations
                    //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);
                    ByteBuffer dirtyTimestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(),
                            buffer + bufferDirtyTimestampOffset, bufferDirtyTimestampSize);

                    //actually get dirty timestamp from db
                    size = access.get(this.storage.columnDirtyTimestamp, keyBuffer, dirtyTimestampBuffer);

                    //release ByteBuffers again
                    byteBufferRecycler.release(dirtyTimestampBuffer);
                    byteBufferRecycler.release(keyBuffer);
                }

                return size >= 0
                        ? PUnsafe.getUnalignedLongLE(buffer
                                                     + bufferDirtyTimestampOffset) //we assume that the dirty timestamp, if it exists, is always exactly 8 bytes long
                        : TIMESTAMP_BLANK; //key isn't present in db -> the tile doesn't exist or doesn't have a dirty timestamp
            } finally {
                PUnsafe.freeMemory(buffer);
            }
        });
    }

    @Override
    @SneakyThrows(FStorageException.class)
    public boolean markDirty(long dirtyTimestamp) {
        boolean result = this.storage.storageInternal.transactAtomicGet(access -> {
            /*
             * struct buffer {
             *     POS pos; //db key
             *     int64_t timestamp; //load target for db value (timestamp)
             *     int64_t dirtyTimestamp; //load target for db value (dirtyTimestamp)
             * };
             */

            final long bufferKeyOffset = 0L;
            final int bufferKeySize = toIntExact(TilePosCodec.size());

            final long bufferTimestampOffset = bufferKeyOffset + bufferKeySize;
            final int bufferTimestampSize = LONG_SIZE;

            final long bufferDirtyTimestampOffset = bufferTimestampOffset + bufferTimestampSize;
            final int bufferDirtyTimestampSize = LONG_SIZE;

            final long bufferSize = bufferDirtyTimestampOffset + bufferDirtyTimestampSize;

            long buffer = PUnsafe.allocateMemory(bufferSize);
            try {
                //serialize position
                TilePosCodec.store(this.pos, buffer + bufferKeyOffset);

                int sizeTimestamp;
                int sizeDirtyTimestamp;

                { //db operations: load timestamp and dirtyTimestamp from db
                    //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);
                    ByteBuffer timestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferTimestampOffset, bufferTimestampSize);
                    ByteBuffer dirtyTimestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(),
                            buffer + bufferDirtyTimestampOffset, bufferDirtyTimestampSize);

                    //temporarily allocate int[] for sizes array
                    ArrayAllocator<int[]> intArrayAlloc = ALLOC_INT.get();
                    int[] sizes = intArrayAlloc.atLeast(2);

                    //actually get timestamp and dirtyTimestamp from db
                    boolean success = access.multiGet(
                            this.storage.listColumns_Timestamp_DirtyTimestamp,
                            ListUtils.repeat(keyBuffer, 2),
                            ListUtils.immutableListOf(timestampBuffer, dirtyTimestampBuffer), //this allocates a single two-element list referencing the two buffers
                            sizes);

                    sizeTimestamp = sizes[0];
                    sizeDirtyTimestamp = sizes[1];

                    //release int[] again
                    intArrayAlloc.release(sizes);

                    //release ByteBuffers again
                    byteBufferRecycler.release(dirtyTimestampBuffer);
                    byteBufferRecycler.release(timestampBuffer);
                    byteBufferRecycler.release(keyBuffer);

                    checkState(success, "insufficient buffer space???");
                }

                long timestamp = sizeTimestamp < 0
                        ? TIMESTAMP_BLANK
                        : PUnsafe.getUnalignedLongLE(buffer + bufferTimestampOffset); //we assume that the timestamp, if it exists, is always exactly 8 bytes long

                long existingDirtyTimestamp = sizeDirtyTimestamp < 0
                        ? TIMESTAMP_BLANK
                        : PUnsafe.getUnalignedLongLE(buffer + bufferDirtyTimestampOffset); //we assume that the timestamp, if it exists, is always exactly 8 bytes long

                if (timestamp == TIMESTAMP_BLANK //the tile doesn't exist, so we can't mark it as dirty
                    || dirtyTimestamp <= timestamp
                    || dirtyTimestamp <= existingDirtyTimestamp) { //the new dirty timestamp isn't newer than the existing one, so we can't replace it
                    //exit without making any changes
                    return false;
                }

                { //db operations: put new dirtyTimestamp into db
                    //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);
                    ByteBuffer dirtyTimestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(),
                            buffer + bufferDirtyTimestampOffset, bufferDirtyTimestampSize);

                    //store new dirtyTimestamp in buffer
                    PUnsafe.putUnalignedLongLE(buffer + bufferDirtyTimestampOffset, dirtyTimestamp);

                    //actually put new dirtyTimestamp into db
                    access.put(this.storage.columnDirtyTimestamp, keyBuffer, dirtyTimestampBuffer);

                    //release ByteBuffers again
                    byteBufferRecycler.release(dirtyTimestampBuffer);
                    byteBufferRecycler.release(keyBuffer);
                }

                return true;
            } finally {
                PUnsafe.freeMemory(buffer);
            }
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
            /*
             * struct buffer {
             *     POS pos; //db key
             * };
             */

            final long bufferKeyOffset = 0L;
            final int bufferKeySize = toIntExact(TilePosCodec.size());

            final long bufferSize = bufferKeyOffset + bufferKeySize;

            long buffer = PUnsafe.allocateMemory(bufferSize);
            try {
                //serialize position
                TilePosCodec.store(this.pos, buffer + bufferKeyOffset);

                int size;

                { //db operations: check if dirty timestamp is present in db
                    //temporarily allocate fake direct ByteBuffers with no contents and point them to the corresponding regions of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);
                    ByteBuffer dirtyTimestampBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), 0L, 0);

                    //actually get dirty timestamp from db
                    size = access.get(this.storage.columnDirtyTimestamp, keyBuffer, dirtyTimestampBuffer);

                    //release ByteBuffers again
                    byteBufferRecycler.release(dirtyTimestampBuffer);
                    byteBufferRecycler.release(keyBuffer);
                }

                if (size < 0) { //the tile's dirty timestamp isn't set
                    //exit without making any changes
                    return false;
                }

                { //db operations: clear dirty timestamp in db
                    //temporarily allocate fake direct ByteBuffer with no contents and point it to the corresponding region of the real buffer
                    Recycler<ByteBuffer> byteBufferRecycler = DirectBufferHackery.byteRecycler();
                    ByteBuffer keyBuffer = DirectBufferHackery.reset(byteBufferRecycler.allocate(), buffer + bufferKeyOffset, bufferKeySize);

                    //actually delete dirty timestamp from
                    access.delete(this.storage.columnDirtyTimestamp, keyBuffer);

                    //release ByteBuffer again
                    byteBufferRecycler.release(keyBuffer);
                }

                return true;
            } finally {
                PUnsafe.freeMemory(buffer);
            }
        });
    }
}
