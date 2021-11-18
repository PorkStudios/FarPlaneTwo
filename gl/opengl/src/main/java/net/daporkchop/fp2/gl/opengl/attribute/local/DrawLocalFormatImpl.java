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

package net.daporkchop.fp2.gl.opengl.attribute.local;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalBuffer;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalFormat;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalWriter;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.VertexAttributeFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructInfo;
import net.daporkchop.fp2.gl.opengl.attribute.struct.VertexAttributeLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;

/**
 * @author DaPorkchop_
 */
@Getter
public class DrawLocalFormatImpl<S> extends BaseAttributeFormatImpl<S, InterleavedStructFormat<S>> implements DrawLocalFormat<S>, VertexAttributeFormat {
    public DrawLocalFormatImpl(@NonNull OpenGL gl, @NonNull Class<S> clazz) {
        super(gl, gl.structFormatGenerator().getInterleaved(VertexAttributeLayout.interleaved(gl, new StructInfo<>(clazz))));
    }

    @Override
    public DrawLocalWriter<S> createWriter() {
        return new DrawLocalWriterImpl<>(this);
    }

    @Override
    public DrawLocalBuffer<S> createBuffer(@NonNull BufferUsage usage) {
        return new DrawLocalBufferImpl<>(this, usage);
    }
}
