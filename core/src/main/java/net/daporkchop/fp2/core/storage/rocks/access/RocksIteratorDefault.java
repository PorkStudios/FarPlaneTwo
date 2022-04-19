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

package net.daporkchop.fp2.core.storage.rocks.access;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.access.FStorageIterator;
import org.rocksdb.RocksIterator;

/**
 * Implementation of {@link FStorageIterator} which simply delegates to a child {@link RocksIterator} instance.
 *
 * @author DaPorkchop_
 * @see RocksIteratorBounded
 */
@RequiredArgsConstructor
@Getter
public class RocksIteratorDefault implements FStorageIterator {
    @NonNull
    protected final RocksIterator delegate;

    @Override
    public boolean isValid() throws FStorageException {
        return this.delegate.isValid();
    }

    @Override
    public void seekToFirst() throws FStorageException {
        this.delegate.seekToFirst();
    }

    @Override
    public void seekToLast() throws FStorageException {
        this.delegate.seekToLast();
    }

    @Override
    public void seekCeil(@NonNull byte[] key) throws FStorageException {
        this.delegate.seek(key);
    }

    @Override
    public void seekFloor(@NonNull byte[] key) throws FStorageException {
        this.delegate.seekForPrev(key);
    }

    @Override
    public void next() throws FStorageException {
        this.delegate.next();
    }

    @Override
    public void prev() throws FStorageException {
        this.delegate.prev();
    }

    @Override
    public byte[] key() throws FStorageException {
        return this.delegate.key();
    }

    @Override
    public byte[] value() throws FStorageException {
        return this.delegate.value();
    }

    @Override
    public void close() throws FStorageException {
        this.delegate.close();
    }
}
