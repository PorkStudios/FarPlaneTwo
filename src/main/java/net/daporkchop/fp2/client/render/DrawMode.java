/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.mode.common.client.FarRenderIndex;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.misc.string.PUnsafeStrings;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;

import java.util.stream.IntStream;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public enum DrawMode implements AutoCloseable {
    MULTIDRAW {
        @Override
        public DrawMode start(@NonNull FarRenderIndex index, @NonNull GLBuffer commandBuffer, @NonNull IShaderHolder shaders) {
            super.start(index, commandBuffer, shaders);
            commandBuffer.bind(GL_DRAW_INDIRECT_BUFFER);
            return this;
        }

        @Override
        public void close() {
            this.commandBuffer.close();
            super.close();
        }

        @Override
        public void draw(@NonNull RenderPass pass, int passNumber) {
            int tileCount = this.index.upload(passNumber, this.commandBuffer);
            if (tileCount > 0) {
                pass.render(this, tileCount);
            }
        }

        @Override
        void draw0(int tileCount) {
            glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, FarRenderIndex.POSITION_SIZE_BYTES, tileCount, FarRenderIndex.ENTRY_SIZE_BYTES);
        }
    },
    TRANSFORM_FEEDBACK {
        protected final GLBuffer buffer = new GLBuffer(GL_DYNAMIC_COPY);
        protected final int query = glGenQueries();

        protected final VertexArrayObject vao = new VertexArrayObject();

        @Override
        public DrawMode start(@NonNull FarRenderIndex index, @NonNull GLBuffer commandBuffer, @NonNull IShaderHolder shaders) {
            super.start(index, commandBuffer, shaders);
            commandBuffer.bind(GL_DRAW_INDIRECT_BUFFER);
            return this;
        }

        @Override
        public void close() {
            this.commandBuffer.close();
            super.close();
        }

        @Override
        public void draw(@NonNull RenderPass pass, int passNumber) {
            if (pass != RenderPass.SOLID) {
                return;
            }

            int tileCount = this.index.upload(passNumber, this.commandBuffer);
            if (tileCount <= 0) {
                return;
            }

            int requiredCapacity = IntStream.range(0, tileCount).map(i -> this.index.buffers()[passNumber].get(i * FarRenderIndex.ENTRY_SIZE + FarRenderIndex.POSITION_SIZE)).sum();
            requiredCapacity *= 11 * FLOAT_SIZE;
            if (this.buffer.capacity() != requiredCapacity) { //ensure buffer is big enough to fit all the data
                checkGLError("pre capacity");
                try (GLBuffer buffer = this.buffer.bind(GL_TRANSFORM_FEEDBACK_BUFFER)) {
                    buffer.capacity(requiredCapacity);
                }
                checkGLError("post capacity");
            }
            this.buffer.bindBase(GL_TRANSFORM_FEEDBACK_BUFFER, 0);
            checkGLError("post bind xfb buffer");

            checkGLError("pre xfb");
            try (ShaderProgram program = this.shaders.getAndUseShader(this, pass, false)) {
                checkGLError("post use shader");
                this.draw0(tileCount);
            }
            checkGLError("post xfb");
            glBindVertexArray(0);

            //GlStateManager.disableCull();

            try (GLBuffer buffer = this.buffer.bind(GL_ARRAY_BUFFER)) {
                checkGLError("pre state init");

                GlStateManager.glEnableClientState(32884);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.glEnableClientState(32888);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.glEnableClientState(32888);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.glEnableClientState(32886);

                GlStateManager.resetColor();

                checkGLError("pre vertex init");

                GlStateManager.glVertexPointer(3, GL_FLOAT, 11 * FLOAT_SIZE, 0);
                GlStateManager.glColorPointer(4, GL_FLOAT, 11 * FLOAT_SIZE, 3 * FLOAT_SIZE);
                GlStateManager.glTexCoordPointer(2, GL_FLOAT, 11 * FLOAT_SIZE, 7 * FLOAT_SIZE);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.glTexCoordPointer(2, GL_FLOAT, 11 * FLOAT_SIZE, 9 * FLOAT_SIZE);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);

                checkGLError("pre render");

                //glDrawTransformFeedback(GL_TRIANGLES, this.buffer.id());
                glDrawArrays(GL_TRIANGLES, 0, glGetQueryObjectui(this.query, GL_QUERY_RESULT) * 3);

                checkGLError("pre reset state");

                GlStateManager.glDisableClientState(32884);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.glDisableClientState(32888);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.glDisableClientState(32888);
                OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
                GlStateManager.glDisableClientState(32886);

                checkGLError("post");
            }
            checkGLError("post2");

            //GlStateManager.enableCull();
        }

        @Override
        void draw0(int tileCount) {
            glEnable(GL_RASTERIZER_DISCARD);
            checkGLError("enable discard");

            glBeginQuery(GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN, this.query);
            glBeginTransformFeedback(GL_TRIANGLES);
            checkGLError("begin xfb");

            glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, FarRenderIndex.POSITION_SIZE_BYTES, tileCount, FarRenderIndex.ENTRY_SIZE_BYTES);
            checkGLError("glMultiDrawElementsIndirect");

            glEndTransformFeedback();
            checkGLError("end xfb");

            glEndQuery(GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN);

            glDisable(GL_RASTERIZER_DISCARD);
            checkGLError("disable discard");

            glFlush();
            checkGLError("flush");

            //Constants.LOGGER.info("transform feedback rendered {} primitives", glGetQueryObjectui(this.query, GL_QUERY_RESULT));
        }
    };

    protected FarRenderIndex index;
    protected GLBuffer commandBuffer;
    protected IShaderHolder shaders;

    DrawMode() {
        PUnsafeStrings.setEnumName(this, PStrings.split(this.name(), '_').titleFormat().join(' ').intern());
    }

    public DrawMode start(@NonNull FarRenderIndex index, @NonNull GLBuffer commandBuffer, @NonNull IShaderHolder shaders) {
        this.index = index;
        this.commandBuffer = commandBuffer;
        this.shaders = shaders;
        return this;
    }

    @Override
    public void close() {
        this.index = null;
        this.commandBuffer = null;
        this.shaders = null;
    }

    public abstract void draw(@NonNull RenderPass pass, int passNumber);

    abstract void draw0(int tileCount);
}
