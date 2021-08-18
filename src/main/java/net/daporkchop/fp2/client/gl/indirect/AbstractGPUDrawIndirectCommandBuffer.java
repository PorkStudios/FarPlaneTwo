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

package net.daporkchop.fp2.client.gl.indirect;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.util.alloc.Allocator;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL42.*;

/**
 * Base implementation of {@link IDrawIndirectCommandBuffer} whose commands are buffered in GPU video memory.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractGPUDrawIndirectCommandBuffer<C extends IDrawIndirectCommand> extends AbstractDrawIndirectCommandBuffer<C> {
    protected final GLBuffer buffer = new GLBuffer(GL_STREAM_DRAW);
    protected final boolean barrier;

    public AbstractGPUDrawIndirectCommandBuffer(@NonNull Allocator alloc, int mode, boolean barrier) {
        super(alloc, mode);

        this.barrier = barrier;
    }

    @Override
    protected void doRelease() {
        super.doRelease();
        this.buffer.delete();
    }

    @Override
    public void resize(long capacity) {
        super.resize(capacity);

        if (this.dirty) { //clear buffer contents to free up vram immediately, space will be totally re-allocated once the buffer is re-uploaded anyway
            try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
                buffer.capacity(0L);
            }
        }
    }

    @Override
    public GLBuffer openglBuffer() {
        this.upload(); //re-upload to make sure any changes are made visible

        return this.buffer;
    }

    @Override
    protected void draw0(long offset, long stride, long count) {
        this.upload();

        try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) { //bind buffer to ensure the commands are accessible to the draw functions
            if (this.barrier) { //place a memory barrier before drawing (if requested)
                glMemoryBarrier(GL_COMMAND_BARRIER_BIT);
            }

            super.draw0(offset, stride, count);
        }
    }

    protected void upload() {
        if (this.dirty) { //re-upload buffer contents if needed
            this.dirty = false;

            try (GLBuffer buffer = this.buffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
                buffer.upload(this.addr, this.capacity * this.commandSize());
            }
        }
    }
}
