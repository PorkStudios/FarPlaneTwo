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

package net.daporkchop.fp2.gl.opengl.attribute.glsl;

import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.IAttributeFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.attribute.InternalAttributeUsage;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.uniform.InterleavedDrawGlobalAttributeBindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.uniform.InterleavedDrawLocalAttributeBindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.uniform.InterleavedUniformArrayAttributeBindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.uniform.InterleavedUniformAttributeBindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLBlockMemoryLayout;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author DaPorkchop_
 */
public final class GLSLBlockAttributeFormat<S> extends InterleavedAttributeFormatImpl<S> {
    public static final Set<InternalAttributeUsage> VALID_USAGES = ImmutableSet.copyOf(EnumSet.of(
            InternalAttributeUsage.UNIFORM,
            InternalAttributeUsage.UNIFORM_ARRAY,
            InternalAttributeUsage.DRAW_LOCAL,
            InternalAttributeUsage.DRAW_GLOBAL
    ));

    public GLSLBlockAttributeFormat(@NonNull IAttributeFormatBuilderImpl<S> builder, @NonNull GLSLBlockMemoryLayout layout) {
        super(builder.gl(), layout.layout(builder.structInfo()));
    }

    @Override
    public Set<InternalAttributeUsage> validUsages() {
        return VALID_USAGES;
    }

    @Override
    public BindingLocation<?> bindingLocation(@NonNull InternalAttributeUsage usage, @NonNull BindingLocationAssigner assigner) {
        switch (usage) {
            case UNIFORM:
                return new InterleavedUniformAttributeBindingLocation<>(this.structFormat(), assigner);
            case UNIFORM_ARRAY:
                return new InterleavedUniformArrayAttributeBindingLocation<>(this.structFormat(), assigner);
            case DRAW_LOCAL:
                return new InterleavedDrawLocalAttributeBindingLocation<>(this.structFormat(), assigner);
            case DRAW_GLOBAL:
                return new InterleavedDrawGlobalAttributeBindingLocation<>(this.structFormat(), assigner);
            default:
                throw new IllegalArgumentException("unsupported usage: " + usage);
        }
    }
}
