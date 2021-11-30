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

package net.daporkchop.fp2.gl;

import lombok.NonNull;
import net.daporkchop.fp2.common.GlobalProperties;
import net.daporkchop.fp2.gl.attribute.AttributeFormatBuilder;
import net.daporkchop.fp2.gl.attribute.global.DrawGlobalFormat;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalFormat;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayFormat;
import net.daporkchop.fp2.gl.attribute.uniform.UniformFormat;
import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.bitset.GLBitSetBuilder;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.compute.GLCompute;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.DrawLayoutBuilder;
import net.daporkchop.fp2.gl.draw.command.DrawCommandBuffer;
import net.daporkchop.fp2.gl.draw.command.DrawCommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexFormatBuilder;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.draw.shader.FragmentShader;
import net.daporkchop.fp2.gl.draw.shader.VertexShader;
import net.daporkchop.fp2.gl.shader.BaseShaderBuilder;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;

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
     * Creates a new OpenGL buffer.
     *
     * @param usage the buffer's usage hint
     * @return a new {@link GLBuffer}
     */
    GLBuffer createBuffer(@NonNull BufferUsage usage);

    /**
     * @return a builder for constructing a new {@link GLBitSet}
     */
    GLBitSetBuilder createBitSet();

    /**
     * @return a builder for constructing a new {@link IndexFormat}
     */
    IndexFormatBuilder.TypeSelectionStage createIndexFormat();

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
    <S> AttributeFormatBuilder<UniformFormat<S>> createUniformFormat(@NonNull Class<S> clazz);

    /**
     * Gets a {@link UniformArrayFormat} for the given struct class.
     *
     * @param clazz the struct class
     * @param <S>   the struct type
     * @return a {@link UniformArrayFormat}
     */
    <S> AttributeFormatBuilder<UniformArrayFormat<S>> createUniformArrayFormat(@NonNull Class<S> clazz);

    /**
     * Gets a {@link DrawGlobalFormat} for the given struct class.
     *
     * @param clazz the struct class
     * @param <S>   the struct type
     * @return a {@link DrawGlobalFormat}
     */
    <S> AttributeFormatBuilder<DrawGlobalFormat<S>> createDrawGlobalFormat(@NonNull Class<S> clazz);

    /**
     * Gets a {@link DrawLocalFormat} for the given struct class.
     *
     * @param clazz the struct class
     * @param <S>   the struct type
     * @return a {@link DrawLocalFormat}
     */
    <S> AttributeFormatBuilder<DrawLocalFormat<S>> createDrawLocalFormat(@NonNull Class<S> clazz);

    /**
     * @return a builder for constructing a new {@link DrawLayout}
     */
    DrawLayoutBuilder createDrawLayout();

    /**
     * @return a builder for constructing a new {@link DrawCommandBuffer}
     */
    DrawCommandBufferBuilder.TypeStage createCommandBuffer();

    //
    // SHADERS
    //

    /**
     * @return a builder for constructing a new {@link VertexShader}
     */
    BaseShaderBuilder<VertexShader> createVertexShader(@NonNull DrawLayout layout);

    /**
     * @return a builder for constructing a new {@link FragmentShader}
     */
    BaseShaderBuilder<FragmentShader> createFragmentShader(@NonNull DrawLayout layout);

    /**
     * Links a {@link DrawShaderProgram} from the given {@link VertexShader} and {@link FragmentShader}, tuned for rendering data formatted with the given {@link DrawLayout}.
     *
     * @param layout         the {@link DrawLayout}
     * @param vertexShader   the {@link VertexShader}
     * @param fragmentShader the {@link FragmentShader}
     * @return the linked {@link DrawShaderProgram}
     * @throws ShaderLinkageException if shader linkage fails
     */
    DrawShaderProgram linkShaderProgram(@NonNull DrawLayout layout, @NonNull VertexShader vertexShader, @NonNull FragmentShader fragmentShader) throws ShaderLinkageException;

    //
    // MODULES
    //

    /**
     * @return the module for accessing compute shaders
     */
    GLCompute compute();
}
