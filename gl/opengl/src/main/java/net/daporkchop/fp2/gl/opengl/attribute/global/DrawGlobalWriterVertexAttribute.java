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

package net.daporkchop.fp2.gl.opengl.attribute.global;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.global.DrawGlobalWriter;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;

/**
 * @author DaPorkchop_
 */
@Getter
public class DrawGlobalWriterVertexAttribute<S> implements DrawGlobalWriter<S> {
    protected final OpenGL gl;
    protected final DrawGlobalFormatVertexAttribute<S> format;
    protected final InterleavedStructFormat<S> structFormat;

    protected final long addr;

    public DrawGlobalWriterVertexAttribute(@NonNull DrawGlobalFormatVertexAttribute<S> format) {
        this.gl = format.gl();
        this.format = format;
        this.structFormat = format.structFormat();

        this.addr = this.gl.directMemoryAllocator().alloc(this.structFormat.stride());
    }

    @Override
    public void close() {
        this.gl.directMemoryAllocator().free(this.addr);
    }

    @Override
    public void set(@NonNull S struct) {
        this.structFormat.copy(struct, null, this.addr);
    }
}