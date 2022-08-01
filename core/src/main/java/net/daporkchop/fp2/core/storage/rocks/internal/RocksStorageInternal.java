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

package net.daporkchop.fp2.core.storage.rocks.internal;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.external.FStorageItem;
import net.daporkchop.fp2.api.storage.external.FStorageItemFactory;
import net.daporkchop.fp2.api.storage.internal.FStorageColumn;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageInternal;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageReadAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageWriteAccess;
import net.daporkchop.fp2.api.util.function.ThrowingConsumer;
import net.daporkchop.fp2.api.util.function.ThrowingFunction;
import net.daporkchop.fp2.core.storage.rocks.RocksStorage;
import net.daporkchop.fp2.core.storage.rocks.RocksStorageCategory;
import net.daporkchop.fp2.core.storage.rocks.RocksStorageColumn;
import net.daporkchop.fp2.core.storage.rocks.manifest.RocksItemManifest;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link FStorageInternal} for {@link RocksStorage}.
 *
 * @author DaPorkchop_
 */
@Getter
public class RocksStorageInternal extends ReentrantReadWriteLock implements FStorageInternal {
    /*
     * Implementation notes:
     *
     * This relies on the observation that as long as this item is open, the underlying storage will also be. It is therefore not necessary to acquire any global locks unless
     * accessing state outside of this item (storage/category/manifest).
     *
     * Additionally, we assume that the storage is being used correctly and that no method calls are made while or after the storage is closed.
     */

    private final RocksStorage<?> storage;
    private final RocksItemManifest manifestData;

    private ImmutableBiMap<String, RocksStorageColumn> columnFamilyNamesToColumns;
    private ImmutableBiMap<String, RocksStorageColumn> columnNamesToColumns;
    private Collection<String> columnFamilyNames;

    private RocksStorageCategory parent;
    @Getter
    private String nameInParent;

    private FStorageItem externalItem;

    private int modificationCounter = 0;

    private volatile boolean open = true;

