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

package net.daporkchop.fp2.gl.opengl.attribute;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.glsl.GLSLBlockAttributeFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLBlockMemoryLayout;

import java.util.Optional;

/**
 * All of the supported types of {@link AttributeFormatImpl}.
 *
 * @author DaPorkchop_
 */
public enum AttributeFormatTypes {
    GLSL_BLOCK_STD140 {
        @Override
        public <S> Optional<AttributeFormatImpl<S, ?>> createFormat(@NonNull IAttributeFormatBuilderImpl<S> builder) {
            return GLSLBlockAttributeFormat.VALID_USAGES.containsAll(builder.usages()) //should always be supported
                    ? Optional.of(new GLSLBlockAttributeFormat<>(builder, GLSLBlockMemoryLayout.STD140))
                    : Optional.empty();
        }
    };

    /**
     * Creates an {@link AttributeFormatImpl} for the given {@link IAttributeFormatBuilderImpl} using this format type if possible.
     *
     * @param builder the {@link IAttributeFormatBuilderImpl}
     * @param <S>     the struct type
     * @return the created {@link AttributeFormatImpl}, or an empty {@link Optional} if this type cannot support the given {@link IAttributeFormatBuilderImpl}'s options
     */
    public abstract <S> Optional<AttributeFormatImpl<S, ?>> createFormat(@NonNull IAttributeFormatBuilderImpl<S> builder);
}
