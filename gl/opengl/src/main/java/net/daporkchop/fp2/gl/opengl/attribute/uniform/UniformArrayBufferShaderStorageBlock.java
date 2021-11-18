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
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayFormat;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.ShaderStorageBlockBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.buffer.GLBufferImpl;

/**
 * @author DaPorkchop_
 */
@Getter
public class UniformArrayBufferShaderStorageBlock<S> extends BaseAttributeBufferImpl<S, UniformArrayFormatShaderStorageBlock<S>, UniformArrayFormat<S>> implements UniformArrayBuffer<S>, ShaderStorageBlockBuffer {
    protected final InterleavedStructFormat<S> structFormat;

    protected final GLBufferImpl internalBuffer;

    protected final long stride;

    public UniformArrayBufferShaderStorageBlock(@NonNull UniformArrayFormatShaderStorageBlock<S> format, @NonNull BufferUsage usage) {
        super(format);
        this.structFormat = format.structFormat();

        this.internalBuffer = this.gl.createBuffer(usage);
        this.stride = format.structFormat().stride();
    }

    @Override
    public void close() {
        this.internalBuffer.close();
    }

    @Override
    public void set(@NonNull S[] structs) {
        //set buffer capacity
        this.internalBuffer.capacity(structs.length * this.stride);

        //map the entire buffer write-only and copy the structs into it
        this.internalBuffer.map(false, true, addr -> {
            for (S struct : structs) {
                this.structFormat.copy(struct, null, addr);
                addr += this.stride;
            }
        });
    }
}
