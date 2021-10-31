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
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.compute.GLCompute;
import net.daporkchop.fp2.gl.draw.DrawBinding;
import net.daporkchop.fp2.gl.draw.DrawBindingBuilder;
import net.daporkchop.fp2.gl.index.IndexFormat;
import net.daporkchop.fp2.gl.index.IndexFormatBuilder;
import net.daporkchop.fp2.gl.shader.FragmentShader;
import net.daporkchop.fp2.gl.shader.ShaderBuilder;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.VertexShader;
import net.daporkchop.fp2.gl.vertex.VertexFormat;
import net.daporkchop.fp2.gl.vertex.VertexFormatBuilder;

import java.util.Set;
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
     * @return the context's OpenGL version
     */
    GLVersion version();

    /**
     * @return the context's OpenGL profile
     */
    GLProfile profile();

    /**
     * @return all extensions supported by this context
     */
    Set<GLExtension> extensions();

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
     * @return a builder for constructing a new {@link VertexFormat}
     */
    VertexFormatBuilder.LayoutSelectionStage createVertexFormat();

    /**
     * @return a builder for constructing a new {@link DrawBinding}
     */
    DrawBindingBuilder.ProgramStage createDrawBinding();

    //
    // SHADERS
    //

    /**
     * @return a builder for constructing a new {@link VertexShader}
     */
    ShaderBuilder.SourceStage<VertexShader> createVertexShader();

    /**
     * @return a builder for constructing a new {@link FragmentShader}
     */
    ShaderBuilder.SourceStage<FragmentShader> createFragmentShader();

    /**
     * Links a {@link ShaderProgram} from the given {@link VertexShader} and {@link FragmentShader}.
     *
     * @param vertexShader   the {@link VertexShader}
     * @param fragmentShader the {@link FragmentShader}
     * @return the linked {@link ShaderProgram}
     * @throws ShaderLinkageException if shader linkage fails
     */
    ShaderProgram linkShaderProgram(@NonNull VertexShader vertexShader, @NonNull FragmentShader fragmentShader) throws ShaderLinkageException;

    //
    // MODULES
    //

    /**
     * @return the module for accessing compute shaders
     */
    GLCompute compute();
}
