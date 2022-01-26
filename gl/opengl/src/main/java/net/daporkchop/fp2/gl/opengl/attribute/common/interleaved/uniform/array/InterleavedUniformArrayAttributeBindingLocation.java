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

package net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.uniform.array;

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
public class InterleavedUniformArrayAttributeBindingLocation<S> implements BindingLocation<InterleavedAttributeBufferImpl<S, ?>> {
    protected final InterleavedStructFormat<S> structFormat;
    protected final int bindingIndex;

    public InterleavedUniformArrayAttributeBindingLocation(@NonNull InterleavedStructFormat<S> structFormat, @NonNull BindingLocationAssigner assigner) {
        this.structFormat = structFormat;
        this.bindingIndex = assigner.shaderStorageBuffer();
    }

    @Override
    public InternalAttributeUsage usage() {
        return InternalAttributeUsage.UNIFORM_ARRAY;
    }

    @Override
    public void configureProgramPreLink(@NonNull GLAPI api, int program) {
        //no-op
    }

    @Override
    public void configureProgramPostLink(@NonNull GLAPI api, int program) {
        int blockIndex = api.glGetProgramResourceIndex(program, GL_SHADER_STORAGE_BLOCK, "BUFFER_" + this.structFormat.structName());
        checkArg(blockIndex != GL_INVALID_INDEX, "unable to find shader storage block: %s", "BUFFER_" + this.structFormat.structName());

        api.glShaderStorageBlockBinding(program, blockIndex, this.bindingIndex);
    }

    @Override
    public void generateGLSL(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        builder.append("struct STRUCT_").append(this.structFormat.structName()).append(" {\n");
        this.structFormat.glslFields().forEach(field -> builder.append("    ").append(field.declaration(this.usage().glslPrefix())).append(";\n"));
        builder.append("};\n");

        builder.append("layout(").append(this.structFormat.layoutName()).append(") buffer BUFFER_").append(this.structFormat.structName()).append(" {\n");
        builder.append("    STRUCT_").append(this.structFormat.structName()).append(" buffer_").append(this.structFormat.structName()).append("[];\n");
        builder.append("};\n");

        this.structFormat.glslFields().forEach(field -> {
            builder.append(field.declaration(this.usage().glslPrefix())).append("(in uint index) {\n");
            builder.append("    return buffer_").append(this.structFormat.structName()).append("[index].").append(this.usage().glslPrefix()).append(field.name()).append(";\n");
            builder.append("}\n");
        });
    }

    @Override
    public void configureBuffer(@NonNull GLAPI api, @NonNull InterleavedAttributeBufferImpl<S, ?> buffer) {
        //no-op
    }

    @Override
    public void configureState(@NonNull MutableState state, @NonNull InterleavedAttributeBufferImpl<S, ?> buffer) {
        state.set(StateProperties.BOUND_SHADER_STORAGE_BUFFER[this.bindingIndex], buffer.buffer().id());
    }
}
