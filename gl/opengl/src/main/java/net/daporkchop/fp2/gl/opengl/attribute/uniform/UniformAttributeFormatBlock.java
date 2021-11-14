/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.attribute.uniform;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.uniform.UniformAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformAttributeFormat;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.UniformBlockFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLBlockMemoryLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructInfo;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;

import java.util.List;

/**
 * @author DaPorkchop_
 */
@Getter
public class UniformAttributeFormatBlock<S> extends BaseAttributeFormatImpl<S> implements UniformAttributeFormat<S>, UniformBlockFormat {
    protected final InterleavedStructFormat<S> structFormat;

    public UniformAttributeFormatBlock(@NonNull OpenGL gl, @NonNull Class<S> clazz) {
        super(gl);

        this.structFormat = gl.structFormatGenerator().getInterleaved(GLSLBlockMemoryLayout.STD140.layout(new StructInfo<>(clazz)));
    }

    @Override
    public String name() {
        return this.structFormat.structName();
    }

    @Override
    public List<GLSLField> attributeFields() {
        return this.structFormat.glslFields();
    }

    @Override
    public UniformAttributeBuffer<S> createBuffer(@NonNull BufferUsage usage) {
        return new UniformAttributeBufferBlock<>(this, usage);
    }
}
