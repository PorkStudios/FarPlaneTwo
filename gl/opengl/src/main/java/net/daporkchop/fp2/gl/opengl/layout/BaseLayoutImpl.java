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

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.layout.BaseLayout;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.vertex.VertexFormatImpl;

import java.util.Arrays;
import java.util.Set;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseLayoutImpl implements BaseLayout {
    protected final OpenGL gl;
    protected final GLAPI api;

    protected final Set<VertexFormatImpl> globalFormats;
    protected final Set<VertexFormatImpl> localFormats;
    
    public BaseLayoutImpl(@NonNull BaseLayoutBuilderImpl builder) {
        this.gl = builder.gl;
        this.api = this.gl.api();

        this.globalFormats = ImmutableSet.copyOf(builder.globals);
        if (this.globalFormats.size() != builder.globals.length) {
            throw new IllegalArgumentException("cannot construct draw layout with duplicate global vertex formats! " + Arrays.toString(builder.globals));
        }

        this.localFormats = ImmutableSet.copyOf(builder.locals);
        if (this.localFormats.size() != builder.locals.length) {
            throw new IllegalArgumentException("cannot construct draw layout with duplicate local vertex formats! " + Arrays.toString(builder.locals));
        }
    }

    public abstract void configureProgramPreLink(int program);

    public abstract void configureProgramPostLink(int program);
}
