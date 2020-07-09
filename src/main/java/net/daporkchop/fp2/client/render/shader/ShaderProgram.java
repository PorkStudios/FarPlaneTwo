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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.render.OpenGL;
import net.daporkchop.lib.unsafe.PCleaner;
import net.minecraft.client.Minecraft;

import static org.lwjgl.opengl.GL20.*;

/**
 * Basic wrapper around a shader.
 *
 * @author DaPorkchop_
 */
public final class ShaderProgram implements AutoCloseable {
    @Getter
    protected final String name;
    protected final Shader vertex;
    protected final Shader fragment;
    protected final int id;

    /**
     * Creates a new shader program by attaching the given vertex shader with the given fragment shader.
     *
     * @param name     the program's name
     * @param vertex   the vertex shader
     * @param fragment fragment shader
     */
    protected ShaderProgram(@NonNull String name, @NonNull Shader vertex, @NonNull Shader fragment) {
        fragment.assertCompatible(vertex);

        OpenGL.assertOpenGL();
        this.name = name;

        //allocate program
        this.id = OpenGL.glCreateProgram();

        int id = this.id;
        PCleaner.cleaner(this, () -> Minecraft.getMinecraft().addScheduledTask(() -> glDeleteProgram(id)));

        //attach shaders
        vertex.attach(this);
        fragment.attach(this);

        //link and validate
        OpenGL.glLinkProgram(this.id);
        ShaderManager.validate(this.name, this.id, OpenGL.GL_LINK_STATUS);
        OpenGL.glValidateProgram(this.id);
        ShaderManager.validate(this.name, this.id, OpenGL.GL_VALIDATE_STATUS);

        this.vertex = vertex;
        this.fragment = fragment;
    }

    /**
     * Gets the location of a uniform value in the fragment shader.
     *
     * @param name the uniform's name
     * @return the uniform's location
     */
    public int uniformLocation(@NonNull String name) {
        return OpenGL.glGetUniformLocation(this.id, name);
    }

    /**
     * Binds this shader for use when rendering.
     * <p>
     * This method returns itself, for use in a try-with-resources block.
     */
    public ShaderProgram use() {
        OpenGL.glUseProgram(this.id);
        return this;
    }

    @Override
    public void close() {
        OpenGL.glUseProgram(0);
    }
}
