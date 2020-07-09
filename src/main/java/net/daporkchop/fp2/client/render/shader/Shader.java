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

package net.daporkchop.fp2.client.render.shader;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.render.OpenGL;
import net.daporkchop.lib.unsafe.PCleaner;
import net.minecraft.client.Minecraft;

import java.util.Collection;

import static net.minecraft.client.renderer.OpenGlHelper.*;

/**
 * A container for a vertex or fragment shader.
 *
 * @author DaPorkchop_
 */
@Getter
abstract class Shader {
    protected final String name;
    protected final int id;

    protected Shader(@NonNull String name, @NonNull String code, @NonNull JsonObject meta) {
        OpenGL.assertOpenGL();
        this.name = name;

        this.load(meta);

        //allocate shader
        this.id = OpenGL.glCreateShader(this.type().openGlId);

        int id = this.id;
        PCleaner.cleaner(this, () -> Minecraft.getMinecraft().addScheduledTask(() -> glDeleteShader(id)));

        //set shader source code
        OpenGL.glShaderSource(this.id, code);

        //compile and validate shader
        OpenGL.glCompileShader(this.id);
        ShaderManager.validate(name, this.id, OpenGL.GL_COMPILE_STATUS);
    }

    /**
     * @return this shader's type
     */
    protected abstract ShaderType type();

    /**
     * Loads shader metadata.
     *
     * @param meta the shader metadata
     */
    protected abstract void load(@NonNull JsonObject meta);

    /**
     * Gets the list of variables provided by this vertex shader.
     *
     * @return the list of variables provided by this vertex shader
     * @throws UnsupportedOperationException if this shader is a fragment shader
     */
    protected abstract Collection<String> provides() throws UnsupportedOperationException;

    /**
     * Gets the list of variables required by this fragment shader.
     *
     * @return the list of variables required by this fragment shader
     * @throws UnsupportedOperationException if this shader is a vertex shader
     */
    protected abstract Collection<String> requires() throws UnsupportedOperationException;

    /**
     * Asserts that this shader can be linked with another one.
     *
     * @param counterpart the shader to check for compatibility with
     * @throws IllegalArgumentException if the shaders are not compatible
     */
    protected void assertCompatible(@NonNull Shader counterpart) throws IllegalArgumentException {
        if (counterpart == this) {
            throw new IllegalArgumentException("Cannot be linked to self!");
        } else if (counterpart.type() == this.type()) {
            throw new IllegalArgumentException("Cannot be linked with other shader of same type!");
        }
    }

    /**
     * Attaches this shader to the given shader program.
     *
     * @param program the program to which to attach this shader
     */
    protected void attach(@NonNull ShaderProgram program) {
        OpenGL.assertOpenGL();
        OpenGL.glAttachShader(program.id, this.id);
    }
}
