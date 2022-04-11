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
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import org.rocksdb.ColumnFamilyHandle;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class RocksStorageCategory implements FStorageCategory {
    @NonNull
    private final RocksStorage<?> storage;

    @NonNull
    private final RocksStorageManifest.CategoryData manifestData;

    private final Map<String, FStorageCategory> openCategories = new ObjObjOpenHashMap<>();
    private final Map<String, ? extends FStorageItem> openItems = new ObjObjOpenHashMap<>();

    private volatile boolean open = true;

    @Override
    public void close() throws FStorageException {
        this.storage.writeLock().lock();
        try {
            this.ensureOpen();
            checkState(this.openCategories.isEmpty(), "cannot close category when some sub-categories are still open! %s", this.openCategories);
            checkState(this.openItems.isEmpty(), "cannot close category when some sub-items are still open! %s", this.openItems);

            this.close0();
        } finally {
            this.storage.writeLock().unlock();
        }
    }

    protected void close0() throws FStorageException {
        this.open = false;
    }

    @Override
    public Set<String> allCategories() {
        this.storage.readLock().lock();
        try {
            this.ensureOpen();

            //take snapshot of categories set from manifest
            return ImmutableSet.copyOf(this.manifestData.categories().keySet());
        } finally {
            this.storage.readLock().unlock();
        }
    }

    @Override
    public Map<String, FStorageCategory> openCategories() {
        this.storage.readLock().lock();
        try {
            this.ensureOpen();

            //take snapshot of open categories map
            return ImmutableMap.copyOf(this.openCategories);
        } finally {
            this.storage.readLock().unlock();
        }
    }

    @Override
    public FStorageCategory openCategory(@NonNull String name) throws FStorageException, NoSuchElementException {
        this.storage.writeLock().lock();
        try {
            this.ensureOpen();

            //ensure the category isn't already open
            checkState(!this.openCategories.containsKey(name), "category '%s' is already open!", name);

            //ensure the category exists
            RocksStorageManifest.CategoryData data = this.manifestData.categories().get(name);
            if (data == null) {
                throw new NoSuchElementException("no category exists with name: " + name);
            }

            //open the category
            RocksStorageCategory category = new Normal(this.storage, data, this);
            this.openCategories.put(name, category);
            return category;
        } finally {
            this.storage.writeLock().unlock();
        }
    }

    @Override
    public FStorageCategory openOrCreateCategory(@NonNull String name) throws FStorageException {
        this.storage.writeLock().lock();
        try {
            this.ensureOpen();

            //ensure the category isn't already open
            checkState(!this.openCategories.containsKey(name), "category '%s' is already open!", name);

            //check if the category exists
            RocksStorageManifest.CategoryData data = this.manifestData.categories().get(name);
            if (data == null) { //create a new category
                data = new RocksStorageManifest.CategoryData();

                this.storage.manifest().snapshot();
                try {
                    //add the category to the manifest, then save the updated manifest
                    this.manifestData.categories().put(name, data);
                    this.storage.writeManifest();
                } catch (Exception e) { //something failed, roll back the changes and rethrow exception
                    this.storage.manifest().rollback();
                    throw new FStorageException("failed to create category", e);
                } finally {
                    this.storage.manifest().clearSnapshot();
                }
            }

            //open the category
            RocksStorageCategory category = new Normal(this.storage, data, this);
            this.openCategories.put(name, category);
            return category;
        } finally {
            this.storage.writeLock().unlock();
        }
    }

    @Override
    public void deleteCategories(@NonNull Collection<String> names) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.storage.writeLock().lock();
        try {
            this.ensureOpen();

            //validate the names being removed
            names.forEach(name -> {
                //ensure the category isn't already open
                checkState(!this.openCategories.containsKey(name), "category '%s' is already open!", name);

                //ensure the category exists
                if (!this.manifestData.categories().containsKey(name)) {
                    throw new NoSuchElementException("no category exists with name: " + name);
                }
            });

            this.storage.manifest().snapshot();
            try {
                //remove the categories from the manifest, then save the updated manifest
                names.forEach(name -> this.recursiveDeleteCategory(this.manifestData.categories().remove(name)));
                this.storage.writeManifest();
            } catch (Exception e) { //something failed, roll back the changes and rethrow exception
                this.storage.manifest().rollback();
                throw new FStorageException("failed to delete categories", e);
            } finally {
                this.storage.manifest().clearSnapshot();
            }
        } finally {
            this.storage.writeLock().unlock();
        }
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
            return ImmutableMap.copyOf(this.openItems);
        } finally {
            this.storage.readLock().unlock();
        }
    }

    @Override
    public <I extends FStorageItem> I openItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, NoSuchElementException {
        return this.openItem0(name, factory, false);
    }

    @Override
    public <I extends FStorageItem> I openOrCreateItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException {
        return this.openItem0(name, factory, true);
    }

    protected <I extends FStorageItem> I openItem0(@NonNull String name, @NonNull FStorageItemFactory<I> factory, boolean createIfMissing) throws FStorageException, NoSuchElementException {
        this.storage.writeLock().lock();
        try {
            this.ensureOpen();

            //ensure the item isn't already open
            checkState(!this.openItems.containsKey(name), "item '%s' is already open!", name);

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
                boolean isNewItem = false;

                switch (result) {
                    case DELETE_EXISTING_AND_CREATE:
                        if (data != null) { //the item already exists
                            //we're about to modify the storage manifest, take a snapshot of it to allow rollbacks
                            this.storage.manifest().snapshot();
                            hasTakenSnapshot = true;

                            //delete the item data
                            // this will mark all the item's column families for deletion
                            this.recursiveDeleteItem(data);

                            //the item is effectively being created from scratch
                            isNewItem = true;
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

                            //remember that the item is new
                            isNewItem = true;
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

            //begin using (and create if necessary) all of the column families used by this item
            Map<String, ColumnFamilyHandle> columnFamilyHandles = this.storage.beginUsingColumnFamilies(data.columnNamesToColumnFamilyNames().values());

            //TODO: something
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
                checkState(!this.openItems.containsKey(name), "item '%s' is already open!", name);

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

    /**
     * The root storage category.
     *
     * @author DaPorkchop_
     */
    public static class Root extends RocksStorageCategory {
        public Root(@NonNull RocksStorage<?> storage, @NonNull RocksStorageManifest.CategoryData manifestData) {
            super(storage, manifestData);
        }
    }

    /**
     * An ordinary storage category.
     *
     * @author DaPorkchop_
     */
    public static class Normal extends RocksStorageCategory {
        private final RocksStorageCategory parent;

        public Normal(@NonNull RocksStorage<?> storage, @NonNull RocksStorageManifest.CategoryData manifestData, @NonNull RocksStorageCategory parent) {
            super(storage, manifestData);
            this.parent = parent;
        }

        @Override
        protected void close0() throws FStorageException {
            super.close0();

            //we hold a write lock, so we can modify the parent's openCategories map
            checkState(this.parent.openCategories.values().remove(this), "failed to remove self from parent!");
        }
    }
}
