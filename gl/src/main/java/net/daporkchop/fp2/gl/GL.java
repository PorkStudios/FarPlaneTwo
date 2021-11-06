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
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeFormatBuilder;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.command.buffer.DrawCommandBuffer;
import net.daporkchop.fp2.gl.command.buffer.DrawCommandBufferBuilder;
import net.daporkchop.fp2.gl.compute.GLCompute;
import net.daporkchop.fp2.gl.index.IndexFormat;
import net.daporkchop.fp2.gl.index.IndexFormatBuilder;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.gl.layout.LayoutBuilder;
import net.daporkchop.fp2.gl.shader.FragmentShader;
import net.daporkchop.fp2.gl.shader.ShaderBuilder;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.VertexShader;

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
     * @return a builder for constructing a new {@link IndexFormat}
     */
    IndexFormatBuilder.TypeSelectionStage createIndexFormat();

    /**
     * @return a builder for constructing a new {@link AttributeFormat}
     */
    AttributeFormatBuilder.NameSelectionStage createAttributeFormat();

    /**
     * @return a builder for constructing a new {@link DrawLayout}
     */
    LayoutBuilder<DrawLayout> createDrawLayout();

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
    ShaderBuilder.LayoutStage<VertexShader, DrawLayout> createVertexShader();

    /**
     * @return a builder for constructing a new {@link FragmentShader}
     */
    ShaderBuilder.LayoutStage<FragmentShader, DrawLayout> createFragmentShader();

    /**
     * Links a {@link ShaderProgram} from the given {@link VertexShader} and {@link FragmentShader}, tuned for rendering data formatted with the given {@link DrawLayout}.
     *
     * @param layout         the {@link DrawLayout}
     * @param vertexShader   the {@link VertexShader}
     * @param fragmentShader the {@link FragmentShader}
     * @return the linked {@link ShaderProgram}
     * @throws ShaderLinkageException if shader linkage fails
     */
    ShaderProgram linkShaderProgram(@NonNull DrawLayout layout, @NonNull VertexShader vertexShader, @NonNull FragmentShader fragmentShader) throws ShaderLinkageException;

    //
    // MODULES
    //

    /**
     * @return the module for accessing compute shaders
     */
    GLCompute compute();
}
