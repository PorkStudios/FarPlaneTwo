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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorage;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.storage.external.FStorageItem;
import net.daporkchop.fp2.api.storage.external.FStorageItemFactory;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksAccess;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksReadAccess;
import net.daporkchop.fp2.core.storage.rocks.access.IRocksWriteAccess;
import net.daporkchop.fp2.core.storage.rocks.access.RocksAccessDB;
import net.daporkchop.fp2.core.storage.rocks.access.RocksAccessReadMasqueradingAsReadWrite;
import net.daporkchop.fp2.core.storage.rocks.access.RocksAccessTransaction;
import net.daporkchop.fp2.core.storage.rocks.access.RocksAccessWriteBatch;
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksStorageManifest;
import net.daporkchop.lib.common.misc.file.PFiles;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompressionOptions;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.FlushOptions;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.OptimisticTransactionOptions;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.Transaction;
import org.rocksdb.TransactionDB;
import org.rocksdb.TransactionDBOptions;
import org.rocksdb.TransactionOptions;
import org.rocksdb.WriteOptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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

    public static final Options LIST_COLUMN_FAMILIES_OPTIONS = new Options().setCreateIfMissing(true);

    public static final ReadOptions READ_OPTIONS = new ReadOptions();
    public static final WriteOptions WRITE_OPTIONS = new WriteOptions();
    public static final FlushOptions FLUSH_OPTIONS = new FlushOptions().setWaitForFlush(true).setAllowWriteStall(true);

    protected static final String DEFAULT_COLUMN_FAMILY_NAME = new String(RocksDB.DEFAULT_COLUMN_FAMILY).intern();

    public static final Comparator<byte[]> LEX_BYTES_COMPARATOR = (a, b) -> {
        int diff;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (diff != 0) {
                return diff;
            }
        }
        // if array entries are equal till the first ends, then the
        // longer is "bigger"
        return a.length - b.length;
    };

    public static FStorage open(@NonNull Path root) throws FStorageException {
        return Boolean.getBoolean("fp2.core.storage.rocksdb.optimistic")
                ? new OptimisticRocksStorage(root)
                : new TransactionRocksStorage(root);
    }

    /**
     * Checks whether a {@link RocksDBException} is likely the result of a transaction commit failure.
     *
     * @param e the {@link RocksDBException}
     * @return whether or not the exception is likely the result of a transaction commit failure
     */
    public static boolean isTransactionCommitFailure(RocksDBException e) {
        switch (e.getStatus().getCode()) {
            case TimedOut:
            case Busy:
                return true;
            default:
                return false;
        }
    }

    private final Path root;

    @Getter
    private final DB db;

    private final DBOptions dbOptions = this.createDBOptions();
    private final Map<FStorageColumnHintsInternal, ColumnFamilyOptions> cachedColumnFamilyOptionsForHints = new ConcurrentHashMap<>();

    private final Map<String, ColumnFamilyHandle> openColumnFamilyHandles = new ConcurrentHashMap<>();
    private final Set<String> lockedColumnFamilyNames = ConcurrentHashMap.newKeySet();

    @Getter
    private final ColumnFamilyHandle defaultColumnFamily;

    @Getter
    private final RocksStorageManifest manifest;

    private final RocksStorageCategory rootCategory;

    private volatile boolean open = true;

    protected RocksStorage(Path root) throws FStorageException {
        this.root = PFiles.ensureDirectoryExists(root);

        //find the names of all the column families in the db so that we can open it
        List<byte[]> initialColumnFamilyNames;
        try {
            initialColumnFamilyNames = RocksDB.listColumnFamilies(LIST_COLUMN_FAMILIES_OPTIONS, root.toString());
        } catch (RocksDBException e) {
            throw new FStorageException("failed to list column families", e);
        }

        List<String> cfNames = new ArrayList<>();
        List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();

        //open database read-only so that we can access the root manifest data and determine what options to use for each column family
        List<ColumnFamilyDescriptor> initialColumnFamilyDescriptors = initialColumnFamilyNames.stream()
                .map(columnFamilyName -> new ColumnFamilyDescriptor(columnFamilyName, this.getCachedColumnFamilyOptionsForHints(FStorageColumnHintsInternal.DEFAULT)))
                .collect(Collectors.toList());
        List<ColumnFamilyHandle> initialColumnFamilyHandles = new ArrayList<>(initialColumnFamilyNames.size());
        try (RocksDB db = RocksDB.openReadOnly(root.toString(), initialColumnFamilyDescriptors, initialColumnFamilyHandles)) {
            IRocksAccess access = new RocksAccessReadMasqueradingAsReadWrite(new RocksAccessDB(db));

            //determine the real options to use for each column family based on the column hints
            new RocksStorageManifest(db.getDefaultColumnFamily(), "", access)
                    .forEachColumnFamily(access, (columnFamilyName, hints) -> {
                        cfNames.add(columnFamilyName);
                        cfDescriptors.add(new ColumnFamilyDescriptor(columnFamilyName.getBytes(StandardCharsets.UTF_8), this.getCachedColumnFamilyOptionsForHints(hints)));
                    });

            //close all the column family handles again before closing the db
            initialColumnFamilyHandles.forEach(ColumnFamilyHandle::close);
        } catch (RocksDBException e) {
            throw new FStorageException("failed to initially load manifest data", e);
        }

        //default column family must be registered
        cfNames.add(DEFAULT_COLUMN_FAMILY_NAME);
        cfDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, this.getCachedColumnFamilyOptionsForHints(FStorageColumnHintsInternal.DEFAULT)));

        try {
            //open db
            List<ColumnFamilyHandle> cfHandles = new ArrayList<>();
            this.db = this.openDB(this.dbOptions, root.toString(), cfDescriptors, cfHandles);

            //save all the opened column family handles
            for (int i = 0; i < cfNames.size(); i++) {
                this.openColumnFamilyHandles.put(cfNames.get(i), cfHandles.get(i));
            }

            //get the default column family handle
            this.defaultColumnFamily = this.db.getDefaultColumnFamily();
        } catch (RocksDBException e) {
            throw new FStorageException("failed to open db", e);
        }

        try {
            //open the storage manifest
            this.manifest = this.transactGet(access -> new RocksStorageManifest(this.db.getDefaultColumnFamily(), "", access));

            //create root storage category
            this.rootCategory = this.transactGet(access -> new RocksStorageCategory(this, "root", access));

            //run cleanup to delete any column families whose deletion was previously scheduled
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

    protected abstract DB openDB(DBOptions dbOptions, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException;

    /**
     * Performs a set of read operations.
     *
     * @param action a callback function which accepts a {@link IRocksReadAccess} with which the read actions should be made. Once the callback function returns, all of the write operations
     *               will be applied atomically. Note that the function may be called multiple times, so be careful if modifying any state outside of the database!
     */
    public void readRun(@NonNull Consumer<IRocksReadAccess> action) throws RocksDBException {
        action.accept(new RocksAccessDB(this.db));
    }

    /**
     * Performs a set of read operations.
     *
     * @param action a callback function which accepts a {@link IRocksReadAccess} with which the read actions should be made. Once the callback function returns, all of the write operations
     *               will be applied atomically. Note that the function may be called multiple times, so be careful if modifying any state outside of the database!
     * @return the callback function's return value
     */
    public <T> T readGet(@NonNull Function<IRocksReadAccess, T> action) throws RocksDBException {
        return action.apply(new RocksAccessDB(this.db));
    }

    /**
     * Atomically performs a set of write operations.
     *
     * @param action a callback function which accepts a {@link IRocksWriteAccess} to which the write actions should be made. Once the callback function returns, all of the write operations
     *               will be applied atomically. Note that the function may be called multiple times, so be careful if modifying any state outside of the database!
     */
    public void batchWrites(@NonNull Consumer<IRocksWriteAccess> action) throws RocksDBException {
        do {
            try (RocksAccessWriteBatch batch = new RocksAccessWriteBatch()) {
                //execute the callback with the created WriteBatch
                action.accept(batch);

                //execute the WriteBatch
                this.db.write(WRITE_OPTIONS, batch);
                return;
            } catch (RocksDBException e) {
                if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw e;
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    /**
     * Atomically performs a set of read and write operations.
     *
     * @param action a callback function which accepts a {@link IRocksAccess} with which the read/write actions should be made. Once the callback function returns, all of the write operations
     *               will be applied atomically. Note that the function may be called multiple times, so be careful if modifying any state outside of the database!
     */
    public void transactRun(@NonNull Consumer<IRocksAccess> action) throws RocksDBException {
        do {
            try (Transaction txn = this.beginTransaction(WRITE_OPTIONS, true);
                 RocksAccessTransaction access = new RocksAccessTransaction(txn)) {
                //execute the callback with the transaction
                action.accept(access);

                //try to commit the transaction
                txn.commit();
                return;
            } catch (RocksDBException e) {
                if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw e;
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    /**
     * Atomically performs a set of read and write operations.
     *
     * @param action a callback function which accepts a {@link IRocksAccess} with which the read/write actions should be made. Once the callback function returns, all of the write operations
     *               will be applied atomically. Note that the function may be called multiple times, so be careful if modifying any state outside of the database!
     * @return the callback function's return value
     */
    public <T> T transactGet(@NonNull Function<IRocksAccess, T> action) throws RocksDBException {
        do {
            try (Transaction txn = this.beginTransaction(WRITE_OPTIONS, true);
                 RocksAccessTransaction access = new RocksAccessTransaction(txn)) {
                //execute the callback with the transaction
                T result = action.apply(access);

                //try to commit the transaction
                txn.commit();
                return result;
            } catch (RocksDBException e) {
                if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw e;
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    protected abstract Transaction beginTransaction(@NonNull WriteOptions writeOptions, boolean setSnapshot);

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

    public Map<String, String> createColumnFamilies(@NonNull IRocksAccess access, @NonNull Map<String, FStorageColumnHintsInternal> columnNamesAndHintsToCreate) {
        this.ensureOpen();

        //noinspection UnstableApiUsage
        return columnNamesAndHintsToCreate.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
                entry -> this.manifest.assignNewColumnFamilyName(access, entry.getValue())));
    }

    public Map<String, ColumnFamilyHandle> acquireColumnFamilies(@NonNull Collection<String> columnFamilyNames) throws RocksDBException {
        this.ensureOpen();

        Collection<String> lockedColumnFamiliesToReleaseOnFailure = new ArrayList<>(columnFamilyNames.size());
        try {
            //atomically acquire ownership of every column family being acquired
            for (String columnFamilyName : columnFamilyNames) {
                checkState(this.lockedColumnFamilyNames.add(columnFamilyName), "cannot acquire column family '%s' because it is currently used!", columnFamilyName);
                lockedColumnFamiliesToReleaseOnFailure.add(columnFamilyName);
            }
            lockedColumnFamiliesToReleaseOnFailure = columnFamilyNames; //all of the column families have been acquired, replace with original input set to save memory lol

            if (!this.openColumnFamilyHandles.keySet().containsAll(columnFamilyNames)) { //some of the column families being acquired aren't open!
                this.readRun(access -> {
                    //first off, make sure that all of the column families actually exist...
                    if (!this.manifest.containsAllColumnFamilyNames(access, columnFamilyNames)) { //some of the column families being acquired don't actually *exist*...
                        columnFamilyNames.forEach(columnFamilyName -> checkState(this.manifest.containsColumnFamilyName(access, columnFamilyName), "cannot acquire column family '%s' because it doesn't exist!", columnFamilyName));
                    }

                    //figure out which column families we need to create
                    //create new column families for each of the newly assigned column family names
                    List<String> newColumnFamilyNames = new ArrayList<>(columnFamilyNames.size());
                    List<ColumnFamilyDescriptor> newColumnFamilyDescriptors = new ArrayList<>(columnFamilyNames.size());

                    //build column family descriptors for the column families which need to be opened
                    columnFamilyNames.forEach(columnFamilyName -> {
                        if (!this.openColumnFamilyHandles.containsKey(columnFamilyName)) {
                            newColumnFamilyNames.add(columnFamilyName);

                            FStorageColumnHintsInternal hints = this.manifest.getHintsForColumnFamily(access, columnFamilyName);
                            newColumnFamilyDescriptors.add(new ColumnFamilyDescriptor(columnFamilyName.getBytes(StandardCharsets.UTF_8), this.getCachedColumnFamilyOptionsForHints(hints)));
                        }
                    });

                    //actually create the column families
                    List<ColumnFamilyHandle> columnFamilyHandles;
                    try {
                        columnFamilyHandles = this.db.createColumnFamilies(newColumnFamilyDescriptors);
                    } catch (RocksDBException e) {
                        PUnsafe.throwException(e); //hack to throw RocksDBException from inside lambda
                        throw new AssertionError(); //impossible
                    }

                    //save new column family handles into the global map
                    for (int i = 0; i < newColumnFamilyNames.size(); i++) {
                        this.openColumnFamilyHandles.put(newColumnFamilyNames.get(i), columnFamilyHandles.get(i));
                    }
                });
            }

            //retrieve columnFamilyHandle instances
            return columnFamilyNames.stream().collect(Collectors.toMap(Function.identity(), this.openColumnFamilyHandles::get));
        } catch (Exception e) {
            //unlock all of the column family names which have been acquired so far
            this.lockedColumnFamilyNames.removeAll(lockedColumnFamiliesToReleaseOnFailure);

            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        }
    }

    public void releaseColumnFamilies(@NonNull Collection<String> columnFamilyNames) {
        this.ensureOpen();

        Collection<String> releasedColumnFamiliesToReacquireOnFailure = new ArrayList<>(columnFamilyNames.size());
        try {
            //atomically release ownership of every column family being released
            for (String columnFamilyName : columnFamilyNames) {
                checkState(this.lockedColumnFamilyNames.remove(columnFamilyName), "cannot release column family '%s' because it isn't being used!", columnFamilyName);
                releasedColumnFamiliesToReacquireOnFailure.add(columnFamilyName);
            }
        } catch (Exception e) {
            //re-lock all of the column family names which have been released so far
            this.lockedColumnFamilyNames.addAll(releasedColumnFamiliesToReacquireOnFailure);

            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        }
    }

    public void deleteColumnFamily(@NonNull IRocksAccess access, @NonNull String columnFamilyName) {
        this.deleteColumnFamilies(access, Collections.singleton(columnFamilyName));
    }

    public void deleteColumnFamilies(@NonNull IRocksAccess access, @NonNull Collection<String> columnFamilyNames) {
        this.ensureOpen();

        Collection<String> lockedColumnFamiliesToReleaseOnFailure = new ArrayList<>(columnFamilyNames.size());
        try {
            //atomically acquire ownership of every column family being deleted
            for (String columnFamilyName : columnFamilyNames) {
                checkState(this.lockedColumnFamilyNames.add(columnFamilyName), "cannot acquire column family '%s' because it is currently used!", columnFamilyName);
                lockedColumnFamiliesToReleaseOnFailure.add(columnFamilyName);
            }
            lockedColumnFamiliesToReleaseOnFailure = columnFamilyNames; //all of the column families have been acquired, replace with original input set to save memory lol

            if (!this.openColumnFamilyHandles.keySet().containsAll(columnFamilyNames) //some of the column families being acquired aren't open!
                && !this.manifest.containsAllColumnFamilyNames(access, columnFamilyNames)) { //some of the column families being deleted don't even exist!
                columnFamilyNames.forEach(columnFamilyName -> checkState(this.manifest.containsColumnFamilyName(access, columnFamilyName), "cannot delete column family '%s' because it doesn't exist!", columnFamilyName));
            }

            //mark all of the column families for deletion
            this.manifest.markColumnFamiliesForDeletion(access, columnFamilyNames);
        } catch (Exception e) {
            //unlock all of the column family names which have been acquired so far
            this.lockedColumnFamilyNames.removeAll(lockedColumnFamiliesToReleaseOnFailure);

            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        }
    }

    protected void deleteQueuedColumnFamilies() throws RocksDBException {
        if (!this.readGet(this.manifest::isAnyColumnFamilyPendingDeletion)) { //nothing to do
            return;
        }

        Set<String> columnFamilyNamesToDelete = new ObjectOpenHashSet<>();
        try {
            //atomically acquire a lock on as many column families queued for deletion as possible
            this.readRun(access -> this.manifest.forEachColumnFamilyNamePendingDeletion(access, columnFamilyName -> {
                if (this.lockedColumnFamilyNames.add(columnFamilyName)) { //we managed to acquire a lock on the column family
                    columnFamilyNamesToDelete.add(columnFamilyName);
                }
            }));

            //get handles of all the column families we're about to delete and drop them
            this.db.dropColumnFamilies(columnFamilyNamesToDelete.stream()
                    .map(this.openColumnFamilyHandles::get)
                    .filter(Objects::nonNull) //the column family might not be open if it was scheduled for deletion without ever being created (it exists solely in the manifest)
                    .collect(Collectors.toList()));

            //update manifest to show that the column families have been deleted
            this.transactRun(access -> columnFamilyNamesToDelete.forEach(columnFamilyName -> this.manifest.removeColumnFamilyFromDeletionQueue(access, columnFamilyName)));
        } catch (Exception e) {
            //unlock all of the column family names which have been acquired so far
            this.lockedColumnFamilyNames.removeAll(columnFamilyNamesToDelete);

            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        }
    }

    //
    // FStorageCategory methods
    // (these all delegate to this.rootCategory)
    //

    @Override
    public Set<String> allCategories() throws FStorageException {
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
    public Set<String> allItems() throws FStorageException {
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
        protected static final TransactionOptions TRANSACTION_OPTIONS_DEFAULT = new TransactionOptions();
        protected static final TransactionOptions TRANSACTION_OPTIONS_SET_SNAPSHOT = new TransactionOptions().setSetSnapshot(true);

        protected TransactionDBOptions transactionDBOptions;

        protected TransactionRocksStorage(Path root) throws FStorageException {
            super(root);
        }

        @Override
        protected TransactionDB openDB(DBOptions dbOptions, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
            if (this.transactionDBOptions == null) { //allocate transactionDBOptions if they haven't been yet
                this.transactionDBOptions = new TransactionDBOptions();
            }

            return TransactionDB.open(dbOptions, this.transactionDBOptions, path, columnFamilyDescriptors, columnFamilyHandles);
        }

        @Override
        public Transaction beginTransaction(@NonNull WriteOptions writeOptions, boolean setSnapshot) {
            return this.db().beginTransaction(writeOptions, setSnapshot ? TRANSACTION_OPTIONS_SET_SNAPSHOT : TRANSACTION_OPTIONS_DEFAULT);
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
        protected static final OptimisticTransactionOptions TRANSACTION_OPTIONS_DEFAULT = new OptimisticTransactionOptions();
        protected static final OptimisticTransactionOptions TRANSACTION_OPTIONS_SET_SNAPSHOT = new OptimisticTransactionOptions().setSetSnapshot(true);

        protected OptimisticRocksStorage(Path root) throws FStorageException {
            super(root);
        }

        @Override
        protected OptimisticTransactionDB openDB(DBOptions dbOptions, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
            return OptimisticTransactionDB.open(dbOptions, path, columnFamilyDescriptors, columnFamilyHandles);
        }

        @Override
        public Transaction beginTransaction(@NonNull WriteOptions writeOptions, boolean setSnapshot) {
            return this.db().beginTransaction(writeOptions, setSnapshot ? TRANSACTION_OPTIONS_SET_SNAPSHOT : TRANSACTION_OPTIONS_DEFAULT);
        }
    }
}
