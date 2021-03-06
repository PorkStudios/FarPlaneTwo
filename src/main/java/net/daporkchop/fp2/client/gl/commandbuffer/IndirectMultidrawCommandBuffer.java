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

package net.daporkchop.fp2.client.gl.commandbuffer;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;

import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public abstract class IndirectMultidrawCommandBuffer extends AbstractDrawCommandBuffer {
    protected final VertexArrayObject vao = new VertexArrayObject();

    public IndirectMultidrawCommandBuffer(long entrySize, @NonNull Consumer<VertexArrayObject> vaoInitializer) {
        super(entrySize);

        try (VertexArrayObject vao = this.vao.bindForChange()) {
            vao.attrI(this.buffer, 4, GL_INT, toInt(this.entrySize), 0, 1);
            vaoInitializer.accept(vao);
        }
    }

    public IndirectMultidrawCommandBuffer(long entrySize, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray) {
        super(entrySize);

        try (VertexArrayObject vao = this.vao.bindForChange()) {
            vao.attrI(this.buffer, 4, GL_INT, toInt(this.entrySize), 0, 1);
            vaoInitializer.accept(vao);

            vao.putElementArray(elementArray);
        }
    }

    @Override
    protected void upload(long addr, long size) {
        try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
            buffer.upload(addr, size);
        }
    }

    @Override
    protected void draw0() {
        try (VertexArrayObject vao = this.vao.bind();
             GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
            this.multidraw0();
        }
    }

    protected abstract void multidraw0();
}
