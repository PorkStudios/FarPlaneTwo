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

package net.daporkchop.fp2.client.gl.shader;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.daporkchop.lib.unsafe.PCleaner;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL20;

import java.util.Arrays;

import static net.minecraft.client.renderer.OpenGlHelper.*;

/**
 * A container for a vertex or fragment shader.
 *
 * @author DaPorkchop_
 */
@Getter
class Shader {
    protected final ShaderType type;
    protected final int id;

    protected Shader(@NonNull String[] names, @NonNull String[] code, @NonNull ShaderType type) {
        this.type = type;

        //allocate shader
        this.id = glCreateShader(this.type().openGlId);

        int id = this.id;
        PCleaner.cleaner(this, () -> Minecraft.getMinecraft().addScheduledTask(() -> glDeleteShader(id)));

        //set shader source code
        GL20.glShaderSource(this.id, code);

        //compile and validate shader
        glCompileShader(this.id);
        ShaderManager.validateShaderCompile(Arrays.toString(names), this.id);
    }

    /**
     * Attaches this shader to the given shader program.
     *
     * @param program the program to which to attach this shader
     */
    protected void attach(@NonNull ShaderProgram program) {
        glAttachShader(program.id, this.id);
    }
}
