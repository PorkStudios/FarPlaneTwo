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

package net.daporkchop.fp2.gl.layout;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;

/**
 * Generic builder type for {@link BaseLayout}s.
 *
 * @param <L> the type of {@link BaseLayout} to build
 * @author DaPorkchop_
 */
public interface BaseLayoutBuilder<BUILDER extends BaseLayoutBuilder<BUILDER, L>, L extends BaseLayout> {
    /**
     * Adds the given {@link AttributeFormat} to be used for attributes of the type defined by the given {@link AttributeUsage}.
     *
     * @param usage  the {@link AttributeUsage}
     * @param format the {@link AttributeFormat}
     * @throws IllegalArgumentException if the given {@link AttributeFormat} does not support the given {@link AttributeUsage}
     * @throws IllegalArgumentException if this builder does not support the given {@link AttributeUsage}
     */
    BUILDER with(@NonNull AttributeUsage usage, @NonNull AttributeFormat<?> format);

    /**
     * Adds a {@link AttributeFormat} which will be used for uniform attributes.
     * <p>
     * Alias for {@code with(AttributeUsage.UNIFORM, format)}.
     *
     * @param format the {@link AttributeFormat} of the uniform attributes
     * @see #with(AttributeUsage, AttributeFormat)
     */
    default BUILDER withUniform(@NonNull AttributeFormat<?> format) {
        return this.with(AttributeUsage.UNIFORM, format);
    }

    /**
     * Adds a {@link AttributeFormat} which will be used for uniform array attributes.
     * <p>
     * Alias for {@code with(AttributeUsage.UNIFORM_ARRAY, format)}.
     *
     * @param format the {@link AttributeFormat} of the uniform array attributes
     * @see #with(AttributeUsage, AttributeFormat)
     */
    default BUILDER withUniformArray(@NonNull AttributeFormat<?> format) {
        return this.with(AttributeUsage.UNIFORM_ARRAY, format);
    }

    /**
     * Adds a {@link TextureFormat2D}.
     *
     * @param format the format of the texture
     */
    BUILDER withTexture(@NonNull TextureFormat2D<?> format);

    /**
     * Adds all the formats from the given {@link L}.
     *
     * @param layout the {@link L}
     */
    BUILDER with(@NonNull L layout);

    /**
     * @return the constructed {@link L}
     */
    L build();
}
