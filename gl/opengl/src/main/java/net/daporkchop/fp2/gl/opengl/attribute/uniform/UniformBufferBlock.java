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
import net.daporkchop.fp2.gl.attribute.uniform.UniformBuffer;
import net.daporkchop.fp2.gl.attribute.uniform.UniformFormat;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.UniformBlockBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.buffer.GLBuffer;

/**
 * @author DaPorkchop_
 */
@Getter
public class UniformBufferBlock<S> extends BaseAttributeBufferImpl<S, UniformFormatBlock<S>, UniformFormat<S>> implements UniformBuffer<S>, UniformBlockBuffer {
    protected final InterleavedStructFormat<S> structFormat;

    protected final GLBuffer internalBuffer;

    protected final long addr;
    protected final long stride;

    public UniformBufferBlock(@NonNull UniformFormatBlock<S> format, @NonNull BufferUsage usage) {
        super(format);
        this.structFormat = format.structFormat();

        this.internalBuffer = this.gl.createBuffer(usage);
        this.stride = format.structFormat().stride();
        this.addr = this.gl.directMemoryAllocator().alloc(this.stride);
    }

    @Override
    public void close() {
        this.gl.directMemoryAllocator().free(this.addr);
        this.internalBuffer.close();
    }

    @Override
    public void set(@NonNull S struct) {
        this.structFormat.copy(struct, null, this.addr);
        this.internalBuffer.upload(this.addr, this.stride);
    }
}
