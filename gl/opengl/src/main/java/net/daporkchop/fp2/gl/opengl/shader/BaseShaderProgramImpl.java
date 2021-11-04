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

package net.daporkchop.fp2.gl.opengl.shader;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.layout.BaseLayoutImpl;
import net.daporkchop.fp2.gl.shader.BaseShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseShaderProgramImpl implements BaseShaderProgram {
    protected final OpenGL gl;
    protected final GLAPI api;

    protected final int id;

    public BaseShaderProgramImpl(@NonNull OpenGL gl, @NonNull BaseLayoutImpl layout, @NonNull BaseShaderImpl... shaders) throws ShaderLinkageException {
        this.gl = gl;
        this.api = gl.api();

        //allocate new shader
        this.id = this.api.glCreateProgram();
        this.gl.resourceArena().register(this, this.id, this.api::glDeleteProgram);

        //attach shaders and link, then detach shaders again
        for (BaseShaderImpl shader : shaders) {
            this.api.glAttachShader(this.id, shader.id);
        }
        layout.configureProgramPreLink(this.id);
        this.api.glLinkProgram(this.id);
        for (BaseShaderImpl shader : shaders) {
            this.api.glDetachShader(this.id, shader.id);
        }

        //check for errors
        if (this.api.glGetProgrami(this.id, GL_LINK_STATUS) == GL_FALSE) {
            String log = this.api.glGetProgramInfoLog(this.id);
            throw new ShaderLinkageException(log);
        }

        layout.configureProgramPostLink(this.id);
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    public void bind(@NonNull Runnable callback) {
        int oldProgram = this.api.glGetInteger(GL_CURRENT_PROGRAM);
        assert oldProgram == 0 : "a shader is already bound!";

        try {
            this.api.glUseProgram(this.id);

            callback.run();
        } finally {
            this.api.glUseProgram(oldProgram);
        }
    }
}
