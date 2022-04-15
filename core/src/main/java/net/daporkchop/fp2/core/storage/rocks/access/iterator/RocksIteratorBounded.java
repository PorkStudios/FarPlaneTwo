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

package net.daporkchop.fp2.core.storage.rocks.access.iterator;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Slice;

/**
 * Implementation of {@link IRocksIterator} which simply delegates to a child {@link RocksIterator} instance, but also allows defining the lower and upper iteration bounds.
 *
 * @author DaPorkchop_
 */
public abstract class RocksIteratorBounded implements IRocksIterator {
    @Getter
    protected final RocksIterator delegate;

    protected final ReadOptions readOptions;
    protected final Slice fromKeyInclusiveSlice;
    protected final Slice toKeyExclusiveSlice;

    public RocksIteratorBounded(@NonNull ReadOptions defaultReadOptions, @NonNull ColumnFamilyHandle columnFamily, byte[] fromKeyInclusive, byte[] toKeyExclusive) throws RocksDBException {
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

    protected abstract RocksIterator createDelegate(@NonNull ReadOptions options, @NonNull ColumnFamilyHandle columnFamily) throws RocksDBException;

    @Override
    public boolean isValid() throws RocksDBException {
        return this.delegate.isValid();
    }

    @Override
    public void seekToFirst() throws RocksDBException {
        this.delegate.seekToFirst();
    }

    @Override
    public void seekToLast() throws RocksDBException {
        this.delegate.seekToLast();
    }

    @Override
    public void seekCeil(@NonNull byte[] key) throws RocksDBException {
        this.delegate.seek(key);
    }

    @Override
    public void seekFloor(@NonNull byte[] key) throws RocksDBException {
        this.delegate.seekForPrev(key);
    }

    @Override
    public void next() throws RocksDBException {
        this.delegate.next();
    }

    @Override
    public void prev() throws RocksDBException {
        this.delegate.prev();
    }

    @Override
    public byte[] key() throws RocksDBException {
        return this.delegate.key();
    }

    @Override
    public byte[] value() throws RocksDBException {
        return this.delegate.value();
    }

    @Override
    public void close() throws RocksDBException {
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
