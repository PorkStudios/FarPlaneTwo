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

package net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.uniform.standard;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.layout.LayoutEntry;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class InterleavedUniformAttributeBindingLocation<S> implements BindingLocation<InterleavedAttributeBufferImpl<?, S>> {
    protected final LayoutEntry<? extends InterleavedAttributeFormatImpl<?, S>> layout;
    protected final int bindingIndex;

    public InterleavedUniformAttributeBindingLocation(@NonNull LayoutEntry<? extends InterleavedAttributeFormatImpl<?, S>> layout, @NonNull BindingLocationAssigner assigner) {
        this.layout = layout;
        this.bindingIndex = assigner.uniformBuffer();
    }

    @Override
    public AttributeUsage usage() {
        return AttributeUsage.UNIFORM;
    }

    @Override
    public void configureProgramPreLink(@NonNull GLAPI api, int program) {
        //no-op
    }

    @Override
    public void configureProgramPostLink(@NonNull GLAPI api, int program) {
        int blockIndex = api.glGetUniformBlockIndex(program, "UNIFORM_" + this.layout.name());
        checkArg(blockIndex != GL_INVALID_INDEX, "unable to find uniform block: %s", "UNIFORM_" + this.layout.name());

        api.glUniformBlockBinding(program, blockIndex, this.bindingIndex);
    }

    @Override
    public void generateGLSL(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        builder.append("layout(").append(this.layout.format().structFormat().layoutName()).append(") uniform UNIFORM_").append(this.layout.name()).append(" {\n");
        this.layout.attributeFields().forEach(field -> builder.append("    ").append(field.declaration()).append(";\n"));
        builder.append("};\n");
    }

    @Override
    public void configureBuffer(@NonNull GLAPI api, @NonNull InterleavedAttributeBufferImpl<?, S> buffer) {
        //no-op
    }

    @Override
    public void configureState(@NonNull MutableState state, @NonNull InterleavedAttributeBufferImpl<?, S> buffer) {
        state.set(StateProperties.BOUND_UNIFORM_BUFFER[this.bindingIndex], buffer.buffer().id());
    }
}
