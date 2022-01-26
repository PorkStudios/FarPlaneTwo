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

package net.daporkchop.fp2.gl.layout.binding;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.texture.Texture2D;

/**
 * Generic builder type for {@link BaseBinding}s.
 *
 * @param <B> the type of {@link BaseBinding} to build
 * @author DaPorkchop_
 */
public interface BaseBindingBuilder<BUILDER extends BaseBindingBuilder<BUILDER, B>, B extends BaseBinding> {
    /**
     * Adds the given {@link AttributeBuffer} which contains attributes of the type defined by the given {@link AttributeUsage}.
     *
     * @param usage  the {@link AttributeUsage}
     * @param buffer the {@link AttributeBuffer}
     */
    BUILDER with(@NonNull AttributeUsage usage, @NonNull AttributeBuffer<?> buffer);

    /**
     * Adds a {@link AttributeBuffer} which contains uniform attributes.
     * <p>
     * Alias for {@code with(AttributeUsage.UNIFORM, buffer)}.
     *
     * @param buffer the {@link AttributeBuffer} containing the uniform attributes
     * @see #with(AttributeUsage, AttributeBuffer)
     */
    default BUILDER withUniform(@NonNull AttributeBuffer<?> buffer) {
        return this.with(AttributeUsage.UNIFORM, buffer);
    }

    /**
     * Adds a {@link AttributeBuffer} which contains uniform array attributes.
     * <p>
     * Alias for {@code with(AttributeUsage.UNIFORM_ARRAY, buffer)}.
     *
     * @param buffer the {@link AttributeBuffer} containing the uniform attributes
     * @see #with(AttributeUsage, AttributeBuffer)
     */
    default BUILDER withUniformArray(@NonNull AttributeBuffer<?> buffer) {
        return this.with(AttributeUsage.UNIFORM_ARRAY, buffer);
    }

    /**
     * Adds a {@link Texture2D} which contains a 2D texture.
     *
     * @param texture the texture
     */
    BUILDER withTexture(@NonNull Texture2D<?> texture);

    /**
     * @return the constructed {@link B}
     */
    B build();
}
