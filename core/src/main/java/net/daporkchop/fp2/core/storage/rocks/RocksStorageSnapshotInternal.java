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

package net.daporkchop.fp2.core.storage.rocks;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.storage.FStorageException;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageSnapshotInternal;
import net.daporkchop.lib.unsafe.PCleaner;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.Snapshot;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of {@link FStorageSnapshotInternal} for {@link RocksStorageInternal}.
 *
 * @author DaPorkchop_
 */
@Getter
public class RocksStorageSnapshotInternal implements FStorageSnapshotInternal {
    private final RocksStorageInternal storageInternal;

    private final RocksDB db;

    private final Snapshot snapshot;
    private final ReadOptions readOptions;

    private final PCleaner cleaner;

    private final Map<FStorageColumnInternal, ColumnFamilyHandle> columnFamilyHandlesSnapshot = new IdentityHashMap<>();

    public RocksStorageSnapshotInternal(@NonNull RocksStorageInternal storageInternal) {
        this.storageInternal = storageInternal;

        storageInternal.readLock().lock();
        try {
            //take a snapshot of the FStorageColumnInternal -> ColumnFamilyHandle mappings
            storageInternal.columns().values().forEach(column -> this.columnFamilyHandlesSnapshot.put(column, column.handle()));

            //create a snapshot
            RocksDB db = this.db = storageInternal.storage().db();
            Snapshot snapshot = this.snapshot = db.getSnapshot();

            //use a cleaner to ensure the snapshot is released when this instance is garbage-collected
            this.cleaner = PCleaner.cleaner(this, () -> db.releaseSnapshot(snapshot));

            //create new ReadOptions and configure them with the snapshot
            this.readOptions = new ReadOptions(RocksStorage.READ_OPTIONS);
            this.readOptions.setSnapshot(snapshot);
        } finally {
            storageInternal.readLock().unlock();
        }
    }

    public void validate() throws FStorageException {
        this.storageInternal.ensureOpen();
    }

    @Override
    public void close() {
        this.readOptions.close();

        this.cleaner.clean();
        this.snapshot.close();
    }

    @Override
    public byte[] get(@NonNull FStorageColumnInternal column, @NonNull byte[] key) throws FStorageException {
        this.validate();

        try {
            return this.db.get(this.columnFamilyHandlesSnapshot.get(column), this.readOptions, key);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        }
    }

    @Override
    public List<byte[]> multiGet(@NonNull List<FStorageColumnInternal> columns, @NonNull List<byte[]> keys) throws FStorageException {
        this.validate();

        try {
            return this.db.multiGetAsList(this.readOptions, columns.stream().map(this.columnFamilyHandlesSnapshot::get).collect(Collectors.toList()), keys);
        } catch (RocksDBException e) {
            throw new FStorageException(e);
        }
    }
}
