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

package net.daporkchop.fp2.gl.opengl.draw;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.common.ShaderStorageBlockFormat;
import net.daporkchop.fp2.gl.opengl.attribute.common.TextureFormat;
import net.daporkchop.fp2.gl.opengl.attribute.common.UniformBlockFormat;
import net.daporkchop.fp2.gl.opengl.attribute.common.VertexAttributeBuffer;
import net.daporkchop.fp2.gl.opengl.attribute.common.VertexAttributeFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLMatrixType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;
import net.daporkchop.fp2.gl.opengl.attribute.texture.TextureTarget;
import net.daporkchop.fp2.gl.opengl.draw.binding.DrawBindingBuilderImpl;
import net.daporkchop.fp2.gl.opengl.layout.BaseLayoutImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;
import net.daporkchop.lib.common.util.PorkUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class DrawLayoutImpl extends BaseLayoutImpl implements DrawLayout {
    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> allFormatsByName;
    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> uniformFormatsByName;
    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> uniformArrayFormatsByName;
    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> globalFormatsByName;
    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> localFormatsByName;
    protected final BiMap<String, BaseAttributeFormatImpl<?, ?>> textureFormatsByName;

    protected final BiMap<String, GLSLField> allAttribsByName;
    protected final BiMap<String, GLSLField> uniformAttribsByName;
    protected final BiMap<String, GLSLField> uniformArrayAttribsByName;
    protected final BiMap<String, GLSLField> globalAttribsByName;
    protected final BiMap<String, GLSLField> localAttribsByName;
    protected final BiMap<String, GLSLField> textureAttribsByName;

    protected final Map<BaseAttributeFormatImpl<?, ?>, VertexAttributeBindings> vertexAttributeBindingsByFormat;

    protected final List<FragmentColorBinding> fragmentColorBindings;
    protected final List<ShaderStorageBlockBinding> shaderStorageBlockBindings;
    protected final List<TextureBinding> textureBindings;
    protected final List<VertexAttributeBindings> vertexAttributeBindings;
    protected final List<UniformBlockBinding> uniformBlockBindings;

    public DrawLayoutImpl(@NonNull DrawLayoutBuilderImpl builder) {
        super(builder.gl);

        {
            Collector<BaseAttributeFormatImpl<?, ?>, ?, BiMap<String, BaseAttributeFormatImpl<?, ?>>> formatToMapCollector = Collectors.collectingAndThen(
                    Collectors.toMap(BaseAttributeFormatImpl::name, Function.identity()),
                    ImmutableBiMap::copyOf);

            //collect all attribute formats into a single map (also ensures names are unique)
            this.allFormatsByName = builder.allFormatsAndChildren().collect(formatToMapCollector);

            //create maps for formats, separated by usage
            this.uniformFormatsByName = builder.uniforms.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).collect(formatToMapCollector);
            this.uniformArrayFormatsByName = builder.uniformArrays.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).collect(formatToMapCollector);
            this.globalFormatsByName = builder.globals.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).collect(formatToMapCollector);
            this.localFormatsByName = builder.locals.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).collect(formatToMapCollector);
            this.textureFormatsByName = builder.textures.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).collect(formatToMapCollector);
        }

        {
            Collector<GLSLField, ?, BiMap<String, GLSLField>> attribToMapCollector = Collectors.collectingAndThen(
                    Collectors.toMap(GLSLField::name, Function.identity()),
                    ImmutableBiMap::copyOf);

            //collect all attributes into a single map (also ensures names are unique)
            this.allAttribsByName = builder.allFormatsAndChildren().map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);

            //create maps for attributes, separated by usage
            this.uniformAttribsByName = builder.uniforms.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);
            this.uniformArrayAttribsByName = builder.uniformArrays.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);
            this.globalAttribsByName = builder.globals.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);
            this.localAttribsByName = builder.locals.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);
            this.textureAttribsByName = builder.textures.stream().flatMap(BaseAttributeFormatImpl::selfAndChildren).map(BaseAttributeFormatImpl::attributeFields).flatMap(List::stream).collect(attribToMapCollector);
        }

        //create temporary lists
        ImmutableList.Builder<FragmentColorBinding> fragmentColorBindings = ImmutableList.builder();
        ImmutableList.Builder<ShaderStorageBlockBinding> shaderStorageBlockBindings = ImmutableList.builder();
        ImmutableList.Builder<TextureBinding> textureBindings = ImmutableList.builder();
        ImmutableList.Builder<VertexAttributeBindings> vertexAttributeBindings = ImmutableList.builder();
        ImmutableList.Builder<UniformBlockBinding> uniformBlockBindings = ImmutableList.builder();

        BindingLocationAssigner assigner = new BindingLocationAssigner(this.gl, this.api);

        //register everything
        builder.allFormatsAndChildren()
                .forEach(format -> {
                    boolean handled = false;
                    if (format instanceof ShaderStorageBlockFormat) {
                        handled = true;
                        shaderStorageBlockBindings.add(new ShaderStorageBlockBinding(format, assigner.shaderStorageBuffer()));
                    }
                    if (format instanceof TextureFormat) {
                        handled = true;
                        textureBindings.add(new TextureBinding(format, assigner.textureUnit()));
                    }
                    if (format instanceof VertexAttributeFormat) {
                        handled = true;
                        vertexAttributeBindings.add(new VertexAttributeBindings(format, assigner));
                    }
                    if (format instanceof UniformBlockFormat) {
                        handled = true;
                        uniformBlockBindings.add(new UniformBlockBinding(format, assigner.uniformBuffer()));
                    }

                    if (!handled) {
                        throw new UnsupportedOperationException("don't know how to handle " + PorkUtil.className(format) + ": " + format);
                    }
                });

        fragmentColorBindings.add(new FragmentColorBinding(assigner.fragmentColor()));

        //make temporary lists immutable
        this.fragmentColorBindings = fragmentColorBindings.build();
        this.shaderStorageBlockBindings = shaderStorageBlockBindings.build();
        this.textureBindings = textureBindings.build();
        this.vertexAttributeBindings = vertexAttributeBindings.build();
        this.uniformBlockBindings = uniformBlockBindings.build();

        //final groupings
        this.vertexAttributeBindingsByFormat = this.vertexAttributeBindings.stream().collect(Collectors.collectingAndThen(
                Collectors.toMap(
                        VertexAttributeBindings::format,
                        Function.identity()),
                ImmutableMap::copyOf));
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public void prefixShaderSource(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        switch (type) {
            case VERTEX: {
                this.shaderStorageBlockBindings.forEach(binding -> binding.generateGLSL(builder));
                this.textureBindings.forEach(binding -> binding.generateGLSL(builder));
                this.vertexAttributeBindings.forEach(binding -> binding.generateGLSL(builder));
                this.uniformBlockBindings.forEach(binding -> binding.generateGLSL(builder));
                break;
            }
            case FRAGMENT: {
                this.fragmentColorBindings.forEach(binding -> binding.generateGLSL(builder));
                this.shaderStorageBlockBindings.forEach(binding -> binding.generateGLSL(builder));
                this.textureBindings.forEach(binding -> binding.generateGLSL(builder));
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
        this.shaderStorageBlockBindings.forEach(binding -> binding.bindBlockLocation(this.api, program));
        this.textureBindings.forEach(binding -> binding.bindSamplerLocations(this.api, program));
        this.uniformBlockBindings.forEach(binding -> binding.bindBlockLocation(this.api, program));
    }

    @Override
    public DrawBindingBuilder.OptionallyIndexedStage createBinding() {
        return new DrawBindingBuilderImpl(this);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    @ToString
    @EqualsAndHashCode
    public static class FragmentColorBinding {
        protected final GLSLField field = new GLSLField(GLSLType.vec(GLSLPrimitiveType.FLOAT, 4), "f_color");

        protected final int colorIndex;

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
    @ToString
    @EqualsAndHashCode
    public static class VertexAttributeBindings {
        @Getter
        protected final BaseAttributeFormatImpl<?, ?> format;
        protected final int[] attributeIndices;

        protected VertexAttributeBindings(@NonNull BaseAttributeFormatImpl<?, ?> format, @NonNull BindingLocationAssigner assigner) {
            this.format = format;
            this.attributeIndices = format.attributeFields().stream()
                    .map(GLSLField::type)
                    .mapToInt(type -> assigner.vertexAttribute(type instanceof GLSLMatrixType ? ((GLSLMatrixType) type).columns() : 1))
                    .toArray();
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
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class ShaderStorageBlockBinding {
        @NonNull
        protected final BaseAttributeFormatImpl<?, ?> format;

        protected final int bindingIndex;

        public void bindBlockLocation(@NonNull GLAPI api, int program) {
            int blockIndex = api.glGetProgramResourceIndex(program, GL_SHADER_STORAGE_BLOCK, "BUFFER_" + this.format.name());
            checkArg(blockIndex != GL_INVALID_INDEX, "unable to find uniform block: %s", this.format.name());

            api.glShaderStorageBlockBinding(program, blockIndex, this.bindingIndex);
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            if (((ShaderStorageBlockFormat) this.format).isArray()) {
                builder.append("struct STRUCT_").append(this.format.name()).append(" {\n");
                this.format.attributeFields().forEach(field -> builder.append("    ").append(field.declaration()).append(";\n"));
                builder.append("};\n");

                builder.append("layout(").append(((ShaderStorageBlockFormat) this.format).interfaceBlockLayout()).append(") buffer BUFFER_").append(this.format.name()).append(" {\n");
                builder.append("    STRUCT_").append(this.format.name()).append(" buffer_").append(this.format.name()).append("[];\n");
                builder.append("};\n");

                this.format.attributeFields().forEach(field -> {
                    builder.append(field.declaration()).append("(in uint index) {\n");
                    builder.append("    return buffer_").append(this.format.name()).append("[index].").append(field.name()).append(";\n");
                    builder.append("}\n");
                });
            } else {
                builder.append("layout(").append(((ShaderStorageBlockFormat) this.format).interfaceBlockLayout()).append(") buffer BUFFER_").append(this.format.name()).append(" {\n");
                this.format.attributeFields().forEach(field -> builder.append("    ").append(field.declaration()).append(";\n"));
                builder.append("};\n");
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Getter
    @ToString
    @EqualsAndHashCode
    public static class TextureBinding {
        protected final BaseAttributeFormatImpl<?, ?> format;
        protected final TextureTarget target;

        protected final int unit;

        protected TextureBinding(@NonNull BaseAttributeFormatImpl<?, ?> format, int unit) {
            this.format = format;
            this.target = ((TextureFormat) format).target();
            this.unit = unit;
        }

        public void bindSamplerLocations(@NonNull GLAPI api, int program) {
            int oldProgram = api.glGetInteger(GL_CURRENT_PROGRAM);
            try {
                api.glUseProgram(program);

                String name = "sampler_" + this.format.attributeFields().get(0).name();
                int location = api.glGetUniformLocation(program, name);
                if (location >= 0) { //sampler may have been optimized out, so only set it if it's present
                    api.glUniform(location, this.unit);
                }
            } finally {
                api.glUseProgram(oldProgram);
            }
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            GLSLField field = this.format.attributeFields().get(0);
            builder.append("uniform ").append(this.target.glslSamplerName()).append(" sampler_").append(field.name()).append(";\n");

            builder.append(field.declaration()).append("(in vec").append(this.target.coordVectorComponents()).append(" coord) {\n");
            builder.append("    return texture(sampler_").append(field.name()).append(", coord);\n");
            builder.append("}\n");

            builder.append(field.declaration()).append("(in vec").append(this.target.coordVectorComponents()).append(" coord, ")
                    .append("in vec").append(this.target.gradientVectorComponents()).append(" gradX, ")
                    .append("in vec").append(this.target.gradientVectorComponents()).append(" gradY) {\n");
            builder.append("    return textureGrad(sampler_").append(field.name()).append(", coord, gradX, gradY);\n");
            builder.append("}\n");
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
        protected final BaseAttributeFormatImpl<?, ?> format;

        protected final int bindingIndex;

        public void bindBlockLocation(@NonNull GLAPI api, int program) {
            int blockIndex = api.glGetUniformBlockIndex(program, "UNIFORM_" + this.format.name());
            checkArg(blockIndex != GL_INVALID_INDEX, "unable to find uniform block: %s", this.format.name());

            api.glUniformBlockBinding(program, blockIndex, this.bindingIndex);
        }

        protected void generateGLSL(@NonNull StringBuilder builder) {
            builder.append("layout(").append(((UniformBlockFormat) this.format).interfaceBlockLayout()).append(") uniform UNIFORM_").append(this.format.name()).append(" {\n");
            this.format.attributeFields().forEach(field -> builder.append("    ").append(field.declaration()).append(";\n"));
            builder.append("};\n");
        }
    }
}
