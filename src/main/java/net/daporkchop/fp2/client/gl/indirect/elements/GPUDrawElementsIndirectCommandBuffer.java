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

package net.daporkchop.fp2.client.gl.indirect.elements;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.indirect.AbstractGPUDrawIndirectCommandBuffer;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommandBuffer;
import net.daporkchop.fp2.util.alloc.Allocator;

import static net.daporkchop.fp2.client.gl.GLCompatibilityHelper.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Implementation of {@link IDrawIndirectCommandBuffer} for {@link DrawElementsIndirectCommand}s whose commands are buffered in GPU video memory.
 *
 * @author DaPorkchop_
 */
final class GPUDrawElementsIndirectCommandBuffer extends AbstractGPUDrawIndirectCommandBuffer<DrawElementsIndirectCommand> implements IDrawElementsIndirectCommandBuffer {
    protected final int type;

    public GPUDrawElementsIndirectCommandBuffer(@NonNull Allocator alloc, int mode, boolean barrier, int type) {
        super(alloc, mode, barrier);

        this.type = type;
    }

    @Override
    protected void drawBatch(long offset, int count, int stride) {
        if (ALLOW_MULTIDRAW) { //fast: execute all commands at once
            glMultiDrawElementsIndirect(this.mode, this.type, offset, count, stride);
        } else { //slow: execute commands one-at-a-time
            for (long addr = offset, end = addr + (long) count * stride; addr != end; addr += stride) {
                glDrawElementsIndirect(this.mode, this.type, addr);
            }
        }
    }
}
