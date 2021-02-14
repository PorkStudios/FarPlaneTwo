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

package net.daporkchop.fp2.client.render;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class IndirectMultiDrawMode implements IDrawMode {
    public static final int POSITION_SIZE = 4;
    public static final int COMMAND_SIZE = 5;
    public static final int ENTRY_SIZE = POSITION_SIZE + COMMAND_SIZE;

    public static final int POSITION_SIZE_BYTES = POSITION_SIZE * INT_SIZE;
    public static final int COMMAND_SIZE_BYTES = COMMAND_SIZE * INT_SIZE;
    public static final int ENTRY_SIZE_BYTES = POSITION_SIZE_BYTES + COMMAND_SIZE_BYTES;

    protected final GLBuffer buffer = new GLBuffer(GL_STREAM_DRAW);

    protected long mappingAddress = -1L;

    protected int size;
    protected int capacity;

    public IndirectMultiDrawMode(int attributeIndex, @NonNull VertexArrayObject vao) {
        try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) {
            glVertexAttribIPointer(attributeIndex, 4, GL_INT, ENTRY_SIZE_BYTES, 0L);
            glVertexAttribDivisor(attributeIndex, 1);
            vao.putDependency(attributeIndex, buffer);

            this.capacity = 1;
            buffer.capacity(this.capacity * ENTRY_SIZE_BYTES);
        }
    }

    @Override
    public IDrawMode begin() {
        this.size = 0;

        try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
            this.mappingAddress = buffer.map(GL_WRITE_ONLY);
        }
        return this;
    }

    @Override
    public void drawElements(int tileX, int tileY, int tileZ, int level, int baseVertex, int firstIndex, int count) {
        int size = this.size++;
        if (size == this.capacity) {
            this.grow();
        }

        long baseAddr = this.mappingAddress + (long) size * ENTRY_SIZE_BYTES;
        PUnsafe.putInt(baseAddr + 0 * INT_SIZE, tileX);
        PUnsafe.putInt(baseAddr + 1 * INT_SIZE, tileY);
        PUnsafe.putInt(baseAddr + 2 * INT_SIZE, tileZ);
        PUnsafe.putInt(baseAddr + 3 * INT_SIZE, level);

        PUnsafe.putInt(baseAddr + 4 * INT_SIZE, count); //count
        PUnsafe.putInt(baseAddr + 5 * INT_SIZE, 1); //instanceCount
        PUnsafe.putInt(baseAddr + 6 * INT_SIZE, firstIndex); //firstIndex
        PUnsafe.putInt(baseAddr + 7 * INT_SIZE, baseVertex); //baseVertex
        PUnsafe.putInt(baseAddr + 8 * INT_SIZE, size); //baseInstance
    }

    protected void grow() {
        try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
            buffer.unmap();

            this.capacity <<= 1;
            buffer.resize((long) this.capacity * ENTRY_SIZE_BYTES);

            this.mappingAddress = buffer.map(GL_WRITE_ONLY);
        }
    }

    @Override
    public void close() {
        try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
            buffer.unmap();
        }
    }

    @Override
    public void draw() {
        try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
            glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, POSITION_SIZE_BYTES, this.size, ENTRY_SIZE_BYTES);
        }
    }
}
