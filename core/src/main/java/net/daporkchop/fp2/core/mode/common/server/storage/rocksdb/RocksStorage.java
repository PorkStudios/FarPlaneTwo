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
 *
 */

package net.daporkchop.fp2.core.mode.common.server.storage.rocksdb;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.storage.IFarStorage;
import net.daporkchop.fp2.core.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.core.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.core.mode.api.tile.ITileSnapshot;
import net.daporkchop.fp2.core.mode.api.tile.TileSnapshot;
import net.daporkchop.fp2.core.mode.common.server.AbstractFarTileProvider;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.compression.context.PDeflater;
import net.daporkchop.lib.compression.context.PInflater;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.primitive.list.LongList;
import net.daporkchop.lib.primitive.list.array.LongArrayList;
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
import org.rocksdb.Transaction;
import org.rocksdb.TransactionDB;
import org.rocksdb.TransactionDBOptions;
import org.rocksdb.WriteOptions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.mode.api.tile.ITileMetadata.*;
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

    protected static byte[][] multiGetForUpdate(@NonNull Transaction txn, @NonNull ReadOptions readOptions, @NonNull List<ColumnFamilyHandle> handles, @NonNull byte[][] keys) throws RocksDBException {
        final int MAX_BATCH_SIZE = 65536;
        if (keys.length <= MAX_BATCH_SIZE) {
            return txn.multiGetForUpdate(readOptions, handles, keys);
        } else { //workaround for https://github.com/facebook/rocksdb/issues/9006: read results in increments of at most MAX_BATCH_SIZE at a time
            byte[][] result = new byte[keys.length][];

            for (int i = 0; i < keys.length; ) {
                int batchSize = min(keys.length - i, MAX_BATCH_SIZE);

                byte[][] tmp = txn.multiGetForUpdate(readOptions, handles.subList(i, i + batchSize), Arrays.copyOfRange(keys, i, i + batchSize));
                System.arraycopy(tmp, 0, result, i, batchSize);

                i += batchSize;
            }

            return result;
        }
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

    protected final Set<Listener<POS, T>> listeners = new CopyOnWriteArraySet<>();

    protected final int version;

    protected final LoadingCache<POS, ITileHandle<POS, T>> handleCache = CacheBuilder.newBuilder()
            .concurrencyLevel(fp2().globalConfig().performance().terrainThreads())
            .weakValues()
            .build(CacheLoader.from(pos -> new RocksTileHandle<>(pos, this)));

    @SneakyThrows({ IOException.class, RocksDBException.class })
    public RocksStorage(@NonNull AbstractFarTileProvider<POS, T> world, @NonNull Path storageRoot) {
        this.world = world;
        this.version = world.mode().storageVersion();

        byte[] token = this.token();

        Path markerFile = storageRoot.resolve("marker_v6");
        if (PFiles.checkDirectoryExists(storageRoot) && (!PFiles.checkFileExists(markerFile) || !Arrays.equals(token, this.decompress(Files.readAllBytes(markerFile))))) {
            //storage was created with a different registry
            PFiles.rmContentsParallel(storageRoot);
        }
        PFiles.ensureDirectoryExists(storageRoot);

        List<ColumnFamilyDescriptor> descriptors = Arrays.asList(
                new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_TILE_TIMESTAMP, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_TILE_DIRTY_TIMESTAMP, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_TILE_DATA, CF_OPTIONS));
        this.handles = new ArrayList<>(descriptors.size());

        this.db = TransactionDB.open(DB_OPTIONS, TX_DB_OPTIONS, storageRoot.toString(), descriptors, this.handles);

        this.cfTileTimestamp = this.handles.get(1);
        this.cfTileDirtyTimestamp = this.handles.get(2);
        this.cfTileData = this.handles.get(3);

        Files.write(markerFile, this.compress(token));
    }

    protected byte[] token() {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        try {
            buf.writeCharSequence(this.getClass().getTypeName(), StandardCharsets.UTF_8); //class name
            buf.writeByte(0);

            buf.writeIntLE(7); //storage format version
            buf.writeIntLE(this.version); //tile format version

            { //registry
                byte[] registryToken = this.world.world().fp2_IFarWorld_registry().registryToken();
                buf.writeIntLE(registryToken.length).writeBytes(registryToken);
            }

            //copy buffer contents to a byte[]
            byte[] arr = new byte[buf.readableBytes()];
            buf.readBytes(arr);
            return arr;
        } finally {
            buf.release();
        }
    }

    protected byte[] compress(@NonNull byte[] content) {
        ByteBuf compressed = ByteBufAllocator.DEFAULT.buffer(Zstd.PROVIDER.compressBound(content.length));
        try (PDeflater deflater = Zstd.PROVIDER.deflater()) {
            checkState(deflater.compress(Unpooled.wrappedBuffer(content), compressed), "failed to decompress data");

            //copy buffer contents to a byte[]
            byte[] arr = new byte[compressed.readableBytes()];
            compressed.readBytes(arr);
            return arr;
        } finally {
            compressed.release();
        }
    }

    protected byte[] decompress(@NonNull byte[] content) {
        byte[] decompressed = new byte[Zstd.PROVIDER.frameContentSize(Unpooled.wrappedBuffer(content))];
        try (PInflater inflater = Zstd.PROVIDER.inflater()) {
            checkState(inflater.decompress(Unpooled.wrappedBuffer(content), Unpooled.wrappedBuffer(decompressed).clear()));
        }
        return decompressed;
    }

    @Override
    public ITileHandle<POS, T> handleFor(@NonNull POS pos) {
        return this.handleCache.getUnchecked(pos);
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public List<ITileSnapshot<POS, T>> multiSnapshot(@NonNull List<POS> positions) {
        int length = positions.size();

        List<byte[]> valueBytes;
        {
            //prepare parameter arrays for MultiGet
            int doubleLength = multiplyExact(length, 2);
            ColumnFamilyHandle[] handles = new ColumnFamilyHandle[doubleLength];
            byte[][] keys = new byte[doubleLength][];

            for (int i = 0; i < doubleLength; ) {
                byte[] keyBytes = positions.get(i).toBytes();

                handles[i] = this.cfTileTimestamp;
                keys[i++] = keyBytes;
                handles[i] = this.cfTileDirtyTimestamp;
                keys[i++] = keyBytes;
            }

            //read all timestamps and values simultaneously
            valueBytes = this.db.multiGetAsList(READ_OPTIONS, Arrays.asList(handles), Arrays.asList(keys));
        }

        //construct snapshots
        List<ITileSnapshot<POS, T>> out = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            byte[] timestampBytes = valueBytes.get((i << 1) + 0);
            byte[] tileBytes = valueBytes.get((i << 1) + 1);

            out.add(timestampBytes != null
                    ? new TileSnapshot<>(positions.get(i), readLongLE(timestampBytes), tileBytes)
                    : null);
        }
        return out;
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public LongList multiTimestamp(@NonNull List<POS> positions) {
        int length = positions.size();

        //read all timestamps simultaneously
        List<byte[]> timestamps = this.db.multiGetAsList(READ_OPTIONS,
                Arrays.asList(PArrays.filled(length, ColumnFamilyHandle.class, this.cfTileTimestamp)),
                positions.stream().map(POS::toBytes).collect(Collectors.toList()));

        //deserialize timestamp values
        LongList out = new LongArrayList(length);
        timestamps.forEach(timestamp -> out.add(timestamp != null ? readLongLE(timestamp) : TIMESTAMP_BLANK));
        return out;
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public BitSet multiSet(@NonNull List<POS> positions, @NonNull List<ITileMetadata> metadatas, @NonNull List<T> tiles) {
        int length = positions.size();
        BitSet out = new BitSet(length);

        ByteBuf buf = ByteBufAllocator.DEFAULT.heapBuffer();
        try (Transaction txn = this.db.beginTransaction(WRITE_OPTIONS)) {
            //convert positions to key bytes
            byte[][] allKeyBytes = positions.stream().map(POS::toBytes).toArray(byte[][]::new);

            byte[][] get;
            {
                //prepare parameter arrays for MultiGet
                int doubleLength = multiplyExact(length, 2);
                ColumnFamilyHandle[] handles = new ColumnFamilyHandle[doubleLength];
                byte[][] keys = new byte[doubleLength][];

                for (int i = 0; i < doubleLength; ) {
                    byte[] keyBytes = positions.get(i).toBytes();

                    handles[i] = this.cfTileTimestamp;
                    keys[i++] = keyBytes;
                    handles[i] = this.cfTileDirtyTimestamp;
                    keys[i++] = keyBytes;
                }

                //obtain an exclusive lock on both timestamp keys to ensure coherency
                get = multiGetForUpdate(txn, READ_OPTIONS, Arrays.asList(handles), keys);
            }

            for (int i = 0; i < length; i++) {
                byte[] timestampBytes = get[(i << 1) + 0];
                long timestamp = timestampBytes != null
                        ? readLongLE(timestampBytes) //timestamp for this tile exists, extract it from the byte array
                        : TIMESTAMP_BLANK;

                byte[] dirtyTimestampBytes = get[(i << 1) + 1];
                long dirtyTimestamp = dirtyTimestampBytes != null
                        ? readLongLE(dirtyTimestampBytes) //dirty timestamp for this tile exists, extract it from the byte array
                        : TIMESTAMP_BLANK;

                byte[] keyBytes = allKeyBytes[i];
                ITileMetadata metadata = metadatas.get(i);
                T tile = tiles.get(i);

                if (metadata.timestamp() <= timestamp) { //the new timestamp isn't newer than the existing one, so we can't replace it
                    //skip this position
                    continue;
                }

                //the tile is about to be modified
                out.set(i);

                //store new timestamp in db
                txn.put(this.cfTileTimestamp, keyBytes, writeLongLE(metadata.timestamp()));

                //clear dirty timestamp if needed
                if (metadata.timestamp() >= dirtyTimestamp) {
                    txn.delete(this.cfTileDirtyTimestamp, keyBytes);
                }

                //encode tile and store it in db
                buf.clear();
                if (tile.write(buf)) { //the tile was empty, remove it from the db!
                    txn.delete(this.cfTileData, keyBytes);
                } else { //the tile was non-empty, store it in the db
                    txn.put(this.cfTileData, keyBytes, Arrays.copyOfRange(buf.array(), buf.arrayOffset(), buf.arrayOffset() + buf.writerIndex()));
                }
            }

            if (!out.isEmpty()) { //non-empty bitset indicates at least some positions were modified, so we should commit the transaction and notify listeners
                txn.commit();

                this.listeners.forEach(listener -> listener.tilesChanged(out.stream().mapToObj(positions::get)));
            }
        } finally {
            buf.release();
        }

        return out;
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public LongList multiDirtyTimestamp(@NonNull List<POS> positions) {
        int length = positions.size();

        //read all dirty timestamps simultaneously
        List<byte[]> dirtyTimestamps = this.db.multiGetAsList(READ_OPTIONS,
                Arrays.asList(PArrays.filled(length, ColumnFamilyHandle.class, this.cfTileDirtyTimestamp)),
                positions.stream().map(POS::toBytes).collect(Collectors.toList()));

        //deserialize dirty timestamp values
        LongList out = new LongArrayList(length);
        dirtyTimestamps.forEach(dirtyTimestamp -> out.add(dirtyTimestamp != null ? readLongLE(dirtyTimestamp) : TIMESTAMP_BLANK));
        return out;
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public BitSet multiMarkDirty(@NonNull List<POS> positions, long dirtyTimestamp) {
        //we'll lock all of the positions at once, compare each one and then commit as many as needed. the logic here is identical to
        //  RocksTileHandle#markDirty(long), but in bulk, and since RocksTileHandle doesn't cache anything internally, we don't need to get any instances
        //  of RocksTileHandle or do any additional synchronization.

        int length = positions.size();
        BitSet out = new BitSet(length);

        if (length == 0) { //nothing to do!
            return out;
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
                get = multiGetForUpdate(txn, READ_OPTIONS, Arrays.asList(handles), keys);
            }

            //iterate through positions, updating the dirty timestamps as needed
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
                out.set(i);
            }

            if (!out.isEmpty()) { //non-empty bitset indicates at least some positions were modified, so we should commit the transaction and notify listeners
                txn.commit();

                this.listeners.forEach(listener -> listener.tilesDirty(out.stream().mapToObj(positions::get)));
            }
        }

        return out;
    }

    @Override
    @SneakyThrows(RocksDBException.class)
    public BitSet multiClearDirty(@NonNull List<POS> positions) {
        int length = positions.size();
        BitSet out = new BitSet(length);

        try (Transaction txn = this.db.beginTransaction(WRITE_OPTIONS)) {
            //convert positions to key bytes
            byte[][] allKeyBytes = positions.stream().map(POS::toBytes).toArray(byte[][]::new);

            //read all dirty timestamps simultaneously
            byte[][] dirtyTimestamps = multiGetForUpdate(txn, READ_OPTIONS,
                    Arrays.asList(PArrays.filled(length, ColumnFamilyHandle.class, this.cfTileDirtyTimestamp)),
                    allKeyBytes);

            for (int i = 0; i < length; i++) {
                if (dirtyTimestamps[i] != null) { //the tile is dirty
                    out.set(i);

                    //clear dirty timestamp in db
                    txn.delete(this.cfTileDirtyTimestamp, allKeyBytes[i]);
                }
            }

            if (!out.isEmpty()) { //non-empty bitset indicates that at least one tile was modified, so we should commit the transaction
                txn.commit();
            }
        }

        return out;
    }

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
