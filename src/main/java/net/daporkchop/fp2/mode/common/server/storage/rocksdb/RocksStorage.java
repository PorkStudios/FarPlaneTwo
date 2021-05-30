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
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.server.storage.IFarStorage;
import net.daporkchop.fp2.util.IReusablePersistent;
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
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public final class RocksStorage<POS extends IFarPos, V extends IReusablePersistent> implements IFarStorage<POS, V> {
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

    protected static final byte[] COLUMN_NAME_TILES = "tiles".getBytes(StandardCharsets.UTF_8);
    protected static final byte[] COLUMN_NAME_DIRTY = "dirty".getBytes(StandardCharsets.UTF_8);

    protected static void writeInterleavedBits(@NonNull ByteBuf dst, @NonNull int... coords) {
        for (int bit = 0; bit < coords.length * 32; ) {
            int b = 0;
            for (int i = 7; i >= 0; i--, bit++) {
                b |= ((coords[bit % coords.length] >>> (bit / coords.length)) & 1) << i;
            }
            dst.writeByte(b);
        }
    }

    //
    // rocksdb helper methods
    //

    @SneakyThrows(RocksDBException.class)
    protected static ByteBuf get(@NonNull RocksDB db, @NonNull ColumnFamilyHandle handle, @NonNull ByteBuf key) {
        ByteBuffer keyNioBuffer = key.nioBuffer();

        //pre-allocate 64KiB for initial buffer
        ByteBuf value = ByteBufAllocator.DEFAULT.directBuffer(1 << 16);

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

    protected final ColumnFamilyHandle cfTiles;
    protected final ColumnFamilyHandle cfDirty;

    protected final int version;

    @Getter
    protected final RocksDirtyTracker<POS> dirtyTracker;

    @SneakyThrows(RocksDBException.class)
    public RocksStorage(@NonNull IFarRenderMode<POS, ?> mode, @NonNull File storageRoot) {
        this.mode = mode;
        this.version = mode.storageVersion();

        File markerFile = new File(storageRoot, "v2");
        if (PFiles.checkDirectoryExists(storageRoot) && !PFiles.checkFileExists(markerFile)) { //it's an old storage
            PFiles.rmContentsParallel(storageRoot);
        }
        PFiles.ensureDirectoryExists(storageRoot);

        List<ColumnFamilyDescriptor> descriptors = Arrays.asList(
                new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_TILES, CF_OPTIONS),
                new ColumnFamilyDescriptor(COLUMN_NAME_DIRTY, CF_OPTIONS));
        this.handles = new ArrayList<>(descriptors.size());

        this.db = TransactionDB.open(DB_OPTIONS, TX_DB_OPTIONS, storageRoot.getPath(), descriptors, this.handles);

        this.cfTiles = this.handles.get(1);
        this.cfDirty = this.handles.get(2);

        PFiles.ensureFileExists(markerFile); //create marker file

        this.dirtyTracker = new RocksDirtyTracker<>(this);
    }

    @Override
    public Compressed<POS, V> load(@NonNull POS pos) {
        if (FP2_DEBUG && (FP2Config.debug.disableRead || FP2Config.debug.disablePersistence)) {
            return null;
        }

        //read from db
        ByteBuf key = ByteBufAllocator.DEFAULT.directBuffer();
        ByteBuf packed;
        try {
            key.writeInt(pos.level()); //encode position
            writeInterleavedBits(key, pos.coordinates());

            packed = get(this.db, this.cfTiles, key);
        } finally {
            key.release();
        }

        if (packed == null) { //the data doesn't exist on disk
            return null;
        }

        //unpack
        try {
            int packedVersion = readVarInt(packed);
            if (packedVersion == this.version) {
                return new Compressed<>(pos, packed);
            }
            return null;
        } finally {
            packed.release();
        }
    }

    @Override
    public void store(@NonNull POS pos, @NonNull Compressed<POS, V> value) {
        if (FP2_DEBUG && (FP2Config.debug.disableWrite || FP2Config.debug.disablePersistence)) {
            return;
        }

        ByteBuf key = ByteBufAllocator.DEFAULT.directBuffer();
        ByteBuf packed = ByteBufAllocator.DEFAULT.directBuffer();
        try {
            key.writeInt(pos.level()); //encode position
            writeInterleavedBits(key, pos.coordinates());

            //write value
            writeVarInt(packed, this.version); //prefix with version
            value.write(packed);

            //write to db
            put(this.db, this.cfTiles, key, packed);
        } finally {
            packed.release();
            key.release();
        }
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
