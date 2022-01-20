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

package net.daporkchop.fp2.gl;

import lombok.NonNull;
import net.daporkchop.fp2.common.GlobalProperties;
import net.daporkchop.fp2.gl.attribute.AttributeFormatBuilder;
import net.daporkchop.fp2.gl.attribute.common.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.common.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.IAttributeFormatBuilder;
import net.daporkchop.fp2.gl.attribute.global.DrawGlobalFormat;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalFormat;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayFormat;
import net.daporkchop.fp2.gl.attribute.uniform.UniformFormat;
import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.bitset.GLBitSetBuilder;
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
     * @return a builder for constructing a new {@link GLBitSet}
     */
    GLBitSetBuilder createBitSet();

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
     * @param usage a list of the {@link AttributeUsage}s which this attribute format may be used for
     * @param <S>   the struct type
     * @return an {@link AttributeFormat}
     */
    <S> IAttributeFormatBuilder<S> createAttributeFormat(@NonNull Class<S> clazz, @NonNull AttributeUsage... usage);

    /**
     * Gets a {@link TextureFormat2D} for the given struct class.
     *
     * @param clazz the struct class
     * @param <S>   the struct type
     * @return a {@link TextureFormat2D}
     */
    <S> AttributeFormatBuilder<TextureFormat2D<S>> createTextureFormat2D(@NonNull Class<S> clazz);

    /**
     * Gets a {@link UniformFormat} for the given struct class.
     *
     * @param clazz the struct class
     * @param <S>   the struct type
     * @return a {@link UniformFormat}
     */
    @Deprecated
    <S> AttributeFormatBuilder<UniformFormat<S>> createUniformFormat(@NonNull Class<S> clazz);

    /**
     * Gets a {@link UniformArrayFormat} for the given struct class.
     *
     * @param clazz the struct class
     * @param <S>   the struct type
     * @return a {@link UniformArrayFormat}
     */
    @Deprecated
    <S> AttributeFormatBuilder<UniformArrayFormat<S>> createUniformArrayFormat(@NonNull Class<S> clazz);

    /**
     * Gets a {@link DrawGlobalFormat} for the given struct class.
     *
     * @param clazz the struct class
     * @param <S>   the struct type
     * @return a {@link DrawGlobalFormat}
     */
    @Deprecated
    <S> AttributeFormatBuilder<DrawGlobalFormat<S>> createDrawGlobalFormat(@NonNull Class<S> clazz);

    /**
     * Gets a {@link DrawLocalFormat} for the given struct class.
     *
     * @param clazz the struct class
     * @param <S>   the struct type
     * @return a {@link DrawLocalFormat}
     */
    @Deprecated
    <S> AttributeFormatBuilder<DrawLocalFormat<S>> createDrawLocalFormat(@NonNull Class<S> clazz);

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
}
