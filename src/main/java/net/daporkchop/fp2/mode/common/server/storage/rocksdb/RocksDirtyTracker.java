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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.server.storage.IFarDirtyTracker;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.primitive.lambda.ObjLongConsumer;
import net.daporkchop.lib.primitive.lambda.ObjLongLongFunction;
import net.daporkchop.lib.primitive.map.ObjLongMap;
import net.daporkchop.lib.primitive.map.concurrent.ObjLongConcurrentHashMap;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import static net.daporkchop.fp2.mode.common.server.storage.rocksdb.RocksStorage.*;

/**
 * Implementation of {@link IFarDirtyTracker} on rocksdb.
 * <p>
 * Despite all mutating operations being forwarded to rocksdb, the entries are still stored in memory. This enhances performance by avoiding the need for database
 * transactions, which are overkill when everything consists of simple CAS operations.
 *
 * @author DaPorkchop_
 */
public class RocksDirtyTracker<POS extends IFarPos> implements IFarDirtyTracker<POS> {
    protected final RocksStorage<POS, ?> storage;

    protected final ObjLongMap<POS> map = new ObjLongConcurrentHashMap<>(-1L);

    public RocksDirtyTracker(@NonNull RocksStorage<POS, ?> storage) {
        this.storage = storage;

        //load all dirty entries into memory
        try (RocksIterator itr = this.storage.db.newIterator(this.storage.cfDirty)) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                byte[] key = itr.key();
                byte[] value = itr.value();

                this.map.put(storage.mode.readPos(Unpooled.wrappedBuffer(key)), Unpooled.wrappedBuffer(value).readLong());
            }
        }
        Constants.LOGGER.info("Restored {} dirty tiles", this.map.size());
    }

    @Override
    public boolean markDirty(@NonNull POS pos, long timestamp) {
        class State implements ObjLongLongFunction<POS> {
            boolean inserted = false;

            @Override
            @SneakyThrows(RocksDBException.class)
            public long applyAsLong(@NonNull POS pos, long oldTimestamp) {
                if (oldTimestamp < timestamp) { //old timestamp is less than new one, entry should be added
                    ByteBuf key = ByteBufAllocator.DEFAULT.directBuffer();
                    ByteBuf value = ByteBufAllocator.DEFAULT.directBuffer();
                    try {
                        pos.writePos(key); //encode position
                        value.writeLong(timestamp);

                        //delete entry from database
                        RocksDirtyTracker.this.storage.db.put(RocksDirtyTracker.this.storage.cfDirty, WRITE_OPTIONS, key.nioBuffer(), value.nioBuffer());
                    } finally {
                        value.release();
                        key.release();
                    }

                    this.inserted = true;
                    return timestamp; //insert new entry into map
                }

                return oldTimestamp; //keep existing entry
            }
        }

        State state = new State();
        this.map.compute(pos, state);
        return state.inserted;
    }

    @Override
    public long dirtyTimestamp(@NonNull POS pos) {
        return this.map.get(pos); //returns -1L if not found
    }

    @Override
    public boolean clearDirty(@NonNull POS pos, long timestamp) {
        class State implements ObjLongLongFunction<POS> {
            boolean removed = false;

            @Override
            @SneakyThrows(RocksDBException.class)
            public long applyAsLong(@NonNull POS pos, long oldTimestamp) {
                if (timestamp == oldTimestamp) { //timestamp matches, entry should be removed
                    ByteBuf key = ByteBufAllocator.DEFAULT.directBuffer();
                    try {
                        pos.writePos(key); //encode position

                        //delete entry from database
                        RocksDirtyTracker.this.storage.db.delete(RocksDirtyTracker.this.storage.cfDirty, WRITE_OPTIONS, key.nioBuffer());
                    } finally {
                        key.release();
                    }

                    this.removed = true;
                    return -1L; //return -1L to remove entry from map
                }

                return oldTimestamp; //keep existing entry
            }
        }

        State state = new State();
        this.map.compute(pos, state);
        return state.removed;
    }

    @Override
    public void forEachDirtyPos(@NonNull ObjLongConsumer<POS> callback) {
        this.map.forEach(callback);
    }
}
