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
import net.daporkchop.fp2.client.gl.indirect.AbstractDrawIndirectCommandBuffer;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommandBuffer;
import net.daporkchop.fp2.util.DirectBufferReuse;
import net.daporkchop.fp2.util.alloc.Allocator;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gl.GLCompatibilityHelper.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Implementation of {@link IDrawIndirectCommandBuffer} for {@link DrawElementsIndirectCommand}s whose commands are stored only in client memory.
 *
 * @author DaPorkchop_
 */
final class CPUDrawElementsIndirectCommandBuffer extends AbstractDrawIndirectCommandBuffer<DrawElementsIndirectCommand> implements IDrawElementsIndirectCommandBuffer {
    protected final int type;

    public CPUDrawElementsIndirectCommandBuffer(@NonNull Allocator alloc, int mode, int type) {
        super(alloc, mode);

        this.type = type;
    }

    @Override
    protected void drawBatch(long offset, int count, int stride) {
        if (ALLOW_MULTIDRAW) { //fast: execute all commands at once using multidraw
            //LWJGL is fucking garbage and doesn't let me do this unless drawing commands from GPU memory
            //glMultiDrawElementsIndirect(this.mode, this.type, this.addr + offset, count, stride);

            //this is gross because it might not have enough capacity
            //glMultiDrawElementsIndirect(this.mode, this.type, DirectBufferReuse.wrapInt(this.addr + offset, toInt(this.commandSize() * count * stride)), count, stride);

            int maxCommandsPerBatch = Integer.MAX_VALUE / stride; //maximum number of commands that we can fit into Integer.MAX_VALUE bytes, therefore allowing us to fit it into a ByteBuffer
            for (int done = 0, batchCount; done < count; done += batchCount) {
                batchCount = min(count - done, maxCommandsPerBatch);
                glMultiDrawElementsIndirect(this.mode, this.type, DirectBufferReuse.wrapByte(this.addr + offset + (long) done * stride, batchCount * stride), batchCount, stride);
            }
        } else {
            for (long addr = offset, end = addr + (long) count * stride; addr != end; addr += stride) {
                //LWJGL is fucking garbage and doesn't let me do this unless drawing commands from GPU memory
                //  glDrawElementsIndirect(this.mode, this.type, addr);

                glDrawElementsIndirect(this.mode, this.type, DirectBufferReuse.wrapByte(addr, toInt(DrawElementsIndirectCommand._SIZE)));
            }
        }
    }
}
