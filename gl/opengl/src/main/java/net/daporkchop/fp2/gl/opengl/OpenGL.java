/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.GlobalProperties;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.*;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeFormatBuilder;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormat2D;
import net.daporkchop.fp2.gl.attribute.texture.TextureFormatBuilder;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormat;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatBuilder;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.DrawLayoutBuilder;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.draw.index.IndexFormatBuilder;
import net.daporkchop.fp2.gl.draw.list.DrawCommandArrays;
import net.daporkchop.fp2.gl.draw.list.DrawCommandIndexed;
import net.daporkchop.fp2.gl.draw.list.DrawList;
import net.daporkchop.fp2.gl.draw.list.DrawListBuilder;
import net.daporkchop.fp2.gl.draw.list.selected.JavaSelectedDrawList;
import net.daporkchop.fp2.gl.draw.list.selected.ShaderSelectedDrawList;
import net.daporkchop.fp2.gl.draw.shader.BaseDrawShader;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.draw.shader.FragmentShader;
import net.daporkchop.fp2.gl.draw.shader.VertexShader;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.codegen.StructFormatGenerator;
import net.daporkchop.fp2.gl.opengl.attribute.texture.TextureFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.image.PixelFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.image.PixelFormatFactory;
import net.daporkchop.fp2.gl.opengl.attribute.texture.image.PixelFormatImpl;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.opengl.command.CommandBufferBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.DrawLayoutBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.DrawLayoutImpl;
import net.daporkchop.fp2.gl.opengl.draw.binding.DrawBindingImpl;
import net.daporkchop.fp2.gl.opengl.draw.index.IndexFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.arrays.multidrawindirect.DrawListMultiDrawArraysIndirect;
import net.daporkchop.fp2.gl.opengl.draw.list.arrays.multidrawindirect.JavaSelectedDrawListMultiDrawArraysIndirect;
import net.daporkchop.fp2.gl.opengl.draw.list.arrays.multidrawindirect.ShaderSelectedDrawListMultiDrawArraysIndirect;
import net.daporkchop.fp2.gl.opengl.draw.list.elements.multidrawindirect.DrawListMultiDrawElementsIndirect;
import net.daporkchop.fp2.gl.opengl.draw.list.elements.multidrawindirect.JavaSelectedDrawListMultiDrawElementsIndirect;
import net.daporkchop.fp2.gl.opengl.draw.list.elements.multidrawindirect.ShaderSelectedDrawListMultiDrawElementsIndirect;
import net.daporkchop.fp2.gl.opengl.draw.shader.DrawShaderProgramImpl;
import net.daporkchop.fp2.gl.opengl.draw.shader.FragmentShaderImpl;
import net.daporkchop.fp2.gl.opengl.draw.shader.VertexShaderImpl;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderBuilderImpl;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderProgramBuilderImpl;
import net.daporkchop.fp2.gl.shader.ShaderType;
import net.daporkchop.fp2.gl.shader.source.SourceLine;
import net.daporkchop.fp2.gl.opengl.transform.TransformLayoutBuilderImpl;
import net.daporkchop.fp2.gl.opengl.transform.TransformLayoutImpl;
import net.daporkchop.fp2.gl.opengl.transform.shader.TransformShaderBuilderImpl;
import net.daporkchop.fp2.gl.opengl.transform.shader.TransformShaderProgramBuilderImpl;
import net.daporkchop.fp2.gl.shader.BaseShaderBuilder;
import net.daporkchop.fp2.gl.shader.BaseShaderProgramBuilder;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.transform.TransformLayout;
import net.daporkchop.fp2.gl.transform.TransformLayoutBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderBuilder;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgramBuilder;
import net.daporkchop.lib.common.pool.handle.HandledPool;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.getInternalName;

/**
 * @author DaPorkchop_
 */
@Getter
public class OpenGL implements GL {
    public static final boolean DEBUG = Boolean.getBoolean(preventInline("fp2.gl.opengl.") + "debug");

    //the :gl:opengl package name, including the trailing '.'
    public static final String OPENGL_PACKAGE = OpenGL.class.getTypeName().substring(0, OpenGL.class.getTypeName().length() - OpenGL.class.getSimpleName().length()).intern();

    public static final String OPENGL_NAMESPACE = "fp2_gl_opengl";

    protected final GLAPI api;

    protected final GLVersion version;
    protected final GLProfile profile;
    protected final Set<GLExtension> extensions;
    protected final boolean forwardCompatibility;

