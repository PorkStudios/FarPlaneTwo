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

package net.daporkchop.fp2.api.storage.internal;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import static java.lang.Math.*;

/**
 * Provides hints to the implementation as to what kind of data a {@link FStorageColumn} will contain. This allows the implementation to optimize the column specifically
 * for its expected contents.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = true)
@Data
public class FStorageColumnHintsInternal {
    /**
     * The default column hints.
     */
    public static final FStorageColumnHintsInternal DEFAULT = builder().build();

    /**
     * The expected size of a key, in bytes, or a value {@code < 0} if unknown.
     * <p>
     * Default: {@code -1}
     */
    @Builder.Default
    private final int expectedAverageKeySize = -1;

    /**
     * The expected average size of a value, in bytes, or a value {@code < 0} if unknown.
     * <p>
     * Default: {@code -1}
     */
    @Builder.Default
    private final int expectedAverageValueSize = -1;

    /**
     * The expected compressability of data.
     * <p>
     * Default: {@link Compressability#NORMAL}
     */
    @Builder.Default
    @NonNull
    private final Compressability compressability = Compressability.NORMAL;

    /**
     * @see #expectedAverageKeySize
     */
    public int expectedAverageKeySize() {
        return max(this.expectedAverageKeySize, -1);
    }

    /**
     * @see #expectedAverageValueSize
     */
    public int expectedAverageValueSize() {
        return max(this.expectedAverageValueSize, -1);
    }

    /**
     * Defines how compressable data is.
     *
     * @author DaPorkchop_
     */
    public enum Compressability {
        /**
         * The data is extremely compressable, a high compression ratio is expected from even the fastest compression algorithms.
         */
        HIGH,
        /**
         * The data is moderately compressable.
         */
        NORMAL,
        /**
         * The data is not very compressable, but compression is still likely to yield measurable storage benefits.
         */
        LOW,
        /**
         * The data is not compressable, consisting of effectively random bytes.
         */
        NONE,
    }
}
