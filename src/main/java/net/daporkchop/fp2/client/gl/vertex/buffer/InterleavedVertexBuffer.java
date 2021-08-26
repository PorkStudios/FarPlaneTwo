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

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.vertex.attribute.IVertexAttribute;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
public class InterleavedVertexBuffer extends AbstractVertexBuffer {
    protected final GLBuffer buffer = new GLBuffer(GL_DYNAMIC_DRAW);

    public InterleavedVertexBuffer(@NonNull Allocator alloc, @NonNull VertexFormat format) {
        super(alloc, format);
    }

    @Override
    public void configureVAO(@NonNull VertexArrayObject vao) {
        IVertexAttribute[] attributes = this.format.attributes();

        for (int i = 0; i < this.attributeCount; i++) {
            long addr = this.offsetsStrides + i * OffsetStride._SIZE;
            attributes[i].configureVAO(vao, this.buffer, OffsetStride._offset(addr), OffsetStride._stride(addr));
        }
    }

    @Override
    protected OffsetStride[] computeOffsetsStrides(@NonNull VertexFormat format) {
        IVertexAttribute[] attributes = format.attributes();

        //offset is the sum of the sizes of the previous attributes, stride is the total size
        OffsetStride[] offsetStrides = new OffsetStride[attributes.length];
        for (int i = 0, offset = 0; i < attributes.length; i++) {
            offsetStrides[i] = new OffsetStride(offset, format.vertexSize());
            offset += attributes[i].size();
        }
        return offsetStrides;
    }

    @Override
    protected void resize0(int oldCapacity, int newCapacity) {
        try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) {
            buffer.resize(newCapacity * this.vertexSize);
        }
    }

    @Override
    public void set(int startIndex, @NonNull IVertexBuilder builderIn) {
        VertexBuilder builder = (VertexBuilder) builderIn;
        checkRangeLen(this.capacity, startIndex, builder.size);

        try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) { //upload vertex data to GPU in one go
            buffer.uploadRange(startIndex * this.vertexSize, builder.vertexAddr, builder.size * this.vertexSize);
        }
    }

    @Override
    public IVertexBuilder builder() {
        return new VertexBuilder();
    }

    /**
     * @author DaPorkchop_
     */
    protected class VertexBuilder extends AbstractVertexBuilder {
        protected final long stride = InterleavedVertexBuffer.this.vertexSize;

        protected long vertexAddr = 0L;

        @Override
        protected void resize(int capacity) {
            this.capacity = notNegative(capacity, "capacity");
            this.vertexAddr = this.vertexAddr == 0L
                    ? InterleavedVertexBuffer.this.alloc.alloc(capacity * this.stride)
                    : InterleavedVertexBuffer.this.alloc.realloc(this.vertexAddr, capacity * this.stride);
        }

        @Override
        public long addressFor(@NonNull IVertexAttribute attribute, int vertIndex) {
            return this.vertexAddr + checkIndex(this.size, vertIndex) * this.stride + OffsetStride._offset(this.offsetsStrides + attribute.index() * OffsetStride._SIZE);
        }

        @Override
        public int appendDuplicateVertex(int srcIndex) {
            checkIndex(this.size, srcIndex);

            int dstIndex = this.appendVertex();
            //copy all attributes at once in a single run
            PUnsafe.copyMemory(this.vertexAddr + srcIndex * this.stride, this.vertexAddr + dstIndex * this.stride, this.stride);
            return dstIndex;
        }

        @Override
        protected void doRelease() {
            if (this.vertexAddr != 0L) {
                InterleavedVertexBuffer.this.alloc.free(this.vertexAddr);
            }
        }
    }
}
