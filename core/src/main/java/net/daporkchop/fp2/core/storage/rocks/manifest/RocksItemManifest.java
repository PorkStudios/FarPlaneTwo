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

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksAccess;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksReadAccess;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksWriteAccess;
import net.daporkchop.fp2.core.storage.rocks.access.RocksConflictDetectionHint;
import net.daporkchop.fp2.core.storage.rocks.access.iterator.IRocksIterator;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class RocksItemManifest extends AbstractRocksManifest<RocksItemManifest> {
    private static final String COLUMN_NAMES = escape("column_names").intern();
    private static final String TOKEN = escape("token").intern();
    private static final String INITIALIZED = escape("initialized").intern();

    public RocksItemManifest(@NonNull ColumnFamilyHandle columnFamily, @NonNull String inode, @NonNull IRocksAccess access) {
        super(columnFamily, inode, access);
    }

    @Override
    protected int version() {
        return 0;
    }

    @Override
    protected void initialize(@NonNull IRocksAccess access) throws RocksDBException {
        //no-op
    }

    @Override
    protected void upgrade(int savedVersion, @NonNull IRocksAccess access) throws RocksDBException {
        //no-op
    }

    //
    // accessor methods
    //

    @SneakyThrows(RocksDBException.class)
    public Map<String, String> snapshotColumnNamesToColumnFamilyNames(@NonNull IRocksReadAccess access) {
        byte[] keyBase = (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR).getBytes(StandardCharsets.UTF_8);

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        try (IRocksIterator itr = access.iterator(this.columnFamily, keyBase, increment(keyBase))) {
            for (itr.seekToFirst(); itr.isValid(); ) {
                //strip keyBase prefix, parse as UTF-8 and unescape
                byte[] key = itr.key();
                String columnName = unescape(new String(key, keyBase.length, key.length - keyBase.length, StandardCharsets.UTF_8)).intern();

                builder.put(columnName, new String(itr.value(), StandardCharsets.UTF_8));
            }
        }

        return builder.build();
    }

    @SneakyThrows(RocksDBException.class)
    public boolean columnExistsWithName(@NonNull IRocksReadAccess access, @NonNull String columnName) {
        return access.get(this.columnFamily,
                (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR + escape(columnName)).getBytes(StandardCharsets.UTF_8),
                RocksConflictDetectionHint.SHARED) != null;
    }

    @SneakyThrows(RocksDBException.class)
    public Optional<String> getColumnFamilyNameForColumn(@NonNull IRocksReadAccess access, @NonNull String columnName) {
        return Optional.ofNullable(access.get(this.columnFamily,
                (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR + escape(columnName)).getBytes(StandardCharsets.UTF_8),
                RocksConflictDetectionHint.SHARED)).map(arr -> new String(arr, StandardCharsets.UTF_8));
    }

    @SneakyThrows(RocksDBException.class)
    public void addColumn(@NonNull IRocksAccess access, @NonNull String columnName, @NonNull String columnFamilyName) {
        byte[] key = (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR + escape(columnName)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.columnFamily, key) == null, "can't add column '%s' because another column with the same name already exists!", columnName);
        access.put(this.columnFamily, key, columnFamilyName.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows(RocksDBException.class)
    public void removeColumnByName(@NonNull IRocksAccess access, @NonNull String columnName) {
        byte[] key = (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR + escape(columnName)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.columnFamily, key) != null, "can't remove column '%s' because it doesn't exist!", columnName);
        access.delete(this.columnFamily, key);
    }

    @SneakyThrows(RocksDBException.class)
    public Optional<byte[]> getToken(@NonNull IRocksReadAccess access) {
        return Optional.ofNullable(access.get(this.columnFamily, (this.inode + SEPARATOR + TOKEN).getBytes(StandardCharsets.UTF_8)));
    }

    @SneakyThrows(RocksDBException.class)
    public void setToken(@NonNull IRocksWriteAccess access, @NonNull byte[] token) {
        access.put(this.columnFamily, (this.inode + SEPARATOR + TOKEN).getBytes(StandardCharsets.UTF_8), token);
    }

    @SneakyThrows(RocksDBException.class)
    public void removeToken(@NonNull IRocksWriteAccess access) {
        access.delete(this.columnFamily, (this.inode + SEPARATOR + TOKEN).getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows(RocksDBException.class)
    public boolean isInitialized(@NonNull IRocksReadAccess access) {
        return access.get(this.columnFamily,
                (this.inode + SEPARATOR + INITIALIZED).getBytes(StandardCharsets.UTF_8),
                RocksConflictDetectionHint.SHARED) != null;
    }

    @SneakyThrows(RocksDBException.class)
    public void markInitialized(@NonNull IRocksAccess access) {
        byte[] key = (this.inode + SEPARATOR + INITIALIZED).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.columnFamily, key) == null, "already initialized!");
        access.put(this.columnFamily, key, EMPTY_BYTE_ARRAY);
    }
}
