/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.gl.opengl.layout.binding;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.GLResource;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.layout.BaseLayoutImpl;

import java.util.List;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseBindingImpl<L extends BaseLayoutImpl> implements GLResource {
    protected final OpenGL gl;
    protected final GLAPI api;

    protected final L layout;

    protected final List<BindingEntry> entries;
    protected final int vao;

    public BaseBindingImpl(@NonNull BaseBindingBuilderImpl<?, ?, L> builder) {
        this.layout = builder.layout();

        this.gl = this.layout.gl();
        this.api = this.layout.gl().api();

        this.entries = builder.entries.build();
        checkArg(this.entries.size() == this.layout.entries().size(), "mismatch between layout formats and binding buffers (%s has fewer entries than %s)", this, this.layout);

        //create a VAO
        this.vao = this.api.glGenVertexArray();
        this.gl.resourceArena().register(this, this.vao, this.api::glDeleteVertexArray);

        //configure all vertex attributes in the VAO
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        try {
            this.api.glBindVertexArray(this.vao);

            this.entries.forEach(entry -> entry.layoutEntry().location().configureBuffer(this.api, uncheckedCast(entry.buffer())));
        } finally {
            this.api.glBindVertexArray(oldVao);
        }
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    public void configureBoundState(@NonNull MutableState state) {
        state.set(StateProperties.BOUND_VAO, this.vao);

        this.entries.forEach(entry -> entry.layoutEntry().location().configureState(state, uncheckedCast(entry.buffer())));
    }
}