    @SuppressWarnings("OptionalAssignedToNull")
    public RocksStorageInternal(@NonNull RocksStorage<?> storage, @NonNull String inode, @NonNull FStorageItemFactory<?> factory, @NonNull FStorageAccess access) {
        this.storage = storage;
        this.manifestData = new RocksItemManifest(storage.defaultColumn(), inode, access);

        @RequiredArgsConstructor
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        class CallbackImpl implements FStorageItemFactory.ConfigurationCallback {
            final Set<String> columnNames = new ObjectOpenHashSet<>();
            final Map<String, FStorageColumnHintsInternal> columnHints = new ObjObjOpenHashMap<>();
            final Map<String, FStorageItemFactory.ColumnRequirement> columnRequirements = new ObjObjOpenHashMap<>();

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

            @Override
            public void registerColumn(@NonNull String name, @NonNull FStorageColumnHintsInternal hints, @NonNull FStorageItemFactory.ColumnRequirement requirement) {
                checkArg(this.columnNames.add(name), "column '%s' was already registered!", name);

                this.columnHints.put(name, hints);
                this.columnRequirements.put(name, requirement);
            }
        }

        //configure the item
        CallbackImpl callback = new CallbackImpl(this.manifestData.getToken(access));
        FStorageItemFactory.ConfigurationResult result = factory.configure(callback);

        switch (result) {
            case DELETE_EXISTING_AND_CREATE:
                if (this.manifestData.isInitialized(access)) { //the item already exists
                    //delete the item data by deleting all of the previous column families
                    this.storage.deleteColumnFamilies(access, this.manifestData.snapshotColumnNamesToColumnFamilyNames(access).values());

                    //if no new token is being set, preserve the old one
                    if (callback.newToken == null) {
                        callback.newToken = callback.existingToken;
                    }

                    //clear the item manifest
                    this.manifestData.clear(access);
                }
            case CREATE_IF_MISSING:
                //no-op
                break;
            default:
                throw new IllegalArgumentException("unsupported configuration result: " + result);
        }

        boolean isNewItem = !this.manifestData.isInitialized(access);

        //prepare to create and acquire column families
        Map<String, FStorageColumnHintsInternal> columnsToCreate = new ObjObjOpenHashMap<>();

        for (String columnName : callback.columnNames) {
            FStorageItemFactory.ColumnRequirement columnRequirement = callback.columnRequirements.get(columnName);
            FStorageColumnHintsInternal columnHints = callback.columnHints.get(columnName);

            Optional<String> optionalColumnFamilyName = this.manifestData.getColumnFamilyNameForColumn(access, columnName);

            switch (columnRequirement) {
                case DELETE_EXISTING_AND_CREATE:
                    if (optionalColumnFamilyName.isPresent()) { //the column with the given name exists
                        //delete the column family
                        this.storage.deleteColumnFamily(access, optionalColumnFamilyName.get());

                        //remove the column family from the item's manifest
                        this.manifestData.removeColumnByName(access, columnName);
                        optionalColumnFamilyName = Optional.empty();
                    }
                case CREATE_IF_MISSING:
                    if (optionalColumnFamilyName.isPresent()) { //the column with the given name exists
                        //TODO: consider updating the column family hints?
                    } else { //the column with the given name doesn't exist, create a new one
                        //schedule a new column family to be created
                        columnsToCreate.put(columnName, columnHints);
                    }
                    break;
                case FAIL_IF_MISSING:
                    if (optionalColumnFamilyName.isPresent()) { //the column with the given name exists
                        //TODO: consider updating the column family hints?
                    } else { //the column with the given name doesn't exist
                        if (isNewItem) {
                            //schedule a new column family to be created
                            columnsToCreate.put(columnName, columnHints);
                        } else { //FAIL_IF_MISSING isn't allowed to create new columns when opening an existing item
                            throw new NoSuchElementException("the column '" + columnName + "' doesn't exist, but was requested with " + FStorageItemFactory.ColumnRequirement.FAIL_IF_MISSING);
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unsupported column requirement: " + columnRequirement);
            }
        }

        if (!columnsToCreate.isEmpty()) { //some columns need to have a new column family created
            //assign a column family name for each of the columns, then save all the newly created column name -> column family name mappings into the item manifest
            this.storage.createColumnFamilies(access, columnsToCreate)
                    .forEach((columnName, columnFamilyName) -> this.manifestData.addColumn(access, columnName, columnFamilyName));
        }

        if (callback.newToken != null) { //the token is being updated
            if (callback.newToken.isPresent()) {
                this.manifestData.setToken(access, callback.newToken.get());
            } else {
                this.manifestData.removeToken(access);
            }
        }

        if (isNewItem) {
            //the item has been fully initialized
            this.manifestData.markInitialized(access);
        }
    }

    public void open(@NonNull FStorageItemFactory<?> factory, @NonNull RocksStorageCategory parent, @NonNull String nameInParent) throws FStorageException {
        Map<String, String> columnNamesToColumnFamilyNames = this.storage.readGet(this.manifestData::snapshotColumnNamesToColumnFamilyNames);

        this.columnFamilyNames = ImmutableSet.copyOf(columnNamesToColumnFamilyNames.values());

        try {
            //acquire all of the column families
            //noinspection UnstableApiUsage
            this.columnFamilyNamesToColumns = this.storage.acquireColumnFamilies(columnNamesToColumnFamilyNames.values()).entrySet().stream()
                    .collect(ImmutableBiMap.toImmutableBiMap(Map.Entry::getKey, entry -> new RocksStorageColumn(entry.getValue())));

            //noinspection UnstableApiUsage
            this.columnNamesToColumns = columnNamesToColumnFamilyNames.entrySet().stream()
                    .collect(ImmutableBiMap.toImmutableBiMap(Map.Entry::getKey, entry -> this.columnFamilyNamesToColumns.get(entry.getValue())));

            //create user item instance
            this.externalItem = factory.create(this);
        } catch (Exception e) { //something failed, release all of the column families again
            try {
                this.close();
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }

            PUnsafe.throwException(e); //rethrow original exception
            throw new AssertionError(); //impossible
        }

        this.parent = parent;
        this.nameInParent = nameInParent;
    }

    public void ensureOpen() {
        if (!this.open) {
            throw new IllegalStateException("storage has been closed!");
        }
    }

    @Override
    public void close() throws FStorageException {
        this.ensureOpen();

        try {
            if (this.parent != null) { //remove self from parent
                this.parent.doCloseItem(this);
            }
        } finally {
            this.open = false;

            //set the column family handle in each RocksStorageColumnInternal to null in order to cause any illegal accesses after we've already closed to fail
            this.columnFamilyNamesToColumns.values().forEach(column -> column.handle(null));

            //notify the storage that we've stopped using all the column families
            // (assume we already hold a write lock on the storage)
            this.storage.releaseColumnFamilies(this.columnFamilyNames);
        }
    }

    @Override
    public void readRun(@NonNull ThrowingConsumer<? super FStorageReadAccess, ? extends FStorageException> action) throws FStorageException {
        this.storage.readRun(action);
    }

    @Override
    public <R> R readGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
        return this.storage.readGet(action);
    }

    @Override
    public <R> R readGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
        return this.storage.readGet(action, cleanup);
    }

