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

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorage;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.storage.external.FStorageItem;
import net.daporkchop.fp2.api.storage.external.FStorageItemFactory;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksStorageManifest;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompressionOptions;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.FlushOptions;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.Transaction;
import org.rocksdb.TransactionDB;
import org.rocksdb.TransactionDBOptions;
import org.rocksdb.WriteOptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link FStorage} built on <a href="https://github.com/facebook/rocksdb">rocksdb</a>.
 *
 * @author DaPorkchop_
 */
public abstract class RocksStorage<DB extends RocksDB> implements FStorage {
    static {
        RocksDB.loadLibrary(); //ensure rocksdb is loaded (ForgeRocks does this automatically, but whatever)
    }

    protected static final ReadOptions READ_OPTIONS = new ReadOptions();
    protected static final WriteOptions WRITE_OPTIONS = new WriteOptions();
    protected static final FlushOptions FLUSH_OPTIONS = new FlushOptions().setWaitForFlush(true).setAllowWriteStall(true);

    private final Path root;

    @Getter
    private final DB db;

    private final DBOptions dbOptions = this.createDBOptions();
    private final Map<FStorageColumnHintsInternal, ColumnFamilyOptions> cachedColumnFamilyOptionsForHints = new ConcurrentHashMap<>();

    private final Map<String, ColumnFamilyHandle> openColumnFamilyHandles = new ConcurrentHashMap<>();
    private final Set<String> lockedColumnFamilyNames = ConcurrentHashMap.newKeySet();

    @Getter
    private final RocksStorageManifest manifest;

    private final RocksStorageCategory rootCategory;

    private volatile boolean open = true;

