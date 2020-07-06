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

package net.daporkchop.pepsimod.util.render;

import com.hackoeur.jglm.Mat4;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.pepsimod.util.render.shader.ShaderProgram;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.util.vector.Matrix4f;

import java.nio.FloatBuffer;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class MatrixHelper {
    private static final FloatBuffer BUFFER = BufferUtils.createFloatBuffer(16);

    public static void setMatrixUniform(ShaderProgram shader, String uniform, Mat4 matrix)  {
        BUFFER.clear();
        BUFFER.put(matrix.getBuffer()).flip();

        ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation(uniform), false, BUFFER);
    }

    public static FloatBuffer getModelViewMatrix(FloatBuffer buffer)  {
        if (buffer == null) {
            buffer = BufferUtils.createFloatBuffer(16);
        }
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
        return buffer;
    }

    public static FloatBuffer getProjectionMatrix(FloatBuffer buffer)  {
        if (buffer == null) {
            buffer = BufferUtils.createFloatBuffer(16);
        }
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, buffer);
        return buffer;
    }
}
