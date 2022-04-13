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
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksCategoryManifest;
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksStorageManifest;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
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

    private final Path manifestRoot;
    private final RocksCategoryManifest manifestData;

    private final Map<String, RocksStorageCategory> openCategories = new ConcurrentHashMap<>();
    private final Map<String, RocksStorageInternal> openItemsInternal = new ConcurrentHashMap<>();
    private final Map<String, ? extends FStorageItem> openItemsExternal = new ConcurrentHashMap<>();

    private volatile boolean open = true;

    public RocksStorageCategory(@NonNull RocksStorage<?> storage, @NonNull Path manifestRoot) throws FStorageException {
        this.storage = storage;
        this.manifestRoot = PFiles.ensureDirectoryExists(manifestRoot);

        this.manifestData = new RocksCategoryManifest(manifestRoot.resolve("m_manifest_v0")).load();
    }

    protected void doDelete() throws FStorageException { //must only be called when accessing this category externally
        //recursively delete all sub-categories
        for (String category : this.allCategories()) {
        }
    }

    protected void doClose() throws FStorageException {
        this.ensureOpen();
        checkState(this.openCategories.isEmpty(), "cannot close category when some sub-categories are still open! %s", this.openCategories);
        checkState(this.openItemsExternal.isEmpty(), "cannot close category when some sub-items are still open! %s", this.openItemsExternal);

        this.open = false;
    }

    @Override
    public Set<String> allCategories() {
        this.ensureOpen();

        //take snapshot of open child categories data from manifest
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.manifestData.forEachChildCategoryName(builder::add);
        return builder.build();
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

            //ensure the category exists
            if (!this.manifestData.hasCategory(name)) {
                throw new NoSuchElementException("no category exists with name: " + name);
            }

            try {
                //open the category
                return new RocksStorageCategory(this.storage, this.manifestRoot.resolve("c_" + name));
            } catch (FStorageException e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
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
                //check if the category exists
                if (!this.manifestData.hasCategory(name)) { //the category doesn't exist, create it
                    this.manifestData.update(manifest -> manifest.addCategory(name));
                }

                //open the category
                return new RocksStorageCategory(this.storage, this.manifestRoot.resolve("c_" + name));
            } catch (FStorageException e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    @Override
    public void closeCategory(@NonNull String nameIn) throws FStorageException, NoSuchElementException {
        this.ensureOpen();

        this.openCategories.compute(nameIn, (name, category) -> {
            //ensure the category is already open
            checkState(category != null, "category '%s' isn't open!", name);

            try {
                //try to close the category
                category.doClose();

                //remove the category from this category
                return null;
            } catch (FStorageException e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    @Override
    public void deleteCategory(@NonNull String nameIn) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.ensureOpen();

        this.openCategories.compute(nameIn, (name, category) -> {
            //ensure the category isn't open
            checkState(category == null, "category '%s' is currently open!", name);

            //ensure the category exists
            if (!this.manifestData.hasCategory(name)) {
                throw new NoSuchElementException("no category exists with name: " + name);
            }

            try {
                //remove the category from the manifest
                this.manifestData.update(manifest -> manifest.removeCategory(name));

                //recursively erase the category's contents
                PFiles.rm(this.manifestRoot.resolve("c_" + name));

                //return null because the category still isn't open
                return null;
            } catch (Exception e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    protected void recursiveDeleteCategory(@NonNull RocksStorageManifest.CategoryData data) {
        //delete all sub-categories
        data.categories().values().forEach(this::recursiveDeleteCategory);
        data.categories().clear();

        //delete all sub-items
        data.items().values().forEach(this::recursiveDeleteItem);
        data.items().clear();
    }

    @Override
    public Set<String> allItems() {
        this.storage.readLock().lock();
        try {
            //take snapshot of items set from manifest
            return ImmutableSet.copyOf(this.manifestData.items().keySet());
        } finally {
            this.storage.readLock().unlock();
        }
    }

    @Override
    public Map<String, ? extends FStorageItem> openItems() {
        this.storage.readLock().lock();
        try {
            this.ensureOpen();

            //take snapshot of open items map
            return ImmutableMap.copyOf(this.openItemsExternal);
        } finally {
            this.storage.readLock().unlock();
        }
    }

    @Override
    public <I extends FStorageItem> I openItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, NoSuchElementException, IllegalStateException {
        return this.openItem0(name, factory, false);
    }

    @Override
    public <I extends FStorageItem> I openOrCreateItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, IllegalStateException {
        return this.openItem0(name, factory, true);
    }

    protected <I extends FStorageItem> I openItem0(@NonNull String name, @NonNull FStorageItemFactory<I> factory, boolean createIfMissing) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.storage.writeLock().lock();
        try {
            this.ensureOpen();

            //ensure the item isn't already open
            checkState(!this.openItemsExternal.containsKey(name), "item '%s' is already open!", name);

            //ensure the item exists
            RocksStorageManifest.ItemData data = this.manifestData.items().get(name);

            @RequiredArgsConstructor
            class CallbackImpl implements FStorageItemFactory.ConfigurationCallback {
                final RocksStorageManifest.ItemData data;

                final Set<String> columnNames = new HashSet<>();
                final Map<String, FStorageColumnHintsInternal> columnHints = new ObjObjOpenHashMap<>();
                final Map<String, FStorageItemFactory.ColumnRequirement> columnRequirements = new ObjObjOpenHashMap<>();

                @Override
                public Optional<byte[]> getExistingToken() {
                    return this.data != null ? Optional.ofNullable(this.data.token()) : Optional.empty();
                }

                @Override
                public void registerColumn(@NonNull String name, @NonNull FStorageColumnHintsInternal hints, @NonNull FStorageItemFactory.ColumnRequirement requirement) {
                    checkArg(this.columnNames.add(name), "column '%s' already registered!", name);

                    this.columnHints.put(name, hints);
                    this.columnRequirements.put(name, requirement);
                }
            }

            //configure the item
            CallbackImpl callback = new CallbackImpl(data);
            FStorageItemFactory.ConfigurationResult result = factory.configure(callback);

            if (data == null //the item doesn't exist
                && !createIfMissing) { //create a new item
                //we aren't allowed to create the item, fail
                throw new NoSuchElementException("no item exists with name: " + name);
            }

            boolean hasTakenSnapshot = false;
            try {
                switch (result) {
                    case DELETE_EXISTING_AND_CREATE:
                        if (data != null) { //the item already exists
                            //we're about to modify the storage manifest, take a snapshot of it to allow rollbacks
                            this.storage.manifest().snapshot();
                            hasTakenSnapshot = true;

                            //delete the item data
                            // this will mark all the item's column families for deletion
                            this.recursiveDeleteItem(data);
                        }
                    case CREATE_IF_MISSING:
                        if (data == null) { //the item doesn't exist
                            //we checked above to see if (data == null && !createIfMissing), so if this code is run we know that createIfMissing is true

                            //we're about to modify the storage manifest, take a snapshot of it to allow rollbacks
                            this.storage.manifest().snapshot();
                            hasTakenSnapshot = true;

                            //create a new, empty storage item
                            data = new RocksStorageManifest.ItemData();
                            this.manifestData.items().put(name, data);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("unsupported configuration result: " + result);
                }

                //prepare manifest for creating the column families (and deleting any existing ones if necessary)
                for (String columnName : callback.columnNames) {
                    FStorageItemFactory.ColumnRequirement columnRequirement = callback.columnRequirements.get(columnName);
                    FStorageColumnHintsInternal columnHints = callback.columnHints.get(columnName);

                    String columnFamilyName = data.columnNamesToColumnFamilyNames().get(columnName);

                    switch (columnRequirement) {
                        case FAIL_IF_MISSING:
                            if (columnFamilyName == null) { //the column with the given name doesn't exist
                                throw new NoSuchElementException("the column '" + columnName + "' doesn't exist, but was requested with " + FStorageItemFactory.ColumnRequirement.FAIL_IF_MISSING);
                            }
                            break;
                        case DELETE_EXISTING_AND_CREATE:
                            if (columnFamilyName != null) { //the column with the given name exists
                                //we're about to modify the storage manifest, take a snapshot of it if we haven't already to allow rollbacks
                                if (!hasTakenSnapshot) {
                                    this.storage.manifest().snapshot();
                                    hasTakenSnapshot = true;
                                }

                                //mark the column family for deletion
                                this.storage.manifest().columnFamiliesPendingDeletion().add(columnFamilyName);

                                //remove the column family from the item's manifest
                                checkState(data.columnNamesToColumnFamilyNames().remove(columnName, columnFamilyName));
                                columnFamilyName = null;
                            }
                        case CREATE_IF_MISSING:
                            if (columnFamilyName == null) { //the column with the given name doesn't exist, create a new one
                                //we're about to modify the storage manifest, take a snapshot of it if we haven't already to allow rollbacks
                                if (!hasTakenSnapshot) {
                                    this.storage.manifest().snapshot();
                                    hasTakenSnapshot = true;
                                }

                                //allocate a new column family
                                columnFamilyName = this.storage.manifest().assignNewColumnFamilyName(columnHints);

                                //save the column family into the item
                                data.columnNamesToColumnFamilyNames().put(columnName, columnFamilyName);
                            } else { //the column with the given name exists
                                //check if the existing column family has the same usage hints, and modify them if the hints have changed
                                FStorageColumnHintsInternal existingHints = this.storage.manifest().allColumnFamilies().get(columnFamilyName);
                                if (!existingHints.equals(columnHints)) { //existing column family has different usage hints, replace them
                                    //we're about to modify the storage manifest, take a snapshot of it if we haven't already to allow rollbacks
                                    if (!hasTakenSnapshot) {
                                        this.storage.manifest().snapshot();
                                        hasTakenSnapshot = true;
                                    }

                                    this.storage.manifest().allColumnFamilies().put(columnFamilyName, columnHints);
                                }
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("unsupported column requirement: " + columnRequirement);
                    }
                }

                if (hasTakenSnapshot) { //the manifest has been changed, save it
                    this.storage.writeManifest();
                }
            } catch (Exception e) { //something failed, roll back the changes (if any) and rethrow exception
                if (hasTakenSnapshot) {
                    this.storage.manifest().rollback();
                }
                throw new FStorageException("failed to open item", e);
            } finally {
                if (hasTakenSnapshot) {
                    this.storage.manifest().clearSnapshot();
                }
            }

            //begin using (and create if necessary) all of the column families used by this item, then wrap into a RocksStorageInternal
            RocksStorageInternal storageInternal = new RocksStorageInternal(this.storage, data, this.storage.acquireColumnFamilies(data.columnNamesToColumnFamilyNames().values()));

            //try to construct item
            I item;
            try {
                item = factory.create(storageInternal);
            } catch (Exception e) { //failure, close the internal storage again to release the column families back to the storage
                storageInternal.close();
                throw new FStorageException("failed to open item", e);
            }

            //save the storage and item
            this.openItemsInternal.put(name, storageInternal);
            this.openItemsExternal.put(name, uncheckedCast(item));

            return item;
        } finally {
            this.storage.writeLock().unlock();
        }
    }

    @Override
    public void closeItem(@NonNull String name) throws FStorageException, NoSuchElementException {
        this.storage.writeLock().lock();
        try {
            this.ensureOpen();

            RocksStorageInternal storageInternal = this.openItemsInternal.get(name);
            if (storageInternal == null) { //the item wasn't open
                throw new NoSuchElementException("no item is open with name: " + name);
            }

            this.openItemsInternal.remove(name);
            FStorageItem item = this.openItemsExternal.remove(name);

            try {
                //notify the user code that the item is being closed
                item.closeInternal();
            } finally {
                //close the internal storage
                storageInternal.close();
            }
        } finally {
            this.storage.writeLock().unlock();
        }
    }

    @Override
    public void deleteItems(@NonNull Collection<String> names) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.storage.writeLock().lock();
        try {
            this.ensureOpen();

            //validate the names being removed
            names.forEach(name -> {
                //ensure the item isn't already open
                checkState(!this.openItemsExternal.containsKey(name), "item '%s' is already open!", name);

                //ensure the item exists
                if (!this.manifestData.items().containsKey(name)) {
                    throw new NoSuchElementException("no item exists with name: " + name);
                }
            });

            this.storage.manifest().snapshot();
            try {
                //remove the items from the manifest, then save the updated manifest
                names.forEach(name -> this.recursiveDeleteItem(this.manifestData.items().remove(name)));
                this.storage.writeManifest();
            } catch (Exception e) { //something failed, roll back the changes and rethrow exception
                this.storage.manifest().rollback();
                throw new FStorageException("failed to delete items", e);
            } finally {
                this.storage.manifest().clearSnapshot();
            }
        } finally {
            this.storage.writeLock().unlock();
        }
    }

    protected void recursiveDeleteItem(@NonNull RocksStorageManifest.ItemData data) {
        //mark all column families as pending deletion
        this.storage.manifest().columnFamiliesPendingDeletion().addAll(data.columnNamesToColumnFamilyNames().values());
        data.columnNamesToColumnFamilyNames().clear();

        //clear token
        data.token(null);
    }

    public void ensureOpen() {
        if (!this.open) {
            throw new IllegalStateException("category has been closed!");
        }
    }
}
