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
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.draw.DrawBindingBuilder;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeImpl;
import net.daporkchop.fp2.gl.opengl.attribute.local.LocalAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.draw.DrawBindingBuilderImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class DrawLayoutImpl extends BaseLayoutImpl implements DrawLayout {
    protected final Map<AttributeFormatImpl, List<VertexBinding>> vertexBindingsByFormat;

    protected final List<VertexBinding> vertexBindings;
    protected final List<UniformBufferBinding> uniformBufferBindings;

    public DrawLayoutImpl(@NonNull DrawLayoutBuilderImpl builder) {
        super(builder);

        //create temporary lists
        List<VertexBinding> vertexBindings = new ArrayList<>();
        List<UniformBufferBinding> uniformBufferBindings = new ArrayList<>();

        //locals
        for (AttributeFormatImpl format : builder.locals) { //register all locals as standard vertex attributes
            for (AttributeImpl attrib : format.attribsArray()) {
                this.addVertexAttribute(vertexBindings, attrib, false);
            }
            format.attribs().forEach((name, attrib) -> this.addVertexAttribute(vertexBindings, (AttributeImpl) attrib, false));
        }

        //globals
        if (this.gl.version().compareTo(GLVersion.OpenGL33) >= 0 || this.gl.extensions().contains(GLExtension.GL_ARB_instanced_arrays)) { //register globals as instanced vertex attributes
            for (AttributeFormatImpl format : builder.globals) {
                for (AttributeImpl attrib : format.attribsArray()) {
                    this.addVertexAttribute(vertexBindings, attrib, true);
                }
            }
        } else {
            throw new UnsupportedOperationException();
        }

        //uniforms
        if (this.gl.version().compareTo(GLVersion.OpenGL31) >= 0 || this.gl.extensions().contains(GLExtension.GL_ARB_uniform_buffer_object)) { //register uniforms as uniform buffer blocks
            for (AttributeFormatImpl format : builder.uniforms) {
                this.addUniformBuffer(uniformBufferBindings, format);
            }
        } else {
            throw new UnsupportedOperationException();
        }

        //make temporary lists immutable
        this.vertexBindings = ImmutableList.copyOf(vertexBindings);
        this.uniformBufferBindings = ImmutableList.copyOf(uniformBufferBindings);

        //final groupings
        this.vertexBindingsByFormat = this.vertexBindings.stream().collect(Collectors.collectingAndThen(
                Collectors.groupingBy(
                        VertexBinding::format,
                        Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf)),
                ImmutableMap::copyOf));
    }

    protected void addVertexAttribute(@NonNull List<VertexBinding> vertexBindings, @NonNull AttributeImpl attrib, boolean instanced) {
        int index = vertexBindings.size();
        int max = this.api.glGetInteger(GL_MAX_VERTEX_ATTRIBS);
        checkIndex(index < max, "cannot use more than %d vertex attributes!", max);

        vertexBindings.add(new VertexBinding(attrib.format(), attrib, index, instanced));
    }

    protected void addUniformBuffer(@NonNull List<UniformBufferBinding> uniformBufferBindings, @NonNull AttributeFormatImpl format) {
        int index = uniformBufferBindings.size();
        int max = this.api.glGetInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS);
        checkIndex(index < max, "cannot use more than %d vertex attributes!", max);

        uniformBufferBindings.add(new UniformBufferBinding(format, index));
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public void configureProgramPreLink(int program) {
        this.vertexBindings.forEach(binding -> binding.bindAttribLocation(this.api, program));
    }

    @Override
    public void configureProgramPostLink(int program) {
        this.uniformBufferBindings.forEach(binding -> binding.bindBlockLocation(this.api, program));
    }

    @Override
    public DrawBindingBuilder.UniformsStage createBinding() {
        return new DrawBindingBuilderImpl(this);
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

        public void enableAndBind(@NonNull GLAPI api, @NonNull LocalAttributeBufferImpl buffer) {
            api.glEnableVertexAttribArray(this.bindingIndex);
            buffer.bindAttribute(api, this.bindingIndex, this.attrib);

            if (this.instanced) {
                api.glVertexAttribDivisor(this.bindingIndex, 1);
            }
        }

        public void bindAttribLocation(@NonNull GLAPI api, int program) {
            api.glBindAttribLocation(program, this.bindingIndex, this.attrib.name());
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    @ToString
    @EqualsAndHashCode
    public static class UniformBufferBinding {
        @Getter
        protected final AttributeFormatImpl format;

        protected final int bindingIndex;

        public void bindBlockLocation(@NonNull GLAPI api, int program) {
            int blockIndex = api.glGetUniformBlockIndex(program, this.format.name());
            checkArg(blockIndex != GL_INVALID_INDEX, "unable to find uniform block: %s", this.format.name());

            api.glUniformBlockBinding(program, blockIndex, this.bindingIndex);
        }
    }
}