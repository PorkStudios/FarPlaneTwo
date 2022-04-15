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
import net.daporkchop.fp2.core.storage.rocks.RocksStorage;
import net.daporkchop.fp2.core.storage.rocks.access.iterator.IRocksIterator;
import net.daporkchop.fp2.core.storage.rocks.access.iterator.RocksIteratorBounded;
import net.daporkchop.fp2.core.storage.rocks.access.iterator.RocksIteratorDefault;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.util.List;

import static net.daporkchop.fp2.core.storage.rocks.RocksStorage.*;

/**
 * Implements {@link IRocksAccess} by simply wrapping a {@link RocksDB}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class RocksAccessDB implements IRocksAccess {
    @NonNull
    protected final RocksDB db;

    @Override
    public byte[] get(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key) throws RocksDBException {
        return this.db.get(columnFamily, READ_OPTIONS, key);
    }

    @Override
    public byte[] get(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        return this.get(columnFamily, key);
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<ColumnFamilyHandle> columnFamilies, @NonNull List<byte[]> keys) throws RocksDBException {
        return this.db.multiGetAsList(READ_OPTIONS, columnFamilies, keys);
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<ColumnFamilyHandle> columnFamilies, @NonNull List<byte[]> keys, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        return this.multiGet(columnFamilies, keys);
    }

    @Override
    public IRocksIterator iterator(@NonNull ColumnFamilyHandle columnFamily) throws RocksDBException {
        return new RocksIteratorDefault(this.db.newIterator(columnFamily, READ_OPTIONS));
    }

    @Override
    public IRocksIterator iterator(@NonNull ColumnFamilyHandle columnFamily, byte[] fromKeyInclusive, byte[] toKeyExclusive) throws RocksDBException {
        if (fromKeyInclusive == null && toKeyExclusive == null) { //both lower and upper bounds are null, create a regular iterator
            return this.iterator(columnFamily);
        }

        return new RocksIteratorBounded(READ_OPTIONS, columnFamily, fromKeyInclusive, toKeyExclusive) {
            @Override
            protected RocksIterator createDelegate(@NonNull ReadOptions options, @NonNull ColumnFamilyHandle columnFamily) throws RocksDBException {
                return RocksAccessDB.this.db.newIterator(columnFamily, options);
            }
        };
    }

    @Override
    public void put(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key, @NonNull byte[] value) throws RocksDBException {
        do {
            try {
                this.db.put(columnFamily, WRITE_OPTIONS, key, value);
                return;
            } catch (RocksDBException e) {
                if (!RocksStorage.isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw e;
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    @Override
    public void put(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key, @NonNull byte[] value, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        this.put(columnFamily, key, value);
    }

    @Override
    public void delete(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key) throws RocksDBException {
        do {
            try {
                this.db.delete(columnFamily, WRITE_OPTIONS, key);
                return;
            } catch (RocksDBException e) {
                if (!RocksStorage.isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw e;
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    @Override
    public void delete(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        this.delete(columnFamily, key);
    }

    @Override
    public void deleteRange(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] fromKeyInclusive, @NonNull byte[] toKeyExclusive) throws RocksDBException {
        do {
            try {
                this.db.deleteRange(columnFamily, WRITE_OPTIONS, fromKeyInclusive, toKeyExclusive);
                return;
            } catch (RocksDBException e) {
                if (!RocksStorage.isTransactionCommitFailure(e)) { //"regular" exception, rethrow
                    throw e;
                }

                //the database is transactional and there was a commit failure, try again until it works!
            }
        } while (true);
    }

    @Override
    public void deleteRange(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] fromKeyInclusive, @NonNull byte[] toKeyExclusive, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        this.deleteRange(columnFamily, fromKeyInclusive, toKeyExclusive);
    }
}
