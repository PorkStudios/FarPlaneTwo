/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.gl.opengl.attribute.format;

import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.draw.global.InterleavedDrawGlobalAttributeBindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.draw.local.InterleavedDrawLocalAttributeBindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.uniform.array.InterleavedUniformArrayAttributeBindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.uniform.standard.InterleavedUniformAttributeBindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLBlockMemoryLayout;
import net.daporkchop.fp2.gl.opengl.layout.LayoutEntry;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author DaPorkchop_
 */
public final class Std140BlockAttributeFormat<S> extends InterleavedAttributeFormatImpl<Std140BlockAttributeFormat<S>, S> {
    private static final Set<AttributeUsage> VALID_USAGES = ImmutableSet.copyOf(EnumSet.of(
            AttributeUsage.UNIFORM,
            AttributeUsage.UNIFORM_ARRAY,
            AttributeUsage.DRAW_LOCAL,
            AttributeUsage.DRAW_GLOBAL
    ));

    public static boolean supports(@NonNull AttributeFormatBuilderImpl<?> builder) {
        return VALID_USAGES.containsAll(builder.usages())
               && GLExtension.GL_ARB_uniform_buffer_object.supported(builder.gl().env());
    }

    public Std140BlockAttributeFormat(@NonNull AttributeFormatBuilderImpl<S> builder) {
        super(builder.gl(), GLSLBlockMemoryLayout.STD140.layout(builder.structInfo()));
    }

    @Override
    public Set<AttributeUsage> validUsages() {
        return VALID_USAGES;
    }

    @Override
    public BindingLocation<?> bindingLocation(@NonNull LayoutEntry<Std140BlockAttributeFormat<S>> layout, @NonNull BindingLocationAssigner assigner) {
        switch (layout.usage()) {
            case UNIFORM:
                return new InterleavedUniformAttributeBindingLocation<>(layout, assigner);
            case UNIFORM_ARRAY:
                return new InterleavedUniformArrayAttributeBindingLocation<>(layout, assigner);
            case DRAW_LOCAL:
                return new InterleavedDrawLocalAttributeBindingLocation<>(layout, assigner);
            case DRAW_GLOBAL:
                return new InterleavedDrawGlobalAttributeBindingLocation<>(layout, assigner);
            default:
                throw new IllegalArgumentException("unsupported usage: " + layout.usage());
        }
    }
}
