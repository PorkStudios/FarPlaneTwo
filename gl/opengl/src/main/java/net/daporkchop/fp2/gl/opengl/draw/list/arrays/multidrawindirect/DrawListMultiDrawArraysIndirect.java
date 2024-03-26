/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.gl.opengl.draw.list.arrays.multidrawindirect;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.buffer.GLMutableBuffer;
import net.daporkchop.fp2.gl.draw.list.DrawCommandArrays;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.opengl.command.state.State;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.command.state.StateValueProperty;
import net.daporkchop.fp2.gl.opengl.draw.binding.DrawBindingImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.AbstractDrawListImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.SimpleDrawListImpl;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Collections;
import java.util.Map;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.fp2.gl.opengl.draw.list.arrays.multidrawindirect.MultiDrawArraysIndirect.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class DrawListMultiDrawArraysIndirect extends AbstractDrawListImpl<DrawCommandArrays, DrawBindingImpl> implements SimpleDrawListImpl<DrawCommandArrays> {
    protected final GLMutableBuffer buffer;

    protected long commandsAddr;

    public DrawListMultiDrawArraysIndirect(@NonNull DrawListBuilderImpl builder) {
        super(builder);

        this.buffer = this.gl().createBuffer();
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
    public Map<StateValueProperty<?>, Object> configureStateForDraw0(@NonNull State state) {
        return Collections.singletonMap(StateProperties.BOUND_BUFFER.get(BufferTarget.DRAW_INDIRECT_BUFFER), this.buffer.id());
    }

    @Override
    public void draw0(GLAPI api, int mode) {
        api.glBufferData(GL_DRAW_INDIRECT_BUFFER, this.capacity * _SIZE, this.commandsAddr, GL_STREAM_DRAW);
        api.glMultiDrawArraysIndirect(mode, 0L, this.capacity, 0);
    }
}
