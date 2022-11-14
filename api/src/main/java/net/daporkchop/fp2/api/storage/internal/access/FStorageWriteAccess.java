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

package net.daporkchop.fp2.api.storage.internal.access;

import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.FStorageColumn;
import net.daporkchop.fp2.api.storage.internal.FStorageInternal;

import java.nio.ByteBuffer;

/**
 * Interface defining a set of write operations which span an entire {@link FStorageInternal}.
 *
 * @author DaPorkchop_
 */
public interface FStorageWriteAccess {
    /**
     * Inserts the given key-value pair into the given storage column. If the key was already associated with a value, it will be silently replaced.
     * <p>
     * This is a write operation.
     *
     * @param column the storage column
     * @param key    the key
     * @param value  the value
     * @throws FStorageException if the operation fails
     */
    void put(@NonNull FStorageColumn column, @NonNull byte[] key, @NonNull byte[] value) throws FStorageException;

    /**
     * Inserts the given key-value pair into the given storage column. If the key was already associated with a value, it will be silently replaced.
     * <p>
     * This is a write operation.
     *
     * @param column the storage column
     * @param key    the key
     * @param value  the value
     * @throws FStorageException if the operation fails
     */
    void put(@NonNull FStorageColumn column, @NonNull ByteBuffer key, @NonNull ByteBuffer value) throws FStorageException;

    /**
     * Removes the key-value pair with the given key from the given storage column.
     * <p>
     * This is a write operation.
     *
     * @param column the storage column
     * @param key    the key
     * @throws FStorageException if the operation fails
     */
    void delete(@NonNull FStorageColumn column, @NonNull byte[] key) throws FStorageException;

    /**
     * Removes the key-value pair with the given key from the given storage column.
     * <p>
     * This is a write operation.
     *
     * @param column the storage column
     * @param key    the key
     * @throws FStorageException if the operation fails
     */
    void delete(@NonNull FStorageColumn column, @NonNull ByteBuffer key) throws FStorageException;

    /**
     * Atomically removes all the key-value pair with keys in the given key range from the given storage column.
     * <p>
     * This is a write operation.
     *
     * @param column           the storage column
     * @param fromKeyInclusive the lower bound (inclusive) of the range of keys to delete
     * @param toKeyExclusive   the upper bound (exclusive) of the range of keys to delete
     * @throws FStorageException if the operation fails
     */
    void deleteRange(@NonNull FStorageColumn column, @NonNull byte[] fromKeyInclusive, @NonNull byte[] toKeyExclusive) throws FStorageException;

    /**
     * Atomically removes all the key-value pair with keys in the given key range from the given storage column.
     * <p>
     * This is a write operation.
     *
     * @param column           the storage column
     * @param fromKeyInclusive the lower bound (inclusive) of the range of keys to delete
     * @param toKeyExclusive   the upper bound (exclusive) of the range of keys to delete
     * @throws FStorageException if the operation fails
     */
    void deleteRange(@NonNull FStorageColumn column, @NonNull ByteBuffer fromKeyInclusive, @NonNull ByteBuffer toKeyExclusive) throws FStorageException;
}
