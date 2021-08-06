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
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Implementation of {@link IDrawCommandBuffer} which implements indirect multidraw for indexed geometry.
 *
 * @author DaPorkchop_
 */
public class FrustumCulledIndirectIndexedMultidrawCommandBuffer extends IndirectIndexedMultidrawCommandBuffer {
    protected final ShaderProgram cullProgram;

    public FrustumCulledIndirectIndexedMultidrawCommandBuffer(@NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray, @NonNull ShaderProgram cullProgram) {
        super(vaoInitializer, elementArray);

        this.cullProgram = cullProgram;
    }

    @Override
    protected void multidraw0() {
        //hacky workaround - the rendering shader is likely already bound, so we need to save the ID for later
        int oldProgram = glGetInteger(GL_CURRENT_PROGRAM);

        //bind frustum culling compute shader and dispatch it
        try (ShaderProgram cullProgram = this.cullProgram.use()) {
            this.buffer.bindBase(GL_SHADER_STORAGE_BUFFER, 3);
            glDispatchCompute(this.size, 1, 1);
        }

        //restore previous shader program
        glUseProgram(oldProgram);

        //set a memory barrier in order to ensure that the compute shader's modifications to the command buffer are visible
        glMemoryBarrier(GL_COMMAND_BARRIER_BIT);

        super.multidraw0();
    }
}