    protected RocksStorage(Path root) throws FStorageException {
        this.root = PFiles.ensureDirectoryExists(root);

        //try to load the manifest
        this.manifest = new RocksStorageManifest(this.root.resolve("manifest_v0")).load();

        //prepare column families for load
        List<String> cfNames = new ArrayList<>();
        List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();
        this.manifest.forEachColumnFamily((name, hints) -> {
            cfNames.add(name);
            cfDescriptors.add(new ColumnFamilyDescriptor(name.getBytes(StandardCharsets.UTF_8), this.getCachedColumnFamilyOptionsForHints(hints)));
        });

        //default column family must be registered
        cfNames.add(new String(RocksDB.DEFAULT_COLUMN_FAMILY).intern());
        cfDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));

        try {
            //open db
            List<ColumnFamilyHandle> cfHandles = new ArrayList<>();
            this.db = this.createDB(this.dbOptions, root.toString(), cfDescriptors, cfHandles);

            //save all the opened column family handles
            for (int i = 0; i < cfNames.size(); i++) {
                this.openColumnFamilyHandles.put(cfNames.get(i), cfHandles.get(i));
            }
        } catch (RocksDBException e) {
            throw new FStorageException("failed to open db", e);
        }

        try {
            //create root storage category
            this.rootCategory = new RocksStorageCategory(this, this.root.resolve("manifest"));

            //run cleanup on root category (this will recursively get a Set of all the column family names in use)
            Set<String> usedColumnFamilyNames = this.rootCategory.doCleanup();

            //make sure all of the used column families exist
            if (!this.openColumnFamilyHandles.keySet().containsAll(usedColumnFamilyNames)) {
                usedColumnFamilyNames.forEach(columnFamilyName -> checkState(this.openColumnFamilyHandles.containsKey(columnFamilyName), "column family '%s' doesn't exist!", columnFamilyName));
            }

            //delete all the column families which are unused
            Set<String> columnFamiliesForDeletion = new ObjectOpenHashSet<>(this.openColumnFamilyHandles.keySet());
            columnFamiliesForDeletion.removeAll(usedColumnFamilyNames);
            if (!columnFamiliesForDeletion.isEmpty()) { //there are some column families to delete
                this.deleteColumnFamilies(columnFamiliesForDeletion);
            }

            //no column families are used right now, so cleanup will delete every column family that was in the deletion queue
            this.deleteQueuedColumnFamilies();
        } catch (Exception e) {
            try { //try to close the db again
                this.close();
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }

            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        }
    }

    protected abstract DB createDB(DBOptions dbOptions, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException;

    public abstract Transaction beginTransaction(@NonNull WriteOptions writeOptions);

    protected DBOptions createDBOptions() {
        return new DBOptions()
                .setCreateIfMissing(true)
                .setCreateMissingColumnFamilies(true)
                .setAllowConcurrentMemtableWrite(true)
                .setKeepLogFileNum(1L);
    }

    protected void releaseDBOptions(@NonNull DBOptions options) {
        options.close();
    }

    protected ColumnFamilyOptions getCachedColumnFamilyOptionsForHints(@NonNull FStorageColumnHintsInternal hints) {
        return this.cachedColumnFamilyOptionsForHints.computeIfAbsent(hints, this::createColumnFamilyOptionsForHints);
    }

    protected ColumnFamilyOptions createColumnFamilyOptionsForHints(@NonNull FStorageColumnHintsInternal hints) {
        ColumnFamilyOptions options = new ColumnFamilyOptions();

        if (hints.compressability() != FStorageColumnHintsInternal.Compressability.NONE) {
            options.setCompressionType(CompressionType.ZSTD_COMPRESSION)
                    .setCompressionOptions(new CompressionOptions().setEnabled(true)
                            .setMaxDictBytes(64 << 10)
                            .setZStdMaxTrainBytes(64 << 10));
        }

        return options;
    }

    protected void releaseColumnFamilyOptions(@NonNull ColumnFamilyOptions options) {
        if (options.compressionOptions() != null) { //compression options are set, close them
            options.compressionOptions().close();
        }

        options.close();
    }

    protected void releaseOptions() {
        this.releaseDBOptions(this.dbOptions);
        this.cachedColumnFamilyOptionsForHints.values().forEach(this::releaseColumnFamilyOptions);
    }

    @Override
    public void close() throws FStorageException {
        try {
            this.ensureOpen();
            this.open = false;

            //close the root category
            this.rootCategory.doClose();

            try {
                //flush all column families
                this.db.flush(FLUSH_OPTIONS, new ArrayList<>(this.openColumnFamilyHandles.values()));

                //delete any column families whose deletion is pending
                this.deleteQueuedColumnFamilies();

                //close all column families, then the db itself
                this.openColumnFamilyHandles.values().forEach(ColumnFamilyHandle::close);
            } finally {
                this.db.closeE();
            }
        } catch (RocksDBException e) {
            throw new FStorageException("failed to close db", e);
        } finally {
            //release allocated Options instances
            this.releaseOptions();
        }
    }

    //
    // State management helpers
    //

    public void ensureOpen() {
        if (!this.open) {
            throw new IllegalStateException("storage has been closed!");
        }
    }

    public Map<String, String> modifyColumns(@NonNull Set<String> columnFamilyNamesToDelete, @NonNull Map<String, FStorageColumnHintsInternal> columnNamesAndHintsToCreate) throws FStorageException {
        this.ensureOpen();

        Map<String, String> out = new ObjObjOpenHashMap<>(columnNamesAndHintsToCreate.size());

        Collection<String> lockedColumnFamiliesToDelete = new ArrayList<>(columnFamilyNamesToDelete.size());
        try {
            //atomically acquire ownership of every column family being deleted
            for (String columnFamilyName : columnFamilyNamesToDelete) {
                checkState(this.lockedColumnFamilyNames.add(columnFamilyName), "cannot delete column family '%s' because it is currently used!", columnFamilyName);
                lockedColumnFamiliesToDelete.add(columnFamilyName);
            }
            lockedColumnFamiliesToDelete = columnFamilyNamesToDelete; //all of the column families have been acquired, replace with original input set to save memory lol

            //perform the operations atomically by grouping them all into a single manifest update
            this.manifest.update(manifest -> {
                if (!manifest.containsAllColumnFamilyNames(columnFamilyNamesToDelete)) { //some of the column family names don't exist
                    columnFamilyNamesToDelete.forEach(columnFamilyName -> checkArg(manifest.containsColumnFamilyName(columnFamilyName), "cannot delete column family '%s' because it doesn't exist", columnFamilyName));
                }

                //mark all of the column families for deletion
                manifest.markColumnFamiliesForDeletion(columnFamilyNamesToDelete);

                //assign column family names to each of the columns being created
                columnNamesAndHintsToCreate.forEach((columnName, hints) -> out.put(columnName, manifest.assignNewColumnFamilyName(hints)));
            });

            try {
                //create new column families for each of the newly assigned column family names
                List<String> columnFamilyNames = new ArrayList<>(columnNamesAndHintsToCreate.size());
                List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>(columnNamesAndHintsToCreate.size());

                //build column family descriptors
                columnNamesAndHintsToCreate.forEach((columnName, hints) -> {
                    String columnFamilyName = out.get(columnName);
                    columnFamilyNames.add(columnFamilyName);
                    columnFamilyDescriptors.add(new ColumnFamilyDescriptor(columnFamilyName.getBytes(StandardCharsets.UTF_8), this.getCachedColumnFamilyOptionsForHints(hints)));
                });

                //actually create the column families
                List<ColumnFamilyHandle> columnFamilyHandles = this.db.createColumnFamilies(columnFamilyDescriptors);

                //save new column family handles into the global map
                for (int i = 0; i < columnFamilyNames.size(); i++) {
                    this.openColumnFamilyHandles.put(columnFamilyNames.get(i), columnFamilyHandles.get(i));
                }
            } catch (RocksDBException e) { //technically, this should probably rollback everything if it fails, but that's so unlikely that i simply can't be bothered to care
                throw new FStorageException("failed to create new column families", e);
            }
        } finally {
            //unlock all of the column family names which have been acquired so far
            this.lockedColumnFamilyNames.removeAll(lockedColumnFamiliesToDelete);
        }

        return out;
    }

    public Map<String, ColumnFamilyHandle> acquireColumnFamilies(@NonNull Set<String> columnFamilyNames) throws FStorageException {
        this.ensureOpen();

        Collection<String> lockedColumnFamiliesToReleaseOnFailure = new ArrayList<>(columnFamilyNames.size());
        try {
            //atomically acquire ownership of every column family being acquired
            for (String columnFamilyName : columnFamilyNames) {
                checkState(this.lockedColumnFamilyNames.add(columnFamilyName), "cannot acquire column family '%s' because it is currently used!", columnFamilyName);
                lockedColumnFamiliesToReleaseOnFailure.add(columnFamilyName);
            }
            lockedColumnFamiliesToReleaseOnFailure = columnFamilyNames; //all of the column families have been acquired, replace with original input set to save memory lol

            if (!this.openColumnFamilyHandles.keySet().containsAll(columnFamilyNames)) { //some of the column families being acquired don't actually exist!
                columnFamilyNames.forEach(columnFamilyName -> checkState(this.openColumnFamilyHandles.containsKey(columnFamilyName), "cannot acquire column family '%s' because it doesn't exist!", columnFamilyName));
            }

            //retrieve columnFamilyHandle instances
            return columnFamilyNames.stream().collect(Collectors.toMap(Function.identity(), this.openColumnFamilyHandles::get));
        } catch (Exception e) {
            //unlock all of the column family names which have been acquired so far
            this.lockedColumnFamilyNames.removeAll(lockedColumnFamiliesToReleaseOnFailure);
            throw new FStorageException("failed to acquire column families", e);
        }
    }

    public void releaseColumnFamilies(@NonNull Set<String> columnFamilyNames) throws FStorageException {
        this.ensureOpen();

        Collection<String> releasedColumnFamilesToReacquireOnFailure = new ArrayList<>(columnFamilyNames.size());
        try {
            //atomically release ownership of every column family being released
            for (String columnFamilyName : columnFamilyNames) {
                checkState(this.lockedColumnFamilyNames.remove(columnFamilyName), "cannot release column family '%s' because it isn't being used!", columnFamilyName);
                releasedColumnFamilesToReacquireOnFailure.add(columnFamilyName);
            }
        } catch (Exception e) {
            //re-lock all of the column family names which have been released so far
            this.lockedColumnFamilyNames.addAll(releasedColumnFamilesToReacquireOnFailure);
            throw new FStorageException("failed to release column families", e);
        }
    }

    public void deleteColumnFamilies(@NonNull Set<String> columnFamilyNames) throws FStorageException {
        this.ensureOpen();

        Collection<String> lockedColumnFamiliesToReleaseOnFailure = new ArrayList<>(columnFamilyNames.size());
        try {
            //atomically acquire ownership of every column family being deleted
            for (String columnFamilyName : columnFamilyNames) {
                checkState(this.lockedColumnFamilyNames.add(columnFamilyName), "cannot acquire column family '%s' because it is currently used!", columnFamilyName);
                lockedColumnFamiliesToReleaseOnFailure.add(columnFamilyName);
            }
            lockedColumnFamiliesToReleaseOnFailure = columnFamilyNames; //all of the column families have been acquired, replace with original input set to save memory lol

            if (!this.openColumnFamilyHandles.keySet().containsAll(columnFamilyNames)) { //some of the column families being deleted don't actually exist!
                columnFamilyNames.forEach(columnFamilyName -> checkState(this.openColumnFamilyHandles.containsKey(columnFamilyName), "cannot delete column family '%s' because it doesn't exist!", columnFamilyName));
            }

            //mark all of the column families for deletion
            this.manifest.update(manifest -> manifest.markColumnFamiliesForDeletion(columnFamilyNames));
        } catch (Exception e) {
            //unlock all of the column family names which have been acquired so far
            this.lockedColumnFamilyNames.removeAll(lockedColumnFamiliesToReleaseOnFailure);
            throw new FStorageException("failed to delete column families", e);
        }

        //run column family cleanup
        this.deleteQueuedColumnFamilies();
    }

    protected void deleteQueuedColumnFamilies() throws FStorageException {
        if (!this.manifest.isAnyColumnFamilyPendingDeletion()) { //nothing to do
            return;
        }

        Set<String> columnFamilyNamesToDelete = new ObjectOpenHashSet<>();
        try {
            //atomically acquire a lock on as many column families queued for deletion as possible
            this.manifest.forEachColumnFamilyNamePendingDeletion(columnFamilyName -> {
                if (this.openColumnFamilyHandles.containsKey(columnFamilyName) //the column family is actually open (this should always be true, but sanity check just in case)
                    && this.lockedColumnFamilyNames.add(columnFamilyName)) { //we managed to acquire a lock on the column family
                    columnFamilyNamesToDelete.add(columnFamilyName);
                }
            });

            try {
                //get handles of all the column families we're about to delete and drop them
                this.db.dropColumnFamilies(columnFamilyNamesToDelete.stream()
                        .map(this.openColumnFamilyHandles::get)
                        .collect(Collectors.toList()));
            } catch (RocksDBException e) {
                throw new FStorageException("failed to drop column families", e);
            }

            //update manifest to show that the column families have been deleted
            this.manifest.update(manifest -> {
                columnFamilyNamesToDelete.forEach(manifest::removeColumnFamilyFromDeletionQueue);
            });
        } catch (Exception e) {
            //unlock all of the column family names which have been acquired so far
            this.lockedColumnFamilyNames.removeAll(columnFamilyNamesToDelete);
            throw new FStorageException("failed to cleanup column families", e);
        }
    }

    //
    // FStorageCategory methods
    // (these all delegate to this.rootCategory)
    //

    @Override
    public Set<String> allCategories() {
        return this.rootCategory.allCategories();
    }

    @Override
    public Map<String, FStorageCategory> openCategories() {
        return this.rootCategory.openCategories();
    }

    @Override
    public FStorageCategory openCategory(@NonNull String name) throws FStorageException, NoSuchElementException, IllegalStateException {
        return this.rootCategory.openCategory(name);
    }

    @Override
    public FStorageCategory openOrCreateCategory(@NonNull String name) throws FStorageException, IllegalStateException {
        return this.rootCategory.openOrCreateCategory(name);
    }

    @Override
    public void closeCategory(@NonNull String name) throws FStorageException, NoSuchElementException {
        this.rootCategory.closeCategory(name);
    }

    @Override
    public void deleteCategory(@NonNull String name) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.rootCategory.deleteCategory(name);
    }

    @Override
    public Set<String> allItems() {
        return this.rootCategory.allItems();
    }

    @Override
    public Map<String, ? extends FStorageItem> openItems() {
        return this.rootCategory.openItems();
    }

    @Override
    public <I extends FStorageItem> I openItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, NoSuchElementException, IllegalStateException {
        return this.rootCategory.openItem(name, factory);
    }

    @Override
    public <I extends FStorageItem> I openOrCreateItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, IllegalStateException {
        return this.rootCategory.openOrCreateItem(name, factory);
    }

    @Override
    public void closeItem(@NonNull String name) throws FStorageException, NoSuchElementException {
        this.rootCategory.closeItem(name);
    }

    @Override
    public void deleteItem(@NonNull String name) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.rootCategory.deleteItem(name);
    }

    /**
     * Implementation of {@link RocksStorage} backed by an ordinary {@link TransactionDB}.
     *
     * @author DaPorkchop_
     */
    protected static class TransactionRocksStorage extends RocksStorage<TransactionDB> {
        protected TransactionDBOptions transactionDBOptions;

        protected TransactionRocksStorage(Path root) throws FStorageException {
            super(root);
        }

        @Override
        protected TransactionDB createDB(DBOptions dbOptions, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
            if (this.transactionDBOptions == null) { //allocate transactionDBOptions if they haven't been yet
                this.transactionDBOptions = new TransactionDBOptions();
            }

            return TransactionDB.open(dbOptions, this.transactionDBOptions, path, columnFamilyDescriptors, columnFamilyHandles);
        }

        @Override
        public Transaction beginTransaction(@NonNull WriteOptions writeOptions) {
            return this.db().beginTransaction(writeOptions);
        }

        @Override
        protected void releaseDBOptions(@NonNull DBOptions options) {
            super.releaseDBOptions(options);

            if (this.transactionDBOptions != null) { //transactionDBOptions have been allocated, release them
                this.transactionDBOptions.close();
            }
        }
    }

    /**
     * Implementation of {@link RocksStorage} backed by an {@link OptimisticTransactionDB}.
     *
     * @author DaPorkchop_
     */
    protected static class OptimisticRocksStorage extends RocksStorage<OptimisticTransactionDB> {
        protected OptimisticRocksStorage(Path root) throws FStorageException {
            super(root);
        }

        @Override
        protected OptimisticTransactionDB createDB(DBOptions dbOptions, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
            return OptimisticTransactionDB.open(dbOptions, path, columnFamilyDescriptors, columnFamilyHandles);
        }

        @Override
        public Transaction beginTransaction(@NonNull WriteOptions writeOptions) {
            return this.db().beginTransaction(writeOptions);
        }
    }
}
