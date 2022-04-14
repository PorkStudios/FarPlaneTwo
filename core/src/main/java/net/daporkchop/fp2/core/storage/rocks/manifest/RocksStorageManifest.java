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

package net.daporkchop.fp2.core.storage.rocks.manifest;

import lombok.NonNull;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.function.io.IOBiConsumer;
import net.daporkchop.lib.common.function.io.IOConsumer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class RocksStorageManifest extends AbstractRocksManifest<RocksStorageManifest> {
    private Map<String, FStorageColumnHintsInternal> allColumnFamilies = new TreeMap<>();
    private transient Map<String, FStorageColumnHintsInternal> allColumnFamiliesSnapshot;

    private Set<String> columnFamiliesPendingDeletion = new TreeSet<>();
    private transient Set<String> columnFamiliesPendingDeletionSnapshot;

    public RocksStorageManifest(@NonNull Path filePath) {
        super(filePath);
    }

    //
    // manifest implementation
    //

    @Override
    protected int version() {
        return 0;
    }

    @Override
    protected void clear0() {
        this.allColumnFamilies = new TreeMap<>();
        this.columnFamiliesPendingDeletion = new TreeSet<>();
    }

    @Override
    protected void read0(int version, DataIn in) throws IOException {
        //read all column families
        this.allColumnFamilies = new TreeMap<>();
        for (int i = 0, count = in.readVarInt(); i < count; i++) {
            this.allColumnFamilies.put(in.readVarUTF().intern(), FStorageColumnHintsInternal.builder()
                    .expectedKeySize(in.readVarIntZigZag())
                    .compressability(FStorageColumnHintsInternal.Compressability.valueOf(in.readVarUTF()))
                    .build());
        }

        //read column families pending deletion
        this.columnFamiliesPendingDeletion = new TreeSet<>();
        for (int i = 0, count = in.readVarInt(); i < count; i++) {
            this.columnFamiliesPendingDeletion.add(in.readVarUTF().intern());
        }
    }

    @Override
    protected void write0(DataOut out) throws IOException {
        //write all column families (with length prefix)
        out.writeVarInt(this.allColumnFamilies.size());
        this.allColumnFamilies.forEach((IOBiConsumer<String, FStorageColumnHintsInternal>) (name, hints) -> {
            out.writeVarUTF(name);

            //write column hints
            out.writeVarIntZigZag(hints.expectedKeySize());
            out.writeVarUTF(hints.compressability().name());
        });

        //write column families pending deletion (with length prefix)
        out.writeVarInt(this.columnFamiliesPendingDeletion.size());
        this.columnFamiliesPendingDeletion.forEach((IOConsumer<String>) out::writeVarUTF);
    }

    @Override
    protected void snapshot0() {
        //clone primary collections to snapshots
        this.allColumnFamiliesSnapshot = new TreeMap<>(this.allColumnFamilies);
        this.columnFamiliesPendingDeletionSnapshot = new TreeSet<>(this.columnFamiliesPendingDeletion);
    }

    @Override
    protected void rollback0() {
        //clone snapshot collections to primaries
        this.allColumnFamilies = new TreeMap<>(this.allColumnFamiliesSnapshot);
        this.columnFamiliesPendingDeletion = new TreeSet<>(this.columnFamiliesPendingDeletionSnapshot);
    }

    @Override
    protected void clearSnapshot0() {
        this.allColumnFamiliesSnapshot = null;
        this.columnFamiliesPendingDeletionSnapshot = null;
    }

    //
    // helper methods
    //

    public void forEachColumnFamily(@NonNull BiConsumer<String, FStorageColumnHintsInternal> action) {
        this.runWithReadLock(manifest -> manifest.allColumnFamilies.forEach(action));
    }

    public String assignNewColumnFamilyName(@NonNull FStorageColumnHintsInternal hints) {
        return this.getWithWriteLock(manifest -> {
            String name;
            do {
                name = UUID.randomUUID().toString();
            } while (manifest.allColumnFamilies.putIfAbsent(name, hints) != null); //if there's a duplicate, spin until the name is unique
            return name;
        });
    }

    public boolean containsColumnFamilyName(@NonNull String name) {
        return this.getWithReadLock(manifest -> manifest.allColumnFamilies.containsKey(name));
    }

    public boolean containsAllColumnFamilyNames(@NonNull Collection<String> c) {
        return this.getWithReadLock(manifest -> manifest.allColumnFamilies.keySet().containsAll(c));
    }

    public void markColumnFamiliesForDeletion(@NonNull Collection<String> columnFamilyNames) {
        this.runWithWriteLock(manifest -> {
            manifest.columnFamiliesPendingDeletion.addAll(columnFamilyNames);
        });
    }

    public boolean isAnyColumnFamilyPendingDeletion() {
        return this.getWithReadLock(manifest -> !manifest.columnFamiliesPendingDeletion.isEmpty());
    }

    public void forEachColumnFamilyNamePendingDeletion(@NonNull Consumer<String> action) {
        this.runWithReadLock(manifest -> manifest.columnFamiliesPendingDeletion.forEach(action));
    }

    public void removeColumnFamilyFromDeletionQueue(@NonNull String columnFamilyName) {
        this.runWithWriteLock(manifest -> {
            checkState(this.allColumnFamilies.remove(columnFamilyName) != null, "column family '%s' doesn't exist", columnFamilyName);
            checkState(this.columnFamiliesPendingDeletion.remove(columnFamilyName), "column family '%s' isn't marked for deletion", columnFamilyName);
        });
    }
}
