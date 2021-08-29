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

package net.daporkchop.fp2.client.gl.vertex.buffer.interleaved;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.vertex.attribute.IVertexAttribute;
import net.daporkchop.fp2.client.gl.vertex.buffer.AbstractVertexBuilder;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class InterleavedVertexBuilder extends AbstractVertexBuilder<InterleavedVertexLayout> {
    protected final Allocator alloc;
    protected final long vertexSize;

    protected final long offsets; //shared pointer, managed by InterleavedVertexLayout

    protected long vertexAddr = 0L;

    protected InterleavedVertexBuilder(@NonNull InterleavedVertexLayout layout) {
        super(layout);

        this.alloc = this.layout.alloc();
        this.vertexSize = this.layout.vertexSize();
        this.offsets = this.layout.offsets();
    }

    @Override
    protected void resize(int capacity) {
        this.capacity = notNegative(capacity, "capacity");
        this.vertexAddr = this.alloc.realloc(this.vertexAddr, capacity * this.vertexSize);
    }

    @Override
    public long addressFor(@NonNull IVertexAttribute attribute, int vertIndex) {
        return this.vertexAddr + checkIndex(this.size, vertIndex) * this.vertexSize + PUnsafe.getInt(this.offsets + attribute.index() * (long) Integer.BYTES);
    }

    @Override
    public int appendDuplicateVertex(int srcIndex) {
        checkIndex(this.size, srcIndex);

        int dstIndex = this.appendVertex();
        //copy all attributes at once in a single run
        PUnsafe.copyMemory(this.vertexAddr + srcIndex * this.vertexSize, this.vertexAddr + dstIndex * this.vertexSize, this.vertexSize);
        return dstIndex;
    }

    @Override
    protected void doRelease() {
        super.doRelease();

        if (this.vertexAddr != 0L) {
            this.alloc.free(this.vertexAddr);
        }
    }
}
