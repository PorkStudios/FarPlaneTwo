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

package net.daporkchop.fp2.gl.opengl.vertex;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.vertex.VertexAttribute;
import net.daporkchop.fp2.gl.vertex.VertexWriter;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * @author DaPorkchop_
 */
public abstract class VertexWriterImpl implements VertexWriter {
    @Getter
    protected final VertexFormatImpl format;
    protected final Allocator alloc = new DirectMemoryAllocator();

    protected int index;
    protected int capacity;

    protected VertexWriterImpl(@NonNull VertexFormatImpl format) {
        this.format = format;
    }

    @Override
    public int size() {
        return this.index;
    }

    @Override
    public int endVertex() {
        if (this.index + 1 == this.capacity) { //grow buffer if needed
            this.setCapacity(this.capacity << 1);
        }
        return this.index++;
    }

    protected abstract void setCapacity(int capacity);

    protected abstract long offset(int vertexIndex, int attributeIndex);

    @Override
    public VertexWriter set(@NonNull VertexAttribute.Int1 attribIn, int v0) {
        VertexAttributeImpl.Int1 attrib = (VertexAttributeImpl.Int1) attribIn;
        attrib.set(null, this.offset(this.index, attrib.index), v0);
        return this;
    }

    @Override
    public VertexWriter set(@NonNull VertexAttribute.Int2 attribIn, int v0, int v1) {
        VertexAttributeImpl.Int2 attrib = (VertexAttributeImpl.Int2) attribIn;
        attrib.set(null, this.offset(this.index, attrib.index), v0, v1);
        return this;
    }

    @Override
    public VertexWriter set(@NonNull VertexAttribute.Int3 attribIn, int v0, int v1, int v2) {
        VertexAttributeImpl.Int3 attrib = (VertexAttributeImpl.Int3) attribIn;
        attrib.set(null, this.offset(this.index, attrib.index), v0, v1, v2);
        return this;
    }

    @Override
    public VertexWriter setARGB(@NonNull VertexAttribute.Int3 attribIn, int argb) {
        VertexAttributeImpl.Int3 attrib = (VertexAttributeImpl.Int3) attribIn;
        attrib.setARGB(null, this.offset(this.index, attrib.index), argb);
        return this;
    }

    @Override
    public VertexWriter set(@NonNull VertexAttribute.Int4 attribIn, int v0, int v1, int v2, int v3) {
        VertexAttributeImpl.Int4 attrib = (VertexAttributeImpl.Int4) attribIn;
        attrib.set(null, this.offset(this.index, attrib.index), v0, v1, v2, v3);
        return this;
    }

    @Override
    public VertexWriter setARGB(@NonNull VertexAttribute.Int4 attribIn, int argb) {
        VertexAttributeImpl.Int4 attrib = (VertexAttributeImpl.Int4) attribIn;
        attrib.setARGB(null, this.offset(this.index, attrib.index), argb);
        return this;
    }

    /**
     * @author DaPorkchop_
     */
    public static class Interleaved extends VertexWriterImpl {
        protected final long stride = this.format.size();
        protected final long[] offsets = new long[this.format.attribs.length];

        protected long addr;

        protected Interleaved(@NonNull VertexFormatImpl format) {
            super(format);

            this.setCapacity(16);
        }

        @Override
        protected void setCapacity(int capacity) {
            this.capacity = capacity;
            this.addr = this.alloc.realloc(this.addr, capacity * this.stride);

            for (VertexAttributeImpl attrib : this.format.attribs) {
                this.offsets[attrib.index] = this.addr + attrib.offset;
            }
        }

        @Override
        protected long offset(int vertexIndex, int attributeIndex) {
            return vertexIndex * this.stride + this.offsets[attributeIndex];
        }

        @Override
        public VertexWriter copyFrom(int srcVertexIndex) {
            //who needs parameter validation anyway, amiright?
            PUnsafe.copyMemory(srcVertexIndex * this.stride + this.addr, this.index * this.stride + this.addr, this.stride);
            return this;
        }

        @Override
        public void close() {
            this.alloc.free(this.addr);
        }
    }
}
