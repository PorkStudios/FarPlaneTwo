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

package net.daporkchop.fp2.core.storage.rocks.manifest;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksAccess;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksReadAccess;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksWriteAccess;
import net.daporkchop.fp2.core.storage.rocks.access.RocksConflictDetectionHint;
import net.daporkchop.fp2.core.storage.rocks.access.iterator.IRocksIterator;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class RocksStorageManifest extends AbstractRocksManifest<RocksStorageManifest> {
    private static final String ALL_COLUMN_FAMILIES = escape("all_column_families").intern();
    private static final String COLUMN_FAMILIES_PENDING_DELETION = escape("column_families_pending_deletion").intern();
    private static final String INODES = escape("inodes").intern();

    private static FStorageColumnHintsInternal readColumnHints(@NonNull byte[] arr) {
        ByteBuf buf = Unpooled.wrappedBuffer(arr);

        return FStorageColumnHintsInternal.builder()
                .expectedKeySize(buf.readIntLE())
                .compressability(FStorageColumnHintsInternal.Compressability.valueOf(buf.readCharSequence(buf.readIntLE(), StandardCharsets.UTF_8).toString()))
                .build();
    }

    private static byte[] writeColumnHints(@NonNull FStorageColumnHintsInternal hints) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        try {
            //serialize hints
            buf.writeIntLE(hints.expectedKeySize());
            buf.setIntLE(buf.writerIndex(), buf.writeInt(0).writeCharSequence(hints.compressability().name(), StandardCharsets.UTF_8));

            //copy ByteBuf to a byte[]
            byte[] out = new byte[buf.readableBytes()];
            buf.readBytes(out);
            return out;
        } finally {
            buf.release();
        }
    }

    public RocksStorageManifest(@NonNull ColumnFamilyHandle columnFamily, @NonNull String inode) {
        super(columnFamily, inode);
    }

    //
    // accessor methods
    //

    @SneakyThrows(RocksDBException.class)
    public void forEachColumnFamily(@NonNull IRocksReadAccess access, @NonNull BiConsumer<String, FStorageColumnHintsInternal> action) {
        String prefix = this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR;
        byte[] keyBase = prefix.getBytes(StandardCharsets.UTF_8);

        try (IRocksIterator itr = access.iterator(this.columnFamily, keyBase, increment(keyBase))) {
            for (itr.seekToFirst(); itr.isValid(); ) {
                //strip keyBase prefix, parse as UTF-8 and unescape
                byte[] key = itr.key();
                String columnFamilyName = unescape(new String(key, keyBase.length, key.length - keyBase.length, StandardCharsets.UTF_8));

                //run callback
                action.accept(columnFamilyName, readColumnHints(itr.value()));
            }
        }
    }

    @SneakyThrows(RocksDBException.class)
    public String assignNewColumnFamilyName(@NonNull IRocksAccess access, @NonNull FStorageColumnHintsInternal hints) {
        String name;
        byte[] key;

        do {
            name = UUID.randomUUID().toString();
            key = (this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8);
        } while (access.get(this.columnFamily, key, RocksConflictDetectionHint.SHARED) != null); //if the name is already taken, keep trying until we get one which is unique

        access.put(this.columnFamily, key, writeColumnHints(hints));
        return name;
    }

    @SneakyThrows(RocksDBException.class)
    public boolean containsColumnFamilyName(@NonNull IRocksReadAccess access, @NonNull String name) {
        return access.get(this.columnFamily,
                (this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8),
                RocksConflictDetectionHint.SHARED) != null;
    }

    @SneakyThrows(RocksDBException.class)
    public FStorageColumnHintsInternal getHintsForColumnFamily(@NonNull IRocksReadAccess access, @NonNull String name) {
        byte[] data = access.get(this.columnFamily,
                (this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8),
                RocksConflictDetectionHint.SHARED);

        checkArg(data != null, "column family '%s' doesn't exist!", name);
        return readColumnHints(data);
    }

    public boolean containsAllColumnFamilyNames(@NonNull IRocksReadAccess access, @NonNull Collection<String> c) {
        //i literally don't care that this isn't optimized
        return c.stream().distinct().allMatch(name -> this.containsColumnFamilyName(access, name));
    }

    @SneakyThrows(RocksDBException.class)
    public void markColumnFamiliesForDeletion(@NonNull IRocksWriteAccess access, @NonNull Collection<String> columnFamilyNames) {
        for (String name : columnFamilyNames) {
            access.put(this.columnFamily, (this.inode + SEPARATOR + COLUMN_FAMILIES_PENDING_DELETION + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8), EMPTY_BYTE_ARRAY);
        }
    }

    @SneakyThrows(RocksDBException.class)
    public boolean isAnyColumnFamilyPendingDeletion(@NonNull IRocksReadAccess access) {
        byte[] keyBase = (this.inode + SEPARATOR + COLUMN_FAMILIES_PENDING_DELETION + SEPARATOR).getBytes(StandardCharsets.UTF_8);

        try (IRocksIterator itr = access.iterator(this.columnFamily, keyBase, increment(keyBase))) {
            itr.seekToFirst();
            return itr.isValid(); //if there is a first element, then the deletion queue isn't empty
        }
    }

    @SneakyThrows(RocksDBException.class)
    public void forEachColumnFamilyNamePendingDeletion(@NonNull IRocksReadAccess access, @NonNull Consumer<String> action) {
        byte[] keyBase = (this.inode + SEPARATOR + COLUMN_FAMILIES_PENDING_DELETION + SEPARATOR).getBytes(StandardCharsets.UTF_8);

        try (IRocksIterator itr = access.iterator(this.columnFamily, keyBase, increment(keyBase))) {
            for (itr.seekToFirst(); itr.isValid(); ) {
                //strip keyBase prefix, parse as UTF-8 and unescape
                byte[] key = itr.key();
                action.accept(unescape(new String(key, keyBase.length, key.length - keyBase.length, StandardCharsets.UTF_8)));
            }
        }
    }

    @SneakyThrows(RocksDBException.class)
    public void removeColumnFamilyFromDeletionQueue(@NonNull IRocksAccess access, @NonNull String columnFamilyName) {
        byte[] allColumnFamiliesKey = (this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR + escape(columnFamilyName)).getBytes(StandardCharsets.UTF_8);
        byte[] columnFamiliesPendingDeletionKey = (this.inode + SEPARATOR + COLUMN_FAMILIES_PENDING_DELETION + SEPARATOR + escape(columnFamilyName)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.columnFamily, allColumnFamiliesKey) != null, "column family '%s' doesn't exist", columnFamilyName);
        checkState(access.get(this.columnFamily, columnFamiliesPendingDeletionKey) != null, "column family '%s' isn't marked for deletion", columnFamilyName);

        //remove column family from allColumnFamilies map
        access.delete(this.columnFamily, allColumnFamiliesKey);

        //remove column family from deletion queue
        access.delete(this.columnFamily, columnFamiliesPendingDeletionKey);
    }

    @SneakyThrows(RocksDBException.class)
    public String allocateInode(@NonNull IRocksAccess access) {
        String inode;
        byte[] key;

        do {
            inode = UUID.randomUUID().toString();
            key = (this.inode + SEPARATOR + INODES + SEPARATOR + escape(inode)).getBytes(StandardCharsets.UTF_8);
        } while (access.get(this.columnFamily, key, RocksConflictDetectionHint.SHARED) != null); //if the name is already taken, keep trying until we get one which is unique

        access.put(this.columnFamily, key, EMPTY_BYTE_ARRAY);
        return inode;
    }

    @SneakyThrows(RocksDBException.class)
    public void deleteInode(@NonNull IRocksAccess access, @NonNull String inode) {
        byte[] key = (this.inode + SEPARATOR + INODES + SEPARATOR + escape(inode)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.columnFamily, key) != null, "inode '%s' doesn't exist!", inode);
        access.delete(this.columnFamily, key);
    }
}