    protected final ResourceArena resourceArena = new ResourceArena();
    protected final ResourceProvider resourceProvider;

    protected final Allocator directMemoryAllocator = new DirectMemoryAllocator();

    protected final StructFormatGenerator structFormatGenerator = new StructFormatGenerator(this);
    protected final PixelFormatFactory pixelFormatFactory;

    protected final LoadingCache<AttributeFormatBuilderImpl<?>, AttributeFormat<?>> attributeFormatCache = CacheBuilder.newBuilder()
            .weakValues()
            .build(CacheLoader.from(AttributeFormatType::createBestFormat));

    protected final int vertexAttributeAlignment;

    protected final boolean preserveInputGlState;

    protected final HandledPool<GLBuffer> tmpBufferPool = HandledPool.global(() -> this.createBuffer(BufferUsage.STREAM_DRAW), ReferenceStrength.WEAK, 16);

    @Getter(AccessLevel.NONE)
    protected boolean closed = false;

    protected OpenGL(@NonNull OpenGLBuilder builder) {
        this.resourceProvider = ResourceProvider.selectingByNamespace(OPENGL_NAMESPACE, ResourceProvider.loadingClassResources(OpenGL.class), builder.resourceProvider);

        {
            GLAPI api = GlobalProperties.find(OpenGL.class, "opengl")
                    .<Supplier<GLAPI>>getInstance("api.supplier")
                    .get();
            if (DEBUG) { //debug mode is enabled, check for errors
                api = ErrorCheckingWrapperGLAPI.wrap(this, api);
            }
            this.api = api;
        }

        this.version = this.api.determineVersion();
        this.preserveInputGlState = true;

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
                    .filter(extension -> !extension.core(this.version))
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
            this.forwardCompatibility = (contextFlags & GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT) != 0;

            if (this.version.compareTo(GLVersion.OpenGL31) <= 0) { // <= 3.1, profiles don't exist yet
                this.profile = GLProfile.UNKNOWN;
            } else { // >= 3.2
                assert !(compat && core) : "a context can't be using both core and compatibility profiles at once!";

                if (compat) {
                    this.profile = GLProfile.COMPAT;
                } else if (core) {
                    this.profile = GLProfile.CORE;
                } else {
                    this.profile = GLProfile.UNKNOWN;
                }
            }
        }

        //compatibility hacks
        this.vertexAttributeAlignment = this.isOfficialAmdDriver() ? INT_SIZE : 1;

