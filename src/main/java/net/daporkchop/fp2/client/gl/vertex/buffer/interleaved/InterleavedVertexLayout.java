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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.vertex.attribute.IVertexAttribute;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.fp2.client.gl.vertex.buffer.AbstractVertexLayout;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuffer;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuilder;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter(AccessLevel.PROTECTED)
public class InterleavedVertexLayout extends AbstractVertexLayout<InterleavedVertexLayout> {
    protected final long offsets; //off-heap array to avoid array bound checks
    protected final long vertexSize;

    protected final int attribCount;

    public InterleavedVertexLayout(@NonNull Allocator alloc, @NonNull VertexFormat format) {
        super(alloc, format);

        this.vertexSize = this.format.vertexSize();

        IVertexAttribute[] attribs = this.format.attributes();
        this.attribCount = attribs.length;
        this.offsets = this.alloc.alloc(attribs.length * (long) Integer.BYTES);

        int offset = 0;
        for (int i = 0; i < attribs.length; i++) {
            IVertexAttribute attrib = attribs[i];
            PUnsafe.putInt(this.offsets + i * (long) Integer.BYTES, offset);
            offset += attrib.size();
        }
    }

    @Override
    protected void doRelease() {
        this.alloc.free(this.offsets);
    }

    @Override
    public IVertexBuffer createBuffer() {
        return new InterleavedVertexBuffer(this.retain());
    }

    @Override
    public IVertexBuilder createBuilder() {
        return new InterleavedVertexBuilder(this.retain());
    }

    protected int offset(int attribIndex) {
        return PUnsafe.getInt(this.offsets + checkIndex(this.attribCount, attribIndex) * (long) Integer.BYTES);
    }
}
