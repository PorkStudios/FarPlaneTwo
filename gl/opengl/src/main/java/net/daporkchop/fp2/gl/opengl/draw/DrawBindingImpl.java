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

package net.daporkchop.fp2.gl.opengl.draw;

import lombok.NonNull;
import net.daporkchop.fp2.gl.draw.DrawBinding;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.global.GlobalAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.layout.DrawLayoutImpl;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeBufferImpl;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class DrawBindingImpl implements DrawBinding {
    protected final OpenGL gl;
    protected final GLAPI api;

    protected final DrawLayoutImpl layout;

    protected final int vao;

    public DrawBindingImpl(@NonNull DrawBindingBuilderImpl builder) {
        this.layout = builder.layout;
        this.gl = this.layout.gl();
        this.api = this.gl.api();

        //create a VAO
        this.vao = this.api.glGenVertexArray();
        this.gl.resourceArena().register(this, this.vao, this.api::glDeleteVertexArray);

        //configure the VAO
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        try {
            this.api.glBindVertexArray(this.vao);

            //collect local bindings by vertex format
            Map<AttributeFormat, List<DrawLayoutImpl.AttributeBinding>> localBindingsGrouped = this.layout.attributeBindings().values().stream()
                    .collect(Collectors.groupingBy(DrawLayoutImpl.AttributeBinding::format));

            //configure each VAO
            for (LocalAttributeBufferImpl buffer : builder.locals) {
                List<DrawLayoutImpl.AttributeBinding> localBindings = localBindingsGrouped.remove(buffer.format());
                checkArg(localBindings != null, buffer.format());

                localBindings.forEach(binding -> binding.enableAndBind(this.api, buffer));
            }
            for (GlobalAttributeBufferImpl buffer : builder.globals) {
                List<DrawLayoutImpl.AttributeBinding> localBindings = localBindingsGrouped.remove(buffer.format());
                checkArg(localBindings != null, buffer.format()); //TODO: this will no longer be the case once we support something other than instanced attributes

                localBindings.forEach(binding -> binding.enableAndBind(this.api, buffer));
            }

            //ensure every vertex format has been assigned something
            checkArg(localBindingsGrouped.isEmpty(), localBindingsGrouped.keySet());
        } finally {
            this.api.glBindVertexArray(oldVao);
        }
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    public void bind(@NonNull Runnable callback) {
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        try {
            this.api.glBindVertexArray(this.vao);

            callback.run();
        } finally {
            this.api.glBindVertexArray(oldVao);
        }
    }
}
