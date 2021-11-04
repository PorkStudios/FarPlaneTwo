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

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.attribute.global.GlobalAttributeBuffer;
import net.daporkchop.fp2.gl.draw.DrawBinding;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.global.GlobalAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.uniform.UniformAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.buffer.IndexedBufferTarget;
import net.daporkchop.fp2.gl.opengl.layout.DrawLayoutImpl;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeBufferImpl;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    protected final List<UniformBufferBinding> uniformBufffers;

    public DrawBindingImpl(@NonNull DrawBindingBuilderImpl builder) {
        this.layout = builder.layout;
        this.gl = this.layout.gl();
        this.api = this.gl.api();

        //create a VAO
        this.vao = this.api.glGenVertexArray();
        this.gl.resourceArena().register(this, this.vao, this.api::glDeleteVertexArray);

        //group attribute buffers by attribute format
        Map<AttributeFormatImpl, BaseAttributeBufferImpl> buffersByFormat = Stream.of(builder.uniforms, builder.globals, builder.locals)
                .flatMap(Stream::of)
                .collect(Collectors.toMap(BaseAttributeBufferImpl::format, Function.identity()));

        //configure all vertex attributes in the VAO
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        try {
            this.api.glBindVertexArray(this.vao);

            this.layout.vertexBindingsByFormat().forEach((format, bindings) -> {
                LocalAttributeBufferImpl buffer = (LocalAttributeBufferImpl) buffersByFormat.remove(format);
                checkArg(buffer != null, format);

                bindings.forEach(binding -> binding.enableAndBind(this.api, buffer));
            });
        } finally {
            this.api.glBindVertexArray(oldVao);
        }

        //configure uniform buffers
        this.uniformBufffers = this.layout.uniformBlockBindings().stream()
                .map(blockBinding -> {
                    UniformAttributeBufferImpl buffer = (UniformAttributeBufferImpl) buffersByFormat.remove(blockBinding.format());
                    checkArg(buffer != null, blockBinding.format());

                    return new UniformBufferBinding(buffer, blockBinding.bindingIndex());
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    public void bind(@NonNull Runnable callback) {
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);

        try {
            this.api.glBindVertexArray(this.vao);
            this.uniformBufffers.forEach(binding -> this.api.glBindBufferBase(GL_UNIFORM_BUFFER, binding.bindingIndex, binding.buffer.buffer().id()));

            callback.run();
        } finally {
            this.uniformBufffers.forEach(binding -> this.api.glBindBufferBase(GL_UNIFORM_BUFFER, binding.bindingIndex, 0)); //this doesn't actually restore the old binding ID...
            this.api.glBindVertexArray(oldVao);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class UniformBufferBinding {
        @NonNull
        protected final UniformAttributeBufferImpl buffer;
        protected final int bindingIndex;
    }
}
