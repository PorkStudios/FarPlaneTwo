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

package net.daporkchop.fp2.api.storage.external;

import lombok.NonNull;
import net.daporkchop.fp2.api.storage.internal.FStorageColumnHintsInternal;
import net.daporkchop.fp2.api.storage.internal.FStorageInternal;

import java.util.Optional;

/**
 * A factory for an {@link FStorageItem} implementation.
 *
 * @author DaPorkchop_
 */
public interface FStorageItemFactory<I extends FStorageItem> {
    /**
     * Prepares to initialize an item by configuring the resources used by the {@link I storage item}.
     *
     * @param callback a {@link ConfigurationCallback}
     * @return the result of configuration
     */
    ConfigurationResult configure(@NonNull ConfigurationCallback callback);

    /**
     * Creates a new {@link I storage item} which will use the given {@link FStorageInternal}.
     *
     * @param storageInternal the {@link FStorageInternal}
     * @return the new storage item
     */
    I create(@NonNull FStorageInternal storageInternal);

    /**
     * The result of {@link #configure configuring} an item.
     *
     * @author DaPorkchop_
     */
    enum ConfigurationResult {
        /**
         * The existing item's storage will be opened, or a new one created if it doesn't exist.
         * <p>
         * If the item is being opened using {@link FStorageCategory#openItem(String, FStorageItemFactory)} and the item doesn't already exist, a new item cannot be created, so
         * the operation will fail.
         */
        CREATE_IF_MISSING,
        /**
         * If the item's storage exists, it will be deleted. Finally, a new storage will be created.
         * <p>
         * If the item is being opened using {@link FStorageCategory#openItem(String, FStorageItemFactory)} and the item doesn't already exist, the operation will fail.
         */
        DELETE_EXISTING_AND_CREATE,
    }

    /**
     * Defines how a column should be requested.
     *
     * @author DaPorkchop_
     */
    enum ColumnRequirement {
        /**
         * The existing column will be opened, or a new one created if it doesn't exist.
         */
        CREATE_IF_MISSING,
        /**
         * The existing column will be opened, or initialization will fail if it doesn't exist.
         */
        FAIL_IF_MISSING,
        /**
         * If the column exists, it will be deleted. Finally, a new column will be created.
         */
        DELETE_EXISTING_AND_CREATE,
    }

    /**
     * An implementation-defined callback passed to {@link #configure} allowing initializers to control resources pre-configured during initialization.
     *
     * @author DaPorkchop_
     */
    interface ConfigurationCallback {
        /**
         * @return the existing token for the given storage item, if any
         */
        Optional<byte[]> getExistingToken();

        /**
         * Registers a column to be used by the storage item.
         *
         * @param name        the column's name
         * @param hints       {@link FStorageColumnHintsInternal hints} to the implementation as to the column's expected contents
         * @param requirement the handling to be done when opening the column
         */
        void registerColumn(@NonNull String name, @NonNull FStorageColumnHintsInternal hints, @NonNull ColumnRequirement requirement);
    }
}
