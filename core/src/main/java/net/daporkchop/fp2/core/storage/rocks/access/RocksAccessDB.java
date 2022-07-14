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
import net.daporkchop.fp2.api.storage.internal.FStorageColumn;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageIterator;
import net.daporkchop.fp2.core.storage.rocks.RocksStorageColumn;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.util.List;

import static net.daporkchop.fp2.core.storage.rocks.RocksStorage.*;

/**
 * Implements {@link FStorageAccess} by simply wrapping a {@link RocksDB}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class RocksAccessDB implements FStorageAccess {
    @NonNull
    protected final RocksDB db;
    @NonNull
    protected final ReadOptions readOptions;

    //
    // FStorageReadAccess
    //

    @Override
    public byte[] get(@NonNull FStorageColumn column, @NonNull byte[] key) throws FStorageException {
        try {
            return this.db.get(((RocksStorageColumn) column).handle(), this.readOptions, key);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<FStorageColumn> columns, @NonNull List<byte[]> keys) throws FStorageException {
        try {
            return this.db.multiGetAsList(this.readOptions, RocksStorageColumn.toColumnFamilyHandles(columns), keys);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public FStorageIterator iterator(@NonNull FStorageColumn column) throws FStorageException {
        return new RocksIteratorDefault(this.db.newIterator(((RocksStorageColumn) column).handle(), this.readOptions));
    }

    @Override
    public FStorageIterator iterator(@NonNull FStorageColumn column, byte[] fromKeyInclusive, byte[] toKeyExclusive) throws FStorageException {
        if (fromKeyInclusive == null && toKeyExclusive == null) { //both lower and upper bounds are null, create a regular iterator
            return this.iterator(column);
        }

        return new RocksIteratorBounded(this.readOptions, ((RocksStorageColumn) column).handle(), fromKeyInclusive, toKeyExclusive) {
            @Override
            protected RocksIterator createDelegate(@NonNull ReadOptions options, @NonNull ColumnFamilyHandle columnFamily) throws FStorageException {
                return RocksAccessDB.this.db.newIterator(columnFamily, options);
            }
        };
    }

    //
    // FStorageWriteAccess
    //

    @Override
    public void put(@NonNull FStorageColumn column, @NonNull byte[] key, @NonNull byte[] value) throws FStorageException {
        do {
            try {
                this.db.put(((RocksStorageColumn) column).handle(), WRITE_OPTIONS, key, value);
                return;
            } catch (RocksDBException e) {
                if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw wrapException(e);
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    @Override
    public void delete(@NonNull FStorageColumn column, @NonNull byte[] key) throws FStorageException {
        do {
            try {
                this.db.delete(((RocksStorageColumn) column).handle(), WRITE_OPTIONS, key);
                return;
            } catch (RocksDBException e) {
                if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw wrapException(e);
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    @Override
    public void deleteRange(@NonNull FStorageColumn column, @NonNull byte[] fromKeyInclusive, @NonNull byte[] toKeyExclusive) throws FStorageException {
        do {
            try {
                this.db.deleteRange(((RocksStorageColumn) column).handle(), WRITE_OPTIONS, fromKeyInclusive, toKeyExclusive);
                return;
            } catch (RocksDBException e) {
                if (!isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw wrapException(e);
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }
}
