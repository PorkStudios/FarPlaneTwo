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
import net.daporkchop.fp2.api.storage.internal.FStorageTransactionFailedException;
import net.daporkchop.fp2.api.storage.internal.FStorageTransactionInternal;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDBException;
import org.rocksdb.Transaction;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.storage.rocks.RocksStorage.*;

/**
 * Implementation of {@link FStorageTransactionInternal} for {@link RocksStorageInternal}.
 *
 * @author DaPorkchop_
 */
@Getter
public class RocksStorageTransactionInternal implements FStorageTransactionInternal {
    protected static FStorageException getExceptionPossiblyCommitFailure(RocksDBException e) {
        switch (e.getStatus().getCode()) {
            case Busy:
            case TimedOut:
                return new FStorageTransactionFailedException(e);
            default:
                return new FStorageException(e);
        }
    }

    protected static byte[][] multiGetForUpdate(@NonNull Transaction txn, @NonNull ReadOptions readOptions, @NonNull List<ColumnFamilyHandle> handles, @NonNull byte[][] keys) throws RocksDBException {
        final int MAX_BATCH_SIZE = 65536;
        if (keys.length <= MAX_BATCH_SIZE) {
            return txn.multiGetForUpdate(readOptions, handles, keys);
        } else { //workaround for https://github.com/facebook/rocksdb/issues/9006: read results in increments of at most MAX_BATCH_SIZE at a time
            byte[][] result = new byte[keys.length][];

            for (int i = 0; i < keys.length; ) {
                int batchSize = min(keys.length - i, MAX_BATCH_SIZE);

                byte[][] tmp = txn.multiGetForUpdate(readOptions, handles.subList(i, i + batchSize), Arrays.copyOfRange(keys, i, i + batchSize));
                System.arraycopy(tmp, 0, result, i, batchSize);

                i += batchSize;
            }

            return result;
        }
    }

    private final RocksStorageInternal storageInternal;
    private final Transaction transaction;

    private int modificationCounter;

    public RocksStorageTransactionInternal(@NonNull RocksStorageInternal storageInternal) {
        this.storageInternal = storageInternal;

        //save modification counter so that we can check for conflicts
        this.modificationCounter = storageInternal.modificationCounter();

        this.transaction = storageInternal.storage().beginTransaction(WRITE_OPTIONS);
    }

    public void validate() throws FStorageException {
        RocksStorageInternal storageInternal = this.storageInternal;
        storageInternal.ensureOpen();
        if (this.modificationCounter != storageInternal.modificationCounter()) { //something in the internal storage has changed since the transaction began, we can't commit
            throw new FStorageTransactionFailedException();
        }
    }

    @Override
    public void commit() throws FStorageException {
        //acquire read lock on storage to ensure that column families can't be replaced during commit
        this.storageInternal.readLock().lock();
        try {
            this.validate();

            this.transaction.commit();
        } catch (RocksDBException e) {
            throw getExceptionPossiblyCommitFailure(e);
        } finally {
            this.storageInternal.readLock().unlock();
        }
    }

    @Override
    public void rollback() throws FStorageException {
        this.validate();

        try {
            this.transaction.rollback();
        } catch (RocksDBException e) {
            throw new FStorageException("failed to rollback transaction", e);
        }

        //keep modification counter up-to-date
        this.modificationCounter = this.storageInternal.modificationCounter();
    }

    @Override
    public void close() {
        this.transaction.close();
    }

    @Override
    public byte[] get(@NonNull FStorageColumnInternal column, @NonNull byte[] key) throws FStorageException {
        this.validate();

        try {
            return this.transaction.getForUpdate(READ_OPTIONS, ((RocksStorageColumnInternal) column).handle(), key, true);
        } catch (RocksDBException e) {
            throw getExceptionPossiblyCommitFailure(e);
        }
    }

    @Override
    public byte[] get(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull ConflictDetectionLevel conflictDetectionLevel) throws FStorageException {
        this.validate();

        try {
            ColumnFamilyHandle columnFamilyHandle = ((RocksStorageColumnInternal) column).handle();
            switch (conflictDetectionLevel) {
                case UPDATE:
                default:
                    return this.transaction.getForUpdate(READ_OPTIONS, columnFamilyHandle, key, true);
                case NONE:
                    return this.transaction.get(columnFamilyHandle, READ_OPTIONS, key);
            }
        } catch (RocksDBException e) {
            throw getExceptionPossiblyCommitFailure(e);
        }
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<FStorageColumnInternal> columns, @NonNull List<byte[]> keys) throws FStorageException {
        this.validate();

        try {
            return Arrays.asList(multiGetForUpdate(this.transaction, READ_OPTIONS, RocksStorageInternal.toColumnFamilyHandles(columns), keys.toArray(new byte[0][])));
        } catch (RocksDBException e) {
            throw getExceptionPossiblyCommitFailure(e);
        }
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<FStorageColumnInternal> columns, @NonNull List<byte[]> keys, @NonNull ConflictDetectionLevel conflictDetectionLevel) throws FStorageException {
        this.validate();

        try {
            List<ColumnFamilyHandle> columnFamilyHandles = RocksStorageInternal.toColumnFamilyHandles(columns);
            byte[][] keysArray = keys.toArray(new byte[0][]);

            switch (conflictDetectionLevel) {
                case UPDATE:
                default:
                    return Arrays.asList(multiGetForUpdate(this.transaction, READ_OPTIONS, columnFamilyHandles, keysArray));
                case NONE:
                    return Arrays.asList(this.transaction.multiGet(READ_OPTIONS, columnFamilyHandles, keysArray));
            }
        } catch (RocksDBException e) {
            throw getExceptionPossiblyCommitFailure(e);
        }
    }

    @Override
    public void put(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull byte[] value) throws FStorageException {
        this.validate();

        try {
            this.transaction.put(((RocksStorageColumnInternal) column).handle(), key, value);
        } catch (RocksDBException e) {
            throw getExceptionPossiblyCommitFailure(e);
        }
    }

    @Override
    public void put(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull byte[] value, @NonNull ConflictDetectionLevel conflictDetectionLevel) throws FStorageException {
        this.validate();

        try {
            ColumnFamilyHandle columnFamilyHandle = ((RocksStorageColumnInternal) column).handle();
            switch (conflictDetectionLevel) {
                case UPDATE:
                default:
                    this.transaction.put(columnFamilyHandle, key, value);
                    break;
                case NONE:
                    this.transaction.putUntracked(columnFamilyHandle, key, value);
                    break;
            }
        } catch (RocksDBException e) {
            throw getExceptionPossiblyCommitFailure(e);
        }
    }

    @Override
    public void delete(@NonNull FStorageColumnInternal column, @NonNull byte[] key) throws FStorageException {
        this.validate();

        try {
            this.transaction.delete(((RocksStorageColumnInternal) column).handle(), key);
        } catch (RocksDBException e) {
            throw getExceptionPossiblyCommitFailure(e);
        }
    }

    @Override
    public void delete(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull ConflictDetectionLevel conflictDetectionLevel) throws FStorageException {
        this.validate();

        try {
            ColumnFamilyHandle columnFamilyHandle = ((RocksStorageColumnInternal) column).handle();
            switch (conflictDetectionLevel) {
                case UPDATE:
                default:
                    this.transaction.delete(columnFamilyHandle, key);
                    break;
                case NONE:
                    this.transaction.deleteUntracked(columnFamilyHandle, key);
                    break;
            }
        } catch (RocksDBException e) {
            throw getExceptionPossiblyCommitFailure(e);
        }
    }
}
