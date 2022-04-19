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

package net.daporkchop.fp2.core.storage.rocks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.storage.external.FStorageItem;
import net.daporkchop.fp2.api.storage.external.FStorageItemFactory;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksAccess;
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksCategoryManifest;
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksItemManifest;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.rocksdb.RocksDBException;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class RocksStorageCategory implements FStorageCategory {
    private final RocksStorage<?> storage;
    private final RocksCategoryManifest manifestData;

    private final Map<String, RocksStorageCategory> openCategories = new ConcurrentHashMap<>();
    private final Map<String, RocksStorageInternal> openItems = new ConcurrentHashMap<>();

    private volatile boolean open = true;

    public RocksStorageCategory(@NonNull RocksStorage<?> storage, @NonNull String inode, @NonNull IRocksAccess access) {
        this.storage = storage;
        this.manifestData = new RocksCategoryManifest(storage.defaultColumnFamily(), inode, access);
    }

    public void ensureOpen() {
        if (!this.open) {
            throw new IllegalStateException("category has been closed!");
        }
    }

    protected void doDeleteCategory(@NonNull IRocksAccess access, @NonNull String inode) {
        RocksCategoryManifest manifest = new RocksCategoryManifest(this.storage.defaultColumnFamily(), inode, access);

        //recursively delete all sub-categories
        manifest.forEachChildCategory(access, (categoryName, categoryInode) -> this.doDeleteCategory(access, categoryInode));

        //delete all sub-items
        manifest.forEachChildItem(access, (itemName, itemInode) -> this.doDeleteItem(access, itemInode));

        //clear the category's manifest data
        manifest.delete(access);
        this.storage.manifest().deleteInode(access, inode);
    }

    protected void doDeleteItem(@NonNull IRocksAccess access, @NonNull String inode) {
        RocksItemManifest manifest = new RocksItemManifest(this.storage.defaultColumnFamily(), inode, access);

        //delete all of the item's column families
        this.storage.deleteColumnFamilies(access, manifest.snapshotColumnNamesToColumnFamilyNames(access).values());

        //clear the item's manifest data
        manifest.delete(access);
        this.storage.manifest().deleteInode(access, inode);
    }

    protected void doClose() {
        this.ensureOpen();
        checkState(this.openCategories.isEmpty(), "cannot close category when some sub-categories are still open! %s", this.openCategories);
        checkState(this.openItems.isEmpty(), "cannot close category when some sub-items are still open! %s", this.openItems);

        this.open = false;
    }

    @Override
    public Set<String> allCategories() throws FStorageException {
        this.ensureOpen();

        //take snapshot of child categories data from manifest
        try {
            return this.storage.readGet(access -> {
                ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                this.manifestData.forEachChildCategory(access, (categoryName, inode) -> builder.add(categoryName));
                return builder.build();
            });
        } catch (RocksDBException e) {
            throw new FStorageException("failed to list child categories", e);
        }
    }

    @Override
    public Map<String, FStorageCategory> openCategories() {
        this.ensureOpen();

        //take snapshot of open categories map
        return ImmutableMap.copyOf(this.openCategories);
    }

    @Override
    public FStorageCategory openCategory(@NonNull String nameIn) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.ensureOpen();

        return this.openCategories.compute(nameIn, (name, category) -> {
            //ensure the category isn't already open
            checkState(category == null, "category '%s' is already open!", name);

            try {
                Optional<String> optionalInode = this.storage.readGet(access -> this.manifestData.getCategoryInode(access, name));

                //ensure the category exists
                if (!optionalInode.isPresent()) {
                    throw new NoSuchElementException("no category exists with name: " + name);
                }

                //open the category
                String inode = optionalInode.get();
                return this.storage.transactGet(access -> new RocksStorageCategory(this.storage, inode, access));
            } catch (RocksDBException e) {
                PUnsafe.throwException(new FStorageException("failed to open category", e)); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    @Override
    public FStorageCategory openOrCreateCategory(@NonNull String nameIn) throws FStorageException, IllegalStateException {
        this.ensureOpen();

        return this.openCategories.compute(nameIn, (name, category) -> {
            //ensure the category isn't already open
            checkState(category == null, "category '%s' is already open!", name);

            try {
                Optional<String> optionalInode = this.storage.readGet(access -> this.manifestData.getCategoryInode(access, name));

                //check if the category exists
                if (!optionalInode.isPresent()) {
                    //create a new category
                    optionalInode = Optional.of(this.storage.transactGet(access -> {
                        //allocate a new inode
                        String inode = this.storage.manifest().allocateInode(access);

                        //save the inode into this category's manifest data
                        this.manifestData.addCategory(access, name, inode);

                        return inode;
                    }));
                }

                //open the category
                String inode = optionalInode.get();
                return this.storage.transactGet(access -> new RocksStorageCategory(this.storage, inode, access));
            } catch (RocksDBException e) {
                PUnsafe.throwException(new FStorageException("failed to open category", e)); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    @Override
    public void closeCategory(@NonNull String nameIn) throws FStorageException, NoSuchElementException {
        this.ensureOpen();

        this.openCategories.compute(nameIn, (name, category) -> {
            //ensure the category is already open
            if (category == null) {
                throw new NoSuchElementException("no category is open with name: " + name);
            }

            //try to close the category
            category.doClose();

            //remove the category from this category
            return null;
        });
    }

    @Override
    public void deleteCategory(@NonNull String nameIn) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.ensureOpen();

        this.openCategories.compute(nameIn, (name, category) -> {
            //ensure the category isn't open
            checkState(category == null, "category '%s' is currently open!", name);

            try {
                this.storage.transactRun(access -> {
                    Optional<String> optionalInode = this.manifestData.getCategoryInode(access, name);

                    //ensure the category exists
                    if (!optionalInode.isPresent()) {
                        throw new NoSuchElementException("no category exists with name: " + name);
                    }

                    //remove the category from the manifest
                    this.manifestData.removeCategory(access, name);

                    //delete the category's contents
                    this.doDeleteCategory(access, optionalInode.get());
                });

                //return null because the category still isn't open
                return null;
            } catch (RocksDBException e) {
                PUnsafe.throwException(new FStorageException("failed to delete category", e)); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });

        //try to delete any column families that may be left
        try {
            this.storage.deleteQueuedColumnFamilies();
        } catch (RocksDBException e) {
            throw new FStorageException("failed to cleanup column families", e);
        }
    }

    @Override
    public Set<String> allItems() throws FStorageException {
        this.ensureOpen();

        //take snapshot of child items data from manifest
        try {
            return this.storage.readGet(access -> {
                ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                this.manifestData.forEachChildItem(access, (itemName, inode) -> builder.add(itemName));
                return builder.build();
            });
        } catch (RocksDBException e) {
            throw new FStorageException("failed to list child items", e);
        }
    }

    @Override
    public Map<String, ? extends FStorageItem> openItems() {
        this.ensureOpen();

        //take snapshot of open items map
        ImmutableMap.Builder<String, FStorageItem> builder = ImmutableMap.builder();
        this.openItems.forEach((name, storage) -> builder.put(name, storage.externalItem()));
        return builder.build();
    }

    @Override
    public <I extends FStorageItem> I openItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, NoSuchElementException, IllegalStateException {
        return this.openItem0(name, factory, false);
    }

    @Override
    public <I extends FStorageItem> I openOrCreateItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, IllegalStateException {
        return this.openItem0(name, factory, true);
    }

    protected <I extends FStorageItem> I openItem0(@NonNull String nameIn, @NonNull FStorageItemFactory<I> factory, boolean createIfMissing) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.ensureOpen();

        return uncheckedCast(this.openItems.compute(nameIn, (name, storage) -> {
            //ensure the item isn't already open
            checkState(storage == null, "item '%s' is already open!", name);

            try {
                storage = this.storage.transactGet(access -> {
                    Optional<String> optionalInode = this.manifestData.getItemInode(access, name);

                    //check if the item exists
                    if (!optionalInode.isPresent()) {
                        if (createIfMissing) { //create a new item
                            //allocate a new inode
                            String inode = this.storage.manifest().allocateInode(access);

                            //save the inode into this category's manifest data
                            this.manifestData.addItem(access, name, inode);

                            optionalInode = Optional.of(inode);
                        } else {
                            throw new NoSuchElementException("no item exists with name: " + name);
                        }
                    }

                    //actually create the item
                    return new RocksStorageInternal(this.storage, optionalInode.get(), factory, access);
                });

                //all the column family initialization has completed atomically, now we just need to acquire the column families and construct the user object 
                storage.open(factory);

                return storage;
            } catch (RocksDBException e) {
                PUnsafe.throwException(new FStorageException("failed to open item", e)); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        }).externalItem());
    }

    @Override
    public void closeItem(@NonNull String nameIn) throws FStorageException, NoSuchElementException {
        this.ensureOpen();

        this.openItems.compute(nameIn, (name, storage) -> {
            //ensure the item is already open
            if (storage == null) {
                throw new NoSuchElementException("no item is open with name: " + name);
            }

            try {
                //try to close the item
                storage.doClose();

                //remove the item from this category
                return null;
            } catch (FStorageException e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    @Override
    public void deleteItem(@NonNull String nameIn) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.ensureOpen();

        this.openItems.compute(nameIn, (name, storage) -> {
            //ensure the item isn't open
            checkState(storage == null, "item '%s' is currently open!", name);

            try {
                this.storage.transactRun(access -> {
                    Optional<String> optionalInode = this.manifestData.getItemInode(access, name);

                    //ensure the item exists
                    if (!optionalInode.isPresent()) {
                        throw new NoSuchElementException("no item exists with name: " + name);
                    }

                    //remove the item from the manifest
                    this.manifestData.removeItem(access, name);

                    //delete the item's contents
                    this.doDeleteItem(access, optionalInode.get());
                });

                //return null because the item still isn't open
                return null;
            } catch (RocksDBException e) {
                PUnsafe.throwException(new FStorageException("failed to delete item", e)); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });

        //try to delete any column families that may be left
        try {
            this.storage.deleteQueuedColumnFamilies();
        } catch (RocksDBException e) {
            throw new FStorageException("failed to cleanup column families", e);
        }
    }
}
