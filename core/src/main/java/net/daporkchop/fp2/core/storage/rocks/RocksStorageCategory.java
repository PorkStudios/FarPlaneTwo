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
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksCategoryManifest;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

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
    private final Map<String, RocksStorageInternal> openItems = new ConcurrentHashMap<>();

    private volatile boolean open = true;

    public RocksStorageCategory(@NonNull RocksStorage<?> storage, @NonNull Path manifestRoot) throws FStorageException {
        this.storage = storage;
        this.manifestRoot = PFiles.ensureDirectoryExists(manifestRoot);

        this.manifestData = new RocksCategoryManifest(this.manifestFilePath()).load();
    }

    protected Path manifestFilePath() {
        return this.manifestRoot.resolve("m_manifest_v0");
    }

    protected Path categoryPath(String name) {
        return this.manifestRoot.resolve("c_" + name);
    }

    protected Path itemPath(String name) {
        return this.manifestRoot.resolve("i_" + name);
    }

    protected Set<String> doCleanup() throws FStorageException {
        this.ensureOpen();
        checkState(this.openCategories.isEmpty(), "cannot do cleanup when some sub-categories are open! %s", this.openCategories);
        checkState(this.openItems.isEmpty(), "cannot do cleanup when some sub-items are open! %s", this.openItems);

        try {
            //iterate over every path and make sure that there is a valid category/item which it belongs to. if not, delete it
            try (Stream<Path> stream = Files.list(this.manifestRoot)) {
                for (Iterator<Path> itr = stream.iterator(); itr.hasNext(); ) {
                    Path childPath = itr.next();

                    String pathName = childPath.getFileName().toString();
                    if (pathName.startsWith("c_")) { //child path represents a category
                        if (!this.manifestData.hasCategory(pathName.substring("c_".length()))) { //category doesn't exist in the manifest, delete it!
                            new RocksStorageCategory(this.storage, childPath).doDelete();
                        }
                    } else if (pathName.startsWith("i_")) { //child path represents an item
                        if (!this.manifestData.hasItem(pathName.substring("i_".length()))) { //item doesn't exist in the manifest, delete it!
                            new RocksStorageInternal(this.storage, childPath).doDelete();
                        }
                    } else if (this.manifestFilePath().equals(childPath)) { //child path is this category's manifest data
                        //no-op
                    } else { //what?
                        throw new IllegalStateException("don't know what to do with path: " + childPath);
                    }
                }
            }

            //iterate over every category/item registered in this category's manifest and run cleanup on it
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            this.manifestData.forEachChildCategoryName(categoryName -> {
                try {
                    builder.addAll(new RocksStorageCategory(this.storage, this.categoryPath(categoryName)).doCleanup());
                } catch (FStorageException e) {
                    PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                    throw new AssertionError(); //impossible
                }
            });
            this.manifestData.forEachChildItemName(itemName -> {
                try {
                    builder.addAll(new RocksStorageInternal(this.storage, this.itemPath(itemName)).doCleanup());
                } catch (FStorageException e) {
                    PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                    throw new AssertionError(); //impossible
                }
            });
            return builder.build();
        } catch (FStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FStorageException("failed to cleanup in " + this.manifestRoot, e);
        }
    }

    protected void doDelete() throws FStorageException {
        this.doClose();

        //recursively delete all sub-categories
        for (String categoryName : this.allCategories()) {
            new RocksStorageCategory(this.storage, this.categoryPath(categoryName)).doDelete();
        }

        //delete sub-items
        for (String itemName : this.allItems()) {
            new RocksStorageInternal(this.storage, this.itemPath(itemName)).doDelete();
        }

        //delete manifest root directory
        PFiles.rm(this.manifestRoot);
    }

    protected void doClose() throws FStorageException {
        this.ensureOpen();
        checkState(this.openCategories.isEmpty(), "cannot close category when some sub-categories are still open! %s", this.openCategories);
        checkState(this.openItems.isEmpty(), "cannot close category when some sub-items are still open! %s", this.openItems);

        this.open = false;
    }

    @Override
    public Set<String> allCategories() {
        this.ensureOpen();

        //take snapshot of child categories data from manifest
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
                return new RocksStorageCategory(this.storage, this.categoryPath(name));
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
                return new RocksStorageCategory(this.storage, this.categoryPath(name));
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
            if (category == null) {
                throw new NoSuchElementException("no category is open with name: " + name);
            }

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

                //now that the category has been removed from the manifest, it technically no longer exists. we'll delete it now, but even if deletion is interrupted it'll be able
                // to be resumed the next time cleanup is run.
                new RocksStorageCategory(this.storage, this.categoryPath(name)).doDelete();

                //return null because the category still isn't open
                return null;
            } catch (Exception e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    @Override
    public Set<String> allItems() {
        this.ensureOpen();

        //take snapshot of child items data from manifest
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        this.manifestData.forEachChildItemName(builder::add);
        return builder.build();
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

            //check if the item exists
            boolean exists = this.manifestData.hasItem(name);
            if (!createIfMissing && !exists) {
                throw new NoSuchElementException("no item exists with name: " + name);
            }

            try {
                //create the item
                storage = new RocksStorageInternal(this.storage, this.itemPath(name), factory);

                //add item to the manifest
                this.manifestData.update(manifest -> manifest.addItem(name));

                return storage;
            } catch (Exception e) { //something failed, try to rollback changes
                if (!exists) { //the item was newly created
                    //try to close and delete the item
                    try {
                        if (storage != null) { //storage could be non-null if the failure occured while updating the category manifest
                            storage.doClose();
                        }

                        new RocksStorageInternal(this.storage, this.itemPath(name)).doDelete();
                    } catch (Exception e1) {
                        e.addSuppressed(e1);
                    }
                }

                PUnsafe.throwException(e); //hack to throw Exception from inside of the lambda
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
            checkState(storage == null, "category '%s' is currently open!", name);

            //ensure the item exists
            if (!this.manifestData.hasItem(name)) {
                throw new NoSuchElementException("no item exists with name: " + name);
            }

            try {
                //remove the item from the manifest
                this.manifestData.update(manifest -> manifest.removeItem(name));

                //now that the item has been removed from the manifest, it technically no longer exists. we'll delete it now, but even if deletion is interrupted it'll be able
                // to be resumed the next time cleanup is run.
                new RocksStorageInternal(this.storage, this.itemPath(name)).doDelete();

                //return null because the item still isn't open
                return null;
            } catch (Exception e) {
                PUnsafe.throwException(e); //hack to throw FStorageException from inside of the lambda
                throw new AssertionError(); //impossible
            }
        });
    }

    public void ensureOpen() {
        if (!this.open) {
            throw new IllegalStateException("category has been closed!");
        }
    }
}
