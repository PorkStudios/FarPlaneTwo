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
import net.daporkchop.fp2.gl.shader.BaseShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseShaderProgramImpl implements BaseShaderProgram {
    protected final OpenGL gl;
    protected final int id;

    public BaseShaderProgramImpl(@NonNull OpenGL gl, @NonNull BaseShaderImpl... shaders) throws ShaderLinkageException {
        //allocate new shader
        this.gl = gl;

        GLAPI api = gl.api();
        this.id = api.glCreateProgram();

        //attach shaders and link, then detach shaders again
        for (BaseShaderImpl shader : shaders) {
            api.glAttachShader(this.id, shader.id);
        }
        api.glLinkProgram(this.id);
        for (BaseShaderImpl shader : shaders) {
            api.glDetachShader(this.id, shader.id);
        }

        //check for errors
        if (api.glGetProgrami(this.id, GL_LINK_STATUS) == GL_FALSE) {
            String log = api.glGetProgramInfoLog(this.id);

            //delete program
            api.glDeleteProgram(this.id);

            throw new ShaderLinkageException(log);
        }

        //register resource for cleaning
        this.gl.resourceArena().register(this, this.id, api::glDeleteProgram);
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }
}
