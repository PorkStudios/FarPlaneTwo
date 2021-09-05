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
import com.google.common.collect.ImmutableList;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.storage.IFarStorage;
import net.daporkchop.fp2.mode.api.tile.ITileHandle;
import net.daporkchop.fp2.mode.api.tile.ITileMetadata;
import net.daporkchop.fp2.mode.api.tile.TileSnapshot;
import net.daporkchop.fp2.mode.common.tile.AbstractTileHandle;
import net.daporkchop.lib.common.misc.file.PFiles;
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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.daporkchop.fp2.debug.FP2Debug.*;

/**
 * @author DaPorkchop_
 */
public final class RocksStorage<POS extends IFarPos, T extends IFarTile> implements IFarStorage<POS, T> {
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
    protected static final byte[] COLUMN_NAME_TILE_DATA = "tile_data".getBytes(StandardCharsets.UTF_8);
    protected static final byte[] COLUMN_NAME_DIRTY = "dirty".getBytes(StandardCharsets.UTF_8);

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

    protected final IFarRenderMode<POS, ?> mode;

    protected final TransactionDB db;
    protected final List<ColumnFamilyHandle> handles;

    protected final ColumnFamilyHandle cfTileTimestamp;
    protected final ColumnFamilyHandle cfTileData;
    protected final ColumnFamilyHandle cfDirty;

    protected final int version;

    @Getter
    protected final RocksDirtyTracker<POS> dirtyTracker;

    protected final LoadingCache<POS, ITileHandle<POS, T>> handleCache = CacheBuilder.newBuilder()
            .concurrencyLevel(FP2Config.generationThreads)
            .weakValues()
            .build(CacheLoader.from(pos -> new RocksTileHandle<>(pos, this)));

    @SneakyThrows(RocksDBException.class)
    public RocksStorage(@NonNull IFarRenderMode<POS, ?> mode, @NonNull File storageRoot) {
        this.mode = mode;
        this.version = mode.storageVersion();

        File markerFile = new File(storageRoot, "v3");
        if (PFiles.checkDirectoryExists(storageRoot) && !PFiles.checkFileExists(markerFile)) { //it's an old storage
            PFiles.rmContentsParallel(storageRoot);
        }
        PFiles.ensureDirectoryExists(storageRoot);

        List<ColumnFamilyDescriptor> descriptors = Arrays.asList(
                new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_TILE_TIMESTAMP, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_TILE_DATA, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_DIRTY, CF_OPTIONS));
        this.handles = new ArrayList<>(descriptors.size());

        this.db = TransactionDB.open(DB_OPTIONS, TX_DB_OPTIONS, storageRoot.getPath(), descriptors, this.handles);

        this.cfTileTimestamp = this.handles.get(1);
        this.cfTileData = this.handles.get(2);
        this.cfDirty = this.handles.get(3);

        PFiles.ensureFileExists(markerFile); //create marker file

        this.dirtyTracker = new RocksDirtyTracker<>(this);
    }

    @Override
    public ITileHandle<POS, T> handleFor(@NonNull POS pos) {
        return this.handleCache.getUnchecked(pos);
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
}
