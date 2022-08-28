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
import net.daporkchop.fp2.api.storage.internal.FStorageColumn;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageIterator;
import net.daporkchop.fp2.core.storage.rocks.RocksStorageColumn;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Snapshot;
import org.rocksdb.Transaction;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.storage.rocks.RocksStorage.*;

/**
 * Implements {@link FStorageAccess} by simply wrapping a {@link Transaction}.
 *
 * @author DaPorkchop_
 */
@Getter
public class RocksAccessTransaction implements FStorageAccess, AutoCloseable {
    private static byte[] toByteArray(ByteBuffer buffer) {
        byte[] arr = new byte[buffer.remaining()];
        int position = buffer.position();
        buffer.get(arr);
        buffer.position(position);
        return arr;
    }

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
    public void close() {
        if (this.readOptions != READ_OPTIONS) { //we created a private ReadOptions instance, release it
            this.readOptions.close();

            //snapshot doesn't need to be closed because it's owned by the transaction
        }
    }

    //
    // FStorageReadAccess
    //

    @Override
    public byte[] get(@NonNull FStorageColumn column, @NonNull byte[] key) throws FStorageException {
        try {
            return this.transaction.getForUpdate(this.readOptions, ((RocksStorageColumn) column).handle(), key, false);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public int get(@NonNull FStorageColumn column, @NonNull ByteBuffer key, @NonNull ByteBuffer value) throws FStorageException {
        try {
            //workaround for https://github.com/facebook/rocksdb/issues/10322: we have to copy key into a heap buffer, then get it and copy the result back
            byte[] valueArray = this.transaction.getForUpdate(this.readOptions, ((RocksStorageColumn) column).handle(), toByteArray(key), false);
            if (valueArray != null) { //found
                value.put(valueArray, 0, min(value.remaining(), valueArray.length));
                return valueArray.length;
            } else { //not found
                return -1;
            }
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<FStorageColumn> columns, @NonNull List<byte[]> keys) throws FStorageException {
        try {
            List<ColumnFamilyHandle> handles = RocksStorageColumn.toColumnFamilyHandles(columns);
            byte[][] keysArray = keys.toArray(new byte[0][]);

            byte[][] result;

            final int MAX_BATCH_SIZE = 65536;
            if (keysArray.length <= MAX_BATCH_SIZE) {
                result = this.transaction.multiGetForUpdate(this.readOptions, handles, keysArray);
            } else { //workaround for https://github.com/facebook/rocksdb/issues/9006: read results in increments of at most MAX_BATCH_SIZE at a time
                result = new byte[keysArray.length][];

                for (int i = 0; i < keysArray.length; ) {
                    int batchSize = min(keysArray.length - i, MAX_BATCH_SIZE);

                    byte[][] tmp = this.transaction.multiGetForUpdate(this.readOptions, handles.subList(i, i + batchSize), Arrays.copyOfRange(keysArray, i, i + batchSize));
                    System.arraycopy(tmp, 0, result, i, batchSize);

                    i += batchSize;
                }
            }

            return Arrays.asList(result);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public boolean multiGet(@NonNull List<FStorageColumn> columns, @NonNull List<ByteBuffer> keys, @NonNull List<ByteBuffer> values, @NonNull int[] sizes) throws FStorageException {
        List<byte[]> valueArrays = this.multiGet(columns, keys.stream().map(RocksAccessTransaction::toByteArray).collect(Collectors.toList()));

        boolean allSuccessful = true;
        for (int i = 0; i < valueArrays.size(); i++) {
            byte[] valueArray = valueArrays.set(i, null); //set to null to allow fast GC
            ByteBuffer value = values.get(i);

            if (valueArray != null) { //found
                value.put(valueArray, 0, min(value.remaining(), valueArray.length));
                sizes[i] = valueArray.length;
            } else { //not found
                sizes[i] = -1;
                allSuccessful = false;
            }
        }
        return allSuccessful;
    }

    @Override
    public FStorageIterator iterator(@NonNull FStorageColumn column) throws FStorageException {
        return new RocksIteratorDefault(this.transaction.getIterator(this.readOptions, ((RocksStorageColumn) column).handle()));
    }

    @Override
    public FStorageIterator iterator(@NonNull FStorageColumn column, byte[] fromKeyInclusive, byte[] toKeyExclusive) throws FStorageException {
        if (fromKeyInclusive == null && toKeyExclusive == null) { //both lower and upper bounds are null, create a regular iterator
            return this.iterator(column);
        }

        return new RocksIteratorBounded(this.readOptions, ((RocksStorageColumn) column).handle(), fromKeyInclusive, toKeyExclusive) {
            @Override
            protected RocksIterator createDelegate(@NonNull ReadOptions options, @NonNull ColumnFamilyHandle columnFamily) throws FStorageException {
                return RocksAccessTransaction.this.transaction.getIterator(options, columnFamily);
            }

            @Override
            public boolean isValid() throws FStorageException {
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

    //
    // FStorageWriteAccess
    //

    @Override
    public void put(@NonNull FStorageColumn column, @NonNull byte[] key, @NonNull byte[] value) throws FStorageException {
        try {
            this.transaction.put(((RocksStorageColumn) column).handle(), key, value);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public void delete(@NonNull FStorageColumn column, @NonNull byte[] key) throws FStorageException {
        try {
            this.transaction.delete(((RocksStorageColumn) column).handle(), key);
        } catch (RocksDBException e) {
            throw wrapException(e);
        }
    }

    @Override
    public void deleteRange(@NonNull FStorageColumn column, @NonNull byte[] fromKeyInclusive, @NonNull byte[] toKeyExclusive) throws FStorageException {
        //we can't actually do a deleteRange in a transaction, which is unfortunate. fall back to iterating over the range and deleting every key, which is probably close enough,
        //  although technically not perfectly atomic.

        try (FStorageIterator itr = this.iterator(column, fromKeyInclusive, toKeyExclusive)) {
            for (itr.seekToFirst(); itr.isValid(); itr.next()) {
                this.delete(column, itr.key());
            }
        }
    }
}