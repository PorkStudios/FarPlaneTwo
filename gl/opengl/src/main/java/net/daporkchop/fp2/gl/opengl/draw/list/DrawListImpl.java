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

package net.daporkchop.fp2.gl.opengl.draw.list;

import lombok.NonNull;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.list.DrawList;
import net.daporkchop.fp2.gl.draw.list.selected.JavaSelectedDrawList;
import net.daporkchop.fp2.gl.draw.list.selected.ShaderSelectedDrawList;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.command.Uop;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.GeneratedSupplier;
import net.daporkchop.fp2.gl.opengl.command.state.State;
import net.daporkchop.fp2.gl.transform.binding.TransformBinding;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgram;

import java.util.List;
import java.util.function.IntPredicate;

/**
 * @author DaPorkchop_
 */
public interface DrawListImpl<C extends DrawCommand> extends DrawList<C> {
    DrawBinding binding();

    List<Uop> draw(@NonNull State state, @NonNull DrawShaderProgram drawShader, int mode);

    /**
     * @author DaPorkchop_
     */
    interface JavaSelected<C extends DrawCommand> extends DrawListImpl<C>, JavaSelectedDrawList<C> {
        List<Uop> drawSelected(@NonNull State state, @NonNull DrawShaderProgram drawShader, int mode, @NonNull GeneratedSupplier<IntPredicate> selector);
    }

    /**
     * @author DaPorkchop_
     */
    interface ShaderSelected<C extends DrawCommand> extends DrawListImpl<C>, ShaderSelectedDrawList<C> {
        List<Uop> drawSelected(@NonNull State state, @NonNull DrawShaderProgram drawShader, int mode, @NonNull TransformShaderProgram selectionShader, @NonNull TransformBinding selectionBinding);
    }
}
