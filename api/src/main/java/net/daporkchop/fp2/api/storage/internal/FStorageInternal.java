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
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageReadAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageWriteAccess;
import net.daporkchop.fp2.api.util.function.ThrowingConsumer;
import net.daporkchop.fp2.api.util.function.ThrowingFunction;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Provides access to operations which are not specific to a single {@link FStorageColumn internal storage column}.
 *
 * @author DaPorkchop_
 */
public interface FStorageInternal extends AutoCloseable {
    /**
     * Closes this storage.
     * <p>
     * Must only be called by {@link FStorageItem#close()}.
     */
    @Override
    void close() throws FStorageException;

    /**
     * Runs the given function with a {@link FStorageReadAccess} providing read-only access to this storage's data.
     * <p>
     * The read operations performed using the {@link FStorageReadAccess} passed to the function are not necessarily performed atomically. Modifications performed
     * while the function is being executed may be visible.
     * <p>
     * Any {@link FStorageException}s thrown inside the function must be re-thrown without modification, as they may be treated specially by the implementation.
     * <p>
     * Note that the implementation may choose to invoke the function more than once. The user code must be able to handle this, and should treat each invocation as if it were the first.
     *
     * @param action the function
     */
    default void readRun(@NonNull ThrowingConsumer<? super FStorageReadAccess, ? extends FStorageException> action) throws FStorageException {
        this.readAtomicGet(access -> {
            action.acceptThrowing(access);
            return null;
        });
    }

    /**
     * Runs the given function with a {@link FStorageReadAccess} providing read-only access to this storage's data and returns the result.
     * <p>
     * The read operations performed using the {@link FStorageReadAccess} passed to the function are not necessarily performed atomically. Modifications performed
     * while the function is being executed may be visible.
     * <p>
     * Any {@link FStorageException}s thrown inside the function must be re-thrown without modification, as they may be treated specially by the implementation.
     * <p>
     * Note that the implementation may choose to invoke the function more than once. The user code must be able to handle this, and should treat each invocation as if it were the first.
     *
     * @param action the function
     * @return the function's return value
     */
    <R> R readGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action) throws FStorageException;

    /**
     * Runs the given function with a {@link FStorageReadAccess} providing read-only access to this storage's data.
     * <p>
     * The read operations performed using the {@link FStorageReadAccess} passed to the function are performed atomically. Modifications performed while the function
     * is being executed will not be visible.
     * <p>
     * Any {@link FStorageException}s thrown inside the function must be re-thrown without modification, as they may be treated specially by the implementation.
     * <p>
     * Note that the implementation may choose to invoke the function more than once. The user code must be able to handle this, and should treat each invocation as if it were the first.
     *
     * @param action the function
     */
    default void readAtomicRun(@NonNull ThrowingConsumer<? super FStorageReadAccess, ? extends FStorageException> action) throws FStorageException {
        this.readAtomicGet(access -> {
            action.acceptThrowing(access);
            return null;
        });
    }

    /**
     * Runs the given function with a {@link FStorageReadAccess} providing read-only access to this storage's data and returns the result.
     * <p>
     * The read operations performed using the {@link FStorageReadAccess} passed to the function are performed atomically. Modifications performed while the function
     * is being executed will not be visible.
     * <p>
     * Any {@link FStorageException}s thrown inside the function must be re-thrown without modification, as they may be treated specially by the implementation.
     * <p>
     * Note that the implementation may choose to invoke the function more than once. The user code must be able to handle this, and should treat each invocation as if it were the first.
     *
     * @param action the function
     * @return the function's return value
     */
    <R> R readAtomicGet(@NonNull ThrowingFunction<? super FStorageReadAccess, ? extends R, ? extends FStorageException> action) throws FStorageException;

    /**
     * Runs the given function with a {@link FStorageWriteAccess} providing write-only access to this storage's data.
     * <p>
     * The write operations performed using the {@link FStorageWriteAccess} passed to the function are performed atomically.
     * <p>
     * Any {@link FStorageException}s thrown inside the function must be re-thrown without modification, as they may be treated specially by the implementation.
     * <p>
     * Note that the implementation may choose to invoke the function more than once. The user code must be able to handle this, and should treat each invocation as if it were the first.
     *
     * @param action the function
     */
    default void writeAtomicRun(@NonNull ThrowingConsumer<? super FStorageWriteAccess, ? extends FStorageException> action) throws FStorageException {
        this.writeAtomicGet(access -> {
            action.acceptThrowing(access);
            return null;
        });
    }

    /**
     * Runs the given function with a {@link FStorageWriteAccess} providing write-only access to this storage's data and returns the result.
     * <p>
     * The write operations performed using the {@link FStorageWriteAccess} passed to the function are performed atomically.
     * <p>
     * Any {@link FStorageException}s thrown inside the function must be re-thrown without modification, as they may be treated specially by the implementation.
     * <p>
     * Note that the implementation may choose to invoke the function more than once. The user code must be able to handle this, and should treat each invocation as if it were the first.
     *
     * @param action the function
     * @return the function's return value
     */
    <R> R writeAtomicGet(@NonNull ThrowingFunction<? super FStorageWriteAccess, ? extends R, ? extends FStorageException> action) throws FStorageException;

    /**
     * Runs the given function with a {@link FStorageAccess} providing read and write access to this storage's data and returns the result.
     * <p>
     * The read and write operations performed using the {@link FStorageAccess} passed to the function are performed atomically.
     * <p>
     * Any {@link FStorageException}s thrown inside the function must be re-thrown without modification, as they may be treated specially by the implementation.
     * <p>
     * Note that the implementation may choose to invoke the function more than once. The user code must be able to handle this, and should treat each invocation as if it were the first.
     *
     * @param action the function
     */
    default void transactAtomicRun(@NonNull ThrowingConsumer<? super FStorageAccess, ? extends FStorageException> action) throws FStorageException {
        this.transactAtomicGet(access -> {
            action.acceptThrowing(access);
            return null;
        });
    }

    /**
     * Runs the given function with a {@link FStorageAccess} providing read and write access to this storage's data and returns the result.
     * <p>
     * The read and write operations performed using the {@link FStorageAccess} passed to the function are performed atomically.
     * <p>
     * Any {@link FStorageException}s thrown inside the function must be re-thrown without modification, as they may be treated specially by the implementation.
     * <p>
     * Note that the implementation may choose to invoke the function more than once. The user code must be able to handle this, and should treat each invocation as if it were the first.
     *
     * @param action the function
     * @return the function's return value
     */
    <R> R transactAtomicGet(@NonNull ThrowingFunction<? super FStorageAccess, ? extends R, ? extends FStorageException> action) throws FStorageException;

    //
    // TOKEN
    //

    /**
     * @return the current token data, if any
     */
    Optional<byte[]> getToken() throws FStorageException;

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
    Map<String, FStorageColumn> getColumns();

    /**
     * Clears the given column, removing all entries from it. The operation is performed atomically.
     *
     * @param column the column to clear
     */
    default void clearColumn(@NonNull FStorageColumn column) throws FStorageException {
        this.clearColumns(Collections.singleton(column));
    }

    /**
     * Clears the given columns, removing all entries from them. The operation is performed atomically.
     *
     * @param columns the columns to clear
     */
    void clearColumns(@NonNull Collection<FStorageColumn> columns) throws FStorageException;
}
