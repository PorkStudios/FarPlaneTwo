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

package net.daporkchop.fp2.client.gl.vertex.buffer;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.vertex.attribute.IVertexAttribute;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractVertexBuffer extends AbstractRefCounted implements IVertexBuffer {
    protected final Allocator alloc;

    @Getter
    protected final VertexFormat format;

    @Getter
    protected int capacity = -1;

    protected final long offsetsStrides; //stored off-heap rather than using a java array in order to avoid array bound checks
    protected final int attributeCount;

    protected final long vertexSize;

    public AbstractVertexBuffer(@NonNull Allocator alloc, @NonNull VertexFormat format) {
        this.alloc = alloc;
        this.format = format;

        this.vertexSize = format.vertexSize();
        this.attributeCount = format.attributes().length;

        //compute offsets/strides from vertex attributes and copy them off-heap
        OffsetStride[] offsetStrides = this.computeOffsetsStrides(format);
        checkState(this.attributeCount == offsetStrides.length);
        this.offsetsStrides = this.alloc.alloc(this.attributeCount * OffsetStride._SIZE);
        for (int i = 0; i < this.attributeCount; i++) {
            OffsetStride._offset(this.offsetsStrides + i * OffsetStride._SIZE, offsetStrides[i].offset());
            OffsetStride._stride(this.offsetsStrides + i * OffsetStride._SIZE, offsetStrides[i].stride());
        }
    }

    protected abstract OffsetStride[] computeOffsetsStrides(@NonNull VertexFormat format);

    @Override

    public IVertexBuffer retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        this.alloc.free(this.offsetsStrides);
    }

    @Override
    public void resize(int capacity) {
        if (this.capacity == notNegative(capacity, "capacity")) { //do nothing if capacity remains unchanged
            return;
        }

        this.resize0(this.capacity, capacity);
        this.capacity = capacity;
    }

    protected abstract void resize0(int oldCapacity, int newCapacity);

    /**
     * @author DaPorkchop_
     */
    @Data
    protected static class OffsetStride {
        /*
         * struct OffsetStride {
         *   int offset;
         *   int stride;
         * };
         */

        protected static final long _OFFSET_OFFSET = 0;
        protected static final long _STRIDE_OFFSET = _OFFSET_OFFSET + INT_SIZE;

        protected static final long _SIZE = _STRIDE_OFFSET + INT_SIZE;

        protected static int _offset(long addr) {
            return PUnsafe.getInt(addr + _OFFSET_OFFSET);
        }

        protected static void _offset(long addr, int offset) {
            PUnsafe.putInt(addr + _OFFSET_OFFSET, offset);
        }

        protected static int _stride(long addr) {
            return PUnsafe.getInt(addr + _STRIDE_OFFSET);
        }

        protected static void _stride(long addr, int stride) {
            PUnsafe.putInt(addr + _STRIDE_OFFSET, stride);
        }

        protected final int offset;
        protected final int stride;
    }

    /**
     * @author DaPorkchop_
     */
    protected abstract class AbstractVertexBuilder extends AbstractRefCounted implements IVertexBuilder {
        protected final long offsetsStrides = AbstractVertexBuffer.this.offsetsStrides;

        @Getter
        protected int size;
        protected int capacity;

        @Override
        public IVertexBuilder retain() throws AlreadyReleasedException {
            super.retain();
            return this;
        }

        @Override
        public int appendVertex() {
            int idx = this.size;
            if (++this.size >= this.capacity) { //we need to grow!
                this.resize(this.capacity = max(multiplyExact(this.capacity, 2), 64));
            }
            return idx;
        }

        protected abstract void resize(int capacity);

        @Override
        public void clear() {
            this.size = 0;
        }
    }
}
