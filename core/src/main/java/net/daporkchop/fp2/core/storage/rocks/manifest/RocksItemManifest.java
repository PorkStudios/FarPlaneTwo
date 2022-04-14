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
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.function.io.IOBiConsumer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class RocksItemManifest extends AbstractRocksManifest<RocksItemManifest> {
    private Map<String, String> columnNamesToColumnFamilyNames = new TreeMap<>();
    private transient Map<String, String> columnNamesToColumnFamilyNamesSnapshot;

    private byte[] token;
    private transient byte[] tokenSnapshot;

    private boolean initialized = false;
    private transient boolean initializedSnapshot;

    public RocksItemManifest(@NonNull Path filePath) {
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
        this.columnNamesToColumnFamilyNames = new TreeMap<>();
        this.token = null;
        this.initialized = false;
    }

    @Override
    protected void read0(int version, DataIn in) throws IOException {
        //read column name -> column family name mappings
        this.columnNamesToColumnFamilyNames = new TreeMap<>();
        for (int i = 0, count = in.readVarInt(); i < count; i++) {
            this.columnNamesToColumnFamilyNames.put(in.readVarUTF().intern(), in.readVarUTF().intern());
        }

        //read token data
        if (in.readBoolean()) {
            this.token = new byte[in.readVarInt()];
            in.readFully(this.token);
        }

        //read initialized flag
        this.initialized = in.readBoolean();
    }

    @Override
    protected void write0(DataOut out) throws IOException {
        //write column name -> column family name mappings (with length prefix)
        out.writeVarInt(this.columnNamesToColumnFamilyNames.size());
        this.columnNamesToColumnFamilyNames.forEach((IOBiConsumer<String, String>) (name, data) -> {
            out.writeVarUTF(name);
            out.writeVarUTF(data);
        });

        //write token data
        out.writeBoolean(this.token != null);
        if (this.token != null) {
            out.writeVarInt(this.token.length);
            out.write(this.token);
        }

        //write initialized flag
        out.writeBoolean(this.initialized);
    }

    @Override
    protected void snapshot0() {
        //clone primary collection to snapshot
        this.columnNamesToColumnFamilyNamesSnapshot = new TreeMap<>(this.columnNamesToColumnFamilyNames);
        this.tokenSnapshot = this.token;
        this.initializedSnapshot = this.initialized;
    }

    @Override
    protected void rollback0() {
        //clone snapshot collection to primary
        this.columnNamesToColumnFamilyNames = new TreeMap<>(this.columnNamesToColumnFamilyNamesSnapshot);
        this.token = this.tokenSnapshot;
        this.initialized = this.initializedSnapshot;
    }

    @Override
    protected void clearSnapshot0() {
        this.columnNamesToColumnFamilyNamesSnapshot = null;
        this.tokenSnapshot = null;
        this.initializedSnapshot = false;
    }

    //
    // helper methods
    //

    public Map<String, String> snapshotColumnNamesToColumnFamilyNames() {
        return this.getWithReadLock(manifest -> new TreeMap<>(manifest.columnNamesToColumnFamilyNames));
    }

    public void removeColumnByName(@NonNull String columnName) {
        this.runWithWriteLock(manifest -> checkState(manifest.columnNamesToColumnFamilyNames.remove(columnName) != null, "can't remove column '%s' because it doesn't exist!", columnName));
    }

    public boolean columnExistsWithName(@NonNull String columnName) {
        return this.getWithReadLock(manifest -> manifest.columnNamesToColumnFamilyNames.containsKey(columnName));
    }

    public Optional<String> getColumnFamilyNameForColumn(@NonNull String columnName) {
        return this.getWithReadLock(manifest -> Optional.ofNullable(manifest.columnNamesToColumnFamilyNames.get(columnName)));
    }

    public void addColumn(@NonNull String columnName, @NonNull String columnFamilyName) {
        this.runWithWriteLock(manifest -> checkState(manifest.columnNamesToColumnFamilyNames.putIfAbsent(columnName, columnFamilyName) == null, "can't add column '%s' because another column with the same name already exists!", columnName));
    }

    public Optional<byte[]> getToken() {
        return this.getWithReadLock(manifest -> Optional.ofNullable(this.token)).map(byte[]::clone);
    }

    public void setToken(byte[] token) {
        this.runWithWriteLock(manifest -> manifest.token = token.clone());
    }

    public void removeToken() {
        this.runWithWriteLock(manifest -> manifest.token = null);
    }

    public boolean isInitialized() {
        return this.getWithReadLock(manifest -> manifest.initialized);
    }

    public void markInitialized() {
        this.runWithWriteLock(manifest -> {
            checkState(!this.initialized, "already initialized!");
            manifest.initialized = true;
        });
    }
}
