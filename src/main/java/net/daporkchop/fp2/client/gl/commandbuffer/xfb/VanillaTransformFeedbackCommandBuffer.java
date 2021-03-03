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
import net.daporkchop.fp2.client.gl.commandbuffer.IDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.minecraft.client.renderer.OpenGlHelper;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/**
 * @author DaPorkchop_
 */
public class VanillaTransformFeedbackCommandBuffer extends AbstractTransformFeedbackCommandBuffer {
    protected static final int VERTEX_BYTES = 3 * FLOAT_SIZE //pos
                                              + 1 * INT_SIZE //color
                                              + 2 * FLOAT_SIZE //uv
                                              + 1 * INT_SIZE; //light

    public VanillaTransformFeedbackCommandBuffer(@NonNull IDrawCommandBuffer delegate, @NonNull ShaderProgram shader) {
        super(delegate, shader, VERTEX_BYTES);
    }

    @Override
    protected void configureVertexAttributes() {
        glVertexPointer(3, GL_FLOAT, VERTEX_BYTES, 0);
        glColorPointer(4, GL_UNSIGNED_BYTE, VERTEX_BYTES, 3 * FLOAT_SIZE);
        glTexCoordPointer(2, GL_FLOAT, VERTEX_BYTES, 3 * FLOAT_SIZE + 1 * INT_SIZE);
        glClientActiveTexture(OpenGlHelper.lightmapTexUnit);
        glTexCoordPointer(2, GL_SHORT, VERTEX_BYTES, 3 * FLOAT_SIZE + 1 * INT_SIZE + 2 * FLOAT_SIZE);
        glClientActiveTexture(OpenGlHelper.defaultTexUnit);
    }
}
