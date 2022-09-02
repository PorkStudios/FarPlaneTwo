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
import net.daporkchop.fp2.api.storage.internal.access.FStorageReadAccess;
import net.daporkchop.fp2.core.storage.rocks.RocksStorage;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.*;

/**
 * Extension of {@link FStorageReadAccess} which provides default implementations of all {@link ByteBuffer}-based methods.
 * <p>
 * Present only to ease implementations until rocksdbjni adds {@link ByteBuffer} alternatives for all methods.
 *
 * @author DaPorkchop_
 */
public interface ArrayOnlyFStorageReadAccess extends FStorageReadAccess {
    @Override
    default int get(@NonNull FStorageColumn column, @NonNull ByteBuffer key, @NonNull ByteBuffer value) throws FStorageException {
        //we have to copy key into a heap buffer, then get it and copy the result back
        byte[] valueArray = this.get(column, RocksStorage.toByteArrayView(key));
        if (valueArray != null) { //found
            value.put(valueArray, 0, min(value.remaining(), valueArray.length)).flip();
            return valueArray.length;
        } else { //not found
            return -1;
        }
    }

    @Override
    default boolean multiGet(@NonNull List<FStorageColumn> columns, @NonNull List<ByteBuffer> keys, @NonNull List<ByteBuffer> values, @NonNull int[] sizes) throws FStorageException {
        List<byte[]> valueArrays = this.multiGet(columns, keys.stream().map(RocksStorage::toByteArrayView).collect(Collectors.toList()));

        boolean allSuccessful = true;
        for (int i = 0; i < valueArrays.size(); i++) {
            byte[] valueArray = valueArrays.set(i, null); //set to null to allow fast GC
            ByteBuffer value = values.get(i);

            if (valueArray != null) { //found
                value.clear();
                value.put(valueArray, 0, min(value.remaining(), valueArray.length)).flip();
                sizes[i] = valueArray.length;

                if (valueArray.length > value.remaining()) { //the requiredSize is bigger than the buffer
                    allSuccessful = false;
                }
            } else { //not found
                sizes[i] = -1;
            }
        }
        return allSuccessful;
    }
}