    @Override
    public void readAtomicRun(@NonNull ThrowingConsumer<? super FStorageReadAccess, ? extends FStorageException> action) throws FStorageException {
        this.storage.readAtomicRun(action);
    }

    @Override
    public <R> R readAtomicGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
        return this.storage.readAtomicGet(action);
    }

    @Override
    public <R> R readAtomicGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
        return this.storage.readAtomicGet(action, cleanup);
    }

    @Override
    public void writeAtomicRun(@NonNull ThrowingConsumer<? super FStorageWriteAccess, ? extends FStorageException> action) throws FStorageException {
        this.storage.writeAtomicRun(action);
    }

    @Override
    public <R> R writeAtomicGet(@NonNull ThrowingFunction<? super FStorageWriteAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
        return this.storage.writeAtomicGet(action);
    }

    @Override
    public <R> R writeAtomicGet(@NonNull ThrowingFunction<? super FStorageWriteAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
        return this.storage.writeAtomicGet(action, cleanup);
    }

    @Override
    public void transactAtomicRun(@NonNull ThrowingConsumer<? super FStorageAccess, ? extends FStorageException> action) throws FStorageException {
        this.storage.transactAtomicRun(action);
    }

    @Override
    public <R> R transactAtomicGet(@NonNull ThrowingFunction<? super FStorageAccess, ? extends R, ? extends FStorageException> action) throws FStorageException {
        return this.storage.transactAtomicGet(action);
    }

    @Override
    public <R> R transactAtomicGet(@NonNull ThrowingFunction<? super FStorageAccess, ? extends R, ? extends FStorageException> action, @NonNull ThrowingConsumer<R, ? extends FStorageException> cleanup) throws FStorageException {
        return this.storage.transactAtomicGet(action, cleanup);
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
    public Map<String, FStorageColumn> getColumns() {
        this.ensureOpen();

        //take snapshot of columns map
        return ImmutableMap.copyOf(this.columnNamesToColumns);
    }

    @Override
    public void clearColumns(@NonNull Collection<FStorageColumn> columns) throws FStorageException {
        this.ensureOpen();

        //acquire a write lock to prevent other transactions/write batches from inserting new values while we're trying to delete them
        this.writeLock().lock();
        //do a range deletion over all the keys in the column family
        try (WriteBatch batch = new WriteBatch()) {
            //iterate over each of the columns
            for (FStorageColumn column : new ReferenceOpenHashSet<>(columns)) {
                ColumnFamilyHandle handle = ((RocksStorageColumn) column).handle();

                //create an iterator in the column family
                try (RocksIterator itr = this.storage.db().newIterator(handle, RocksStorage.READ_OPTIONS)) {
                    itr.seekToFirst();
                    if (!itr.isValid()) { //the column family is empty, there's nothing to do!
                        continue;
                    }
                    byte[] firstKey = itr.key();

                    itr.seekToLast();
                    checkState(itr.isValid(), "iterator is invalid?!?"); //impossible, we already know the column family isn't empty
                    byte[] lastKey = itr.key();

                    if (Arrays.equals(firstKey, lastKey)) { //the keys are the same, there's only one entry in the column family
                        batch.delete(handle, firstKey);
                    } else { //there are multiple keys in the column family
                        batch.deleteRange(handle, firstKey, lastKey);
                        batch.delete(handle, lastKey); //explicitly delete lastKey because deleteRange()'s upper bound is exclusive
                    }
                }
            }

            //execute all the deleteRange()s
            this.storage.db().write(RocksStorage.WRITE_OPTIONS, batch);

            //increment modification counter in order to cause all the outstanding transactions to fail
            this.modificationCounter++;
        } catch (RocksDBException e) {
            throw new FStorageException("failed to clear old column families", e);
        } finally {
            this.writeLock().unlock();
        }
    }
}
