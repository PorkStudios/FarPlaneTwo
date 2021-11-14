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

package net.daporkchop.fp2.gl.opengl.layout;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.gl.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.UniformBlockFormat;
import net.daporkchop.fp2.gl.opengl.attribute.common.VertexAttributeBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.common.VertexAttributeFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLMatrixType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;
import net.daporkchop.fp2.gl.opengl.binding.DrawBindingBuilderImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class DrawLayoutImpl extends BaseLayoutImpl implements DrawLayout {
    protected final Map<BaseAttributeFormatImpl<?>, VertexAttributeBindings> vertexAttributeBindingsByFormat;

    protected final List<FragmentColorBinding> fragmentColorBindings;
    protected final List<VertexAttributeBindings> vertexAttributeBindings;
    protected final List<UniformBlockBinding> uniformBlockBindings;
    //protected final List<TextureBinding> textureBindings;

    public DrawLayoutImpl(@NonNull DrawLayoutBuilderImpl builder) {
        super(builder);

        //create temporary lists
        ImmutableList.Builder<FragmentColorBinding> fragmentColorBindings = ImmutableList.builder();
        ImmutableList.Builder<VertexAttributeBindings> vertexAttributeBindings = ImmutableList.builder();
        ImmutableList.Builder<UniformBlockBinding> uniformBlockBindings = ImmutableList.builder();
        //ImmutableList.Builder<TextureBinding> textureBindings = ImmutableList.builder();

        //register everything
        Stream.of(builder.uniforms, builder.globals, builder.locals).flatMap(List::stream)
                .forEach(format -> {
                    if (format instanceof VertexAttributeFormat) {
                        vertexAttributeBindings.add(new VertexAttributeBindings(format));
                    } else if (format instanceof UniformBlockFormat) {
                        uniformBlockBindings.add(new UniformBlockBinding(format));
                    } else {
                        throw new UnsupportedOperationException("don't know how to handle " + PorkUtil.className(format) + ": " + format);
                    }
                });

        fragmentColorBindings.add(new FragmentColorBinding());

        //make temporary lists immutable
        this.fragmentColorBindings = fragmentColorBindings.build();
        this.vertexAttributeBindings = vertexAttributeBindings.build();
        this.uniformBlockBindings = uniformBlockBindings.build();
        //this.textureBindings = textureBindings.build();

        //final groupings
        this.vertexAttributeBindingsByFormat = this.vertexAttributeBindings.stream().collect(Collectors.collectingAndThen(
                Collectors.toMap(
                        VertexAttributeBindings::format,
                        Function.identity()),
                ImmutableMap::copyOf));

        //final configuration
        this.configureFragmentColors();
        this.configureVertexAttributes();
        this.configureUniformBlocks();
    }

    protected void configureFragmentColors() {
        int index = 0;
        int max = this.api.glGetInteger(GL_MAX_DRAW_BUFFERS);

        for (FragmentColorBinding binding : this.fragmentColorBindings) {
            checkIndex(index < max, "cannot use more than %d fragment colors!", max);
            binding.colorIndex = index++;
        }
    }

    protected void configureVertexAttributes() {
        int index = 0;
        int max = this.api.glGetInteger(GL_MAX_VERTEX_ATTRIBS);

        for (VertexAttributeBindings bindings : this.vertexAttributeBindings) {
            int[] attributeIndices = bindings.attributeIndices;

            for (int i = 0; i < attributeIndices.length; i++) {
                checkIndex(index < max, "cannot use more than %d vertex attributes!", max);
                attributeIndices[i] = index;

                GLSLType type = bindings.format.attributeFields().get(i).type();
                index += type instanceof GLSLMatrixType ? ((GLSLMatrixType) type).columns() : 1;
            }
        }
    }

    protected void configureUniformBlocks() {
        int index = 0;
        int max = this.api.glGetInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS);

        for (UniformBlockBinding binding : this.uniformBlockBindings) {
            checkIndex(index < max, "cannot use more than %d uniform buffers!", max);
            binding.bindingIndex = index++;
        }
    }

    /*protected void addTexture(@NonNull List<TextureBinding> textureBindings, @NonNull AttributeFormatImpl format, @NonNull TextureTarget target) {
        int unit = textureBindings.size() + 2;
        int max = this.api.glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        checkIndex(unit + format.attribsArray().length <= max, "cannot use more than %d texture units!", max);

        textureBindings.add(new TextureBinding(format, unit, target));
    }*/

    @Override
    public void close() {
        //no-op
    }

    @Override
    public void prefixShaderSource(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        switch (type) {
            case VERTEX: {
                this.vertexAttributeBindings.forEach(binding -> binding.generateGLSL(builder));
                this.uniformBlockBindings.forEach(binding -> binding.generateGLSL(builder));
                //this.textureBindings.forEach(binding -> binding.generateGLSL(this.gl, builder));
                break;
            }
            case FRAGMENT: {
                this.fragmentColorBindings.forEach(binding -> binding.generateGLSL(builder));
                this.uniformBlockBindings.forEach(binding -> binding.generateGLSL(builder));
                break;
            }
            default:
                throw new IllegalArgumentException("draw layout cannot be used by " + type + " shader");
        }
    }

    @Override
    public void configureProgramPreLink(int program) {
        this.vertexAttributeBindings.forEach(binding -> binding.bindAttribLocation(this.api, program));
    }

    @Override
    public void configureProgramPostLink(int program) {
        this.uniformBlockBindings.forEach(binding -> binding.bindBlockLocation(this.api, program));
        //this.textureBindings.forEach(binding -> binding.bindSamplerLocation(this.api, program));
    }

    @Override
    public DrawBindingBuilder.OptionallyIndexedStage createBinding() {
        return new DrawBindingBuilderImpl(this);
    }

    /**
     * @author DaPorkchop_
     */
    @ToString
    @EqualsAndHashCode
    public static class VertexAttributeBindings {
        @Getter
        protected final BaseAttributeFormatImpl<?> format;
        protected final int[] attributeIndices;

        protected VertexAttributeBindings(@NonNull BaseAttributeFormatImpl<?> format) {
            this.format = format;
            this.attributeIndices = PArrays.filled(format.attributeFields().size(), -1);
        }

        public void enableAndBind(@NonNull GLAPI api, @NonNull BaseAttributeBufferImpl<?, ?, ?> buffer) {
            for (int attributeIndex : this.attributeIndices) {
                api.glEnableVertexAttribArray(attributeIndex);
            }

            ((VertexAttributeBuffer) buffer).configureVAO(this.attributeIndices);
        }

        public void bindAttribLocation(@NonNull GLAPI api, int program) {
            for (int i = 0; i < this.attributeIndices.length; i++) {
                api.glBindAttribLocation(program, this.attributeIndices[i], this.format.attributeFields().get(i).name());
            }
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            this.format.attributeFields().forEach(field -> builder.append("in ").append(field.declaration()).append(";\n"));
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    @ToString
    @EqualsAndHashCode
    public static class FragmentColorBinding {
        protected final GLSLField field = new GLSLField(GLSLType.vec(GLSLPrimitiveType.FLOAT, 4), "f_color");

        protected int colorIndex = -1;

        public void bindColorLocation(@NonNull GLAPI api, int program) {
            api.glBindFragDataLocation(program, this.colorIndex, this.field.name());
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            builder.append("out ").append(this.field.declaration()).append(";\n");
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class UniformBlockBinding {
        @NonNull
        protected final BaseAttributeFormatImpl<?> format;

        protected int bindingIndex = -1;

        public void bindBlockLocation(@NonNull GLAPI api, int program) {
            int blockIndex = api.glGetUniformBlockIndex(program, this.format.name());
            checkArg(blockIndex != GL_INVALID_INDEX, "unable to find uniform block: %s", this.format.name());

            api.glUniformBlockBinding(program, blockIndex, this.bindingIndex);
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            builder.append("uniform ").append(this.format.name()).append(" {\n");
            this.format.attributeFields().forEach(field -> builder.append("    ").append(field.declaration()).append(";\n"));
            builder.append("};\n");
        }
    }

    /**
     * @author DaPorkchop_
     */
    /*@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class TextureBinding {
        protected final AttributeFormatImpl format;

        protected final int unit;
        @NonNull
        protected final TextureTarget target;

        protected void generateGLSL(@NonNull OpenGL gl, @NonNull StringBuilder builder) {
            for (AttributeImpl attrib : this.format.attribsArray()) {
                builder.append("uniform ")
                        .append(attrib.interpretation() == AttributeInterpretation.INTEGER ? "i" : "").append(this.target.glslSamplerName()).append(' ')
                        .append("_sampler_").append(attrib.name()).append(";\n");
                builder.append("#define ").append(attrib.name()).append(" (")
                        .append("texelFetch(_sampler_").append(attrib.name()).append(", ")
                        .append(GLExtension.GL_ARB_shader_draw_parameters.core(gl) ? "gl_DrawID" : "gl_DrawIDARB")
                        .append(").").append("rgba", 0, attrib.components())
                        .append(")\n");
            }
        }

        public void bindSamplerLocation(@NonNull GLAPI api, int program) {
            int oldProgram = api.glGetInteger(GL_CURRENT_PROGRAM);
            try {
                api.glUseProgram(program);

                for (AttributeImpl attrib : this.format.attribsArray()) {
                    String name = "_sampler_" + attrib.name();
                    int location = api.glGetUniformLocation(program, name);
                    checkArg(location >= 0, "unable to find sampler uniform: %s", name);

                    api.glUniform(location, this.unit + attrib.index());
                }
            } finally {
                api.glUseProgram(oldProgram);
            }
        }
    }*/
}
