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
 */

package net.daporkchop.fp2.core.storage.rocks;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorage;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.storage.external.FStorageCategoryFactory;
import net.daporkchop.fp2.api.storage.external.FStorageItem;
import net.daporkchop.fp2.api.storage.external.FStorageItemFactory;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageInternal;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageReadAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageWriteAccess;
import net.daporkchop.fp2.api.util.function.ThrowingConsumer;
import net.daporkchop.fp2.api.util.function.ThrowingFunction;
import net.daporkchop.fp2.core.storage.rocks.access.RocksAccessDB;
import net.daporkchop.fp2.core.storage.rocks.access.RocksAccessReadMasqueradingAsReadWrite;
import net.daporkchop.fp2.core.storage.rocks.access.RocksAccessTransaction;
import net.daporkchop.fp2.core.storage.rocks.access.RocksAccessWriteBatch;
import net.daporkchop.fp2.core.storage.rocks.access.RocksAccessWriteBatchWithIndexMasqueradingAsTransaction;
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
import org.rocksdb.Snapshot;
import org.rocksdb.Transaction;
import org.rocksdb.TransactionDB;
import org.rocksdb.TransactionDBOptions;
import org.rocksdb.TransactionOptions;
import org.rocksdb.WriteOptions;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.core.FP2Core.*;
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
        String mode = System.getProperty("fp2.core.storage.rocksdb.mode", "OptimisticTransaction");
        switch (mode) {
            case "Locking":
                return new LockingRocksStorage(root);
            case "OptimisticTransaction":
                return new OptimisticTransactionRocksStorage(root);
            case "PessimisticTransaction":
                return new PessimisticTransactionRocksStorage(root);
            default:
                throw new IllegalArgumentException("unknown or unsupported storage mode: '" + mode + '\'');
        }
    }

    /**
     * Internal API: Checks whether a {@link RocksDBException} is likely the result of a transaction commit failure.
     *
     * @param e the {@link RocksDBException}
     * @return whether the exception is likely the result of a transaction commit failure
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

    public static FStorageException wrapException(RocksDBException e) {
        return new FStorageExceptionWrappedRocksDBException(e);
    }

    public static byte[] toByteArrayView(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }

        if (buffer.hasArray() && buffer.arrayOffset() == 0) { //check if we can expose the buffer's array directly without any allocations
            byte[] array = buffer.array();
            if (buffer.position() == 0 && buffer.limit() == array.length) {
                return array;
            }
        }

        int oldPosition = buffer.position();
        byte[] array = PUnsafe.allocateUninitializedByteArray(buffer.remaining());
        buffer.get(array).position(oldPosition);
        return array;
    }

    private final Path root;

    @Getter
    private final DB db;

    private final DBOptions dbOptions = this.createDBOptions();
    private final Map<FStorageColumnHintsInternal, ColumnFamilyOptions> cachedColumnFamilyOptionsForHints = new ConcurrentHashMap<>();

    private final Map<String, ColumnFamilyHandle> openColumnFamilyHandles = new ConcurrentHashMap<>();
    private final Set<String> lockedColumnFamilyNames = ConcurrentHashMap.newKeySet();

    @Getter
    private final RocksStorageColumn defaultColumn;

    @Getter
    private final RocksStorageManifest manifest;

    private final RocksStorageCategory rootCategory;

    private volatile boolean open = true;

    protected RocksStorage(Path root) throws FStorageException {
        this.root = PFiles.ensureDirectoryExists(root);

        List<String> cfNames = new ArrayList<>();
        List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();

        if (PFiles.checkFileExists(root.resolve("CURRENT"))) { //the database exists!
            //find the names of all the column families in the db so that we can open it
            List<byte[]> initialColumnFamilyNames;
            try {
                initialColumnFamilyNames = RocksDB.listColumnFamilies(LIST_COLUMN_FAMILIES_OPTIONS, root.toString());
            } catch (RocksDBException e) {
                throw new FStorageException("failed to list column families", e);
            }

            //open database read-only so that we can access the root manifest data and determine what options to use for each column family
            List<ColumnFamilyDescriptor> initialColumnFamilyDescriptors = initialColumnFamilyNames.stream()
                    .map(columnFamilyName -> new ColumnFamilyDescriptor(columnFamilyName, this.getCachedColumnFamilyOptionsForHints(FStorageColumnHintsInternal.DEFAULT)))
                    .collect(Collectors.toList());
            List<ColumnFamilyHandle> initialColumnFamilyHandles = new ArrayList<>(initialColumnFamilyNames.size());
            try (RocksDB db = RocksDB.openReadOnly(root.toString(), initialColumnFamilyDescriptors, initialColumnFamilyHandles)) {
                FStorageAccess access = new RocksAccessReadMasqueradingAsReadWrite(new RocksAccessDB(db, READ_OPTIONS));

                //determine the real options to use for each column family based on the column hints
                new RocksStorageManifest(new RocksStorageColumn(db.getDefaultColumnFamily()), "", access)
                        .forEachColumnFamily(access, (columnFamilyName, hints) -> {
                            cfNames.add(columnFamilyName);
                            cfDescriptors.add(new ColumnFamilyDescriptor(columnFamilyName.getBytes(StandardCharsets.UTF_8), this.getCachedColumnFamilyOptionsForHints(hints)));
                        });

                //close all the column family handles again before closing the db
                initialColumnFamilyHandles.forEach(ColumnFamilyHandle::close);
            } catch (RocksDBException e) {
                throw new FStorageException("failed to initially load manifest data", e);
            }

            //check if any column families are present in the db but not yet in the list. this can happen when a column family is created, but creation fails before it is
            //  committed to the manifest.
            //TODO: i'm not sure if these are ever being deleted...
            Set<String> cfNamesAsSet = new HashSet<>(cfNames);
            initialColumnFamilyNames.forEach(initialColumnFamilyName -> {
                String cfName = new String(initialColumnFamilyName, StandardCharsets.UTF_8);

                if (!Arrays.equals(RocksDB.DEFAULT_COLUMN_FAMILY, initialColumnFamilyName) //we don't care about the default column family
                    && !cfNamesAsSet.contains(cfName)) { //the column family wasn't present in the manifest
                    fp2().log().alert("storage at '%s' contains unknown or invalid column family '%s' which was not present in manifest!", root, cfName);

                    cfNames.add(cfName);
                    cfDescriptors.add(new ColumnFamilyDescriptor(initialColumnFamilyName, this.getCachedColumnFamilyOptionsForHints(FStorageColumnHintsInternal.DEFAULT)));
                }
            });
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
            this.defaultColumn = new RocksStorageColumn(this.db().getDefaultColumnFamily());
        } catch (RocksDBException e) {
            throw new FStorageException("failed to open db", e);
        }

        try {
            //open the storage manifest
            this.manifest = this.transactAtomicGet(access -> new RocksStorageManifest(this.defaultColumn, "", access));

            //create root storage category
            this.rootCategory = this.transactAtomicGet(access -> new RocksStorageCategory(this, "root", access, FStorageCategoryFactory.createIfMissing()));

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
     * @see FStorageInternal#readRun(ThrowingConsumer)
     */
    public void readRun(@NonNull ThrowingConsumer<? super FStorageReadAccess, ? extends FStorageException> action) throws FStorageException {
        action.acceptThrowing(new RocksAccessDB(this.db(), READ_OPTIONS));
    }

    /**
     * @see FStorageInternal#readGet(ThrowingFunction)
     */
    public <R> R readGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
        return action.applyThrowing(new RocksAccessDB(this.db(), READ_OPTIONS));
    }

    /**
     * @see FStorageInternal#readGet(ThrowingFunction, ThrowingConsumer)
     */
    public <R> R readGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
        return action.applyThrowing(new RocksAccessDB(this.db(), READ_OPTIONS));
    }

    /**
     * @see FStorageInternal#readAtomicRun(ThrowingConsumer)
     */
    public void readAtomicRun(@NonNull ThrowingConsumer<? super FStorageReadAccess, ? extends FStorageException> action) throws FStorageException {
        try (Snapshot snapshot = Objects.requireNonNull(this.db().getSnapshot(), "create snapshot failed");
             ReadOptions readOptions = new ReadOptions(READ_OPTIONS).setSnapshot(snapshot)) {
            action.acceptThrowing(new RocksAccessDB(this.db(), readOptions));
        }
    }

    /**
     * @see FStorageInternal#readAtomicGet(ThrowingFunction)
     */
    public <R> R readAtomicGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
        try (Snapshot snapshot = Objects.requireNonNull(this.db().getSnapshot(), "create snapshot failed");
             ReadOptions readOptions = new ReadOptions(READ_OPTIONS).setSnapshot(snapshot)) {
            return action.applyThrowing(new RocksAccessDB(this.db(), readOptions));
        }
    }

    /**
     * @see FStorageInternal#readAtomicGet(ThrowingFunction, ThrowingConsumer)
     */
    public <R> R readAtomicGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
        R result = null;
        boolean resultSet = false; //to account for potential null results

        try (Snapshot snapshot = Objects.requireNonNull(this.db().getSnapshot(), "create snapshot failed");
             ReadOptions readOptions = new ReadOptions(READ_OPTIONS).setSnapshot(snapshot)) {
            result = action.applyThrowing(new RocksAccessDB(this.db(), readOptions));
            resultSet = true;
            return result;
        } catch (Throwable t) {
            if (resultSet) { //the result was set before the exception was thrown (the exception occurred while closing up the ReadOptions or Snapshot)
                try {
                    cleanup.acceptThrowing(result);
                } catch (Throwable t1) { //save exception for later
                    t.addSuppressed(t1);
                }
            }

            PUnsafe.throwException(t); //rethrow original exception
            throw new AssertionError(); //impossible
        }
    }

    /**
     * @see FStorageInternal#writeAtomicRun(ThrowingConsumer)
     */
    public void writeAtomicRun(@NonNull ThrowingConsumer<? super FStorageWriteAccess, ? extends FStorageException> action) throws FStorageException {
        do {
            try (RocksAccessWriteBatch batch = new RocksAccessWriteBatch()) {
                try {
                    //execute the callback with the created WriteBatch
                    action.acceptThrowing(batch);
                } catch (FStorageException e) {
                    if (e instanceof FStorageExceptionWrappedRocksDBException) {
                        throw ((FStorageExceptionWrappedRocksDBException) e).getCause();
                    }

                    throw new IllegalArgumentException("don't know what to do with " + e.getClass().getTypeName(), e);
                }

                //execute the WriteBatch
                this.db().write(WRITE_OPTIONS, batch);
                return;
            } catch (RocksDBException e) {
                if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw wrapException(e);
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    /**
     * @see FStorageInternal#writeAtomicGet(ThrowingFunction)
     */
    public <R> R writeAtomicGet(@NonNull ThrowingFunction<? super FStorageWriteAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
        do {
            try (RocksAccessWriteBatch batch = new RocksAccessWriteBatch()) {
                R result;
                try {
                    //execute the callback with the created WriteBatch
                    result = action.applyThrowing(batch);
                } catch (FStorageException e) {
                    if (e instanceof FStorageExceptionWrappedRocksDBException) {
                        throw ((FStorageExceptionWrappedRocksDBException) e).getCause();
                    }

                    throw new IllegalArgumentException("don't know what to do with " + e.getClass().getTypeName(), e);
                }

                //execute the WriteBatch
                this.db().write(WRITE_OPTIONS, batch);
                return result;
            } catch (RocksDBException e) {
                if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw wrapException(e);
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    /**
     * @see FStorageInternal#writeAtomicGet(ThrowingFunction, ThrowingConsumer)
     */
    public <R> R writeAtomicGet(@NonNull ThrowingFunction<? super FStorageWriteAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
        do {
            R result = null;
            boolean resultSet = false; //to account for potential null results

            try (RocksAccessWriteBatch batch = new RocksAccessWriteBatch()) {
                try {
                    //execute the callback with the created WriteBatch
                    result = action.applyThrowing(batch);
                    resultSet = true;
                } catch (FStorageException e) {
                    if (e instanceof FStorageExceptionWrappedRocksDBException) {
                        throw ((FStorageExceptionWrappedRocksDBException) e).getCause();
                    }

                    throw new IllegalArgumentException("don't know what to do with " + e.getClass().getTypeName(), e);
                }

                //execute the WriteBatch
                this.db().write(WRITE_OPTIONS, batch);
                return result;
            } catch (RocksDBException e) {
                if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    if (resultSet) { //the result was set before the exception was thrown, try to clean it up
                        try {
                            cleanup.acceptThrowing(result);
                        } catch (Throwable t1) { //save exception for later
                            e.addSuppressed(t1);
                        }
                    }

                    throw wrapException(e);
                }

                //the database is transactional and there was a commit failure, try again until it works!
                if (resultSet) { //the result was set before the exception was thrown, try to clean it up
                    cleanup.acceptThrowing(result);
                }
            } catch (Throwable t) {
                if (resultSet) { //the result was set before the exception was thrown, try to clean it up
                    try {
                        cleanup.acceptThrowing(result);
                    } catch (Throwable t1) { //save exception for later
                        t.addSuppressed(t1);
                    }
                }

                throw PUnsafe.throwException(t); //rethrow original exception
            }
        } while (true);
    }

    /**
     * @see FStorageInternal#transactAtomicRun(ThrowingConsumer)
     */
    public abstract void transactAtomicRun(@NonNull ThrowingConsumer<? super FStorageAccess, ? extends FStorageException> action) throws FStorageException;

    /**
     * @see FStorageInternal#transactAtomicGet(ThrowingFunction)
     */
    public abstract <R> R transactAtomicGet(@NonNull ThrowingFunction<? super FStorageAccess, ? extends R, ? extends FStorageException> action) throws FStorageException;

    /**
     * @see FStorageInternal#transactAtomicGet(ThrowingFunction, ThrowingConsumer)
     */
    public abstract <R> R transactAtomicGet(@NonNull ThrowingFunction<? super FStorageAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException;

    protected DBOptions createDBOptions() {
        return new DBOptions()
                .setCreateIfMissing(true)
                .setCreateMissingColumnFamilies(true)
                .setAllowConcurrentMemtableWrite(true)
                .setAllowFAllocate(false)
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
                            .setZStdMaxTrainBytes(64 << 16)
                            .setLevel(6));
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

            try {
                //close the root category
                this.rootCategory.close();

                //flush all column families
                this.db().flush(FLUSH_OPTIONS, new ArrayList<>(this.openColumnFamilyHandles.values()));

                //delete any column families whose deletion is pending
                this.deleteQueuedColumnFamilies();

                //close all column families, then the db itself
                this.openColumnFamilyHandles.values().forEach(ColumnFamilyHandle::close);
            } finally {
                this.db().closeE();
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

    public Map<String, String> createColumnFamilies(@NonNull FStorageAccess access, @NonNull Map<String, FStorageColumnHintsInternal> columnNamesAndHintsToCreate) {
        this.ensureOpen();

        //noinspection UnstableApiUsage
        return columnNamesAndHintsToCreate.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey,
                entry -> this.manifest.assignNewColumnFamilyName(access, entry.getValue())));
    }

    public Map<String, ColumnFamilyHandle> acquireColumnFamilies(@NonNull Collection<String> columnFamilyNames) throws FStorageException {
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
                        if (!(this.db instanceof TransactionDB)) {
                            columnFamilyHandles = this.db().createColumnFamilies(newColumnFamilyDescriptors);
                        } else { //workaround for https://github.com/facebook/rocksdb/issues/10322: create column families individually
                            columnFamilyHandles = new ArrayList<>(newColumnFamilyDescriptors.size());
                            try {
                                for (ColumnFamilyDescriptor columnFamilyDescriptor : newColumnFamilyDescriptors) {
                                    columnFamilyHandles.add(this.db().createColumnFamily(columnFamilyDescriptor));
                                }
                            } catch (
                                    RocksDBException e) { //something went wrong, try to drop all the column families which have been created so far in order to pretend like the operation is atomic
                                for (ColumnFamilyHandle columnFamilyHandle : columnFamilyHandles) {
                                    //try-with-resources to ensure the column family handle is closed in any case
                                    try (ColumnFamilyHandle cfHandle = columnFamilyHandle) {
                                        this.db().dropColumnFamily(cfHandle);
                                    } catch (Exception e1) {
                                        e.addSuppressed(e1);
                                    }
                                }

                                //rethrow original exception now that the column families have been dropped again
                                throw e;
                            }
                        }
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

    public void deleteColumnFamily(@NonNull FStorageAccess access, @NonNull String columnFamilyName) {
        this.deleteColumnFamilies(access, Collections.singleton(columnFamilyName));
    }

    public void deleteColumnFamilies(@NonNull FStorageAccess access, @NonNull Collection<String> columnFamilyNames) {
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
            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        } finally {
            //unlock all of the column family names which have been acquired so far
            this.lockedColumnFamilyNames.removeAll(lockedColumnFamiliesToReleaseOnFailure);
        }
    }

    protected void deleteQueuedColumnFamilies() throws FStorageException {
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

            fp2().log().info("deleting named column families: %s", columnFamilyNamesToDelete);

            try { //get handles of all the column families we're about to delete and drop them
                List<ColumnFamilyHandle> handles = columnFamilyNamesToDelete.stream()
                        .map(this.openColumnFamilyHandles::remove)
                        .filter(Objects::nonNull) //the column family might not be open if it was scheduled for deletion without ever being created (it exists solely in the manifest)
                        .collect(Collectors.toList());

                try {
                    if (!(this.db instanceof TransactionDB)) {
                        this.db().dropColumnFamilies(handles);
                    } else { //workaround for https://github.com/facebook/rocksdb/issues/10322: drop column families one-at-a-time
                        RocksDBException e = null;
                        for (ColumnFamilyHandle columnFamilyHandle : handles) {
                            try {
                                this.db().dropColumnFamily(columnFamilyHandle);
                            } catch (RocksDBException e1) { //something went wrong, save the exception to be rethrown later
                                if (e == null) {
                                    e = e1;
                                } else {
                                    e.addSuppressed(e1);
                                }
                            }
                        }

                        //at least one column family threw an exception while being dropped, rethrow the exception
                        if (e != null) {
                            throw e;
                        }
                    }
                } finally {
                    handles.forEach(ColumnFamilyHandle::close);
                }
            } catch (RocksDBException e) {
                throw wrapException(e);
            }

            //update manifest to show that the column families have been deleted
            this.transactAtomicRun(access -> columnFamilyNamesToDelete.forEach(columnFamilyName -> this.manifest.removeColumnFamilyFromDeletionQueue(access, columnFamilyName)));
        } catch (Exception e) {
            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        } finally {
            //unlock all of the column family names which have been acquired so far
            this.lockedColumnFamilyNames.removeAll(columnFamilyNamesToDelete);
        }
    }

    //
    // FStorageCategory methods
    // (these all delegate to this.rootCategory)
    //

    @Override
    public Optional<byte[]> getToken() throws FStorageException {
        return this.rootCategory.getToken();
    }

    @Override
    public void setToken(@NonNull byte[] token) throws FStorageException {
        this.rootCategory.setToken(token);
    }

    @Override
    public void removeToken() throws FStorageException {
        this.rootCategory.removeToken();
    }

    @Override
    public Set<String> allCategories() throws FStorageException {
        return this.rootCategory.allCategories();
    }

    @Override
    public FStorageCategory openCategory(@NonNull String name, @NonNull FStorageCategoryFactory factory) throws FStorageException, NoSuchElementException, IllegalStateException {
        return this.rootCategory.openCategory(name, factory);
    }

    @Override
    public FStorageCategory openOrCreateCategory(@NonNull String name, @NonNull FStorageCategoryFactory factory) throws FStorageException, IllegalStateException {
        return this.rootCategory.openOrCreateCategory(name, factory);
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
    public <I extends FStorageItem> I openItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, NoSuchElementException, IllegalStateException {
        return this.rootCategory.openItem(name, factory);
    }

    @Override
    public <I extends FStorageItem> I openOrCreateItem(@NonNull String name, @NonNull FStorageItemFactory<I> factory) throws FStorageException, IllegalStateException {
        return this.rootCategory.openOrCreateItem(name, factory);
    }

    @Override
    public void deleteItem(@NonNull String name) throws FStorageException, NoSuchElementException, IllegalStateException {
        this.rootCategory.deleteItem(name);
    }

    /**
     * Base implementation of {@link RocksStorage} backed by a transactional {@link RocksDB}.
     *
     * @author DaPorkchop_
     */
    protected static class LockingRocksStorage extends RocksStorage<RocksDB> {
        protected final Lock readLock;
        protected final Lock writeLock;

        protected LockingRocksStorage(Path root) throws FStorageException {
            super(root);

            ReadWriteLock lock = new ReentrantReadWriteLock();
            this.readLock = lock.readLock();
            this.writeLock = lock.writeLock();
        }

        @Override
        protected RocksDB openDB(DBOptions dbOptions, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors, List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
            return RocksDB.open(dbOptions, path, columnFamilyDescriptors, columnFamilyHandles);
        }

        @Override
        public void readRun(@NonNull ThrowingConsumer<? super FStorageReadAccess, ? extends FStorageException> action) throws FStorageException {
            super.readRun(action); //delegate to super without any additional locking: readRun doesn't guarantee any atomicity
        }

        @Override
        public <R> R readGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
            return super.readGet(action); //delegate to super without any additional locking: readGet doesn't guarantee any atomicity
        }

        @Override
        public <R> R readGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
            return super.readGet(action, cleanup); //delegate to super without any additional locking: readGet doesn't guarantee any atomicity
        }

        @Override
        public void readAtomicRun(@NonNull ThrowingConsumer<? super FStorageReadAccess, ? extends FStorageException> action) throws FStorageException {
            super.readAtomicRun(action); //delegate to super without any additional locking: super does all reads from a snapshot, thus fulfilling readAtomicRun's atomicity requirements
        }

        @Override
        public <R> R readAtomicGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
            return super.readAtomicGet(action); //delegate to super without any additional locking: super does all reads from a snapshot, thus fulfilling readAtomicRun's atomicity requirements
        }

        @Override
        public <R> R readAtomicGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
            return super.readAtomicGet(action, cleanup); //delegate to super without any additional locking: super does all reads from a snapshot, thus fulfilling readAtomicRun's atomicity requirements
        }

        @Override
        public void writeAtomicRun(@NonNull ThrowingConsumer<? super FStorageWriteAccess, ? extends FStorageException> action) throws FStorageException {
            //writeAtomicRun only guarantees that the writes will be applied atomically, which the super implementation achieves by using a WriteBatch. we just need to
            //  delegate to super while holding a read lock to avoid clashing with any transactAtomic* methods
            this.readLock.lock();
            try {
                super.writeAtomicRun(action);
            } finally {
                this.readLock.unlock();
            }
        }

        @Override
        public <R> R writeAtomicGet(@NonNull ThrowingFunction<? super FStorageWriteAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
            //writeAtomicGet only guarantees that the writes will be applied atomically, which the super implementation achieves by using a WriteBatch. we just need to
            //  delegate to super while holding a read lock to avoid clashing with any transactAtomic* methods
            this.readLock.lock();
            try {
                return super.writeAtomicGet(action);
            } finally {
                this.readLock.unlock();
            }
        }

        @Override
        public <R> R writeAtomicGet(@NonNull ThrowingFunction<? super FStorageWriteAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
            //writeAtomicGet only guarantees that the writes will be applied atomically, which the super implementation achieves by using a WriteBatch. we just need to
            //  delegate to super while holding a read lock to avoid clashing with any transactAtomic* methods
            this.readLock.lock();
            try {
                return super.writeAtomicGet(action, cleanup);
            } finally {
                this.readLock.unlock();
            }
        }

        @Override
        public void transactAtomicRun(@NonNull ThrowingConsumer<? super FStorageAccess, ? extends FStorageException> action) throws FStorageException {
            this.writeLock.lock();
            try (RocksAccessWriteBatchWithIndexMasqueradingAsTransaction batch = new RocksAccessWriteBatchWithIndexMasqueradingAsTransaction(this.db(), READ_OPTIONS)) {
                try {
                    //execute the callback with the created WriteBatch
                    action.acceptThrowing(batch);
                } catch (FStorageException e) {
                    if (e instanceof FStorageExceptionWrappedRocksDBException) {
                        throw ((FStorageExceptionWrappedRocksDBException) e).getCause();
                    }

                    throw new IllegalArgumentException("don't know what to do with " + e.getClass().getTypeName(), e);
                }

                //execute the WriteBatch
                this.db().write(WRITE_OPTIONS, batch);
            } catch (RocksDBException e) { //the exception cannot possibly be a transaction commit failure, rethrow the exception
                throw wrapException(e);
            } finally {
                this.writeLock.unlock();
            }
        }

        @Override
        public <R> R transactAtomicGet(@NonNull ThrowingFunction<? super FStorageAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
            do {
                this.writeLock.lock();
                try (RocksAccessWriteBatchWithIndexMasqueradingAsTransaction batch = new RocksAccessWriteBatchWithIndexMasqueradingAsTransaction(this.db(), READ_OPTIONS)) {
                    R result;
                    try {
                        //execute the callback with the created WriteBatch
                        result = action.applyThrowing(batch);
                    } catch (FStorageException e) {
                        if (e instanceof FStorageExceptionWrappedRocksDBException) {
                            throw ((FStorageExceptionWrappedRocksDBException) e).getCause();
                        }

                        throw new IllegalArgumentException("don't know what to do with " + e.getClass().getTypeName(), e);
                    }

                    //execute the WriteBatch
                    this.db().write(WRITE_OPTIONS, batch);
                    return result;
                } catch (RocksDBException e) { //the exception cannot possibly be a transaction commit failure, rethrow the exception
                    throw wrapException(e);
                } finally {
                    this.writeLock.unlock();
                }
            } while (true);
        }

        @Override
        public <R> R transactAtomicGet(@NonNull ThrowingFunction<? super FStorageAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
            R result = null;
            boolean resultSet = false; //to account for potential null results

            this.writeLock.lock();
            try (RocksAccessWriteBatchWithIndexMasqueradingAsTransaction batch = new RocksAccessWriteBatchWithIndexMasqueradingAsTransaction(this.db(), READ_OPTIONS)) {
                try {
                    //execute the callback with the created WriteBatch
                    result = action.applyThrowing(batch);
                    resultSet = true;
                } catch (FStorageException e) {
                    if (e instanceof FStorageExceptionWrappedRocksDBException) {
                        throw ((FStorageExceptionWrappedRocksDBException) e).getCause();
                    }

                    throw new IllegalArgumentException("don't know what to do with " + e.getClass().getTypeName(), e);
                }

                //execute the WriteBatch
                this.db().write(WRITE_OPTIONS, batch);
                return result;
            } catch (RocksDBException e) { //the exception cannot possibly be a transaction commit failure, rethrow the exception
                if (resultSet) { //the result was set before the exception was thrown, try to clean it up
                    try {
                        cleanup.acceptThrowing(result);
                    } catch (Throwable t1) { //save exception for later
                        e.addSuppressed(t1);
                    }
                }

                throw wrapException(e);
            } catch (Throwable t) {
                if (resultSet) { //the result was set before the exception was thrown, try to clean it up
                    try {
                        cleanup.acceptThrowing(result);
                    } catch (Throwable t1) { //save exception for later
                        t.addSuppressed(t1);
                    }
                }

                throw PUnsafe.throwException(t); //rethrow original exception
            } finally {
                this.writeLock.unlock();
            }
        }
    }

    /**
     * Base implementation of {@link RocksStorage} backed by a transactional {@link RocksDB}.
     *
     * @author DaPorkchop_
     */
    protected static abstract class AbstractTransactionRocksStorage<DB extends RocksDB> extends RocksStorage<DB> {
        protected AbstractTransactionRocksStorage(Path root) throws FStorageException {
            super(root);
        }

        protected abstract Transaction beginTransaction(@NonNull WriteOptions writeOptions, boolean setSnapshot);

        @Override
        public void transactAtomicRun(@NonNull ThrowingConsumer<? super FStorageAccess, ? extends FStorageException> action) throws FStorageException {
            do {
                try (Transaction txn = this.beginTransaction(WRITE_OPTIONS, true);
                     RocksAccessTransaction access = new RocksAccessTransaction(txn)) {
                    try {
                        //execute the callback with the transaction
                        action.acceptThrowing(access);
                    } catch (FStorageException e) {
                        if (e instanceof FStorageExceptionWrappedRocksDBException) {
                            throw ((FStorageExceptionWrappedRocksDBException) e).getCause();
                        }

                        throw new IllegalArgumentException("don't know what to do with " + e.getClass().getTypeName(), e);
                    }

                    //try to commit the transaction
                    txn.commit();
                    return;
                } catch (RocksDBException e) {
                    if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                        throw wrapException(e);
                    }

                    //the database is transactional and there was a commit failure, try again until it works!
                }
            } while (true);
        }

        @Override
        public <R> R transactAtomicGet(@NonNull ThrowingFunction<? super FStorageAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
            do {
                try (Transaction txn = this.beginTransaction(WRITE_OPTIONS, true);
                     RocksAccessTransaction access = new RocksAccessTransaction(txn)) {
                    R result;
                    try {
                        //execute the callback with the transaction
                        result = action.applyThrowing(access);
                    } catch (FStorageException e) {
                        if (e instanceof FStorageExceptionWrappedRocksDBException) {
                            throw ((FStorageExceptionWrappedRocksDBException) e).getCause();
                        }

                        throw new IllegalArgumentException("don't know what to do with " + e.getClass().getTypeName(), e);
                    }

                    //try to commit the transaction
                    txn.commit();
                    return result;
                } catch (RocksDBException e) {
                    if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                        throw wrapException(e);
                    }

                    //the database is transactional and there was a commit failure, try again until it works!
                }
            } while (true);
        }

        @Override
        public <R> R transactAtomicGet(@NonNull ThrowingFunction<? super FStorageAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
            do {
                R result = null;
                boolean resultSet = false; //to account for potential null results

                try (Transaction txn = this.beginTransaction(WRITE_OPTIONS, true);
                     RocksAccessTransaction access = new RocksAccessTransaction(txn)) {
                    try {
                        //execute the callback with the transaction
                        result = action.applyThrowing(access);
                        resultSet = true;
                    } catch (FStorageException e) {
                        if (e instanceof FStorageExceptionWrappedRocksDBException) {
                            throw ((FStorageExceptionWrappedRocksDBException) e).getCause();
                        }

                        throw new IllegalArgumentException("don't know what to do with " + e.getClass().getTypeName(), e);
                    }

                    //try to commit the transaction
                    txn.commit();
                    return result;
                } catch (RocksDBException e) {
                    if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                        if (resultSet) { //the result was set before the exception was thrown, try to clean it up
                            try {
                                cleanup.acceptThrowing(result);
                            } catch (Throwable t1) { //save exception for later
                                e.addSuppressed(t1);
                            }
                        }

                        throw wrapException(e);
                    }

                    //the database is transactional and there was a commit failure, try again until it works!
                    if (resultSet) { //the result was set before the exception was thrown, try to clean it up
                        cleanup.acceptThrowing(result);
                    }
                } catch (Throwable t) {
                    if (resultSet) { //the result was set before the exception was thrown, try to clean it up
                        try {
                            cleanup.acceptThrowing(result);
                        } catch (Throwable t1) { //save exception for later
                            t.addSuppressed(t1);
                        }
                    }

                    throw PUnsafe.throwException(t); //rethrow original exception
                }
            } while (true);
        }
    }

    /**
     * Implementation of {@link RocksStorage} backed by an ordinary {@link TransactionDB}.
     *
     * @author DaPorkchop_
     */
    protected static class PessimisticTransactionRocksStorage extends AbstractTransactionRocksStorage<TransactionDB> {
        protected static final TransactionOptions TRANSACTION_OPTIONS_DEFAULT = new TransactionOptions();
        protected static final TransactionOptions TRANSACTION_OPTIONS_SET_SNAPSHOT = new TransactionOptions().setSetSnapshot(true);

        protected TransactionDBOptions transactionDBOptions;

        protected PessimisticTransactionRocksStorage(Path root) throws FStorageException {
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
    protected static class OptimisticTransactionRocksStorage extends AbstractTransactionRocksStorage<OptimisticTransactionDB> {
        protected static final OptimisticTransactionOptions TRANSACTION_OPTIONS_DEFAULT = new OptimisticTransactionOptions();
        protected static final OptimisticTransactionOptions TRANSACTION_OPTIONS_SET_SNAPSHOT = new OptimisticTransactionOptions().setSetSnapshot(true);

        protected OptimisticTransactionRocksStorage(Path root) throws FStorageException {
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
