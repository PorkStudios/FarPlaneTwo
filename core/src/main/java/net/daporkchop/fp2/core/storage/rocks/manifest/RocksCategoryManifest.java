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
import net.daporkchop.lib.common.function.io.IOConsumer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class RocksCategoryManifest extends AbstractRocksManifest<RocksCategoryManifest> {
    private Set<String> childCategoryNames = new TreeSet<>();
    private transient Set<String> childCategoryNamesSnapshot;

    private Set<String> childItemNames = new TreeSet<>();
    private transient Set<String> childItemNamesSnapshot;

    public RocksCategoryManifest(@NonNull Path filePath) {
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
        this.childCategoryNames = new TreeSet<>();
        this.childItemNames = new TreeSet<>();
    }

    @Override
    protected void read0(int version, DataIn in) throws IOException {
        //read child category names
        this.childCategoryNames = new TreeSet<>();
        for (int i = 0, count = in.readVarInt(); i < count; i++) {
            this.childCategoryNames.add(in.readVarUTF().intern());
        }

        //read child item names
        this.childItemNames = new TreeSet<>();
        for (int i = 0, count = in.readVarInt(); i < count; i++) {
            this.childCategoryNames.add(in.readVarUTF().intern());
        }
    }

    @Override
    protected void write0(DataOut out) throws IOException {
        //write all child category names (with length prefix)
        out.writeVarInt(this.childCategoryNames.size());
        this.childCategoryNames.forEach((IOConsumer<String>) out::writeVarUTF);

        //write all child item names (with length prefix)
        out.writeVarInt(this.childItemNames.size());
        this.childItemNames.forEach((IOConsumer<String>) out::writeVarUTF);
    }

    @Override
    protected void snapshot0() {
        //clone primary collections to snapshots
        this.childCategoryNamesSnapshot = new TreeSet<>(this.childCategoryNames);
        this.childItemNamesSnapshot = new TreeSet<>(this.childItemNames);
    }

    @Override
    protected void rollback0() {
        //clone snapshot collections to primaries
        this.childCategoryNames = new TreeSet<>(this.childCategoryNamesSnapshot);
        this.childItemNames = new TreeSet<>(this.childItemNamesSnapshot);
    }

    @Override
    protected void clearSnapshot0() {
        this.childCategoryNamesSnapshot = null;
        this.childItemNamesSnapshot = null;
    }

    //
    // helper methods
    //

    public void forEachChildCategoryName(@NonNull Consumer<String> action) {
        this.runWithReadLock(manifest -> manifest.childCategoryNames.forEach(action));
    }

    public boolean hasCategory(@NonNull String name) {
        return this.getWithReadLock(manifest -> manifest.childCategoryNames.contains(name));
    }

    public void addCategory(@NonNull String name) {
        this.runWithWriteLock(manifest -> checkState(manifest.childCategoryNames.add(name), "category '%s' already exists", name));
    }

    public void removeCategory(@NonNull String name) {
        this.runWithWriteLock(manifest -> checkState(manifest.childCategoryNames.remove(name), "category '%s' doesn't exist", name));
    }

    public void forEachChildItemName(@NonNull Consumer<String> action) {
        this.runWithReadLock(manifest -> manifest.childItemNames.forEach(action));
    }

    public boolean hasItem(@NonNull String name) {
        return this.getWithReadLock(manifest -> manifest.childItemNames.contains(name));
    }

    public void addItem(@NonNull String name) {
        this.runWithWriteLock(manifest -> checkState(manifest.childItemNames.add(name), "item '%s' already exists", name));
    }

    public void removeItem(@NonNull String name) {
        this.runWithWriteLock(manifest -> checkState(manifest.childItemNames.remove(name), "item '%s' doesn't exist", name));
    }
}
