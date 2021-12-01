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

package net.daporkchop.fp2.gl.opengl.draw.binding;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.ShaderStorageBlockBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.common.TextureBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.common.UniformBlockBuffer;
import net.daporkchop.fp2.gl.opengl.draw.DrawLayoutImpl;
import net.daporkchop.fp2.gl.opengl.layout.BaseBindingImpl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class DrawBindingImpl extends BaseBindingImpl implements DrawBinding {
    protected final DrawLayoutImpl layout;

    protected final int vao;
    protected final List<ShaderStorageBufferBinding> shaderStorageBuffers;
    protected final List<TextureBinding> textures;
    protected final List<UniformBufferBinding> uniformBuffers;

    public DrawBindingImpl(@NonNull DrawBindingBuilderImpl builder) {
        super(builder.layout.gl());

        this.layout = builder.layout;

        //create a VAO
        this.vao = this.api.glGenVertexArray();
        this.gl.resourceArena().register(this, this.vao, this.api::glDeleteVertexArray);

        //group attribute buffers by attribute format
        Map<BaseAttributeFormatImpl<?, ?>, BaseAttributeBufferImpl<?, ?, ?>> buffersByFormat = builder.allBuffersAndChildren()
                .collect(Collectors.toMap(BaseAttributeBufferImpl::formatImpl, Function.identity()));

        //configure all vertex attributes in the VAO
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        try {
            this.api.glBindVertexArray(this.vao);

            this.layout.vertexAttributeBindingsByFormat().forEach((format, binding) -> {
                BaseAttributeBufferImpl<?, ?, ?> buffer = buffersByFormat.remove(format);
                checkArg(buffer != null, format);

                binding.enableAndBind(this.api, buffer);
            });
        } finally {
            this.api.glBindVertexArray(oldVao);
        }

        //configure shader storage buffers
        this.shaderStorageBuffers = this.layout.shaderStorageBlockBindings().stream()
                .map(blockBinding -> {
                    BaseAttributeBufferImpl<?, ?, ?> buffer = buffersByFormat.remove(blockBinding.format());
                    checkArg(buffer != null, blockBinding.format());

                    return new ShaderStorageBufferBinding(((ShaderStorageBlockBuffer) buffer).internalBuffer(), blockBinding.bindingIndex());
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

        //configure textures
        this.textures = this.layout.textureBindings().stream()
                .map(textureBinding -> {
                    BaseAttributeBufferImpl<?, ?, ?> buffer = buffersByFormat.remove(textureBinding.format());
                    checkArg(buffer != null, textureBinding.format());

                    return new TextureBinding(textureBinding.unit(), textureBinding.target(), ((TextureBuffer) buffer).textureId());
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

        //configure uniform buffers
        this.uniformBuffers = this.layout.uniformBlockBindings().stream()
                .map(blockBinding -> {
                    BaseAttributeBufferImpl<?, ?, ?> buffer = buffersByFormat.remove(blockBinding.format());
                    checkArg(buffer != null, blockBinding.format());

                    return new UniformBufferBinding(((UniformBlockBuffer) buffer).internalBuffer(), blockBinding.bindingIndex());
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

        //ensure every attribute has been used
        checkArg(buffersByFormat.isEmpty(), "some buffers have not been bound to anything!", buffersByFormat.keySet());
    }

    @Override
    public void close() {
        this.gl.resourceArena().delete(this);
    }

    public void bind(@NonNull Runnable callback) {
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);

        int oldTexUnit = this.api.glGetInteger(GL_ACTIVE_TEXTURE);

        try {
            this.api.glBindVertexArray(this.vao);
            this.shaderStorageBuffers.forEach(binding -> this.api.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding.bindingIndex, binding.buffer.id()));
            this.textures.forEach(binding -> {
                this.api.glActiveTexture(GL_TEXTURE0 + binding.unit);
                binding.prevId = this.api.glGetInteger(binding.target.binding());
                this.api.glBindTexture(binding.target.target(), binding.id);
            });
            this.uniformBuffers.forEach(binding -> this.api.glBindBufferBase(GL_UNIFORM_BUFFER, binding.bindingIndex, binding.buffer.id()));

            callback.run();
        } finally {
            this.uniformBuffers.forEach(binding -> this.api.glBindBufferBase(GL_UNIFORM_BUFFER, binding.bindingIndex, 0)); //this doesn't actually restore the old binding ID...
            this.textures.forEach(binding -> { //this doesn't actually restore the old binding ID...
                this.api.glActiveTexture(GL_TEXTURE0 + binding.unit);
                this.api.glBindTexture(binding.target.target(), binding.prevId);
            });
            this.api.glActiveTexture(oldTexUnit);
            this.shaderStorageBuffers.forEach(binding -> this.api.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding.bindingIndex, 0)); //this doesn't actually restore the old binding ID...
            this.api.glBindVertexArray(oldVao);
        }
    }

}
