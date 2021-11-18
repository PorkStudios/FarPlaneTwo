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

package net.daporkchop.fp2.gl.opengl.draw.command.arrays;

import lombok.NonNull;
import net.daporkchop.fp2.gl.bitset.GLBitSet;
import net.daporkchop.fp2.gl.draw.command.DrawCommandArrays;
import net.daporkchop.fp2.gl.draw.binding.DrawMode;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.bitset.AbstractGLBitSet;
import net.daporkchop.fp2.gl.opengl.draw.command.DrawCommandBufferBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.command.DrawCommandBufferImpl;
import net.daporkchop.fp2.gl.opengl.draw.binding.DrawBindingImpl;
import net.daporkchop.fp2.gl.opengl.draw.shader.DrawShaderProgramImpl;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class CommandBufferMultiDrawArrays extends DrawCommandBufferImpl<DrawCommandArrays, DrawBindingImpl> {
    protected long firstAddr;
    protected long countAddr;

    public CommandBufferMultiDrawArrays(@NonNull DrawCommandBufferBuilderImpl builder) {
        super(builder);
    }

    @Override
    protected void resize0(int oldCapacity, int newCapacity) {
        this.firstAddr = this.alloc.realloc(this.firstAddr, newCapacity * (long) INT_SIZE);
        this.countAddr = this.alloc.realloc(this.countAddr, newCapacity * (long) INT_SIZE);

        if (newCapacity > oldCapacity) { //zero out the commands
            PUnsafe.setMemory(this.firstAddr + oldCapacity * (long) INT_SIZE, (newCapacity - oldCapacity) * (long) INT_SIZE, (byte) 0);
            PUnsafe.setMemory(this.countAddr + oldCapacity * (long) INT_SIZE, (newCapacity - oldCapacity) * (long) INT_SIZE, (byte) 0);
        }
    }

    @Override
    public void close() {
        this.alloc.free(this.firstAddr);
        this.alloc.free(this.countAddr);
    }

    @Override
    public void set(int index, @NonNull DrawCommandArrays command) {
        checkIndex(this.capacity, index);

        PUnsafe.putInt(this.firstAddr + index * (long) INT_SIZE, command.first());
        PUnsafe.putInt(this.countAddr + index * (long) INT_SIZE, command.count());
    }

    @Override
    public DrawCommandArrays get(int index) {
        checkIndex(this.capacity, index);

        return new DrawCommandArrays(
                PUnsafe.getInt(this.firstAddr + index * (long) INT_SIZE),
                PUnsafe.getInt(this.countAddr + index * (long) INT_SIZE));
    }

    @Override
    public void clear(int index) {
        PUnsafe.putInt(this.countAddr + checkIndex(this.capacity, index) * (long) INT_SIZE, 0);
    }

    @Override
    public void execute(@NonNull DrawMode mode, @NonNull DrawShaderProgram shader) {
        ((DrawShaderProgramImpl) shader).bind(() -> this.binding.bind(() -> {
            this.api.glMultiDrawArrays(GLEnumUtil.from(mode), this.firstAddr, this.countAddr, this.capacity);
        }));
    }

    @Override
    public void execute(@NonNull DrawMode mode, @NonNull DrawShaderProgram shader, @NonNull GLBitSet _selector) {
        AbstractGLBitSet selector = (AbstractGLBitSet) _selector;

        this.executeMappedClient(mode, shader, selector);
    }

    protected void executeMappedClient(@NonNull DrawMode mode, @NonNull DrawShaderProgram shader, @NonNull AbstractGLBitSet selector) {
        long dstCounts = PUnsafe.allocateMemory(this.capacity * (long) INT_SIZE);
        try {
            selector.mapClient(this.capacity, (bitsBase, bitsOffset) -> {
                long srcCountAddr = this.countAddr;
                long dstCountAddr = dstCounts;
                for (int bitIndex = 0; bitIndex < this.capacity; bitsOffset += INT_SIZE) {
                    int word = PUnsafe.getInt(bitsBase, bitsOffset);

                    for (int mask = 1; mask != 0 && bitIndex < this.capacity; mask <<= 1, bitIndex++, srcCountAddr += INT_SIZE, dstCountAddr += INT_SIZE) {
                        PUnsafe.putInt(dstCountAddr, PUnsafe.getInt(srcCountAddr) & (-(word & mask) >> 31));
                        PUnsafe.putInt(dstCountAddr, 3);
                    }
                }
            });

            ((DrawShaderProgramImpl) shader).bind(() -> this.binding.bind(() -> {
                this.api.glMultiDrawArrays(GLEnumUtil.from(mode), this.firstAddr, this.countAddr, this.capacity);
            }));
        } finally {
            PUnsafe.freeMemory(dstCounts);
        }
    }
}
