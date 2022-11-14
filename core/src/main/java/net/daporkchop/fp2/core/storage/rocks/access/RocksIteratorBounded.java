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

package net.daporkchop.fp2.core.storage.rocks.access;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.access.FStorageIterator;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.rocksdb.AbstractSlice;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.DirectSlice;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksIterator;
import org.rocksdb.Slice;

import java.nio.ByteBuffer;

/**
 * Implementation of {@link FStorageIterator} which simply delegates to a child {@link RocksIterator} instance, but also allows defining the lower and upper iteration bounds.
 *
 * @author DaPorkchop_
 * @see RocksIteratorDefault
 */
public abstract class RocksIteratorBounded implements FStorageIterator {
    @Getter
    protected final RocksIterator delegate;

    protected final ReadOptions readOptions;
    protected final AbstractSlice<?> fromKeyInclusiveSlice;
    protected final AbstractSlice<?> toKeyExclusiveSlice;

    public RocksIteratorBounded(@NonNull ReadOptions defaultReadOptions, @NonNull ColumnFamilyHandle columnFamily, byte[] fromKeyInclusive, byte[] toKeyExclusive) throws FStorageException {
        //wrap lower and upper bounds into Slices
        this.fromKeyInclusiveSlice = fromKeyInclusive != null ? new Slice(fromKeyInclusive) : null;
        this.toKeyExclusiveSlice = toKeyExclusive != null ? new Slice(toKeyExclusive) : null;
        this.readOptions = new ReadOptions(defaultReadOptions);

        //set lower and upper bounds
        if (this.fromKeyInclusiveSlice != null) {
            this.readOptions.setIterateLowerBound(this.fromKeyInclusiveSlice);
        }
        if (this.toKeyExclusiveSlice != null) {
            this.readOptions.setIterateUpperBound(this.toKeyExclusiveSlice);
        }

        try {
            //actually create the delegate RocksIterator
            this.delegate = this.createDelegate(this.readOptions, columnFamily);
        } catch (Exception e) { //something went wrong, close resources
            this.close();

            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        }
    }

    public RocksIteratorBounded(@NonNull ReadOptions defaultReadOptions, @NonNull ColumnFamilyHandle columnFamily, ByteBuffer fromKeyInclusive, ByteBuffer toKeyExclusive) throws FStorageException {
        //wrap lower and upper bounds into Slices
        this.fromKeyInclusiveSlice = fromKeyInclusive != null ? new DirectSlice(fromKeyInclusive) : null;
        this.toKeyExclusiveSlice = toKeyExclusive != null ? new DirectSlice(toKeyExclusive) : null;
        this.readOptions = new ReadOptions(defaultReadOptions);

        //set lower and upper bounds
        if (this.fromKeyInclusiveSlice != null) {
            this.readOptions.setIterateLowerBound(this.fromKeyInclusiveSlice);
        }
        if (this.toKeyExclusiveSlice != null) {
            this.readOptions.setIterateUpperBound(this.toKeyExclusiveSlice);
        }

        try {
            //actually create the delegate RocksIterator
            this.delegate = this.createDelegate(this.readOptions, columnFamily);
        } catch (Exception e) { //something went wrong, close resources
            this.close();

            PUnsafe.throwException(e); //rethrow exception
            throw new AssertionError(); //impossible
        }
    }

    protected abstract RocksIterator createDelegate(@NonNull ReadOptions options, @NonNull ColumnFamilyHandle columnFamily) throws FStorageException;

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
    public void seekCeil(@NonNull ByteBuffer key) throws FStorageException {
        this.delegate.seek(key);
    }

    @Override
    public void seekFloor(@NonNull byte[] key) throws FStorageException {
        this.delegate.seekForPrev(key);
    }

    @Override
    public void seekFloor(@NonNull ByteBuffer key) throws FStorageException {
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
    public int key(@NonNull ByteBuffer key) throws FStorageException {
        return this.delegate.key(key);
    }

    @Override
    public byte[] value() throws FStorageException {
        return this.delegate.value();
    }

    @Override
    public int value(@NonNull ByteBuffer value) throws FStorageException {
        return this.delegate.value(value);
    }

    @Override
    public void close() throws FStorageException {
        //close iterator if it was set (could be null if createDelegate() threw an exception)
        if (this.delegate != null) {
            this.delegate.close();
        }

        //make sure to close resources as well
        this.readOptions.close();
        if (this.fromKeyInclusiveSlice != null) {
            this.fromKeyInclusiveSlice.close();
        }
        if (this.toKeyExclusiveSlice != null) {
            this.toKeyExclusiveSlice.close();
        }
    }
}
