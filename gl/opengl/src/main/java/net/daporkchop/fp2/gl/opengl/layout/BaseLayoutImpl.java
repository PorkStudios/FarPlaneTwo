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

package net.daporkchop.fp2.gl.opengl.layout;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.layout.BaseLayout;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.binding.BindingLocationAssigner;
import net.daporkchop.fp2.gl.opengl.shader.ShaderType;

import java.util.List;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseLayoutImpl implements BaseLayout {
    protected final OpenGL gl;
    protected final GLAPI api;

    protected final List<LayoutEntry<?>> entries;

    public BaseLayoutImpl(@NonNull BaseLayoutBuilderImpl<?, ?> builder) {
        this.gl = builder.gl();
        this.api = builder.gl().api();

        //construct initial list
        this.entries = builder.entries.build();

        //assign binding locations for all attribute formats
        BindingLocationAssigner assigner = new BindingLocationAssigner(this.gl, this.api);
        this.entries.forEach(entry -> entry.location(entry.format().bindingLocation(uncheckedCast(entry), assigner)));
    }

    @Override
    public void close() {
        //no-op
    }

    public void prefixShaderSource(@NonNull ShaderType type, @NonNull StringBuilder builder) {
        this.entries.forEach(entry -> entry.location().generateGLSL(type, builder));
    }

    public void configureProgramPreLink(int program) {
        this.entries.forEach(entry -> entry.location().configureProgramPreLink(this.api, program));
    }

    public void configureProgramPostLink(int program) {
        this.entries.forEach(entry -> entry.location().configureProgramPostLink(this.api, program));
    }
}
