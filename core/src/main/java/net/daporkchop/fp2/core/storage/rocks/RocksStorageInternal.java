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

import com.google.common.collect.ImmutableBiMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageSnapshotInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageTransactionInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageWriteBatchInternal;
import net.daporkchop.lib.common.util.PorkUtil;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

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

    public static List<ColumnFamilyHandle> toColumnFamilyHandles(List<FStorageColumnInternal> columns) {
        ColumnFamilyHandle[] out = new ColumnFamilyHandle[columns.size()];
        columns.forEach(new Consumer<FStorageColumnInternal>() {
            int i = 0;

            @Override
            public void accept(FStorageColumnInternal column) {
                out[this.i++] = ((RocksStorageColumnInternal) column).handle();
            }
        });
        return Arrays.asList(out);
    }

    private final RocksStorage<?> storage;
    private final RocksStorageManifest.ItemData manifestData;

    private final ImmutableBiMap<String, RocksStorageColumnInternal> columns;
    private final Set<String> usedColumnFamilies;

    private int modificationCounter = 0;

    private volatile boolean open = true;

    public RocksStorageInternal(@NonNull RocksStorage<?> storage, @NonNull RocksStorageManifest.ItemData manifestData, @NonNull Map<String, ColumnFamilyHandle> columns) {
        this.storage = storage;
        this.manifestData = manifestData;

        //noinspection UnstableApiUsage
        this.columns = columns.entrySet().stream()
                .collect(ImmutableBiMap.toImmutableBiMap(Map.Entry::getKey, entry -> new RocksStorageColumnInternal(entry.getValue())));

        this.usedColumnFamilies = new ObjectOpenHashSet<>(columns.keySet());
    }

    public void ensureOpen() {
        if (!this.open) {
            throw new IllegalStateException("storage has been closed!");
        }
    }

    public void close() throws FStorageException {
        this.ensureOpen();
        this.open = false;

        //notify the storage that we've stopped using all the column families
        // (assume we already hold a write lock on the storage)
        this.storage.stopUsingColumnFamilies(this.usedColumnFamilies);
    }

    @Override
    public FStorageWriteBatchInternal beginWriteBatch() {
        this.ensureOpen();
        return new RocksStorageWriteBatchInternal(this);
    }

    @Override
    public FStorageTransactionInternal beginTransaction() {
        this.ensureOpen();
        return new RocksStorageTransactionInternal(this);
    }

    @Override
    public FStorageSnapshotInternal snapshot() {
        this.ensureOpen();
        return new RocksStorageSnapshotInternal(this);
    }

    @Override
    public Optional<byte[]> getToken() {
        //the token is stored in the manifest, we don't need to acquire our own lock
        this.storage.readLock().lock();
        try {
            this.ensureOpen();

            return Optional.ofNullable(this.manifestData.token());
        } finally {
            this.storage.readLock().unlock();
        }
    }

    @Override
    public void setToken(@NonNull byte[] token) throws FStorageException {
        this.setToken0(token);
    }

    @Override
    public void removeToken() throws FStorageException {
        this.setToken0(null);
    }

    protected void setToken0(byte[] token) throws FStorageException {
        this.ensureOpen();

        //the token is stored in the manifest, we don't need to acquire our own lock
        this.storage.writeLock().lock();
        try {
            this.storage.manifest().snapshot();
            try {
                //replace the token, then save the updated manifest
                this.manifestData.token(token);
                this.storage.writeManifest();
            } catch (Exception e) { //something failed, roll back the changes and rethrow exception
                this.storage.manifest().rollback();
                throw new FStorageException("failed to set storage token", e);
            } finally {
                this.storage.manifest().clearSnapshot();
            }
        } finally {
            this.storage.writeLock().unlock();
        }
    }

    @Override
    public Map<String, FStorageColumnInternal> getColumns() {
        //we aren't actually reading anything, just returning an IMMUTABLE map. no locking is necessary!
        this.ensureOpen();
        return uncheckedCast(this.columns);
    }

    @Override
    public void clearColumns(@NonNull Collection<FStorageColumnInternal> columns) throws FStorageException {
        this.ensureOpen();

        List<String> columnNames = PorkUtil.<Collection<RocksStorageColumnInternal>>uncheckedCast(columns).stream()
                .distinct().map(this.columns.inverse()::get).collect(Collectors.toList());
        List<ColumnFamilyHandle> oldColumnFamilyHandles = PorkUtil.<Collection<RocksStorageColumnInternal>>uncheckedCast(columns).stream()
                .distinct().map(RocksStorageColumnInternal::handle).collect(Collectors.toList());

        //acquire write lock on the storage since we're about to be modifying the manifest
        this.storage.writeLock().lock();
        //acquire write lock on self since we're about to be modifying the column families
        this.writeLock().lock();
        try {
            List<String> newColumnFamilyNames = new ArrayList<>(columnNames.size());

            this.storage.manifest().snapshot();
            try {
                columnNames.forEach(columnName -> {
                    //find the name of the current column family so we can use that to get the usage hints for the new one
                    String oldColumnFamilyName = this.manifestData.columnNamesToColumnFamilyNames().get(columnName);
                    FStorageColumnHintsInternal oldColumnHints = this.storage.manifest().allColumnFamilies().get(oldColumnFamilyName);

                    //mark the old column family for deletion
                    this.storage.manifest().columnFamiliesPendingDeletion().add(oldColumnFamilyName);

                    //create a new column family
                    String newColumnFamilyName = this.storage.manifest().assignNewColumnFamilyName(oldColumnHints);
                    newColumnFamilyNames.add(newColumnFamilyName);

                    //associate the new column family name with the column being cleared
                    this.manifestData.columnNamesToColumnFamilyNames().put(columnName, newColumnFamilyName);
                });

                //save the updated manifest
                this.storage.writeManifest();
            } catch (Exception e) { //something failed, roll back the changes and rethrow exception
                this.storage.manifest().rollback();
                throw new FStorageException("failed to replace column families", e);
            } finally {
                this.storage.manifest().clearSnapshot();
            }

            //open the new column families
            Map<String, ColumnFamilyHandle> newColumnFamilies = this.storage.beginUsingColumnFamilies(newColumnFamilyNames);

            //remember the new column families' names so that we can release them when closing this storage
            this.usedColumnFamilies.addAll(newColumnFamilyNames);

            //increment the modification counter in order to invalidate active transactions
            this.modificationCounter++;

            //replace the existing column families with the new ones
            for (int i = 0; i < columnNames.size(); i++) {
                this.columns.get(columnNames.get(i)).handle(newColumnFamilies.get(newColumnFamilyNames.get(i)));
            }
        } finally {
            this.writeLock().unlock();
            this.storage.writeLock().unlock();
        }

        //technically, we're done now! the old column families are no longer referenced and will be dropped from the db eventually, and all future db operations will be redirected to
        //  the new column families. however, in order to possibly allow rocksdb to reclaim some storage sooner, we'll go through and do a range delete across every key in the old
        //  column families.

        try (WriteBatch batch = new WriteBatch()) {
            //iterate over each of the old column families
            for (ColumnFamilyHandle oldColumnFamilyHandle : oldColumnFamilyHandles) {
                //create an iterator in the column family
                try (RocksIterator itr = this.storage.db().newIterator(oldColumnFamilyHandle, RocksStorage.READ_OPTIONS)) {
                    itr.seekToFirst();
                    if (!itr.isValid()) { //the column family is empty, there's nothing to do!
                        continue;
                    }
                    byte[] firstKey = itr.key();

                    itr.seekToLast();
                    checkState(itr.isValid(), "iterator is invalid?!?"); //impossible, we already know the column family isn't empty
                    byte[] lastKey = itr.key();

                    if (Arrays.equals(firstKey, lastKey)) { //the keys are the same, there's only one entry in the column family
                        batch.delete(oldColumnFamilyHandle, firstKey);
                    } else { //there are multiple keys in the column family
                        batch.deleteRange(oldColumnFamilyHandle, firstKey, lastKey);
                        batch.delete(oldColumnFamilyHandle, lastKey); //explicitly delete lastKey because deleteRange()'s upper bound is exclusive
                    }
                }
            }

            //execute all the deleteRange()s
            this.storage.db().write(RocksStorage.WRITE_OPTIONS, batch);
        } catch (RocksDBException e) {
            throw new FStorageException("failed to clear old column families", e);
        }
    }

    @Override
    public byte[] get(@NonNull FStorageColumnInternal column, @NonNull byte[] key) throws FStorageException {
        this.ensureOpen();
        try {
            return this.storage.db().get(((RocksStorageColumnInternal) column).handle(), RocksStorage.READ_OPTIONS, key);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        }
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<FStorageColumnInternal> columns, @NonNull List<byte[]> keys) throws FStorageException {
        this.ensureOpen();
        try {
            return this.storage.db().multiGetAsList(RocksStorage.READ_OPTIONS, toColumnFamilyHandles(columns), keys);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        }
    }
}
