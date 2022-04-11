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
import net.daporkchop.fp2.api.storage.external.FStorageItem;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Provides access to operations which are not specific to a single {@link FStorageColumnInternal internal storage column}.
 *
 * @author DaPorkchop_
 */
public interface FStorageInternal extends FStorageReadOperationsInternal, AutoCloseable {
    /**
     * @return a new {@link FStorageWriteBatchInternal write batch}
     */
    FStorageWriteBatchInternal beginWriteBatch();

    /**
     * @return a new {@link FStorageTransactionInternal transaction}
     */
    FStorageTransactionInternal beginTransaction();

    /**
     * @return a new {@link FStorageSnapshotInternal snapshot}
     */
    FStorageSnapshotInternal snapshot();

    /**
     * Closes this storage.
     * <p>
     * Should only be called by {@link FStorageItem#close()}.
     */
    @Override
    void close() throws FStorageException;

    //
    // TOKEN
    //

    /**
     * @return the current token data, if any
     */
    Optional<byte[]> getToken();

    /**
     * Sets the token data.
     *
     * @param token the new token data
     */
    void setToken(@NonNull byte[] token) throws FStorageException;

    /**
     * Removes the token data.
     */
    void removeToken() throws FStorageException;

    //
    // COLUMN MANAGEMENT
    //

    /**
     * @return a {@link Map} containing all of the columns in this storage
     */
    Map<String, FStorageColumnInternal> getColumns();

    /**
     * Clears the given column, removing all entries from it. The operation is performed atomically.
     *
     * @param column the column to clear
     */
    default void clearColumn(@NonNull FStorageColumnInternal column) throws FStorageException {
        this.clearColumns(Collections.singleton(column));
    }

    /**
     * Clears the given columns, removing all entries from them. The operation is performed atomically.
     *
     * @param columns the columns to clear
     */
    void clearColumns(@NonNull Collection<FStorageColumnInternal> columns) throws FStorageException;
}
