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

import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageIterator;
import net.daporkchop.fp2.api.storage.internal.access.FStorageReadAccess;
import net.daporkchop.fp2.core.storage.rocks.RocksStorageColumn;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.BiConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class RocksCategoryManifest extends AbstractRocksManifest<RocksCategoryManifest> {
    private static final String CHILD_CATEGORIES = escape("categories").intern();
    private static final String CHILD_ITEMS = escape("items").intern();

    public RocksCategoryManifest(@NonNull RocksStorageColumn column, @NonNull String inode, @NonNull FStorageAccess access) {
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
    public void forEachChildCategory(@NonNull FStorageReadAccess access, @NonNull BiConsumer<String, String> action) {
        byte[] keyBase = (this.inode + SEPARATOR + CHILD_CATEGORIES + SEPARATOR).getBytes(StandardCharsets.UTF_8);

        try (FStorageIterator itr = access.iterator(this.column, keyBase, increment(keyBase))) {
            for (itr.seekToFirst(); itr.isValid(); ) {
                //strip keyBase prefix, parse as UTF-8 and unescape
                byte[] key = itr.key();
                String categoryName = unescape(new String(key, keyBase.length, key.length - keyBase.length, StandardCharsets.UTF_8)).intern();

                //invoke user function
                action.accept(categoryName, new String(itr.value(), StandardCharsets.UTF_8));
            }
        }
    }

    @SneakyThrows(FStorageException.class)
    public boolean hasCategory(@NonNull FStorageReadAccess access, @NonNull String name) {
        return access.get(this.column,
                (this.inode + SEPARATOR + CHILD_CATEGORIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8)) != null;
    }

    @SneakyThrows(FStorageException.class)
    public Optional<String> getCategoryInode(@NonNull FStorageReadAccess access, @NonNull String name) {
        return Optional.ofNullable(access.get(this.column,
                (this.inode + SEPARATOR + CHILD_CATEGORIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8))).map(arr -> new String(arr, StandardCharsets.UTF_8));
    }

    @SneakyThrows(FStorageException.class)
    public void addCategory(@NonNull FStorageAccess access, @NonNull String name, @NonNull String inode) {
        byte[] key = (this.inode + SEPARATOR + CHILD_CATEGORIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.column, key) == null, "category '%s' already exists", name);
        access.put(this.column, key, inode.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows(FStorageException.class)
    public void removeCategory(@NonNull FStorageAccess access, @NonNull String name) {
        byte[] key = (this.inode + SEPARATOR + CHILD_CATEGORIES + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.column, key) != null, "category '%s' doesn't exist", name);
        access.delete(this.column, key);
    }

    @SneakyThrows(FStorageException.class)
    public void forEachChildItem(@NonNull FStorageReadAccess access, @NonNull BiConsumer<String, String> action) {
        byte[] keyBase = (this.inode + SEPARATOR + CHILD_ITEMS + SEPARATOR).getBytes(StandardCharsets.UTF_8);

        try (FStorageIterator itr = access.iterator(this.column, keyBase, increment(keyBase))) {
            for (itr.seekToFirst(); itr.isValid(); ) {
                //strip keyBase prefix, parse as UTF-8 and unescape
                byte[] key = itr.key();
                String itemName = unescape(new String(key, keyBase.length, key.length - keyBase.length, StandardCharsets.UTF_8)).intern();

                //invoke user function
                action.accept(itemName, new String(itr.value(), StandardCharsets.UTF_8));
            }
        }
    }

    @SneakyThrows(FStorageException.class)
    public boolean hasItem(@NonNull FStorageReadAccess access, @NonNull String name) {
        return access.get(this.column,
                (this.inode + SEPARATOR + CHILD_ITEMS + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8)) != null;
    }

    @SneakyThrows(FStorageException.class)
    public Optional<String> getItemInode(@NonNull FStorageReadAccess access, @NonNull String name) {
        return Optional.ofNullable(access.get(this.column,
                (this.inode + SEPARATOR + CHILD_ITEMS + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8)))
                .map(arr -> new String(arr, StandardCharsets.UTF_8));
    }

    @SneakyThrows(FStorageException.class)
    public void addItem(@NonNull FStorageAccess access, @NonNull String name, @NonNull String inode) {
        byte[] key = (this.inode + SEPARATOR + CHILD_ITEMS + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.column, key) == null, "item '%s' already exists", name);
        access.put(this.column, key, inode.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows(FStorageException.class)
    public void removeItem(@NonNull FStorageAccess access, @NonNull String name) {
        byte[] key = (this.inode + SEPARATOR + CHILD_ITEMS + SEPARATOR + escape(name)).getBytes(StandardCharsets.UTF_8);

        checkState(access.get(this.column, key) != null, "item '%s' doesn't exist", name);
        access.delete(this.column, key);
    }
}
