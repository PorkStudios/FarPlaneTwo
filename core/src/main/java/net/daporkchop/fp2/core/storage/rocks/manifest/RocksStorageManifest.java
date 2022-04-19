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
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageIterator;
import net.daporkchop.fp2.api.storage.internal.access.FStorageReadAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageWriteAccess;
import net.daporkchop.fp2.core.storage.rocks.RocksStorageColumn;

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

    public RocksStorageManifest(@NonNull RocksStorageColumn column, @NonNull String inode, @NonNull FStorageAccess access) {
        super(column, inode, access);
    }

    @Override
    protected int version() {
        return 0;
    }

    @Override
    protected void initialize(@NonNull FStorageAccess access) throws FStorageException {
        //no-op
    }

    @Override
    protected void upgrade(int savedVersion, @NonNull FStorageAccess access) throws FStorageException {
        //no-op
    }

    //
    // accessor methods
    //

    @SneakyThrows(FStorageException.class)
    public void forEachColumnFamily(@NonNull FStorageReadAccess access, @NonNull BiConsumer<String, FStorageColumnHintsInternal> action) {
        String prefix = this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR;
        byte[] keyBase = prefix.getBytes(StandardCharsets.UTF_8);

        try (FStorageIterator itr = access.iterator(this.column, keyBase, increment(keyBase))) {
            for (itr.seekToFirst(); itr.isValid(); ) {
                //strip keyBase prefix, parse as UTF-8 and unescape
                byte[] key = itr.key();
                String columnFamilyName = unescape(new String(key, keyBase.length, key.length - keyBase.length, StandardCharsets.UTF_8));

                //run callback
                action.accept(columnFamilyName, readColumnHints(itr.value()));
            }
        }
    }

    @SneakyThrows(FStorageException.class)
    public String assignNewColumnFamilyName(@NonNull FStorageAccess access, @NonNull FStorageColumnHintsInternal hints) {
        String name;
        byte[] key;

        do {
            name = UUID.randomUUID().toString();
            key = (this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8);
        } while (access.get(this.column, key) != null); //if the name is already taken, keep trying until we get one which is unique

        access.put(this.column, key, writeColumnHints(hints));
        return name;
    }

    @SneakyThrows(FStorageException.class)
    public boolean containsColumnFamilyName(@NonNull FStorageReadAccess access, @NonNull String name) {
        return access.get(this.column,
                (this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8)) != null;
    }

    @SneakyThrows(FStorageException.class)
    public FStorageColumnHintsInternal getHintsForColumnFamily(@NonNull FStorageReadAccess access, @NonNull String name) {
        byte[] data = access.get(this.column,
                (this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8));

        checkArg(data != null, "column family '%s' doesn't exist!", name);
        return readColumnHints(data);
    }

    public boolean containsAllColumnFamilyNames(@NonNull FStorageReadAccess access, @NonNull Collection<String> c) {
        //i literally don't care that this isn't optimized
        return c.stream().distinct().allMatch(name -> this.containsColumnFamilyName(access, name));
    }

    @SneakyThrows(FStorageException.class)
    public void markColumnFamiliesForDeletion(@NonNull FStorageWriteAccess access, @NonNull Collection<String> columnFamilyNames) {
        for (String name : columnFamilyNames) {
            access.put(this.column, (this.inode + SEPARATOR + COLUMN_FAMILIES_PENDING_DELETION + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8), EMPTY_BYTE_ARRAY);
        }
    }

    @SneakyThrows(FStorageException.class)
    public boolean isAnyColumnFamilyPendingDeletion(@NonNull FStorageReadAccess access) {
        byte[] keyBase = (this.inode + SEPARATOR + COLUMN_FAMILIES_PENDING_DELETION + SEPARATOR).getBytes(StandardCharsets.UTF_8);

        try (FStorageIterator itr = access.iterator(this.column, keyBase, increment(keyBase))) {
            itr.seekToFirst();
            return itr.isValid(); //if there is a first element, then the deletion queue isn't empty
        }
    }

    @SneakyThrows(FStorageException.class)
    public void forEachColumnFamilyNamePendingDeletion(@NonNull FStorageReadAccess access, @NonNull Consumer<String> action) {
        byte[] keyBase = (this.inode + SEPARATOR + COLUMN_FAMILIES_PENDING_DELETION + SEPARATOR).getBytes(StandardCharsets.UTF_8);

        try (FStorageIterator itr = access.iterator(this.column, keyBase, increment(keyBase))) {
            for (itr.seekToFirst(); itr.isValid(); ) {
                //strip keyBase prefix, parse as UTF-8 and unescape
                byte[] key = itr.key();
                action.accept(unescape(new String(key, keyBase.length, key.length - keyBase.length, StandardCharsets.UTF_8)));
            }
        }
    }

    @SneakyThrows(FStorageException.class)
    public void removeColumnFamilyFromDeletionQueue(@NonNull FStorageAccess access, @NonNull String columnFamilyName) {
        byte[] allColumnFamiliesKey = (this.inode + SEPARATOR + ALL_COLUMN_FAMILIES + SEPARATOR + escape(columnFamilyName)).getBytes(StandardCharsets.UTF_8);
        byte[] columnFamiliesPendingDeletionKey = (this.inode + SEPARATOR + COLUMN_FAMILIES_PENDING_DELETION + SEPARATOR + escape(columnFamilyName)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.column, allColumnFamiliesKey) != null, "column family '%s' doesn't exist", columnFamilyName);
        checkState(access.get(this.column, columnFamiliesPendingDeletionKey) != null, "column family '%s' isn't marked for deletion", columnFamilyName);

        //remove column family from allColumnFamilies map
        access.delete(this.column, allColumnFamiliesKey);

        //remove column family from deletion queue
        access.delete(this.column, columnFamiliesPendingDeletionKey);
    }

    @SneakyThrows(FStorageException.class)
    public String allocateInode(@NonNull FStorageAccess access) {
        String inode;
        byte[] key;

        do {
            inode = UUID.randomUUID().toString();
            key = (this.inode + SEPARATOR + INODES + SEPARATOR + escape(inode)).getBytes(StandardCharsets.UTF_8);
        } while (access.get(this.column, key) != null); //if the name is already taken, keep trying until we get one which is unique

        access.put(this.column, key, EMPTY_BYTE_ARRAY);
        return inode;
    }

    @SneakyThrows(FStorageException.class)
    public void deleteInode(@NonNull FStorageAccess access, @NonNull String inode) {
        byte[] key = (this.inode + SEPARATOR + INODES + SEPARATOR + escape(inode)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.column, key) != null, "inode '%s' doesn't exist!", inode);
        access.delete(this.column, key);
    }
}
