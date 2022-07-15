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

package net.daporkchop.fp2.core.storage.rocks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.storage.internal.FStorageColumn;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link FStorageColumn} for {@link RocksStorage}.
 *
 * @author DaPorkchop_
 */
@AllArgsConstructor
@Getter
@Setter
public class RocksStorageColumn implements FStorageColumn {
    /**
     * Unpacks the given {@link List list} of {@link FStorageColumn}s into a list of the underlying {@link ColumnFamilyHandle}s.
     * <p>
     * Subsequent changes made to the input list <i>may</i> be reflected in the unpacked list.
     *
     * @param columns the list of {@link FStorageColumn}s to unpack
     * @return the unpacked list
     */
    public static List<ColumnFamilyHandle> toColumnFamilyHandles(List<FStorageColumn> columns) {
        @RequiredArgsConstructor
        class UnpackingList extends AbstractList<ColumnFamilyHandle> {
            protected final List<FStorageColumn> delegate;

            @Override
            public ColumnFamilyHandle get(int index) {
                return ((RocksStorageColumn) this.delegate.get(index)).handle();
            }

            @Override
            public int size() {
                return this.delegate.size();
            }
        }

        final class RandomAccessUnpackingList extends UnpackingList implements RandomAccess {
            public RandomAccessUnpackingList(List<FStorageColumn> delegate) {
                super(delegate);
            }
        }

        return columns instanceof RandomAccess ? new RandomAccessUnpackingList(columns) : new UnpackingList(columns);
    }

    /**
     * Unpacks the given array of {@link FStorageColumn}s into a list of the underlying {@link ColumnFamilyHandle}s.
     * <p>
     * Subsequent changes made to the input array <i>may</i> be reflected in the unpacked list.
     *
     * @param columns the list of {@link FStorageColumn}s to unpack
     * @return the unpacked list
     */
    public static List<ColumnFamilyHandle> toColumnFamilyHandles(FStorageColumn... columns) {
        return toColumnFamilyHandles(columns, 0, columns.length);
    }

    /**
     * Unpacks the given array of {@link FStorageColumn}s into a list of the underlying {@link ColumnFamilyHandle}s.
     * <p>
     * Subsequent changes made to the input array <i>may</i> be reflected in the unpacked list.
     *
     * @param columns the list of {@link FStorageColumn}s to unpack
     * @param off     the offset in the array of the first {@link FStorageColumn} to access
     * @param len     the number of {@link FStorageColumn}s to access from the array
     * @return the unpacked list
     */
    public static List<ColumnFamilyHandle> toColumnFamilyHandles(FStorageColumn[] columns, int off, int len) {
        checkRangeLen(columns.length, off, len);

        final class RandomAccessUnpackingList extends AbstractList<ColumnFamilyHandle> implements RandomAccess {
            @Override
            public ColumnFamilyHandle get(int index) {
                checkIndex(len, index);
                return ((RocksStorageColumn) columns[off + index]).handle();
            }

            @Override
            public int size() {
                return len;
            }
        }

        return new RandomAccessUnpackingList();
    }

    private ColumnFamilyHandle handle;

    @Override
    @SneakyThrows(RocksDBException.class)
    public String toString() {
        return "RocksStorageColumn[internalColumnFamilyName='" + new String(this.handle.getName(), StandardCharsets.UTF_8)
               + "',internalColumnFamilyID=" + this.handle.getID() + ']';
    }
}
