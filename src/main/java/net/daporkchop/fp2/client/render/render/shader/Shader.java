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

package net.daporkchop.fp2.client.render.render.shader;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.render.render.OpenGL;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * A container for a vertex or fragment shader.
 *
 * @author DaPorkchop_
 */
@Getter
abstract class Shader {
    protected final Set<ShaderProgram> usages = Collections.newSetFromMap(new IdentityHashMap<>());
    protected final String name;
    protected       int    id;

    protected Shader(@NonNull String name, @NonNull String code, @NonNull JsonObject meta) {
        OpenGL.assertOpenGL();
        this.name = name;
        this.id = -1;

        this.load(meta);

        try {
            //allocate shader
            this.id = OpenGL.glCreateShader(this.type().openGlId);

            //set shader source code
            OpenGL.glShaderSource(this.id, code);

            //compile and validate shader
            OpenGL.glCompileShader(this.id);
            ShaderManager.validate(name, this.id, OpenGL.GL_COMPILE_STATUS);
        } catch (Exception e) {
            if (this.id != -1) {
                this.internal_dispose();
            }
            throw new RuntimeException(e);
        }
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
        if (this.id == -1) {
            throw new IllegalStateException("Already deleted!");
        } else if (!this.usages.add(program)) {
            throw new IllegalStateException("Already attached to the given shader!");
        } else {
            OpenGL.glAttachShader(program.id, this.id);
        }
    }

    /**
     * Detaches this shader from the given shader program.
     *
     * @param program the program from which to detach this shader
     * @return whether or not this shader has been disposed
     */
    protected boolean detach(@NonNull ShaderProgram program) {
        OpenGL.assertOpenGL();
        if (this.id == -1) {
            throw new IllegalStateException("Already deleted!");
        } else if (!this.usages.remove(program)) {
            throw new IllegalStateException("Not attached to the given shader!");
        } else if (this.usages.isEmpty()) {
            this.internal_dispose();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Less aggressive variant of {@link #detach(ShaderProgram)}.
     *
     * @see #detach(ShaderProgram)
     */
    protected boolean detachSoft(@NonNull ShaderProgram program) {
        OpenGL.assertOpenGL();
        if (this.id == -1) {
            System.err.printf("Warning: Shader \"%s\" (%s) was not detached from program \"%s\" as it had already been disposed!\n", this.name, this.type(), program.name);
        } else if (!this.usages.remove(program)) {
            System.err.printf("Warning: Program \"%s\" incorrectly tried to detach shader \"%s\" (%s) from itself, but it wasn't attached!\n", program.name, this.name, this.type());
        } else if (this.usages.isEmpty()) {
            this.internal_dispose();
            return true;
        }
        return false;
    }

    protected void internal_dispose() {
        OpenGL.assertOpenGL();
        if (this.id == -1) {
            throw new IllegalStateException("Already disposed!");
        } else {
            OpenGL.glDeleteShader(this.id);
            this.id = -1;
            if (!this.type().compiledShaders.remove(this.name, this)) {
                throw new IllegalStateException("Couldn't remove self from compiled shaders registry!");
            }
        }
    }
}
