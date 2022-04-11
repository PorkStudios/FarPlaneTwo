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
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageWriteBatchInternal;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

import static net.daporkchop.fp2.core.storage.rocks.RocksStorage.*;

/**
 * Implementation of {@link FStorageWriteBatchInternal} for {@link RocksStorageInternal}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class RocksStorageWriteBatchInternal extends WriteBatch implements FStorageWriteBatchInternal {
    @NonNull
    private final RocksStorageInternal storageInternal;

    public void validate() throws FStorageException {
        this.storageInternal.ensureOpen();
    }

    @Override
    public void write() throws FStorageException {
        this.validate();

        this.storageInternal.readLock().lock();
        try {
            this.storageInternal.storage().db().write(WRITE_OPTIONS, this);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        } finally {
            this.storageInternal.readLock().unlock();
        }
    }

    @Override
    public void put(@NonNull FStorageColumnInternal column, @NonNull byte[] key, @NonNull byte[] value) throws FStorageException {
        try {
            this.put(((RocksStorageColumnInternal) column).handle(), key, value);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        }
    }

    @Override
    public void delete(@NonNull FStorageColumnInternal column, @NonNull byte[] key) throws FStorageException {
        try {
            this.delete(((RocksStorageColumnInternal) column).handle(), key);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        }
    }
}
