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

package net.daporkchop.fp2.gl.opengl.draw.list.arrays.multidraw;

import lombok.NonNull;
import net.daporkchop.fp2.gl.draw.list.DrawCommandArrays;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.command.state.State;
import net.daporkchop.fp2.gl.opengl.command.state.StateValueProperty;
import net.daporkchop.fp2.gl.opengl.draw.binding.DrawBindingImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.AbstractDrawListImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.list.SimpleDrawListImpl;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.Collections;
import java.util.Map;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class DrawListMultiDrawArrays extends AbstractDrawListImpl<DrawCommandArrays, DrawBindingImpl> implements SimpleDrawListImpl<DrawCommandArrays> {
    protected long firstAddr;
    protected long countAddr;

    public DrawListMultiDrawArrays(@NonNull DrawListBuilderImpl builder) {
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
    public Map<StateValueProperty<?>, Object> configureStateForDraw0(@NonNull State state) {
        return Collections.emptyMap();
    }

    @Override
    public void draw0(GLAPI api, int mode) {
        api.glMultiDrawArrays(mode, this.firstAddr, this.countAddr, this.capacity);
    }
}
