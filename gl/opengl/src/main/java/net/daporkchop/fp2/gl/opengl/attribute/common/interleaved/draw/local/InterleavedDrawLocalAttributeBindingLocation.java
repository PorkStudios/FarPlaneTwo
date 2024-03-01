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

package net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.draw.local;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLBasicType;
import net.daporkchop.fp2.gl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.layout.LayoutEntry;
import net.daporkchop.fp2.gl.shader.ShaderType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author DaPorkchop_
 */
public class InterleavedDrawLocalAttributeBindingLocation<S> implements BindingLocation<InterleavedAttributeBufferImpl<?, S>> {
    protected final LayoutEntry<? extends InterleavedAttributeFormatImpl<?, S>> layout;
    protected final int[] attributeIndices;

    public InterleavedDrawLocalAttributeBindingLocation(@NonNull LayoutEntry<? extends InterleavedAttributeFormatImpl<?, S>> layout, @NonNull BindingLocationAssigner assigner) {
        this.layout = layout;
        this.attributeIndices = this.layout.attributeFields()
                .flatMap(GLSLField::basicFields)
                .map(GLSLField::type)
                .mapToInt(GLSLBasicType::requiredVertexAttributeSlots)
                .map(assigner::vertexAttribute)
                .toArray();
    }

    @Override
    public AttributeUsage usage() {
        return AttributeUsage.DRAW_LOCAL;
    }

    @Override
    public void configureProgramPreLink(@NonNull GLAPI api, int program) {
        List<GLSLField<? extends GLSLBasicType>> fields = this.layout.attributeFields().flatMap(GLSLField::basicFields).collect(Collectors.toList());
        for (int i = 0; i < this.attributeIndices.length; i++) {
            api.glBindAttribLocation(program, this.attributeIndices[i], fields.get(i).name());
        }
    }

    @Override
    public void configureProgramPostLink(@NonNull GLAPI api, int program) {
        //no-op
    }

    @Override
    public void generateGLSL(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        if (type != ShaderType.VERTEX) {
            return;
        }

        this.layout.attributeFields().forEach(field -> builder.append("in ").append(field.declaration()).append(";\n"));
    }

    @Override
    public void configureBuffer(@NonNull GLAPI api, @NonNull InterleavedAttributeBufferImpl<?, S> buffer) {
        //enable attributes
        for (int attributeIndex : this.attributeIndices) {
            api.glEnableVertexAttribArray(attributeIndex);
        }

        //configure attributes
        buffer.buffer().bind(BufferTarget.ARRAY_BUFFER, target -> this.layout.format().structFormat().configureVAO(api, this.attributeIndices));
    }

    @Override
    public void configureState(@NonNull MutableState state, @NonNull InterleavedAttributeBufferImpl<?, S> buffer) {
        //no-op
    }
}
