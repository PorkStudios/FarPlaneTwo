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

import lombok.NonNull;
import net.daporkchop.fp2.core.storage.rocks.access.iterator.IRocksIterator;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

import java.util.List;

/**
 * @author DaPorkchop_
 */
public interface IRocksReadAccess {
    default byte[] get(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key) throws RocksDBException {
        return this.get(columnFamily, key, RocksConflictDetectionHint.EXCLUSIVE);
    }

    byte[] get(@NonNull ColumnFamilyHandle columnFamily, @NonNull byte[] key, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException;

    default List<byte[]> multiGet(@NonNull List<ColumnFamilyHandle> columnFamilies, @NonNull List<byte[]> keys) throws RocksDBException {
        return this.multiGet(columnFamilies, keys, RocksConflictDetectionHint.EXCLUSIVE);
    }

    List<byte[]> multiGet(@NonNull List<ColumnFamilyHandle> columnFamilies, @NonNull List<byte[]> keys, @NonNull RocksConflictDetectionHint conflictDetectionHint) throws RocksDBException;

    IRocksIterator iterator(@NonNull ColumnFamilyHandle columnFamily) throws RocksDBException;

    IRocksIterator iterator(@NonNull ColumnFamilyHandle columnFamily, byte[] fromKeyInclusive, byte[] toKeyExclusive) throws RocksDBException;
}
