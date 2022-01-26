/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.layout;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.GLResource;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;

import java.util.Map;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
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

    protected final Map<BaseAttributeBufferImpl<?, ?>, AttributeUsage> origBuffersUsages;

    protected final int vao;

    public BaseBindingImpl(@NonNull BaseBindingBuilderImpl<?, ?, L> builder) {
        this.layout = builder.layout();

        this.gl = this.layout.gl();
        this.api = this.layout.gl().api();
        this.origBuffersUsages = builder.buffersUsages.build();

        //create a VAO
        this.vao = this.api.glGenVertexArray();
        this.gl.resourceArena().register(this, this.vao, this.api::glDeleteVertexArray);

        checkArg(this.origBuffersUsages.size() == this.layout.bindingLocationsByFormat().size(), "mismatch between layout formats and binding buffers (%s is incompatible with %s)", this.origBuffersUsages, this.layout.bindingLocationsByFormat());

        //configure all vertex attributes in the VAO
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        try {
            this.api.glBindVertexArray(this.vao);

            this.origBuffersUsages.forEach((buffer, usage) -> {
                BindingLocation<?> location = this.layout.bindingLocationsByFormat().get(buffer.format());
                checkArg(location != null, "layout %s does not include %s format %s", this.layout, usage, buffer.format());
                checkArg(location.usage() == usage, "buffer %s cannot be used for %s when its binding location expects %s", buffer, usage, location.usage());

                location.configureBuffer(this.api(), uncheckedCast(buffer));
            });
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

        this.origBuffersUsages.forEach((buffer, usage) -> this.layout.bindingLocationsByFormat().get(buffer.format()).configureState(state, uncheckedCast(buffer)));
    }
}
