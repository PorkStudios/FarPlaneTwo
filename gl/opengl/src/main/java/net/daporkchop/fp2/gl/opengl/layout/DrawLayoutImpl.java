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
import net.daporkchop.fp2.gl.attribute.AttributeInterpretation;
import net.daporkchop.fp2.gl.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLExtension;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeImpl;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.VertexAttributeBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.common.VertexAttributeFormat;
import net.daporkchop.fp2.gl.opengl.attribute.global.GlobalAttributeBufferVertexAttribute;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLMatrixType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;
import net.daporkchop.fp2.gl.opengl.binding.DrawBindingBuilderImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;
import net.daporkchop.fp2.gl.opengl.texture.TextureTarget;
import net.daporkchop.lib.common.util.PArrays;

import java.util.ArrayList;
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
public class DrawLayoutImpl extends BaseLayoutImpl implements DrawLayout {
    protected final Map<AttributeFormatImpl, List<VertexBinding>> vertexBindingsByFormat;
    protected final Map<VertexAttributeFormat, VertexAttributeBindings> vertexAttributeBindingsByFormat;

    protected final List<VertexBinding> vertexBindings;
    protected final List<VertexAttributeBindings> vertexAttributeBindings;
    protected final List<FragmentColorBinding> fragmentColorBindings;
    protected final List<UniformBlockBinding> uniformBlockBindings;
    protected final List<TextureBinding> textureBindings;

    public DrawLayoutImpl(@NonNull DrawLayoutBuilderImpl builder) {
        super(builder);

        //create temporary lists
        List<VertexBinding> vertexBindings = new ArrayList<>();
        ImmutableList.Builder<VertexAttributeBindings> vertexAttributeBindings = ImmutableList.builder();
        List<FragmentColorBinding> fragmentColorBindings = new ArrayList<>();
        List<UniformBlockBinding> uniformBlockBindings = new ArrayList<>();
        List<TextureBinding> textureBindings = new ArrayList<>();

        //locals
        for (LocalAttributeFormatImpl<?> format : builder.locals) { //register all locals as standard vertex attributes
            vertexAttributeBindings.add(new VertexAttributeBindings(format, false));
        }

        //globals
        if (GLExtension.GL_ARB_instanced_arrays.supported(this.gl)) { //register globals as instanced vertex attributes
            for (AttributeFormatImpl format : builder.globals) {
                for (AttributeImpl attrib : format.attribsArray()) {
                    this.addVertexAttribute(vertexBindings, attrib, true);
                }
            }
        } else if (GLExtension.GL_ARB_texture_buffer_object.supported(this.gl)) { //register globals as texture buffers
            for (AttributeFormatImpl format : builder.globals) {
                this.addTexture(textureBindings, format, TextureTarget.TEXTURE_BUFFER);
            }
        } else {
            throw new UnsupportedOperationException();
        }

        //uniforms
        if (GLExtension.GL_ARB_uniform_buffer_object.supported(this.gl)) { //register uniforms as uniform buffer blocks
            for (AttributeFormatImpl format : builder.uniforms) {
                this.addUniformBuffer(uniformBlockBindings, format);
            }
        } else {
            throw new UnsupportedOperationException();
        }

        //outputs
        for (AttributeFormatImpl format : builder.outputs) { //register all outputs as standard fragment colors
            for (AttributeImpl attrib : format.attribsArray()) {
                this.addFragmentColor(fragmentColorBindings, attrib);
            }
        }

        //make temporary lists immutable
        this.vertexBindings = ImmutableList.copyOf(vertexBindings);
        this.vertexAttributeBindings = vertexAttributeBindings.build();
        this.fragmentColorBindings = ImmutableList.copyOf(fragmentColorBindings);
        this.uniformBlockBindings = ImmutableList.copyOf(uniformBlockBindings);
        this.textureBindings = ImmutableList.copyOf(textureBindings);

        //final groupings
        this.vertexBindingsByFormat = this.vertexBindings.stream().collect(Collectors.collectingAndThen(
                Collectors.groupingBy(
                        VertexBinding::format,
                        Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf)),
                ImmutableMap::copyOf));
        this.vertexAttributeBindingsByFormat = this.vertexAttributeBindings.stream().collect(Collectors.collectingAndThen(
                Collectors.toMap(
                        VertexAttributeBindings::format,
                        Function.identity()),
                ImmutableMap::copyOf));

