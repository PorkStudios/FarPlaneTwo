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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageWriteBatchInternal;
import net.daporkchop.lib.primitive.map.IntObjMap;
import net.daporkchop.lib.primitive.map.open.IntObjOpenHashMap;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

import static net.daporkchop.fp2.core.storage.rocks.RocksStorage.*;

/**
 * Implementation of {@link FStorageWriteBatchInternal} for {@link RocksStorageInternal}.
 *
 * @author DaPorkchop_
 */
@Getter
public class RocksStorageWriteBatchInternal implements FStorageWriteBatchInternal {
    private final RocksStorageInternal storageInternal;

    private WriteBatch batch = new WriteBatch();

    private final IntObjMap<ColumnFamilyHandle> savedIdsToColumnFamiliesMappings = new IntObjOpenHashMap<>();
    private int modificationCounter;

    public RocksStorageWriteBatchInternal(@NonNull RocksStorageInternal storageInternal) {
        this.storageInternal = storageInternal;

        storageInternal.readLock().lock();
        try {
            //snapshot the column family id -> RocksStorageColumnInternal mappings
            storageInternal.columns().values().forEach(column -> this.savedIdsToColumnFamiliesMappings.put(column.handle().getID(), column.handle()));

            //save modification counter so that we can check for conflicts
            this.modificationCounter = storageInternal.modificationCounter();
        } finally {
            storageInternal.readLock().unlock();
        }
    }

    public void validate() throws FStorageException {
        this.storageInternal.ensureOpen();
    }

    @Override
    public void write() throws FStorageException {
        this.validate();

        this.storageInternal.readLock().lock();
        try {
            if (this.modificationCounter != this.storageInternal.modificationCounter()) { //the column structure has changed since this write batch began
                this.modificationCounter = this.storageInternal.modificationCounter();

                //update the saved column family id -> RocksStorageColumnInternal mappings so that it contains both old and new IDs (we don't know when the column families changed, so
                // we have to assume it could have happened at any time since this write batch was created)
                this.storageInternal.columns().values().forEach(column -> this.savedIdsToColumnFamiliesMappings.putIfAbsent(column.handle().getID(), column.handle()));

                //rewrite history by copying each operation from the old batch and putting it into a new one with the updated column families
                WriteBatch newBatch = new WriteBatch();
                this.batch.iterate(new WriteBatch.Handler() {
                    @Override
                    public void put(int columnFamilyId, byte[] key, byte[] value) throws RocksDBException {
                        newBatch.put(RocksStorageWriteBatchInternal.this.savedIdsToColumnFamiliesMappings.get(columnFamilyId), key, value);
                    }

                    @Override
                    public void put(byte[] key, byte[] value) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void merge(int columnFamilyId, byte[] key, byte[] value) throws RocksDBException {
                        newBatch.merge(RocksStorageWriteBatchInternal.this.savedIdsToColumnFamiliesMappings.get(columnFamilyId), key, value);
                    }

                    @Override
                    public void merge(byte[] key, byte[] value) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void delete(int columnFamilyId, byte[] key) throws RocksDBException {
                        newBatch.delete(RocksStorageWriteBatchInternal.this.savedIdsToColumnFamiliesMappings.get(columnFamilyId), key);
                    }

                    @Override
                    public void delete(byte[] key) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void singleDelete(int columnFamilyId, byte[] key) throws RocksDBException {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void singleDelete(byte[] key) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void deleteRange(int columnFamilyId, byte[] beginKey, byte[] endKey) throws RocksDBException {
                        newBatch.deleteRange(RocksStorageWriteBatchInternal.this.savedIdsToColumnFamiliesMappings.get(columnFamilyId), beginKey, endKey);
                    }

                    @Override
                    public void deleteRange(byte[] beginKey, byte[] endKey) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void logData(byte[] blob) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void putBlobIndex(int columnFamilyId, byte[] key, byte[] value) throws RocksDBException {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void markBeginPrepare() throws RocksDBException {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void markEndPrepare(byte[] xid) throws RocksDBException {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void markNoop(boolean emptyBatch) throws RocksDBException {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void markRollback(byte[] xid) throws RocksDBException {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void markCommit(byte[] xid) throws RocksDBException {
                        throw new UnsupportedOperationException();
                    }
                });

                //replace the current batch with the rewritten one
                this.batch.close();
                this.batch = newBatch;
            }

            this.storageInternal.storage().db().write(WRITE_OPTIONS, this.batch);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        } finally {
            this.storageInternal.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        this.batch.clear();
    }

    @Override
    public void close() {
        this.batch.close();
    }

    @Override
    public void put(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull byte[] value) throws FStorageException {
        try {
            this.batch.put(((RocksStorageColumnInternal) column).handle(), key, value);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        }
    }

    @Override
    public void delete(@NonNull FStorageColumnInternal column, @NonNull byte[] key) throws FStorageException {
        try {
            this.batch.delete(((RocksStorageColumnInternal) column).handle(), key);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        }
    }
}
