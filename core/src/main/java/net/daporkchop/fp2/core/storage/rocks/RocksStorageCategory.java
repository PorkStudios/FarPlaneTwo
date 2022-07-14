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

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.storage.external.FStorageCategoryFactory;
import net.daporkchop.fp2.api.storage.external.FStorageItem;
import net.daporkchop.fp2.api.storage.external.FStorageItemFactory;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.core.storage.rocks.internal.RocksStorageInternal;
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksCategoryManifest;
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksItemManifest;
import net.daporkchop.lib.unsafe.PUnsafe;

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
@Getter
public class RocksStorageCategory implements FStorageCategory {
    private final RocksStorage<?> storage;
    private final RocksCategoryManifest manifestData;

    private final Map<String, RocksStorageCategory.AsChild> openCategories = new ConcurrentHashMap<>();
    private final Map<String, RocksStorageInternal> openItems = new ConcurrentHashMap<>();

    private volatile boolean open = true;

    @SuppressWarnings("OptionalAssignedToNull")
    public RocksStorageCategory(@NonNull RocksStorage<?> storage, @NonNull String inode, @NonNull FStorageAccess access, @NonNull FStorageCategoryFactory factory) {
        this.storage = storage;
        this.manifestData = new RocksCategoryManifest(storage.defaultColumn(), inode, access);

        @RequiredArgsConstructor
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        class CallbackImpl implements FStorageCategoryFactory.ConfigurationCallback {
            final Optional<byte[]> existingToken;
            Optional<byte[]> newToken;

            @Override
            public Optional<byte[]> getExistingToken() {
                return this.existingToken;
            }

            @Override
            public void setToken(@NonNull byte[] token) {
                this.newToken = Optional.of(token.clone());
            }

            @Override
            public void clearToken() {
                this.newToken = Optional.empty();
            }
        }

        //configure the category
        CallbackImpl callback = new CallbackImpl(this.manifestData.getToken(access));
        FStorageCategoryFactory.ConfigurationResult result = factory.configure(callback);

        switch (result) {
            case DELETE_EXISTING_AND_CREATE:
                if (this.manifestData.isInitialized(access)) { //the category already exists
                    //delete the category data without deleting the inode
                    this.doDeleteCategory(access, inode, false);

                    //if no new token is being set, preserve the old one
                    if (callback.newToken == null) {
                        callback.newToken = callback.existingToken;
                    }
                }
            case CREATE_IF_MISSING:
                //no-op
                break;
            default:
                throw new IllegalArgumentException("unsupported configuration result: " + result);
        }

        if (callback.newToken != null) { //the token is being updated
            if (callback.newToken.isPresent()) {
                this.manifestData.setToken(access, callback.newToken.get());
            } else {
                this.manifestData.removeToken(access);
            }
        }

        if (!this.manifestData.isInitialized(access)) {
            //the category has been fully initialized
            this.manifestData.markInitialized(access);
        }
    }

    public void ensureOpen() {
        if (!this.open) {
            throw new IllegalStateException("category has been closed!");
        }
    }

    protected void doDeleteCategory(@NonNull FStorageAccess access, @NonNull String inode, boolean deleteInode) {
        RocksCategoryManifest manifest = new RocksCategoryManifest(this.storage.defaultColumn(), inode, access);

        //recursively delete all sub-categories
        manifest.forEachChildCategory(access, (categoryName, categoryInode) -> this.doDeleteCategory(access, categoryInode, true));

        //delete all sub-items
        manifest.forEachChildItem(access, (itemName, itemInode) -> this.doDeleteItem(access, itemInode));

        if (deleteInode) {
            //delete the category's manifest inode and remove it from the inode allocation table
            manifest.delete(access);
            this.storage.manifest().deleteInode(access, inode);
        } else {
            //clear the category's manifest data
            manifest.clear(access);
        }
    }

    protected void doDeleteItem(@NonNull FStorageAccess access, @NonNull String inode) {
        RocksItemManifest manifest = new RocksItemManifest(this.storage.defaultColumn(), inode, access);

        //delete all of the item's column families
        this.storage.deleteColumnFamilies(access, manifest.snapshotColumnNamesToColumnFamilyNames(access).values());

        //clear the item's manifest data
        manifest.delete(access);
        this.storage.manifest().deleteInode(access, inode);
    }

    @Override
    public void close() throws FStorageException {
        this.ensureOpen();
        checkState(this.openCategories.isEmpty(), "cannot close category when some sub-categories are still open! %s", this.openCategories);
        checkState(this.openItems.isEmpty(), "cannot close category when some sub-items are still open! %s", this.openItems);

        this.open = false;
    }

    @Override
    public Optional<byte[]> getToken() throws FStorageException {
        this.ensureOpen();
        return this.storage.readGet(this.manifestData::getToken);
    }

    @Override
    public void setToken(@NonNull byte[] token) throws FStorageException {
        this.ensureOpen();
        this.storage.writeAtomicRun(access -> this.manifestData.setToken(access, token));
    }

