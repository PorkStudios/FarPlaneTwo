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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.storage.external.FStorageItem;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.daporkchop.lib.common.function.io.IOBiConsumer;
import net.daporkchop.lib.common.function.io.IOConsumer;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public final class RocksStorageManifest {
    private static final int VERSION = 0;

    public static RocksStorageManifest read(@NonNull DataIn in) throws IOException {
        int version = in.readVarInt();
        checkArg(version == VERSION, "unknown version: %d", version);

        return new RocksStorageManifest(version, in);
    }

    @Getter
    private final CategoryData rootCategory;

    @Getter
    private final Map<String, FStorageColumnHintsInternal> allColumnFamilies = new TreeMap<>();
    private transient Map<String, FStorageColumnHintsInternal> allColumnFamiliesSnapshot;

    @Getter
    private final Set<String> columnFamiliesPendingDeletion = new TreeSet<>();
    private transient Set<String> columnFamiliesPendingDeletionSnapshot;

    private transient boolean hasSnapshot = false;

    public RocksStorageManifest() {
        this.rootCategory = new CategoryData();
    }

    private RocksStorageManifest(int version, @NonNull DataIn in) throws IOException {
        this.rootCategory = new CategoryData(version, in);

        //read all column families
        for (int i = 0, count = in.readVarInt(); i < count; i++) {
            this.allColumnFamilies.put(in.readVarUTF().intern(), FStorageColumnHintsInternal.builder()
                    .expectedKeySize(in.readVarIntZigZag())
                    .compressability(FStorageColumnHintsInternal.Compressability.valueOf(in.readVarUTF()))
                    .build());
        }

        //read column families pending deletion
        for (int i = 0, count = in.readVarInt(); i < count; i++) {
            this.columnFamiliesPendingDeletion.add(in.readVarUTF().intern());
        }
    }

    public void write(@NonNull DataOut out) throws IOException {
        out.writeVarInt(VERSION);

        this.rootCategory.write(out);

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

    public void snapshot() {
        checkState(!this.hasSnapshot, "a snapshot is already active");

        this.rootCategory.snapshot();

        this.allColumnFamiliesSnapshot = ImmutableMap.copyOf(this.allColumnFamilies);
        this.columnFamiliesPendingDeletionSnapshot = ImmutableSet.copyOf(this.columnFamiliesPendingDeletion);

        this.hasSnapshot = true;
    }

    public void rollback() {
        this.rootCategory.rollback();

        this.allColumnFamilies.clear();
        this.allColumnFamilies.putAll(this.allColumnFamiliesSnapshot);

        this.columnFamiliesPendingDeletion.clear();
        this.columnFamiliesPendingDeletion.addAll(this.columnFamiliesPendingDeletionSnapshot);
    }

    public void clearSnapshot() {
        checkState(this.hasSnapshot, "no snapshot is currently active");
        this.hasSnapshot = false;

        this.rootCategory.clearSnapshot();
        this.allColumnFamiliesSnapshot = null;
        this.columnFamiliesPendingDeletionSnapshot = null;
    }

    public String assignNewColumnFamilyName(@NonNull FStorageColumnHintsInternal hints) {
        String name;
        do {
            name = UUID.randomUUID().toString();
        } while (this.allColumnFamilies.putIfAbsent(name, hints) != null); //if there's a duplicate, spin until the name is unique
        return name;
    }

    /**
     * Manifest data for a {@link FStorageCategory}.
     *
     * @author DaPorkchop_
     */
    @NoArgsConstructor
    public static final class CategoryData {
        @Getter
        private final Map<String, CategoryData> categories = new ObjObjOpenHashMap<>();
        private transient Map<String, CategoryData> categoriesSnapshot;

        @Getter
        private final Map<String, ItemData> items = new ObjObjOpenHashMap<>();
        private transient Map<String, ItemData> itemsSnapshot;

        private CategoryData(int version, @NonNull DataIn in) throws IOException {
            //read categories
            for (int i = 0, count = in.readVarInt(); i < count; i++) {
                this.categories.put(in.readVarUTF().intern(), new CategoryData(version, in));
            }

            //read items
            for (int i = 0, count = in.readVarInt(); i < count; i++) {
                this.items.put(in.readVarUTF().intern(), new ItemData(version, in));
            }
        }

        public void write(@NonNull DataOut out) throws IOException {
            //write categories (with length prefix)
            out.writeVarInt(this.categories.size());
            this.categories.forEach((IOBiConsumer<String, CategoryData>) (name, data) -> {
                out.writeVarUTF(name);
                data.write(out);
            });

            //write items (with length prefix)
            out.writeVarInt(this.items.size());
            this.items.forEach((IOBiConsumer<String, ItemData>) (name, data) -> {
                out.writeVarUTF(name);
                data.write(out);
            });
        }

        private void snapshot() {
            this.categories.values().forEach(CategoryData::snapshot);
            this.categoriesSnapshot = ImmutableMap.copyOf(this.categories);

            this.items.values().forEach(ItemData::snapshot);
            this.itemsSnapshot = ImmutableMap.copyOf(this.items);
        }

        private void rollback() {
            this.categories.clear();
            this.categories.putAll(this.categoriesSnapshot);
            this.categories.values().forEach(CategoryData::rollback);

            this.items.clear();
            this.items.putAll(this.itemsSnapshot);
            this.items.values().forEach(ItemData::rollback);
        }

        private void clearSnapshot() {
            this.categories.values().forEach(CategoryData::clearSnapshot);
            this.categoriesSnapshot = null;

            this.items.values().forEach(ItemData::clearSnapshot);
            this.itemsSnapshot = null;
        }
    }

    /**
     * Manifest data for a {@link FStorageItem}.
     *
     * @author DaPorkchop_
     */
    @NoArgsConstructor
    public static final class ItemData {
        @Getter
        private final Map<String, String> columnNamesToColumnFamilyNames = new ObjObjOpenHashMap<>();
        private transient Map<String, String> columnNamesToColumnFamilyNamesSnapshot;

        @Getter
        @Setter
        private byte[] token;
        private transient byte[] tokenSnapshot;

        private ItemData(int version, @NonNull DataIn in) throws IOException {
            //read column name -> column family name mappings
            for (int i = 0, count = in.readVarInt(); i < count; i++) {
                this.columnNamesToColumnFamilyNames.put(in.readVarUTF().intern(), in.readVarUTF().intern());
            }

            //read token data
            if (in.readBoolean()) {
                this.token = new byte[in.readVarInt()];
                in.readFully(this.token);
            }
        }

        public void write(@NonNull DataOut out) throws IOException {
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
        }

        private void snapshot() {
            this.columnNamesToColumnFamilyNamesSnapshot = ImmutableMap.copyOf(this.columnNamesToColumnFamilyNames);

            this.tokenSnapshot = this.token;
        }

        private void rollback() {
            this.columnNamesToColumnFamilyNames.clear();
            this.columnNamesToColumnFamilyNames.putAll(this.columnNamesToColumnFamilyNamesSnapshot);

            this.token = this.tokenSnapshot;
        }

        private void clearSnapshot() {
            this.columnNamesToColumnFamilyNamesSnapshot = null;
            this.tokenSnapshot = null;
        }
    }
}
