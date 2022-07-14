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

import java.util.Optional;

/**
 * A factory for an {@link FStorageCategory}.
 *
 * @author DaPorkchop_
 */
public interface FStorageCategoryFactory {
    static FStorageCategoryFactory createIfMissing() {
        return callback -> ConfigurationResult.CREATE_IF_MISSING;
    }

    /**
     * Prepares to initialize a category by configuring the resources used by the category.
     *
     * @param callback a {@link ConfigurationCallback}
     * @return the result of configuration
     */
    ConfigurationResult configure(@NonNull ConfigurationCallback callback);

    /**
     * The result of {@link #configure configuring} a category.
     *
     * @author DaPorkchop_
     */
    enum ConfigurationResult {
        /**
         * The existing category will be opened, or a new one created if it doesn't exist.
         * <p>
         * If the category is being opened using {@link FStorageCategory#openCategory(String, FStorageCategoryFactory)} and the category doesn't already exist, a new category cannot be
         * created, so the operation will fail.
         */
        CREATE_IF_MISSING,
        /**
         * If the category exists, it will be recursively deleted. Finally, a new category will be created.
         * <p>
         * If the category is being opened using {@link FStorageCategory#openCategory(String, FStorageCategoryFactory)} and the category doesn't already exist, the operation will fail.
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
         * @return the existing token for the given storage category, if any
         */
        Optional<byte[]> getExistingToken();

        /**
         * Updates the category's token once it's opened.
         *
         * @param token the new token
         */
        void setToken(@NonNull byte[] token);

        /**
         * Clears the category's token once it's opened.
         */
        void clearToken();
    }
}
