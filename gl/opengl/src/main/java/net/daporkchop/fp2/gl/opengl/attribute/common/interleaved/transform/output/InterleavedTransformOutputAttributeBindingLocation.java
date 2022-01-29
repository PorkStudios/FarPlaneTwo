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

package net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.transform.output;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.layout.LayoutEntry;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class InterleavedTransformOutputAttributeBindingLocation<S> implements BindingLocation<InterleavedAttributeBufferImpl<?, S>> {
    protected final LayoutEntry<? extends InterleavedAttributeFormatImpl<?, S>> layout;

    public InterleavedTransformOutputAttributeBindingLocation(@NonNull LayoutEntry<? extends InterleavedAttributeFormatImpl<?, S>> layout, @NonNull BindingLocationAssigner assigner) {
        this.layout = layout;
    }

    @Override
    public AttributeUsage usage() {
        return AttributeUsage.TRANSFORM_OUTPUT;
    }

    @Override
    public void configureProgramPreLink(@NonNull GLAPI api, int program) {
        api.glTransformFeedbackVaryings(program,
                this.layout.attributeFields().map(GLSLField::name).toArray(CharSequence[]::new),
                GL_INTERLEAVED_ATTRIBS);
    }

    @Override
    public void configureProgramPostLink(@NonNull GLAPI api, int program) {
        //no-op
    }

    @Override
    public void generateGLSL(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        checkArg(type == ShaderType.VERTEX, "transform output requires only a vertex shader!");

        this.layout.attributeFields().forEach(field -> builder.append("out ").append(field.declaration()).append(";\n"));
    }

    @Override
    public void configureBuffer(@NonNull GLAPI api, @NonNull InterleavedAttributeBufferImpl<?, S> buffer) {
        //no-op
    }

    @Override
    public void configureState(@NonNull MutableState state, @NonNull InterleavedAttributeBufferImpl<?, S> buffer) {
        state.set(StateProperties.BOUND_TRANSFORM_FEEDBACK_BUFFER[0], buffer.buffer().id());
    }
}
