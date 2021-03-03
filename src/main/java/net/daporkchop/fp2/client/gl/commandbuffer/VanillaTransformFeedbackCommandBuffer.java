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
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.TransformFeedbackObject;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class VanillaTransformFeedbackCommandBuffer implements IDrawCommandBuffer {
    protected static final int VERTEX_BYTES = 3 * FLOAT_SIZE + 1 * INT_SIZE + 2 * FLOAT_SIZE + 1 * INT_SIZE;

    protected final GLBuffer buffer = new GLBuffer(GL_DYNAMIC_COPY);
    protected final TransformFeedbackObject xfb = new TransformFeedbackObject();

    @NonNull
    protected final IDrawCommandBuffer delegate;
    @NonNull
    protected final ShaderProgram shader;

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
        long minBytes = this.vertices * VERTEX_BYTES;
        if (minBytes > this.buffer.capacity()) {
            try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) {
                buffer.capacity(minBytes);
            }
        }

        //bind transform feedback object, shader and buffer, then execute a draw
        try (TransformFeedbackObject xfb = this.xfb.bind();
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
        try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) {
            GlStateManager.glVertexPointer(3, GL_FLOAT, VERTEX_BYTES, 0);
            GlStateManager.glColorPointer(4, GL_UNSIGNED_BYTE, VERTEX_BYTES, 3 * FLOAT_SIZE);
            GlStateManager.glTexCoordPointer(2, GL_FLOAT, VERTEX_BYTES, 3 * FLOAT_SIZE + 1 * INT_SIZE);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.glTexCoordPointer(2, GL_SHORT, VERTEX_BYTES, 3 * FLOAT_SIZE + 1 * INT_SIZE + 2 * FLOAT_SIZE);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);

            //optifine shader stuff
            /*glNormalPointer(GL_FLOAT, VERTEX_BYTES, 11 * FLOAT_SIZE);
            glVertexAttribPointer(OF_SHADERS_MIDTEXCOORDATTRIB, 2, GL_FLOAT, false, VERTEX_BYTES, 14 * FLOAT_SIZE);
            glVertexAttribPointer(OF_SHADERS_TANGENTATTRIB, 4, GL_FLOAT, false, VERTEX_BYTES, 16 * FLOAT_SIZE);
            glVertexAttribPointer(OF_SHADERS_ENTITYATTRIB, 3, GL_FLOAT, false, VERTEX_BYTES, 20 * FLOAT_SIZE);*/

            this.xfb.draw(GL_TRIANGLES);
        }
    }

    @Override
    public void delete() {
        this.delegate.delete();

        this.buffer.delete();
        this.xfb.delete();
    }
}
