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
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.vertex.attribute.IVertexAttribute;
import net.daporkchop.fp2.client.gl.vertex.buffer.AbstractVertexBuffer;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuilder;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
public class InterleavedVertexBuffer extends AbstractVertexBuffer<InterleavedVertexLayout> {
    protected final GLBuffer buffer = new GLBuffer(GL_DYNAMIC_DRAW);

    protected final long vertexSize;

    protected InterleavedVertexBuffer(@NonNull InterleavedVertexLayout layout) {
        super(layout);

        this.vertexSize = this.layout.vertexSize();
    }

    @Override
    public void configureVAO(@NonNull VertexArrayObject vao) {
        IVertexAttribute[] attributes = this.layout.format().attributes();

        for (int i = 0; i < attributes.length; i++) {
            attributes[i].configureVAO(vao, this.buffer, this.layout.offset(i), toInt(this.vertexSize));
        }
    }

    @Override
    protected void resize0(int oldCapacity, int newCapacity) {
        try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) {
            buffer.resize(newCapacity * this.vertexSize);
        }
    }

    @Override
    public void set(int startIndex, @NonNull IVertexBuilder builderIn) {
        checkArg(builderIn.layout() == this.layout, "builder must use %s (given=%s)", this.layout, builderIn.layout());

        InterleavedVertexBuilder builder = (InterleavedVertexBuilder) builderIn;
        checkRangeLen(this.capacity, startIndex, builder.size());

        try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) { //upload vertex data to GPU in one go
            buffer.uploadRange(startIndex * this.vertexSize, builder.vertexAddr, builder.size() * this.vertexSize);
        }
    }

}
