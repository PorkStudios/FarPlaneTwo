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
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.FStorageColumn;
import net.daporkchop.fp2.api.storage.internal.access.FStorageAccess;
import net.daporkchop.fp2.api.storage.internal.access.FStorageIterator;
import net.daporkchop.fp2.api.storage.internal.access.FStorageReadAccess;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Implementation of {@link FStorageAccess} which is actually read-only, but is pretending to be read-write.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class RocksAccessReadMasqueradingAsReadWrite implements FStorageAccess {
    @NonNull
    protected final FStorageReadAccess delegate;

    //
    // IRocksReadAccess
    //

    @Override
    public byte[] get(@NonNull FStorageColumn column, @NonNull byte[] key) throws FStorageException {
        return this.delegate.get(column, key);
    }

    @Override
    public int get(@NonNull FStorageColumn column, @NonNull ByteBuffer key, @NonNull ByteBuffer value) throws FStorageException {
        return this.delegate.get(column, key, value);
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<FStorageColumn> columnFamilies, @NonNull List<byte[]> keys) throws FStorageException {
        return this.delegate.multiGet(columnFamilies, keys);
    }

    @Override
    public boolean multiGet(@NonNull List<FStorageColumn> columns, @NonNull List<ByteBuffer> keys, @NonNull List<ByteBuffer> values, @NonNull int[] sizes) throws FStorageException {
        return this.delegate.multiGet(columns, keys, values, sizes);
    }

    @Override
    public FStorageIterator iterator(@NonNull FStorageColumn column) throws FStorageException {
        return this.delegate.iterator(column);
    }

    @Override
    public FStorageIterator iterator(@NonNull FStorageColumn column, byte[] fromKeyInclusive, byte[] toKeyExclusive) throws FStorageException {
        return this.delegate.iterator(column, fromKeyInclusive, toKeyExclusive);
    }

    //
    // IRocksWriteAccess
    //

    @Override
    public void put(@NonNull FStorageColumn column, @NonNull byte[] key, @NonNull byte[] value) throws FStorageException {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public void delete(@NonNull FStorageColumn column, @NonNull byte[] key) throws FStorageException {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public void deleteRange(@NonNull FStorageColumn column, @NonNull byte[] fromKeyInclusive, @NonNull byte[] toKeyExclusive) throws FStorageException {
        throw new UnsupportedOperationException("read-only");
    }
}
