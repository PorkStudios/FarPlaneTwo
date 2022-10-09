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
 */

package net.daporkchop.fp2.gl;

import lombok.NonNull;
import net.daporkchop.fp2.common.GlobalProperties;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeFormatBuilder;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormatBuilder;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormat;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatBuilder;
import net.daporkchop.fp2.gl.command.CommandBuffer;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.DrawLayoutBuilder;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexFormatBuilder;
import net.daporkchop.fp2.gl.draw.list.DrawCommandArrays;
import net.daporkchop.fp2.gl.draw.list.DrawCommandIndexed;
import net.daporkchop.fp2.gl.draw.list.DrawList;
import net.daporkchop.fp2.gl.draw.list.DrawListBuilder;
import net.daporkchop.fp2.gl.draw.shader.BaseDrawShader;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.draw.shader.FragmentShader;
import net.daporkchop.fp2.gl.draw.shader.VertexShader;
import net.daporkchop.fp2.gl.shader.BaseShaderBuilder;
import net.daporkchop.fp2.gl.shader.BaseShaderProgramBuilder;
import net.daporkchop.fp2.gl.transform.TransformLayout;
import net.daporkchop.fp2.gl.transform.TransformLayoutBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShader;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgram;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgramBuilder;

import java.util.function.Supplier;

/**
 * Container for an OpenGL context.
 *
 * @author DaPorkchop_
 */
public interface GL extends AutoCloseable {
    /**
     * @return a new {@link GLBuilder}
     */
    static GLBuilder.ResourceStage builder() {
        return GlobalProperties.find(GL.class, "gl").<Supplier<GLBuilder.ResourceStage>>getInstanceCached("builder.supplier").get();
    }

    //
    // GENERAL
    //

    /**
     * Disposes of any OpenGL resources created by this context whose parent objects have been garbage-collected.
     * <p>
     * This should be called periodically between rendering to avoid leaking memory.
     */
    void runCleanup();

    /**
     * Closes this OpenGL context, releasing all resources allocated by it.
     * <p>
     * Once this method has been called, all methods in all object instances belonging to by this instance will produce undefined behavior.
     */
    @Override
    void close();

    /**
     * @return a builder for constructing a new {@link CommandBuffer}
     */
    CommandBufferBuilder createCommandBuffer();

    //
    // FORMATS
    //

    /**
     * @return a builder for constructing a new {@link IndexFormat}
     */
    IndexFormatBuilder.TypeSelectionStage createIndexFormat();

    /**
     * Gets an {@link AttributeFormat} for the given struct class.
     *
     * @param clazz the struct class
     * @param <S>   the struct type
     * @return an {@link AttributeFormat}
     */
    <S> AttributeFormatBuilder<S> createAttributeFormat(@NonNull Class<S> clazz);

    /**
     * @return a builder for constructing a new {@link PixelFormat}
     */
    PixelFormatBuilder.ChannelSelectionStage createPixelFormat();

    /**
     * Gets a {@link TextureFormat2D} for the given {@link PixelFormat pixel format}.
     *
     * @param pixelFormat the {@link PixelFormat pixel format}
     * @param name
     * @return a {@link TextureFormat2D}
     */
    TextureFormatBuilder<TextureFormat2D> createTextureFormat2D(@NonNull PixelFormat pixelFormat, @NonNull String name);

    //
    // DRAW
    //

    /**
     * @return a builder for constructing a new {@link DrawLayout}
     */
    DrawLayoutBuilder createDrawLayout();

    /**
     * @return a builder for constructing a new {@link DrawList} for array drawing commands
     */
    DrawListBuilder<DrawCommandArrays> createDrawListArrays(@NonNull DrawBinding binding);

    /**
     * @return a builder for constructing a new {@link DrawList} for indexed drawing commands
     */
    DrawListBuilder<DrawCommandIndexed> createDrawListIndexed(@NonNull DrawBindingIndexed binding);

    /**
     * @return a builder for constructing a new {@link VertexShader}
     */
    BaseShaderBuilder<VertexShader> createVertexShader(@NonNull DrawLayout layout);

    /**
     * @return a builder for constructing a new {@link FragmentShader}
     */
    BaseShaderBuilder<FragmentShader> createFragmentShader(@NonNull DrawLayout layout);

    /**
     * @return a builder for constructing a new {@link DrawShaderProgram}
     */
    BaseShaderProgramBuilder<DrawShaderProgram, BaseDrawShader, DrawLayout> createDrawShaderProgram(@NonNull DrawLayout layout);

    //
    // TRANSFORM
    //

    /**
     * @return a builder for constructing a new {@link TransformLayout}
     */
    TransformLayoutBuilder createTransformLayout();

    /**
     * @return a builder for constructing a new {@link TransformShader}
     */
    TransformShaderBuilder createTransformShader(@NonNull TransformLayout layout);

    /**
     * @return a builder for constructing a new {@link TransformShaderProgram}
     */
    TransformShaderProgramBuilder createTransformShaderProgram(@NonNull TransformLayout layout);
}