        //final configuration
        this.configureVertexAttributes();
    }

    protected void configureVertexAttributes() {
        int index = this.vertexBindings.size();
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

    protected void addVertexAttribute(@NonNull List<VertexBinding> vertexBindings, @NonNull AttributeImpl attrib, boolean instanced) {
        int index = vertexBindings.size();
        int max = this.api.glGetInteger(GL_MAX_VERTEX_ATTRIBS);
        checkIndex(index < max, "cannot use more than %d vertex attributes!", max);

        vertexBindings.add(new VertexBinding(attrib.format(), attrib, index, instanced));
    }

    protected void addFragmentColor(@NonNull List<FragmentColorBinding> fragmentColorBindings, @NonNull AttributeImpl attrib) {
        int index = fragmentColorBindings.size();
        int max = this.api.glGetInteger(GL_MAX_DRAW_BUFFERS);
        checkIndex(index < max, "cannot use more than %d fragment colors!", max);

        fragmentColorBindings.add(new FragmentColorBinding(attrib.format(), attrib, index));
    }

    protected void addUniformBuffer(@NonNull List<UniformBlockBinding> uniformBlockBindings, @NonNull AttributeFormatImpl format) {
        int index = uniformBlockBindings.size();
        int max = this.api.glGetInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS);
        checkIndex(index < max, "cannot use more than %d vertex attributes!", max);

        uniformBlockBindings.add(new UniformBlockBinding(format, index));
    }

    protected void addTexture(@NonNull List<TextureBinding> textureBindings, @NonNull AttributeFormatImpl format, @NonNull TextureTarget target) {
        int unit = textureBindings.size() + 2;
        int max = this.api.glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        checkIndex(unit + format.attribsArray().length <= max, "cannot use more than %d texture units!", max);

        textureBindings.add(new TextureBinding(format, unit, target));
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public void prefixShaderSource(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        switch (type) {
            case VERTEX: {
                this.vertexBindings.forEach(binding -> binding.generateGLSL(builder));
                this.vertexAttributeBindings.forEach(binding -> binding.generateGLSL(builder));
                this.uniformBlockBindings.forEach(binding -> binding.generateGLSL(builder));
                this.textureBindings.forEach(binding -> binding.generateGLSL(this.gl, builder));
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
        this.vertexBindings.forEach(binding -> binding.bindAttribLocation(this.api, program));
        this.vertexAttributeBindings.forEach(binding -> binding.bindAttribLocation(this.api, program));
    }

    @Override
    public void configureProgramPostLink(int program) {
        this.uniformBlockBindings.forEach(binding -> binding.bindBlockLocation(this.api, program));
        this.textureBindings.forEach(binding -> binding.bindSamplerLocation(this.api, program));
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
        protected final VertexAttributeFormat format;
        protected final int[] attributeIndices;

        protected final boolean instanced;

        protected VertexAttributeBindings(@NonNull VertexAttributeFormat format, boolean instanced) {
            this.format = format;
            this.attributeIndices = PArrays.filled(format.attributeFields().size(), -1);
            this.instanced = instanced;
        }

        public void enableAndBind(@NonNull GLAPI api, @NonNull VertexAttributeBuffer buffer) {
            for (int attributeIndex : this.attributeIndices) {
                api.glEnableVertexAttribArray(attributeIndex);

                if (this.instanced) {
                    api.glVertexAttribDivisor(attributeIndex, 1);
                }
            }

            buffer.configureVAO(this.attributeIndices);
        }

        public void bindAttribLocation(@NonNull GLAPI api, int program) {
            for (int i = 0; i < this.attributeIndices.length; i++) {
                api.glBindAttribLocation(program, this.attributeIndices[i], this.format.attributeFields().get(i).name());
            }
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            this.format.attributeFields().forEach(field -> builder.append("in ").append(field.type().declaration(field.name())).append(";\n"));
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    @ToString
    @EqualsAndHashCode
    public static class VertexBinding {
        @Getter
        protected final AttributeFormatImpl format;
        protected final AttributeImpl attrib;

        protected final int bindingIndex;
        protected final boolean instanced;

        public void enableAndBind(@NonNull GLAPI api, @NonNull BaseAttributeBufferImpl buffer) {
            api.glEnableVertexAttribArray(this.bindingIndex);
            ((GlobalAttributeBufferVertexAttribute) buffer).bindVertexAttribute(api, this.bindingIndex, this.attrib);

            if (this.instanced) {
                api.glVertexAttribDivisor(this.bindingIndex, 1);
            }
        }

        public void bindAttribLocation(@NonNull GLAPI api, int program) {
            api.glBindAttribLocation(program, this.bindingIndex, this.attrib.name());
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            builder.append("in ").append(this.attrib.glslDeclaration()).append(";\n");
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    @ToString
    @EqualsAndHashCode
    public static class FragmentColorBinding {
        @Getter
        protected final AttributeFormatImpl format;
        protected final AttributeImpl attrib;

        protected final int colorIndex;

        public void bindColorLocation(@NonNull GLAPI api, int program) {
            api.glBindFragDataLocation(program, this.colorIndex, this.attrib.name());
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            builder.append("out ").append(this.attrib.glslDeclaration()).append(";\n");
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
        protected final AttributeFormatImpl format;

        protected final int bindingIndex;

        public void bindBlockLocation(@NonNull GLAPI api, int program) {
            int blockIndex = api.glGetUniformBlockIndex(program, this.format.name());
            checkArg(blockIndex != GL_INVALID_INDEX, "unable to find uniform block: %s", this.format.name());

            api.glUniformBlockBinding(program, blockIndex, this.bindingIndex);
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            builder.append("uniform ").append(this.format.name()).append(" {\n");
            for (AttributeImpl attrib : this.format.attribsArray()) {
                builder.append("    ").append(attrib.glslDeclaration()).append(";\n");
            }
            builder.append("};\n");
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
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
    }
}
