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

package net.daporkchop.fp2.gl.opengl.draw.list.arrays.multidrawindirect;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.draw.list.DrawCommandArrays;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.bitset.AbstractGLBitSet;
import net.daporkchop.fp2.gl.opengl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.opengl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.command.state.StateValueProperty;
import net.daporkchop.fp2.gl.opengl.draw.binding.DrawBindingImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListImpl;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Map;

import static java.lang.Math.*;
import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.fp2.gl.opengl.draw.list.arrays.multidrawindirect.MultiDrawArraysIndirect.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class DrawListMultiDrawArraysIndirect extends DrawListImpl<DrawCommandArrays, DrawBindingImpl> {
    protected final GLBuffer buffer;

    protected long commandsAddr;

    public DrawListMultiDrawArraysIndirect(@NonNull DrawListBuilderImpl builder) {
        super(builder);

        this.buffer = this.gl().createBuffer(BufferUsage.STREAM_DRAW);
    }

    @Override
    protected void resize0(int oldCapacity, int newCapacity) {
        this.commandsAddr = this.alloc.realloc(this.commandsAddr, newCapacity * _SIZE);

        if (newCapacity > oldCapacity) { //zero out the commands
            PUnsafe.setMemory(this.commandsAddr + oldCapacity * _SIZE, (newCapacity - oldCapacity) * _SIZE, (byte) 0);
        }
    }

    @Override
    public void close() {
        this.alloc.free(this.commandsAddr);
        this.buffer.close();
    }

    @Override
    public void set(int index, @NonNull DrawCommandArrays command) {
        long commandAddr = this.commandsAddr + checkIndex(this.capacity, index) * _SIZE;
        _first(commandAddr, command.first());
        _count(commandAddr, command.count());
        _instanceCount(commandAddr, 1);
        _baseInstance(commandAddr, index);
    }

    @Override
    public DrawCommandArrays get(int index) {
        checkIndex(this.capacity, index);

        long commandAddr = this.commandsAddr + index * _SIZE;
        return new DrawCommandArrays(_first(commandAddr), _count(commandAddr));
    }

    @Override
    public void clear(int index) {
        _instanceCount(this.commandsAddr + checkIndex(this.capacity, index) * _SIZE, 0);
    }

    @Override
    public Map<StateValueProperty<?>, Object> stateProperties0() {
        return ImmutableMap.of(StateProperties.BOUND_BUFFER.get(BufferTarget.DRAW_INDIRECT_BUFFER), this.buffer.id());
    }

    @Override
    public void draw0(GLAPI api, int mode) {
        api.glBufferData(GL_DRAW_INDIRECT_BUFFER, this.capacity * _SIZE, this.commandsAddr, GL_STREAM_DRAW);
        api.glMultiDrawArraysIndirect(mode, 0L, this.capacity, 0);
    }

    @Override
    public void draw0(GLAPI api, int mode, AbstractGLBitSet selectionMask) {
        long dstCommands = PUnsafe.allocateMemory(this.capacity * _SIZE);
        try {
            selectionMask.mapClient(this.capacity, (bitsBase, bitsOffset) -> {
                long srcCommandAddr = this.commandsAddr;
                long dstCommandAddr = dstCommands;
                for (int bitIndex = 0; bitIndex < this.capacity; bitsOffset += INT_SIZE) {
                    int word = PUnsafe.getInt(bitsBase, bitsOffset);

                    for (int endBit = min(bitIndex + Integer.SIZE, this.capacity); bitIndex < endBit; bitIndex++, srcCommandAddr += _SIZE, dstCommandAddr += _SIZE) {
                        _first(dstCommandAddr, _first(srcCommandAddr));
                        _count(dstCommandAddr, _count(srcCommandAddr));
                        _baseInstance(dstCommandAddr, _baseInstance(srcCommandAddr));
                        _instanceCount(dstCommandAddr, _instanceCount(srcCommandAddr) & (word >>> bitIndex));
                    }
                }
            });

            api.glBufferData(GL_DRAW_INDIRECT_BUFFER, this.capacity * _SIZE, dstCommands, GL_STREAM_DRAW);
            api.glMultiDrawArraysIndirect(mode, 0L, this.capacity, 0);
        } finally {
            PUnsafe.freeMemory(dstCommands);
        }
    }
}
