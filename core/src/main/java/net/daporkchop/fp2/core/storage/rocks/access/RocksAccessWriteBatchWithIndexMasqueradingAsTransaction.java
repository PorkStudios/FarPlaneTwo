/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
import net.daporkchop.fp2.api.storage.internal.FStorageColumn;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageIterator;
import net.daporkchop.fp2.core.storage.rocks.RocksStorageColumn;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Status;
import org.rocksdb.WriteBatchWithIndex;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static net.daporkchop.fp2.core.storage.rocks.RocksStorage.*;

/**
 * Implements {@link FStorageAccess} by simultaneously extending {@link WriteBatchWithIndex} and wrapping a {@link RocksDB}, providing the illusion of being a transaction
 * for {@link RocksDB}s which do not support transactions.
 * <p>
 * Assumes that synchronization is handled externally.
 *
 * @author DaPorkchop_
 */
//TODO: this could be optimized better, but WriteBatchWithIndex doesn't support ByteBuffer methods
@Getter
public class RocksAccessWriteBatchWithIndexMasqueradingAsTransaction extends WriteBatchWithIndex implements FStorageAccess, ArrayOnlyFStorageAccess {
    protected final RocksDB db;
    protected final ReadOptions readOptions;

    public RocksAccessWriteBatchWithIndexMasqueradingAsTransaction(@NonNull RocksDB db, @NonNull ReadOptions readOptions) {
        super(true); //set overwrite_key=true to prevent reading

        this.db = db;
        this.readOptions = readOptions;
    }

    //
    // FStorageReadAccess
    //

    @Override
    public byte[] get(@NonNull FStorageColumn column, @NonNull byte[] key) throws FStorageException {
        try {
            return super.getFromBatchAndDB(this.db, ((RocksStorageColumn) column).handle(), this.readOptions, key);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<FStorageColumn> columns, @NonNull List<byte[]> keys) throws FStorageException {
        try { //WriteBatchWithIndex doesn't provide any multiGet() methods, so we'll fall back to getting each element individually
            //TODO: this could be optimized by breaking it up into two passes: one which tries to get from WriteBatchWithIndex, then fall back to a single multiGet() for
            //      any entries which weren't present
            byte[][] result = new byte[keys.size()][];

            for (int i = 0; i < result.length; i++) {
                result[i] = super.getFromBatchAndDB(this.db, ((RocksStorageColumn) columns.get(i)).handle(), this.readOptions, keys.get(i));
            }

            return Arrays.asList(result);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public FStorageIterator iterator(@NonNull FStorageColumn column) throws FStorageException {
        ColumnFamilyHandle handle = ((RocksStorageColumn) column).handle();
        return new RocksIteratorDefault(super.newIteratorWithBase(handle, this.db.newIterator(handle, this.readOptions), this.readOptions));
    }

    @Override
    public FStorageIterator iterator(@NonNull FStorageColumn column, byte[] fromKeyInclusive, byte[] toKeyExclusive) throws FStorageException {
        if (fromKeyInclusive == null && toKeyExclusive == null) { //both lower and upper bounds are null, create a regular iterator
            return this.iterator(column);
        }

        return new RocksIteratorBounded(this.readOptions, ((RocksStorageColumn) column).handle(), fromKeyInclusive, toKeyExclusive) {
            @Override
            protected RocksIterator createDelegate(@NonNull ReadOptions options, @NonNull ColumnFamilyHandle columnFamily) throws FStorageException {
                return RocksAccessWriteBatchWithIndexMasqueradingAsTransaction.super.newIteratorWithBase(
                        columnFamily,
                        RocksAccessWriteBatchWithIndexMasqueradingAsTransaction.this.db.newIterator(columnFamily, options),
                        options);
            }
        };
    }

    @Override
    public FStorageIterator iterator(@NonNull FStorageColumn column, ByteBuffer fromKeyInclusive, ByteBuffer toKeyExclusive) throws FStorageException {
        if (fromKeyInclusive == null && toKeyExclusive == null) { //both lower and upper bounds are null, create a regular iterator
            return this.iterator(column);
        }

        return new RocksIteratorBounded(this.readOptions, ((RocksStorageColumn) column).handle(), fromKeyInclusive, toKeyExclusive) {
            @Override
            protected RocksIterator createDelegate(@NonNull ReadOptions options, @NonNull ColumnFamilyHandle columnFamily) throws FStorageException {
                return RocksAccessWriteBatchWithIndexMasqueradingAsTransaction.super.newIteratorWithBase(
                        columnFamily,
                        RocksAccessWriteBatchWithIndexMasqueradingAsTransaction.this.db.newIterator(columnFamily, options),
                        options);
            }
        };
    }

    //
    // FStorageWriteAccess
    //

    @Override
    public void put(@NonNull FStorageColumn column, @NonNull byte[] key, @NonNull byte[] value) throws FStorageException {
        try {
            this.put(((RocksStorageColumn) column).handle(), key, value);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public void put(@NonNull FStorageColumn column, @NonNull ByteBuffer key, @NonNull ByteBuffer value) throws FStorageException {
        try {
            this.put(((RocksStorageColumn) column).handle(), key, value);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public void delete(@NonNull FStorageColumn column, @NonNull byte[] key) throws FStorageException {
        try {
            this.delete(((RocksStorageColumn) column).handle(), key);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public void delete(@NonNull FStorageColumn column, @NonNull ByteBuffer key) throws FStorageException {
        try {
            this.delete(((RocksStorageColumn) column).handle(), key);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public void deleteRange(@NonNull FStorageColumn column, @NonNull byte[] fromKeyInclusive, @NonNull byte[] toKeyExclusive) throws FStorageException {
        try {
            this.deleteRange(((RocksStorageColumn) column).handle(), fromKeyInclusive, toKeyExclusive);
        } catch (RocksDBException e) {
            if (e.getStatus().getCode() == Status.Code.NotSupported) {
                //we can't actually do a deleteRange in a transaction, which is unfortunate. fall back to iterating over the range and deleting every key, which is probably close enough,
                //  although technically not perfectly atomic.

                try (FStorageIterator itr = this.iterator(column, fromKeyInclusive, toKeyExclusive)) {
                    for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                        this.delete(column, itr.key());
                    }
                }
                return;
            }
            throw wrapException(e);
        }
    }
}
