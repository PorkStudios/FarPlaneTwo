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
 */

package net.daporkchop.fp2.gl.opengl.attribute.texture;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocation;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLBasicType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLSamplerType;
import net.daporkchop.fp2.gl.opengl.command.state.MutableState;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.layout.LayoutEntry;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class TextureBindingLocation<B extends BaseTextureImpl<?>> implements BindingLocation<B> {
    protected final LayoutEntry<? extends BaseTextureFormatImpl<?>> layout;

    protected final TextureTarget target;
    protected final int unit;

    public TextureBindingLocation(@NonNull LayoutEntry<? extends BaseTextureFormatImpl<?>> layout, @NonNull TextureTarget target, @NonNull BindingLocationAssigner assigner) {
        this.layout = layout;
        this.target = target;
        this.unit = assigner.textureUnit();
    }

    @Override
    public AttributeUsage usage() {
        return AttributeUsage.TEXTURE;
    }

    @Override
    public void configureProgramPreLink(@NonNull GLAPI api, int program) {
        //no-op
    }

    @Override
    public void configureProgramPostLink(@NonNull GLAPI api, int program) {
        int oldProgram = api.glGetInteger(GL_CURRENT_PROGRAM);
        try {
            api.glUseProgram(program);

            String name = "sampler_" + this.layout.attributeFields().findFirst().get().name();
            int location = api.glGetUniformLocation(program, name);
            if (location >= 0) { //sampler may have been optimized out, so only set it if it's present
                api.glUniform(location, this.unit);
            }
        } finally {
            api.glUseProgram(oldProgram);
        }
    }

    @Override
    public void generateGLSL(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        GLSLField<GLSLSamplerType> field = uncheckedCast(this.layout.attributeFields().findFirst().get());
        String swizzle = field.type().components() == 4 ? "" : ".rgba".substring(0, field.type().components() + 1);

        builder.append("uniform ").append(this.target.glslSamplerName()).append(" sampler_").append(field.name()).append(";\n");

        builder.append(field.declaration()).append("(in vec").append(this.target.coordVectorComponents()).append(" coord) {\n");
        builder.append("    return texture(sampler_").append(field.name()).append(", coord)").append(swizzle).append(";\n");
        builder.append("}\n");

        builder.append(field.declaration()).append("(in vec").append(this.target.coordVectorComponents()).append(" coord, ")
                .append("in vec").append(this.target.gradientVectorComponents()).append(" gradX, ")
                .append("in vec").append(this.target.gradientVectorComponents()).append(" gradY) {\n");
        builder.append("    return textureGrad(sampler_").append(field.name()).append(", coord, gradX, gradY)").append(swizzle).append(";\n");
        builder.append("}\n");
    }

    @Override
    public void configureBuffer(@NonNull GLAPI api, @NonNull B buffer) {
        //no-op
    }

    @Override
    public void configureState(@NonNull MutableState state, @NonNull B buffer) {
        state.set(StateProperties.BOUND_TEXTURE[this.unit].get(this.target), buffer.id());
    }
}
