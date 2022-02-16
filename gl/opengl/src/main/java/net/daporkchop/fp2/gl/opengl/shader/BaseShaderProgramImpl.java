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
import net.daporkchop.fp2.gl.layout.BaseLayout;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.layout.BaseLayoutImpl;
import net.daporkchop.fp2.gl.shader.BaseShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseShaderProgramImpl<L extends BaseLayoutImpl, L_EXTERNAL extends BaseLayout> implements BaseShaderProgram<L_EXTERNAL> {
    protected final OpenGL gl;

    protected final L layoutImpl;

    protected final int id;

    public BaseShaderProgramImpl(@NonNull BaseShaderProgramBuilderImpl<?, ?, L, L_EXTERNAL> builder) throws ShaderLinkageException {
        this.gl = builder.gl;
        this.layoutImpl = builder.layout;

        GLAPI api = this.gl.api();
        this.id = api.glCreateProgram();
        this.gl.resourceArena().register(this, this.id, api::glDeleteProgram);

        //attach shaders and link, then detach shaders again
        for (BaseShaderImpl<L_EXTERNAL> shader : builder.shaders) {
            api.glAttachShader(this.id, shader.id);
        }
        this.layoutImpl.configureProgramPreLink(this.id);
        api.glLinkProgram(this.id);
        for (BaseShaderImpl<L_EXTERNAL> shader : builder.shaders) {
            api.glDetachShader(this.id, shader.id);
        }

        //check for errors
        if (api.glGetProgrami(this.id, GL_LINK_STATUS) == GL_FALSE) {
            String log = api.glGetProgramInfoLog(this.id);
            throw new ShaderLinkageException(log);
        }

        this.layoutImpl.configureProgramPostLink(this.id);
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    @Override
    @Deprecated
    public L_EXTERNAL layout() {
        return uncheckedCast(this.layoutImpl);
    }
}
