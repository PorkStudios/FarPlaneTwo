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
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.GlobalProperties;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.GLModule;
import net.daporkchop.fp2.gl.attribute.global.DrawGlobalFormat;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalFormat;
import net.daporkchop.fp2.gl.attribute.uniform.UniformArrayFormat;
import net.daporkchop.fp2.gl.attribute.uniform.UniformFormat;
import net.daporkchop.fp2.gl.bitset.GLBitSetBuilder;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.draw.command.DrawCommandBufferBuilder;
import net.daporkchop.fp2.gl.compute.GLCompute;
import net.daporkchop.fp2.gl.draw.index.IndexFormatBuilder;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.DrawLayoutBuilder;
import net.daporkchop.fp2.gl.opengl.attribute.global.DrawGlobalFormatVertexAttribute;
import net.daporkchop.fp2.gl.opengl.attribute.local.DrawLocalFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructFormatGenerator;
import net.daporkchop.fp2.gl.opengl.attribute.uniform.UniformArrayFormatShaderStorageBlock;
import net.daporkchop.fp2.gl.opengl.attribute.uniform.UniformFormatBlock;
import net.daporkchop.fp2.gl.opengl.bitset.GLBitSetBuilderImpl;
import net.daporkchop.fp2.gl.opengl.buffer.GLBufferImpl;
import net.daporkchop.fp2.gl.opengl.draw.command.DrawCommandBufferBuilderImpl;
import net.daporkchop.fp2.gl.opengl.compute.ComputeImpl;
import net.daporkchop.fp2.gl.opengl.draw.index.IndexFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.DrawLayoutBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.DrawLayoutImpl;
import net.daporkchop.fp2.gl.opengl.draw.shader.DrawShaderProgramImpl;
import net.daporkchop.fp2.gl.opengl.draw.shader.FragmentShaderImpl;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderBuilderImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;
import net.daporkchop.fp2.gl.opengl.draw.shader.VertexShaderImpl;
import net.daporkchop.fp2.gl.opengl.shader.source.SourceLine;
import net.daporkchop.fp2.gl.opengl.texture.TextureImpl;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.draw.shader.FragmentShader;
import net.daporkchop.fp2.gl.shader.BaseShaderBuilder;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.draw.shader.VertexShader;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class OpenGL implements GL {
    public static final String OPENGL_NAMESPACE = "fp2_gl_opengl";

    protected final GLAPI api;

    protected final GLVersion version;
    protected final GLProfile profile;
    protected final Set<GLExtension> extensions;

    protected final ResourceArena resourceArena = new ResourceArena();
    protected final ResourceProvider resourceProvider;

    protected final GLCompute compute;

    protected final Allocator directMemoryAllocator = new DirectMemoryAllocator();

    protected final StructFormatGenerator structFormatGenerator = new StructFormatGenerator();

    protected final int vertexAttributeAlignment;

    protected OpenGL(@NonNull OpenGLBuilder builder) {
        this.resourceProvider = ResourceProvider.selectingByNamespace(OPENGL_NAMESPACE, ResourceProvider.loadingClassResources(OpenGL.class), builder.resourceProvider);

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
                    .filter(extension -> !extension.core(this))
                    .filter(extension -> extensionNames.contains(extension.name()))
                    .collect(Collectors.collectingAndThen(Collectors.toSet(), ImmutableSet::copyOf));
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
        this.compute = GLExtension.GL_ARB_compute_shader.supported(this)
                ? new ComputeImpl(this)
                : GLModule.unsupportedImplementation(GLCompute.class);

        //compatibility hacks
        this.vertexAttributeAlignment = this.isOfficialAmdDriver() ? INT_SIZE : 1;
    }

    private boolean isOfficialAmdDriver() {
        String brand = this.api.glGetString(GL_VENDOR) + ' ' + this.api.glGetString(GL_VERSION) + ' ' + this.api.glGetString(GL_RENDERER);

        return (brand.contains("AMD") || brand.contains("ATI")) && !brand.contains("Mesa");
    }

    private boolean isOfficialIntelDriver() {
        String brand = this.api.glGetString(GL_VENDOR) + ' ' + this.api.glGetString(GL_VERSION) + ' ' + this.api.glGetString(GL_RENDERER);

        return (brand.contains("Intel")) && !brand.contains("Mesa");
    }

    public TextureImpl createTexture() {
        return new TextureImpl(this);
    }

    @Override
    public GLBufferImpl createBuffer(@NonNull BufferUsage usage) {
        return new GLBufferImpl(this, usage);
    }

    @Override
    public GLBitSetBuilder createBitSet() {
        return new GLBitSetBuilderImpl(this);
    }

    @Override
    public IndexFormatBuilder.TypeSelectionStage createIndexFormat() {
        return new IndexFormatBuilderImpl(this);
    }

    @Override
    public <S> UniformFormat<S> createUniformFormat(@NonNull Class<S> clazz) {
        return new UniformFormatBlock<>(this, clazz);
    }

    @Override
    public <S> UniformArrayFormat<S> createUniformArrayFormat(@NonNull Class<S> clazz) {
        return new UniformArrayFormatShaderStorageBlock<>(this, clazz);
    }

    @Override
    public <S> DrawGlobalFormat<S> createDrawGlobalFormat(@NonNull Class<S> clazz) {
        return new DrawGlobalFormatVertexAttribute<>(this, clazz);
    }

    @Override
    public <S> DrawLocalFormat<S> createDrawLocalFormat(@NonNull Class<S> clazz) {
        return new DrawLocalFormatImpl<>(this, clazz);
    }

    @Override
    public DrawLayoutBuilder createDrawLayout() {
        return new DrawLayoutBuilderImpl(this);
    }

    @Override
    public DrawCommandBufferBuilder.TypeStage createCommandBuffer() {
        return new DrawCommandBufferBuilderImpl(this);
    }

    //
    // SHADERS
    //

    @Override
    public BaseShaderBuilder<VertexShader> createVertexShader(@NonNull DrawLayout layout) {
        return new BaseShaderBuilderImpl<VertexShader, DrawLayout>(this, ShaderType.VERTEX, layout) {
            @Override
            protected VertexShader compile(@NonNull SourceLine... lines) throws ShaderCompilationException {
                return new VertexShaderImpl(this, lines);
            }
        };
    }

    @Override
    public BaseShaderBuilder<FragmentShader> createFragmentShader(@NonNull DrawLayout layout) {
        return new BaseShaderBuilderImpl<FragmentShader, DrawLayout>(this, ShaderType.FRAGMENT, layout) {
            @Override
            protected FragmentShader compile(@NonNull SourceLine... lines) throws ShaderCompilationException {
                return new FragmentShaderImpl(this, lines);
            }
        };
    }

    @Override
    public DrawShaderProgram linkShaderProgram(@NonNull DrawLayout layout, @NonNull VertexShader vertexShader, @NonNull FragmentShader fragmentShader) throws ShaderLinkageException {
        return new DrawShaderProgramImpl(this, (DrawLayoutImpl) layout, (VertexShaderImpl) vertexShader, (FragmentShaderImpl) fragmentShader);
    }

    @Override
    public void runCleanup() {
        this.resourceArena.clean();
    }

    @Override
    public void close() {
        this.resourceArena.release();
    }
}
