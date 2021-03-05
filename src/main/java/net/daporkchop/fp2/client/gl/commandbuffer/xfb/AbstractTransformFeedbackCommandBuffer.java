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

package net.daporkchop.fp2.client.gl.commandbuffer.xfb;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.client.DrawMode;
import net.daporkchop.fp2.client.gl.commandbuffer.IDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.TransformFeedbackObject;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractTransformFeedbackCommandBuffer implements IDrawCommandBuffer {
    protected final GLBuffer buffer = new GLBuffer(GL_DYNAMIC_COPY);
    protected final TransformFeedbackObject xfb = new TransformFeedbackObject();

    @NonNull
    protected final IDrawCommandBuffer delegate;
    @NonNull
    protected final ShaderProgram shader;
    protected final int vertexSize;

    protected long vertices;

    @Override
    public IDrawCommandBuffer begin() {
        this.delegate.begin();
        return this;
    }

    @Override
    public void drawElements(int x, int y, int z, int level, int baseVertex, int firstIndex, int count) {
        this.delegate.drawElements(x, y, z, level, baseVertex, firstIndex, count); //proxy to delegate, we don't manage the command buffer ourselves
    }

    @Override
    public void close() {
        this.delegate.close();
        this.vertices = this.delegate.vertexCount();

        //ensure the buffer is big enough to fit all the vertices
        long minBytes = this.vertices * this.vertexSize;
        if (minBytes > this.buffer.capacity()) {
            try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) {
                buffer.capacity(minBytes);
            }
        }

        //bind transform feedback object, shader and buffer, then execute a draw
        try (TransformFeedbackObject xfb = this.xfb.bind();
             DrawMode drawMode = DrawMode.SHADER.begin(); //we're going to be doing "rendering" using a shader, so we need to be in shader mode
             ShaderProgram shader = this.shader.use()) {
            this.buffer.bindBase(GL_TRANSFORM_FEEDBACK_BUFFER, 0);

            glEnable(GL_RASTERIZER_DISCARD);
            glBeginTransformFeedback(GL_TRIANGLES);

            this.delegate.draw();

            glEndTransformFeedback();
            glDisable(GL_RASTERIZER_DISCARD);
        }
    }

    @Override
    public long vertexCount() {
        return this.vertices;
    }

    @Override
    public void draw() {
        DrawMode.LEGACY.require();

        try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) {
            this.configureVertexAttributes();

            this.xfb.draw(GL_TRIANGLES);
        }
    }

    protected abstract void configureVertexAttributes();

    @Override
    public void delete() {
        this.delegate.delete();

        this.buffer.delete();
        this.xfb.delete();
    }
}