    @Override
    public void removeToken() throws FStorageException {
        this.ensureOpen();
        this.storage.writeAtomicRun(this.manifestData::removeToken);
    }

    @Override
    public Set<String> allCategories() throws FStorageException {
        this.ensureOpen();

        //take snapshot of child categories data from manifest
        return this.storage.readGet(access -> {
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            this.manifestData.forEachChildCategory(access, (categoryName, inode) -> builder.add(categoryName));
            return builder.build();
        });
    }

    @Override
    public FStorageCategory openCategory(@NonNull String nameIn, @NonNull FStorageCategoryFactory factory) throws FStorageException, NoSuchElementException, IllegalStateException {
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
                return this.storage.transactAtomicGet(access -> new RocksStorageCategory.AsChild(this.storage, inode, access, factory, this, name));
            } catch (FStorageException e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    @Override
    public FStorageCategory openOrCreateCategory(@NonNull String nameIn, @NonNull FStorageCategoryFactory factory) throws FStorageException, IllegalStateException {
        this.ensureOpen();

        return this.openCategories.compute(nameIn, (name, category) -> {
            //ensure the category isn't already open
            checkState(category == null, "category '%s' is already open!", name);

            try {
                Optional<String> optionalInode = this.storage.readGet(access -> this.manifestData.getCategoryInode(access, name));

                //check if the category exists
                if (!optionalInode.isPresent()) {
                    //create a new category
                    optionalInode = Optional.of(this.storage.transactAtomicGet(access -> {
                        //allocate a new inode
                        String inode = this.storage.manifest().allocateInode(access);

                        //save the inode into this category's manifest data
                        this.manifestData.addCategory(access, name, inode);

                        return inode;
                    }));
                }

                //open the category
                String inode = optionalInode.get();
                return this.storage.transactAtomicGet(access -> new RocksStorageCategory.AsChild(this.storage, inode, access, factory, this, name));
            } catch (FStorageException e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    public void doCloseCategory(@NonNull RocksStorageCategory.AsChild categoryIn) throws NoSuchElementException {
        this.ensureOpen();

        this.openCategories.compute(categoryIn.nameInParent(), (name, category) -> {
            //ensure the category is already open
            if (category == null) {
                throw new NoSuchElementException("no category is open with name: " + name);
            }

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
                this.storage.transactAtomicRun(access -> {
                    Optional<String> optionalInode = this.manifestData.getCategoryInode(access, name);

                    //ensure the category exists
                    if (!optionalInode.isPresent()) {
                        throw new NoSuchElementException("no category exists with name: " + name);
                    }

                    //remove the category from the manifest
                    this.manifestData.removeCategory(access, name);

                    //delete the category's contents
                    this.doDeleteCategory(access, optionalInode.get(), true);
                });

                //return null because the category still isn't open
                return null;
            } catch (FStorageException e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });

        //try to delete any column families that may be left
        this.storage.deleteQueuedColumnFamilies();
    }

    @Override
    public Set<String> allItems() throws FStorageException {
        this.ensureOpen();

        //take snapshot of child items data from manifest
        return this.storage.readGet(access -> {
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            this.manifestData.forEachChildItem(access, (itemName, inode) -> builder.add(itemName));
            return builder.build();
        });
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
                storage = this.storage.transactAtomicGet(access -> {
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
                storage.open(factory, this, name);

                return storage;
            } catch (FStorageException e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        }).externalItem());
    }

    public void doCloseItem(@NonNull RocksStorageInternal item) throws NoSuchElementException {
        this.ensureOpen();

        this.openItems.compute(item.nameInParent(), (name, storage) -> {
            //ensure the item is already open
            if (storage == null) {
                throw new NoSuchElementException("no item is open with name: " + name);
            }

            //remove the item from this category
            return null;
        });
    }

    @Override
    public void deleteItem(@NonNull String nameIn) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.ensureOpen();

        this.openItems.compute(nameIn, (name, storage) -> {
            //ensure the item isn't open
            checkState(storage == null, "item '%s' is currently open!", name);

            try {
                this.storage.transactAtomicRun(access -> {
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
            } catch (FStorageException e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });

        //try to delete any column families that may be left
        this.storage.deleteQueuedColumnFamilies();
    }

    /**
     * A {@link RocksStorageCategory} which is a child of another {@link RocksStorageCategory}.l
     *
     * @author DaPorkchop_
     */
    @Getter
    public static class AsChild extends RocksStorageCategory {
        private final RocksStorageCategory parent;
        private final String nameInParent;

        public AsChild(@NonNull RocksStorage<?> storage, @NonNull String inode, @NonNull FStorageAccess access, @NonNull FStorageCategoryFactory factory, @NonNull RocksStorageCategory parent, @NonNull String nameInParent) {
            super(storage, inode, access, factory);

            this.parent = parent;
            this.nameInParent = nameInParent;
        }

        @Override
        public void close() throws FStorageException {
            super.close();

            if (this.parent != null) {
                this.parent.doCloseCategory(this);
            }
        }
    }
}
