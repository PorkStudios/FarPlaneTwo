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

package net.daporkchop.fp2.gl.opengl.draw.command;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.draw.command.DrawCommandArrays;
import net.daporkchop.fp2.gl.draw.command.DrawList;
import net.daporkchop.fp2.gl.draw.command.DrawListBuilder;
import net.daporkchop.fp2.gl.draw.command.DrawCommandIndexed;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.draw.command.arrays.DrawListMultiDrawArrays;
import net.daporkchop.fp2.gl.opengl.draw.command.arrays.DrawListMultiDrawArraysIndirect;
import net.daporkchop.fp2.gl.opengl.draw.command.elements.DrawListMultiDrawElementsBaseVertex;
import net.daporkchop.fp2.gl.opengl.draw.command.elements.DrawListMultiDrawElementsIndirect;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DrawListBuilderImpl implements DrawListBuilder.TypeStage, DrawListBuilder.OptimizeStage {
    @NonNull
    protected final OpenGL gl;

    protected DrawBinding binding;
    protected boolean elements;

    protected boolean optimizeForCpuSelection;

    //
    // TypeStage
    //

    @Override
    public OptimizeStage<DrawList<DrawCommandArrays>> forArrays(@NonNull DrawBinding binding) {
        this.binding = binding;
        this.elements = false;
        return uncheckedCast(this);
    }

    @Override
    public OptimizeStage<DrawList<DrawCommandIndexed>> forIndexed(@NonNull DrawBindingIndexed binding) {
        this.binding = binding;
        this.elements = true;
        return uncheckedCast(this);
    }

    //
    // OptimizeStage
    //

    @Override
    public DrawListBuilder optimizeForCpuSelection() {
        this.optimizeForCpuSelection = true;
        return this;
    }

    //
    // CommandBufferBuilder
    //

    @Override
    public DrawList build() {
        if (this.elements) {
            return this.optimizeForCpuSelection
                    ? new DrawListMultiDrawElementsBaseVertex(this)
                    : new DrawListMultiDrawElementsIndirect(this);
        } else {
            return this.optimizeForCpuSelection
                    ? new DrawListMultiDrawArrays(this)
                    : new DrawListMultiDrawArraysIndirect(this);
        }
    }
}
