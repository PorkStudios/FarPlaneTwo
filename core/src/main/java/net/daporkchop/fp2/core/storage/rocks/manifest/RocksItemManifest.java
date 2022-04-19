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
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageIterator;
import net.daporkchop.fp2.api.storage.internal.access.FStorageReadAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageWriteAccess;
import net.daporkchop.fp2.core.storage.rocks.RocksStorageColumn;

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

    public RocksItemManifest(@NonNull RocksStorageColumn column, @NonNull String inode, @NonNull FStorageAccess access) {
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
    public Map<String, String> snapshotColumnNamesToColumnFamilyNames(@NonNull FStorageReadAccess access) {
        byte[] keyBase = (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR).getBytes(StandardCharsets.UTF_8);

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        try (FStorageIterator itr = access.iterator(this.column, keyBase, increment(keyBase))) {
            for (itr.seekToFirst(); itr.isValid(); ) {
                //strip keyBase prefix, parse as UTF-8 and unescape
                byte[] key = itr.key();
                String columnName = unescape(new String(key, keyBase.length, key.length - keyBase.length, StandardCharsets.UTF_8)).intern();

                builder.put(columnName, new String(itr.value(), StandardCharsets.UTF_8));
            }
        }

        return builder.build();
    }

    @SneakyThrows(FStorageException.class)
    public boolean columnExistsWithName(@NonNull FStorageReadAccess access, @NonNull String columnName) {
        return access.get(this.column,
                (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR + escape(columnName)).getBytes(StandardCharsets.UTF_8)) != null;
    }

    @SneakyThrows(FStorageException.class)
    public Optional<String> getColumnFamilyNameForColumn(@NonNull FStorageReadAccess access, @NonNull String columnName) {
        return Optional.ofNullable(access.get(this.column,
                (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR + escape(columnName)).getBytes(StandardCharsets.UTF_8)))
                .map(arr -> new String(arr, StandardCharsets.UTF_8));
    }

    @SneakyThrows(FStorageException.class)
    public void addColumn(@NonNull FStorageAccess access, @NonNull String columnName, @NonNull String columnFamilyName) {
        byte[] key = (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR + escape(columnName)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.column, key) == null, "can't add column '%s' because another column with the same name already exists!", columnName);
        access.put(this.column, key, columnFamilyName.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows(FStorageException.class)
    public void removeColumnByName(@NonNull FStorageAccess access, @NonNull String columnName) {
        byte[] key = (this.inode + SEPARATOR + COLUMN_NAMES + SEPARATOR + escape(columnName)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.column, key) != null, "can't remove column '%s' because it doesn't exist!", columnName);
        access.delete(this.column, key);
    }

    @SneakyThrows(FStorageException.class)
    public Optional<byte[]> getToken(@NonNull FStorageReadAccess access) {
        return Optional.ofNullable(access.get(this.column, (this.inode + SEPARATOR + TOKEN).getBytes(StandardCharsets.UTF_8)));
    }

    @SneakyThrows(FStorageException.class)
    public void setToken(@NonNull FStorageWriteAccess access, @NonNull byte[] token) {
        access.put(this.column, (this.inode + SEPARATOR + TOKEN).getBytes(StandardCharsets.UTF_8), token);
    }

    @SneakyThrows(FStorageException.class)
    public void removeToken(@NonNull FStorageWriteAccess access) {
        access.delete(this.column, (this.inode + SEPARATOR + TOKEN).getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows(FStorageException.class)
    public boolean isInitialized(@NonNull FStorageReadAccess access) {
        return access.get(this.column,
                (this.inode + SEPARATOR + INITIALIZED).getBytes(StandardCharsets.UTF_8)) != null;
    }

    @SneakyThrows(FStorageException.class)
    public void markInitialized(@NonNull FStorageAccess access) {
        byte[] key = (this.inode + SEPARATOR + INITIALIZED).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.column, key) == null, "already initialized!");
        access.put(this.column, key, EMPTY_BYTE_ARRAY);
    }
}
