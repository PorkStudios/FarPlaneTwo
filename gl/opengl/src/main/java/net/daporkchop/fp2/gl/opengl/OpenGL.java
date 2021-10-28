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

package net.daporkchop.fp2.gl.opengl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.GlobalProperties;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLModule;
import net.daporkchop.fp2.gl.GLProfile;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.compute.GLCompute;
import net.daporkchop.fp2.gl.index.IndexFormatBuilder;
import net.daporkchop.fp2.gl.opengl.buffer.GLBufferImpl;
import net.daporkchop.fp2.gl.opengl.compute.ComputeCore;
import net.daporkchop.fp2.gl.opengl.index.IndexFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.shader.FragmentShaderImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderProgramImpl;
import net.daporkchop.fp2.gl.opengl.shader.VertexShaderImpl;
import net.daporkchop.fp2.gl.opengl.vertex.VertexFormatBuilderImpl;
import net.daporkchop.fp2.gl.shader.FragmentShader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.VertexShader;
import net.daporkchop.fp2.gl.vertex.VertexFormatBuilder;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class OpenGL implements GL {
    protected final GLAPI api;

    protected final GLVersion version;
    protected final GLProfile profile;
    protected final Set<GLExtension> extensions;

    protected final ResourceArena resourceArena = new ResourceArena();

    protected final GLCompute compute;

    protected OpenGL() {
        this.api = GlobalProperties.find(OpenGL.class, "opengl")
                .<Supplier<GLAPI>>getInstance("api.supplier")
                .get();

        this.version = this.api.version();

        { //get supported extensions
            Set<String> extensionNames;
            if (this.version.compareTo(GLVersion.OpenGL30) < 0) { //use old extensions field
                String extensions = this.api.glGetString(GL_EXTENSIONS);
                extensionNames = ImmutableSet.copyOf(extensions.trim().split(" "));
            } else { //use new indexed EXTENSIONS property
                extensionNames = IntStream.range(0, this.api.glGetInteger(GL_NUM_EXTENSIONS))
                        .mapToObj(i -> this.api.glGetString(GL_EXTENSIONS, i))
                        .collect(Collectors.toSet());
            }

            this.extensions = Stream.of(GLExtension.values())
                    .filter(extension -> extensionNames.contains(extension.name()))
                    .collect(Sets.toImmutableEnumSet());
        }

        { //get profile
            int contextFlags = 0;
            int contextProfileMask = 0;

            if (this.version.compareTo(GLVersion.OpenGL30) >= 0) { // >= 3.0, we can access context flags
                contextFlags = this.api.glGetInteger(GL_CONTEXT_FLAGS);

                if (this.version.compareTo(GLVersion.OpenGL32) >= 0) { // >= 3.2, we can access profile information
                    contextProfileMask = this.api.glGetInteger(GL_CONTEXT_PROFILE_MASK);
                }
            }

            boolean compat = (contextProfileMask & GL_CONTEXT_COMPATIBILITY_PROFILE_BIT) != 0;
            boolean core = (contextProfileMask & GL_CONTEXT_CORE_PROFILE_BIT) != 0;
            boolean forwards = this.version.compareTo(GLVersion.OpenGL31) >= 0
                               && !(this.extensions.contains(GLExtension.GL_ARB_compatibility) || (contextFlags & GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT) != 0);

            this.profile = !core && !forwards ? GLProfile.COMPAT : GLProfile.CORE;
        }

        //
        // create modules
        //

        //compute
        if (this.version.compareTo(GLVersion.OpenGL43) >= 0 || this.extensions.contains(GLExtension.GL_ARB_compute_shader)) {
            this.compute = new ComputeCore(this);
        } else {
            this.compute = GLModule.unsupportedImplementation(GLCompute.class);
        }
    }

    @Override
    public GLBuffer createBuffer(@NonNull BufferUsage usage) {
        return new GLBufferImpl(this, usage);
    }

    @Override
    public IndexFormatBuilder.TypeSelectionStage createIndexFormat() {
        return new IndexFormatBuilderImpl(this);
    }

    @Override
    public VertexFormatBuilder.LayoutSelectionStage createVertexFormat() {
        return new VertexFormatBuilderImpl(this);
    }

    //
    // SHADERS
    //

    @Override
    public VertexShader compileVertexShader(@NonNull String source) throws ShaderCompilationException {
        return new VertexShaderImpl(this, source);
    }

    @Override
    public FragmentShader compileFragmentShader(@NonNull String source) throws ShaderCompilationException {
        return new FragmentShaderImpl(this, source);
    }

    @Override
    public ShaderProgram linkShaderProgram(@NonNull VertexShader vertexShader, @NonNull FragmentShader fragmentShader) throws ShaderLinkageException {
        return new ShaderProgramImpl(this, (VertexShaderImpl) vertexShader, (FragmentShaderImpl) fragmentShader);
    }

    @Override
    public void close() {
        this.resourceArena.release();
    }
}
