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

package net.daporkchop.fp2.gl.opengl.attribute.common.interleaved;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeWriter;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public final class InterleavedAttributeWriterImpl<S, F extends InterleavedAttributeFormatImpl<S>> implements AttributeWriter<S> {
    protected final OpenGL gl;
    protected final F format;
    protected final InterleavedStructFormat<S> structFormat;

    protected long baseAddr;
    protected final long stride;

    protected int index;
    protected int capacity;

    public InterleavedAttributeWriterImpl(@NonNull F format) {
        this.gl = format.gl();
        this.format = format;
        this.structFormat = format.structFormat();

        this.stride = this.structFormat.stride();

        this.resize(16);
    }

    @Override
    public void close() {
        this.gl.directMemoryAllocator().free(this.baseAddr);
    }

    @Override
    public int size() {
        return this.index;
    }

    @Override
    public int put(@NonNull S struct) {
        if (this.index + 1 == this.capacity) { //grow buffer if needed
            this.resize(this.capacity << 1);
        }

        this.structFormat.copy(struct, null, this.baseAddr + this.index * this.stride);
        return this.index++;
    }

    protected void resize(int capacity) {
        checkArg(capacity > this.capacity, "cannot resize from %d to %d", this.capacity, capacity);

        this.capacity = capacity;
        this.baseAddr = this.gl.directMemoryAllocator().realloc(this.baseAddr, capacity * this.stride);
    }
}
