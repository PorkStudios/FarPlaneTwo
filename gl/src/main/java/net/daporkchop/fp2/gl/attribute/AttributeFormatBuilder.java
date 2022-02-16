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

package net.daporkchop.fp2.gl.attribute;

import lombok.NonNull;

/**
 * Builder for attribute format types.
 *
 * @author DaPorkchop_
 */
public interface AttributeFormatBuilder<S> {
    /**
     * Registers an attribute name override.
     * <p>
     * The attribute with the given original name will be visible in shaders under the given new name.
     *
     * @param originalName the original name
     * @param newName      the new name
     */
    AttributeFormatBuilder<S> rename(@NonNull String originalName, @NonNull String newName);

    /**
     * Registers a {@link AttributeUsage} which the attribute format will support.
     *
     * @param usage the {@link AttributeUsage}
     */
    AttributeFormatBuilder<S> useFor(@NonNull AttributeUsage usage);

    /**
     * Registers multiple {@link AttributeUsage}s which the attribute format will support.
     *
     * @param usages the {@link AttributeUsage}s
     */
    AttributeFormatBuilder<S> useFor(@NonNull AttributeUsage... usages);

    /**
     * @return the constructed {@link AttributeFormat}
     */
    AttributeFormat<S> build();
}
