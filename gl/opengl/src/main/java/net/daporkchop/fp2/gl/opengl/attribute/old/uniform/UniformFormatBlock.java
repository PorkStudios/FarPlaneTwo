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

package net.daporkchop.fp2.gl.opengl.attribute.old.uniform;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.old.uniform.UniformBuffer;
import net.daporkchop.fp2.gl.attribute.old.uniform.UniformFormat;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.UniformBlockFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLBlockMemoryLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructInfo;

/**
 * @author DaPorkchop_
 */
@Getter
public class UniformFormatBlock<S> extends BaseAttributeFormatImpl<S> implements UniformFormat<S>, UniformBlockFormat {
    public UniformFormatBlock(@NonNull AttributeFormatBuilderImpl<S> builder) {
        super(builder.gl(), builder.gl().structFormatGenerator().getInterleaved(GLSLBlockMemoryLayout.STD140.layout(new StructInfo<>(builder))));
    }

    @Override
    public UniformBuffer<S> createBuffer(@NonNull BufferUsage usage) {
        return new UniformBufferBlock<>(this, usage);
    }

    @Override
    public String interfaceBlockLayout() {
        return this.structFormat.layoutName();
    }
}
