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

package net.daporkchop.fp2.gl.opengl.command;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.command.DrawCommand;
import net.daporkchop.fp2.gl.command.buffer.DrawCommandBuffer;
import net.daporkchop.fp2.gl.draw.DrawBinding;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class DrawCommandBufferImpl<C extends DrawCommand, B extends DrawBinding> implements DrawCommandBuffer<C> {
    protected final OpenGL gl;
    protected final GLAPI api;

    protected final Allocator alloc = new DirectMemoryAllocator();

    protected final B binding;

    protected int capacity = 0;

    public DrawCommandBufferImpl(@NonNull DrawCommandBufferBuilderImpl builder) {
        this.gl = builder.gl;
        this.api = this.gl.api();

        this.binding = uncheckedCast(builder.binding);
    }

    @Override
    public void resize(int capacity) {
        if (this.capacity != notNegative(capacity, "capacity")) {
            this.resize0(this.capacity, capacity);
            this.capacity = capacity;
        }
    }

    protected abstract void resize0(int oldCapacity, int newCapacity);
}
