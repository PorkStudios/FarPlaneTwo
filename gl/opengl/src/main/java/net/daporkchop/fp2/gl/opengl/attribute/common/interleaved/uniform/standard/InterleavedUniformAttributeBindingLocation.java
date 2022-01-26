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

package net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.uniform.standard;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.InternalAttributeUsage;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class InterleavedUniformAttributeBindingLocation<S> implements BindingLocation<InterleavedAttributeBufferImpl<S, ?>> {
    protected final InterleavedStructFormat<S> structFormat;
    protected final int bindingIndex;

    public InterleavedUniformAttributeBindingLocation(@NonNull InterleavedStructFormat<S> structFormat, @NonNull BindingLocationAssigner assigner) {
        this.structFormat = structFormat;
        this.bindingIndex = assigner.uniformBuffer();
    }

    @Override
    public InternalAttributeUsage usage() {
        return InternalAttributeUsage.UNIFORM;
    }

    @Override
    public void configureProgramPreLink(@NonNull GLAPI api, int program) {
        //no-op
    }

    @Override
    public void configureProgramPostLink(@NonNull GLAPI api, int program) {
        int blockIndex = api.glGetUniformBlockIndex(program, "UNIFORM_" + this.structFormat.structName());
        checkArg(blockIndex != GL_INVALID_INDEX, "unable to find uniform block: %s", "UNIFORM_" + this.structFormat.structName());

        api.glUniformBlockBinding(program, blockIndex, this.bindingIndex);
    }

    @Override
    public void generateGLSL(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        builder.append("layout(").append(this.structFormat.layoutName()).append(") uniform UNIFORM_").append(this.structFormat.structName()).append(" {\n");
        this.structFormat.glslFields().forEach(field -> builder.append("    ").append(field.declaration(this.usage().glslPrefix())).append(";\n"));
        builder.append("};\n");
    }

    @Override
    public void configureBuffer(@NonNull GLAPI api, @NonNull InterleavedAttributeBufferImpl<S, ?> buffer) {
        //no-op
    }

    @Override
    public void configureState(@NonNull MutableState state, @NonNull InterleavedAttributeBufferImpl<S, ?> buffer) {
        state.set(StateProperties.BOUND_UNIFORM_BUFFER[this.bindingIndex], buffer.buffer().id());
    }
}
