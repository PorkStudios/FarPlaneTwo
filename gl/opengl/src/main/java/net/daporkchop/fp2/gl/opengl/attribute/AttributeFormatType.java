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
import net.daporkchop.fp2.gl.opengl.attribute.format.PackedAttributeFormat;
import net.daporkchop.fp2.gl.opengl.attribute.format.PackedInstancedAttributeFormat;
import net.daporkchop.fp2.gl.opengl.attribute.format.Std140BlockAttributeFormat;

import java.util.Optional;

/**
 * All of the supported types of {@link AttributeFormatImpl}.
 *
 * @author DaPorkchop_
 */
public enum AttributeFormatType {
    PACKED_INSTANCED {
        @Override
        public <S> Optional<AttributeFormatImpl<S, ?>> createFormat(@NonNull AttributeFormatBuilderImpl<S> builder) {
            return PackedInstancedAttributeFormat.supports(builder)
                    ? Optional.of(new PackedInstancedAttributeFormat<>(builder))
                    : Optional.empty();
        }
    },
    PACKED {
        @Override
        public <S> Optional<AttributeFormatImpl<S, ?>> createFormat(@NonNull AttributeFormatBuilderImpl<S> builder) {
            return PackedAttributeFormat.supports(builder)
                    ? Optional.of(new PackedAttributeFormat<>(builder))
                    : Optional.empty();
        }
    },
    STD140_BLOCK {
        @Override
        public <S> Optional<AttributeFormatImpl<S, ?>> createFormat(@NonNull AttributeFormatBuilderImpl<S> builder) {
            return Std140BlockAttributeFormat.supports(builder)
                    ? Optional.of(new Std140BlockAttributeFormat<>(builder))
                    : Optional.empty();
        }
    };

    /**
     * Creates an {@link AttributeFormatImpl} for the given {@link AttributeFormatBuilderImpl} using the best compatible format type.
     *
     * @param builder the {@link AttributeFormatBuilderImpl}
     * @param <S>     the struct type
     * @return the created {@link AttributeFormatImpl}
     */
    public static <S> AttributeFormatImpl<S, ?> createBestFormat(@NonNull AttributeFormatBuilderImpl<S> builder) {
        for (AttributeFormatType type : values()) {
            Optional<AttributeFormatImpl<S, ?>> optionalFormat = type.createFormat(builder);
            if (optionalFormat.isPresent()) {
                return optionalFormat.get();
            }
        }

        throw new IllegalStateException(builder.toString());
    }

    /**
     * Creates an {@link AttributeFormatImpl} for the given {@link AttributeFormatBuilderImpl} using this format type if possible.
     *
     * @param builder the {@link AttributeFormatBuilderImpl}
     * @param <S>     the struct type
     * @return the created {@link AttributeFormatImpl}, or an empty {@link Optional} if this type cannot support the given {@link AttributeFormatBuilderImpl}'s options
     */
    public abstract <S> Optional<AttributeFormatImpl<S, ?>> createFormat(@NonNull AttributeFormatBuilderImpl<S> builder);
}
