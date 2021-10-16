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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.storage.IFarStorage;
import net.daporkchop.fp2.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.mode.common.server.AbstractFarTileProvider;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.FlushOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Transaction;
import org.rocksdb.TransactionDB;
import org.rocksdb.TransactionDBOptions;
import org.rocksdb.WriteOptions;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.mode.api.tile.ITileMetadata.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class RocksStorage<POS extends IFarPos, T extends IFarTile> implements IFarStorage<POS, T> {
    protected static final DBOptions DB_OPTIONS = new DBOptions()
            .setCreateIfMissing(true)
            .setCreateMissingColumnFamilies(true)
            .setAllowConcurrentMemtableWrite(true)
            .setKeepLogFileNum(1L);

    protected static final TransactionDBOptions TX_DB_OPTIONS = new TransactionDBOptions();

    protected static final ColumnFamilyOptions CF_OPTIONS = new ColumnFamilyOptions()
            .setCompressionType(CompressionType.ZSTD_COMPRESSION);

    protected static final ReadOptions READ_OPTIONS = new ReadOptions();
    protected static final WriteOptions WRITE_OPTIONS = new WriteOptions();
    protected static final FlushOptions FLUSH_OPTIONS = new FlushOptions().setWaitForFlush(true).setAllowWriteStall(true);

    protected static final byte[] COLUMN_NAME_TILE_TIMESTAMP = "tile_timestamp".getBytes(StandardCharsets.UTF_8);
    protected static final byte[] COLUMN_NAME_TILE_DIRTY_TIMESTAMP = "tile_dirty_timestamp".getBytes(StandardCharsets.UTF_8);
    protected static final byte[] COLUMN_NAME_TILE_DATA = "tile_data".getBytes(StandardCharsets.UTF_8);
    protected static final byte[] COLUMN_NAME_ANY_VANILLA_EXISTS = "tile_any_vanilla_terrain_exists".getBytes(StandardCharsets.UTF_8);

    //
    // rocksdb helper methods
    //

    @SneakyThrows(RocksDBException.class)
    protected static ByteBuf get(@NonNull RocksDB db, @NonNull ColumnFamilyHandle handle, @NonNull ByteBuf key, int preallocateBytes) {
        ByteBuffer keyNioBuffer = key.nioBuffer();

        ByteBuf value = ByteBufAllocator.DEFAULT.directBuffer(preallocateBytes);

        int len = db.get(handle, READ_OPTIONS, keyNioBuffer, value.nioBuffer(value.readerIndex(), value.capacity()));

        if (len == RocksDB.NOT_FOUND) { //value wasn't found
            value.release();
            return null;
        } else if (len > value.writableBytes()) { //value was found, but is bigger than the buffer
            //grow buffer to required size and try again
            value.ensureWritable(len);

            keyNioBuffer.rewind();
            len = db.get(handle, READ_OPTIONS, keyNioBuffer, value.nioBuffer(value.readerIndex(), value.capacity()));
        }
        return value.writerIndex(len);
    }

    @SneakyThrows(RocksDBException.class)
    protected static void put(@NonNull RocksDB db, @NonNull ColumnFamilyHandle handle, @NonNull ByteBuf key, @NonNull ByteBuf value) {
        db.put(handle, WRITE_OPTIONS, key.nioBuffer(), value.nioBuffer());
    }

    @SneakyThrows(RocksDBException.class)
    protected static void delete(@NonNull RocksDB db, @NonNull ColumnFamilyHandle handle, @NonNull ByteBuf key) {
        db.delete(handle, WRITE_OPTIONS, key.nioBuffer());
    }

    protected static long readLongLE(@NonNull byte[] src) {
        return readLongLE(src, 0);
    }

    protected static long readLongLE(@NonNull byte[] src, int index) {
        checkRangeLen(src.length, index, Long.BYTES);

        long val = PUnsafe.getLong(src, PUnsafe.ARRAY_BYTE_BASE_OFFSET + index);
        return PlatformInfo.IS_BIG_ENDIAN ? Long.reverseBytes(val) : val;
    }

    protected static byte[] writeLongLE(long val) {
        byte[] dst = new byte[Long.BYTES];
        writeLongLE(dst, 0, val);
        return dst;
    }

    protected static void writeLongLE(@NonNull byte[] dst, int index, long val) {
        checkRangeLen(dst.length, index, Long.BYTES);

        PUnsafe.putLong(dst, PUnsafe.ARRAY_BYTE_BASE_OFFSET + index, PlatformInfo.IS_BIG_ENDIAN ? Long.reverseBytes(val) : val);
    }

    protected final AbstractFarTileProvider<POS, T> world;

    protected final TransactionDB db;
    protected final List<ColumnFamilyHandle> handles;

    protected final ColumnFamilyHandle cfTileTimestamp;
    protected final ColumnFamilyHandle cfTileDirtyTimestamp;
    protected final ColumnFamilyHandle cfTileData;
    protected final ColumnFamilyHandle cfAnyVanillaExists;

    protected final Set<Listener<POS, T>> listeners = new CopyOnWriteArraySet<>();

    protected final int version;

    protected final LoadingCache<POS, ITileHandle<POS, T>> handleCache = CacheBuilder.newBuilder()
            .concurrencyLevel(FP2Config.global().performance().terrainThreads())
            .weakValues()
            .build(CacheLoader.from(pos -> new RocksTileHandle<>(pos, this)));

    @SneakyThrows(RocksDBException.class)
    public RocksStorage(@NonNull AbstractFarTileProvider<POS, T> world, @NonNull File storageRoot) {
        this.world = world;
        this.version = world.mode().storageVersion();

        File markerFile = new File(storageRoot, "v4");
        if (PFiles.checkDirectoryExists(storageRoot) && !PFiles.checkFileExists(markerFile)) { //it's an old storage
            PFiles.rmContentsParallel(storageRoot);
        }
        PFiles.ensureDirectoryExists(storageRoot);

        List<ColumnFamilyDescriptor> descriptors = Arrays.asList(
                new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_TILE_TIMESTAMP, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_TILE_DIRTY_TIMESTAMP, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_TILE_DATA, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_ANY_VANILLA_EXISTS, CF_OPTIONS));
        this.handles = new ArrayList<>(descriptors.size());

        this.db = TransactionDB.open(DB_OPTIONS, TX_DB_OPTIONS, storageRoot.getPath(), descriptors, this.handles);

        this.cfTileTimestamp = this.handles.get(1);
        this.cfTileDirtyTimestamp = this.handles.get(2);
        this.cfTileData = this.handles.get(3);
        this.cfAnyVanillaExists = this.handles.get(4);

        PFiles.ensureFileExists(markerFile); //create marker file
    }

    @Override
    public ITileHandle<POS, T> handleFor(@NonNull POS pos) {
        return this.handleCache.getUnchecked(pos);
    }

    @Override
    public void forEachDirtyPos(@NonNull Consumer<POS> callback) {
        IFarRenderMode<POS, T> mode = this.world.mode();

        try (RocksIterator itr = this.db.newIterator(this.cfTileDirtyTimestamp)) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                byte[] key = itr.key();
                callback.accept(mode.readPos(Unpooled.wrappedBuffer(key)));
            }
        }
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public Stream<POS> markAllDirty(@NonNull Stream<POS> positionsIn, long dirtyTimestamp) {
        //we'll buffer all the positions, lock all of them at once, compare each one and then commit as many as needed. the logic here is identical to
        //  RocksTileHandle#markDirty(long), but in bulk, and since RocksTileHandle doesn't cache anything internally, we don't need to get any instances
        //  of RocksTileHandle or do any additional synchronization.

        List<POS> positions = positionsIn.distinct().collect(Collectors.toList());
        int length = positions.size();

        if (length == 0) { //nothing to do!
            return Stream.empty();
        }

        try (Transaction txn = this.db.beginTransaction(WRITE_OPTIONS)) {
            //convert positions to key bytes
            byte[][] allKeyBytes = positions.stream().map(POS::toBytes).toArray(byte[][]::new);

            byte[][] get;
            {
                //double up the keys and column families to pass them to multiGetForUpdate
                int doubleLength = multiplyExact(length, 2);
                ColumnFamilyHandle[] handles = new ColumnFamilyHandle[doubleLength];
                byte[][] keys = new byte[doubleLength][];

                for (int i = 0; i < doubleLength; ) {
                    byte[] keyBytes = allKeyBytes[i >> 1];

                    handles[i] = this.cfTileTimestamp;
                    keys[i++] = keyBytes;
                    handles[i] = this.cfTileDirtyTimestamp;
                    keys[i++] = keyBytes;
                }

                //obtain an exclusive lock on both timestamp keys to ensure coherency
                get = txn.multiGetForUpdate(READ_OPTIONS, Arrays.asList(handles), keys);
            }

            //iterate through positions, updating the dirty timestamps as needed
            List<POS> out = new ArrayList<>(length);
            byte[] dirtyTimestampArray = new byte[Long.BYTES];

            for (int i = 0; i < length; i++) {
                byte[] timestampBytes = get[(i << 1) + 0];
                long timestamp = timestampBytes != null
                        ? readLongLE(timestampBytes) //timestamp for this tile exists, extract it from the byte array
                        : TIMESTAMP_BLANK;

                byte[] dirtyTimestampBytes = get[(i << 1) + 1];
                long existingDirtyTimestamp = dirtyTimestampBytes != null
                        ? readLongLE(dirtyTimestampBytes) //dirty timestamp for this tile exists, extract it from the byte array
                        : TIMESTAMP_BLANK;

                if (timestamp == TIMESTAMP_BLANK //the tile doesn't exist, so we can't mark it as dirty
                    || dirtyTimestamp <= timestamp || dirtyTimestamp <= existingDirtyTimestamp) { //the new dirty timestamp isn't newer than the existing one, so we can't replace it
                    //skip this position
                    continue;
                }

                //store new dirty timestamp in db
                writeLongLE(dirtyTimestampArray, 0, dirtyTimestamp);
                txn.put(this.cfTileDirtyTimestamp, allKeyBytes[i], dirtyTimestampArray);

                //save the position to return it as part of the result stream
                out.add(positions.get(i));
            }

            if (!out.isEmpty()) { //non-empty list indicates that at least some positions were modified, so we should commit the transaction
                txn.commit();

                this.listeners.forEach(listener -> listener.tilesDirty(out.stream()));
                return out.stream();
            } else { //no positions were modified...
                return Stream.empty();
            }
        }
    }

    /*@Override
    @SneakyThrows(RocksDBException.class)
    public void markVanillaRenderable(@NonNull Stream<POS> positionsIn) {
        List<POS> positions = positionsIn.distinct()
                .peek(pos -> checkArg(pos.level() == 0, "%s is not at level 0!", pos))
                .collect(Collectors.toList());
        int length = positions.size();

        if (length == 0) { //nothing to do!
            return;
        }

        try (Transaction txn = this.db.beginTransaction(WRITE_OPTIONS)) {
            {
                byte[][] keys = positions.stream().map(POS::toBytes).toArray(byte[][]::new);

                ColumnFamilyHandle[] handles = new ColumnFamilyHandle[length];
                Arrays.fill(handles, this.cfAnyVanillaExists);

                byte[][] get = txn.multiGetForUpdate(READ_OPTIONS, Arrays.asList(handles), keys);

                List<POS> oldPositions = positions;
                positions = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    if (get[i] == null) {
                        positions.add(oldPositions.get(i));
                        txn.put(this.cfAnyVanillaExists, keys[i], new byte[0]);
                    }
                }
            }

            for (int lvl = 1; lvl < MAX_LODS; lvl++) {
                positions = positions.stream().flatMap(this.world.scaler()::outputs).distinct().collect(Collectors.toList());
                length = positions.size();

                byte[][] keys = positions.stream().map(POS::toBytes).toArray(byte[][]::new);

                ColumnFamilyHandle[] handles = new ColumnFamilyHandle[length];
                Arrays.fill(handles, this.cfAnyVanillaExists);

                byte[][] get = txn.multiGetForUpdate(READ_OPTIONS, Arrays.asList(handles), keys);

                List<POS> oldPositions = positions;
                positions = new ArrayList<>(length);
                for (int i = 0; i < length; i++) {
                    if (get[i] == null) {
                        positions.add(oldPositions.get(i));
                        txn.put(this.cfAnyVanillaExists, keys[i], new byte[0]);
                    }
                }
            }
        }
    }*/

    @Override
    public void close() throws IOException {
        try {
            this.db.flush(FLUSH_OPTIONS, this.handles);
            this.handles.forEach(ColumnFamilyHandle::close); //close column families before db
            this.db.close();
        } catch (RocksDBException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void addListener(@NonNull Listener<POS, T> listener) {
        checkState(this.listeners.add(listener), "listener %s already added?!?", listener);
    }

    @Override
    public void removeListener(@NonNull Listener<POS, T> listener) {
        checkState(this.listeners.remove(listener), "listener %s not present?!?", listener);
    }
}
