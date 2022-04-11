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

package net.daporkchop.fp2.api.storage.internal;

import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.util.CloseableResource;

import java.util.List;

/**
 * A transaction allows buffering a sequence of operations (retrievals/insertions/removals) which can then be applied atomically. Transactions are allowed to span multiple
 * {@link FStorageColumnInternal internal storage columns}.
 * <p>
 * To ensure that the transaction is applied atomically, transactions perform conflict detection. This involves keeping track of keys accessed by the transaction, and failing
 * the transaction if any of the keys are modified externally before the transaction could be completed. Failure due to conflict detection can occur at any time while operating
 * on a transaction, and is indicated by a {@link FStorageTransactionFailedException} being thrown. In such a case, the transaction should be re-started after either
 * {@link #rollback() rolling back} the existing transaction instance, or {@link #close() closing} it and beginning a new one.
 *
 * @author DaPorkchop_
 */
public interface FStorageTransactionInternal extends FStorageOperationsInternal, CloseableResource {
    /**
     * Attempts to commit this transaction.
     * <p>
     * This will atomically perform all of the pending write operations if no conflicts are detected before resetting the transaction will be reset to its initial state.
     */
    void commit() throws FStorageException;

    /**
     * Resets this transaction to its initial state.
     * <p>
     * This will discard any uncommitted writes and release any other keys owned by this transaction.
     */
    void rollback() throws FStorageException;

    //
    // READ OPERATIONS
    //

    @Override
    default byte[] get(@NonNull FStorageColumnInternal column, @NonNull byte[] key) throws FStorageException {
        return this.get(column, key, ConflictDetectionLevel.UPDATE);
    }

    /**
     * @param conflictDetectionLevel how conflict detection should be performed for the key
     * @see #get(FStorageColumnInternal, byte[])
     */
    byte[] get(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull ConflictDetectionLevel conflictDetectionLevel) throws FStorageException;

    @Override
    default List<byte[]> multiGet(@NonNull List<FStorageColumnInternal> columns, @NonNull List<byte[]> keys) throws FStorageException {
        return this.multiGet(columns, keys, ConflictDetectionLevel.UPDATE);
    }

    /**
     * @param conflictDetectionLevel how conflict detection should be performed for the keys
     * @see #multiGet(List, List)
     */
    List<byte[]> multiGet(@NonNull List<FStorageColumnInternal> columns, @NonNull List<byte[]> keys, @NonNull ConflictDetectionLevel conflictDetectionLevel) throws FStorageException;

    //
    // WRITE OPERATIONS
    //

    @Override
    default void put(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull byte[] value) throws FStorageException {
        this.put(column, key, value, ConflictDetectionLevel.UPDATE);
    }

    /**
     * @param conflictDetectionLevel how conflict detection should be performed for the key
     * @see #put(FStorageColumnInternal, byte[], byte[])
     */
    void put(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull byte[] value, @NonNull ConflictDetectionLevel conflictDetectionLevel) throws FStorageException;

    @Override
    default void delete(@NonNull FStorageColumnInternal column, @NonNull byte[] key) throws FStorageException {
        this.delete(column, key, ConflictDetectionLevel.UPDATE);
    }

    /**
     * @param conflictDetectionLevel how conflict detection should be performed for the key
     * @see #delete(FStorageColumnInternal, byte[])
     */
    void delete(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull ConflictDetectionLevel conflictDetectionLevel) throws FStorageException;

    /**
     * Defines how conflict detection should be done for key(s) used by a specific read or write operation.
     *
     * @author DaPorkchop_
     */
    enum ConflictDetectionLevel {
        /**
         * Treats the operation as an update. The transaction will fail if the key(s) is modified before the transaction is committed.
         * <p>
         * This is the default conflict detection level if none is explicitly specified.
         */
        UPDATE,
        /**
         * Provides a hint to the implementation that conflict detection should not be performed on the key(s).
         * <p>
         * For reads, this means that the read would behave as an ordinary (non-transactional) read. For writes, it means that external writes to the same key(s) are not guaranteed
         * to cause the transaction to fail, although the write operation will still not be performed if conflicts are detected on any other key(s).
         * <p>
         * Note that this is only a hint, the implementation may still choose to use a stricter conflict detection level.
         */
        NONE,
    }
}
