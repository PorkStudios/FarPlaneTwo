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

import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.FStorageColumn;
import net.daporkchop.fp2.api.storage.internal.access.FStorageWriteAccess;
import net.daporkchop.fp2.core.storage.rocks.RocksStorageColumn;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

import java.nio.ByteBuffer;

import static net.daporkchop.fp2.core.storage.rocks.RocksStorage.*;

/**
 * Implements {@link FStorageWriteAccess} by simply extending {@link WriteBatch}.
 *
 * @author DaPorkchop_
 */
public class RocksAccessWriteBatch extends WriteBatch implements FStorageWriteAccess {
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
            throw wrapException(e);
        }
    }

    @Override
    public void deleteRange(@NonNull FStorageColumn column, @NonNull ByteBuffer fromKeyInclusive, @NonNull ByteBuffer toKeyExclusive) throws FStorageException {
        //rocksdbjni doesn't provide any versions of deleteRange which take ByteBuffer for the key parameters, so we'll copy them both to ordinary byte[]s and then
        //  continue as usual

        byte[] fromKeyInclusiveArray = new byte[fromKeyInclusive.remaining()];
        fromKeyInclusive.get(fromKeyInclusiveArray).position(fromKeyInclusive.position() - fromKeyInclusiveArray.length);

        byte[] toKeyExclusiveArray = new byte[toKeyExclusive.remaining()];
        toKeyExclusive.get(toKeyExclusiveArray).position(toKeyExclusive.position() - toKeyExclusiveArray.length);

        this.deleteRange(column, fromKeyInclusiveArray, toKeyExclusiveArray);
    }
}