        this.pixelFormatFactory = new PixelFormatFactory(this);
    }

    @SneakyThrows(IllegalAccessException.class)
    public static Optional<String> getNameIfPossible(int constant) {
        Field matchingField = null;
        for (Field field : OpenGLConstants.class.getFields()) {
            if ((field.getModifiers() & Modifier.STATIC) != 0
                && field.getType() == int.class
                && !field.getName().endsWith("_EXT")
                && ((Integer) field.get(null)) == constant) {
                if (matchingField != null) { //there are multiple matching fields!
                    return Optional.empty();
                }
                matchingField = field;
            }
        }

        return matchingField != null
                ? Optional.of(matchingField.getName()) //exactly one matching field was found
                : Optional.empty();
    }

    public static void visitGLConstant(@NonNull MethodVisitor mv, int constant) {
        if (DEBUG) { //debug mode - try to load constant by doing a GETSTATIC on the field in OpenGLConstants with a matching value (assuming there's exactly one)
            Optional<String> name = getNameIfPossible(constant);
            if (name.isPresent()) {
                mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), name.get(), INT_TYPE.getDescriptor());
                return;
            }
        }

        //load the constant value using a standard LDC instruction
        mv.visitLdcInsn(constant);
    }

    public GLEnvironment env() {
        return new GLEnvironment(this.version, this.profile, this.forwardCompatibility, this.extensions);
    }

    private boolean isOfficialAmdDriver() {
        String brand = this.api.glGetString(GL_VENDOR) + ' ' + this.api.glGetString(GL_VERSION) + ' ' + this.api.glGetString(GL_RENDERER);

        return (brand.contains("AMD") || brand.contains("ATI")) && !brand.contains("Mesa");
    }

    private boolean isOfficialIntelDriver() {
        String brand = this.api.glGetString(GL_VENDOR) + ' ' + this.api.glGetString(GL_VERSION) + ' ' + this.api.glGetString(GL_RENDERER);

        return (brand.contains("Intel")) && !brand.contains("Mesa");
    }

    public GLBuffer createBuffer(@NonNull BufferUsage usage) {
        return GLBuffer.create((net.daporkchop.fp2.gl.OpenGL) this.api, usage);
    }

    @Override
    public void runCleanup() {
        this.resourceArena.clean();
    }

    @Override
    public void close() {
        this.ensureOpen();
        this.resourceArena.release();
        this.closed = true;
    }

    @Override
    public CommandBufferBuilder createCommandBuffer() {
        return new CommandBufferBuilderImpl(this);
    }

    //
    // FORMATS
    //

    @Override
    public IndexFormatBuilder.TypeSelectionStage createIndexFormat() {
        return new IndexFormatBuilderImpl(this);
    }

    @Override
    public <S> AttributeFormatBuilder<S> createAttributeFormat(@NonNull Class<S> clazz) {
        return new AttributeFormatBuilderImpl<>(this, clazz);
    }

    @Override
    public PixelFormatBuilder.ChannelSelectionStage createPixelFormat() {
        return new PixelFormatBuilderImpl(this);
    }

    @Override
    public TextureFormatBuilder<TextureFormat2D> createTextureFormat2D(@NonNull PixelFormat pixelFormat, @NonNull String name) {
        return new TextureFormatBuilderImpl<TextureFormat2D>(this, (PixelFormatImpl) pixelFormat, name) {
            @Override
            public TextureFormat2D build() {
                return OpenGL.this.structFormatGenerator().getTexture2D(this.pixelFormat, this.name);
            }
        };
    }

    //
    // DRAW
    //

    @Override
    public DrawLayoutBuilder createDrawLayout() {
        return new DrawLayoutBuilderImpl(this);
    }

    @Override
    public DrawListBuilder<DrawCommandArrays> createDrawListArrays(@NonNull DrawBinding binding) {
        return new DrawListBuilderImpl<DrawCommandArrays>(this, (DrawBindingImpl) binding) {
            @Override
            public DrawList<DrawCommandArrays> buildRegular() {
                return new DrawListMultiDrawArraysIndirect(this);
            }

            @Override
            public JavaSelectedDrawList<DrawCommandArrays> buildJavaSelected() {
                return new JavaSelectedDrawListMultiDrawArraysIndirect(this);
            }

            @Override
            public ShaderSelectedDrawList<DrawCommandArrays> buildShaderSelected() {
                return new ShaderSelectedDrawListMultiDrawArraysIndirect(this);
            }
        };
    }

    @Override
    public DrawListBuilder<DrawCommandIndexed> createDrawListIndexed(@NonNull DrawBindingIndexed binding) {
        return new DrawListBuilderImpl<DrawCommandIndexed>(this, (DrawBindingImpl) binding) {
            @Override
            public DrawList<DrawCommandIndexed> buildRegular() {
                return new DrawListMultiDrawElementsIndirect(this);
            }

            @Override
            public JavaSelectedDrawList<DrawCommandIndexed> buildJavaSelected() {
                return new JavaSelectedDrawListMultiDrawElementsIndirect(this);
            }

            @Override
            public ShaderSelectedDrawList<DrawCommandIndexed> buildShaderSelected() {
                return new ShaderSelectedDrawListMultiDrawElementsIndirect(this);
            }
        };
    }

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
    public BaseShaderProgramBuilder<DrawShaderProgram, BaseDrawShader, DrawLayout> createDrawShaderProgram(@NonNull DrawLayout layout) {
        return new BaseShaderProgramBuilderImpl<DrawShaderProgram, BaseDrawShader, DrawLayoutImpl, DrawLayout>(this, (DrawLayoutImpl) layout) {
            @Override
            public DrawShaderProgram build() throws ShaderLinkageException {
                return new DrawShaderProgramImpl(this);
            }
        };
    }

    //
    // TRANSFORM
    //

    @Override
    public TransformLayoutBuilder createTransformLayout() {
        return new TransformLayoutBuilderImpl(this);
    }

    @Override
    public TransformShaderBuilder createTransformShader(@NonNull TransformLayout layout) {
        return new TransformShaderBuilderImpl(this, layout);
    }

    @Override
    public TransformShaderProgramBuilder createTransformShaderProgram(@NonNull TransformLayout layout) {
        return new TransformShaderProgramBuilderImpl(this, (TransformLayoutImpl) layout);
    }

    //
    // INTERNAL
    //

    public void ensureOpen() {
        checkState(!this.closed, "context closed!");
    }
}
