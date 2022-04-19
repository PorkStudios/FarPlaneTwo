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
import net.daporkchop.fp2.core.storage.rocks.access.iterator.IRocksIterator;
import net.daporkchop.fp2.core.storage.rocks.access.iterator.RocksIteratorBounded;
import net.daporkchop.fp2.core.storage.rocks.access.iterator.RocksIteratorDefault;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Snapshot;
import org.rocksdb.Transaction;

import java.util.Arrays;
import java.util.List;

import static net.daporkchop.fp2.core.storage.rocks.RocksStorage.*;

/**
 * Implements {@link IRocksAccess} by simply wrapping a {@link Transaction}.
 *
 * @author DaPorkchop_
 */
@Getter
public class RocksAccessTransaction implements IRocksAccess, AutoCloseable {
    protected final Transaction transaction;

    protected final ReadOptions readOptions;

    public RocksAccessTransaction(@NonNull Transaction transaction) {
        this.transaction = transaction;

        Snapshot snapshot = transaction.getSnapshot();
        if (snapshot != null) { //the transaction has an associated snapshot, let's create a private ReadOptions instance and configure it accordingly!
            //even when using a ReadOptions with a snapshot configured, the transaction's ability to read-your-own-write seems to override it, so the snapshot is only used when accessing
            // keys not already written by the transaction
            this.readOptions = new ReadOptions(READ_OPTIONS);
            this.readOptions.setSnapshot(snapshot);
        } else {
            this.readOptions = READ_OPTIONS;
        }
    }

    @Override
    public byte[] get(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        return conflictDetectionHint == RocksConflictDetectionHint.NONE
                ? this.transaction.get(columnFamily, this.readOptions, key)
                : this.transaction.getForUpdate(this.readOptions, columnFamily, key, conflictDetectionHint != RocksConflictDetectionHint.SHARED);
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<ColumnFamilyHandle> columnFamilies, @NonNull List<byte[]> keys, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        byte[][] keysArray = keys.toArray(new byte[0][]);

        return Arrays.asList(conflictDetectionHint == RocksConflictDetectionHint.NONE
                ? this.transaction.multiGet(this.readOptions, columnFamilies, keysArray)
                : this.transaction.multiGetForUpdate(this.readOptions, columnFamilies, keysArray)); //assume EXCLUSIVE
    }

    @Override
    public IRocksIterator iterator(@NonNull ColumnFamilyHandle columnFamily) throws RocksDBException {
        return new RocksIteratorDefault(this.transaction.getIterator(this.readOptions, columnFamily));
    }

    @Override
    public IRocksIterator iterator(@NonNull ColumnFamilyHandle columnFamily, byte[] fromKeyInclusive, byte[] toKeyExclusive) throws RocksDBException {
        if (fromKeyInclusive == null && toKeyExclusive == null) { //both lower and upper bounds are null, create a regular iterator
            return this.iterator(columnFamily);
        }

        return new RocksIteratorBounded(this.readOptions, columnFamily, fromKeyInclusive, toKeyExclusive) {
            @Override
            protected RocksIterator createDelegate(@NonNull ReadOptions options, @NonNull ColumnFamilyHandle columnFamily) throws RocksDBException {
                return RocksAccessTransaction.this.transaction.getIterator(options, columnFamily);
            }

            @Override
            public boolean isValid() throws RocksDBException {
                if (!super.isValid()) { //if rocksdb itself reports that the iterator isn't valid, then it certainly isn't valid in any case
                    return false;
                }

                //rocksdb reports that the iterator is valid, but let's manually check to make sure that current key is within the requested iteration range.
                //  workaround for https://github.com/facebook/rocksdb/issues/2343
                byte[] key = this.key();
                return (fromKeyInclusive == null || LEX_BYTES_COMPARATOR.compare(key, fromKeyInclusive) >= 0)
                       && (toKeyExclusive == null || LEX_BYTES_COMPARATOR.compare(key, toKeyExclusive) < 0);
            }
        };
    }

    @Override
    public void put(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key, @NonNull byte[] value, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        if (conflictDetectionHint == RocksConflictDetectionHint.NONE) {
            this.transaction.putUntracked(columnFamily, key, value);
        } else {
            this.transaction.put(columnFamily, key, value); //assume EXCLUSIVE
        }
    }

    @Override
    public void delete(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        if (conflictDetectionHint == RocksConflictDetectionHint.NONE) {
            this.transaction.deleteUntracked(columnFamily, key);
        } else {
            this.transaction.delete(columnFamily, key); //assume EXCLUSIVE
        }
    }

    @Override
    public void deleteRange(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] fromKeyInclusive, @NonNull byte[] toKeyExclusive, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException {
        //we can't actually do a deleteRange in a transaction, which is unfortunate. fall back to iterating over the range and deleting every key, which is probably close enough,
        //  although technically not perfectly atomic.

        try (IRocksIterator itr = this.iterator(columnFamily, fromKeyInclusive, toKeyExclusive)) {
            for (itr.seekToFirst(); itr.isValid(); ) {
                this.delete(columnFamily, itr.key(), conflictDetectionHint);
            }
        }
    }

    @Override
    public void close() {
        if (this.readOptions != READ_OPTIONS) { //we created a private ReadOptions instance, release it
            this.readOptions.close();

            //snapshot doesn't need to be closed because it's owned by the transaction
        }
    }
}
