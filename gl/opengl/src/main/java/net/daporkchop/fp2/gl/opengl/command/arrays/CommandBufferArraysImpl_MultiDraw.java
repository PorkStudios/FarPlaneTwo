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

package net.daporkchop.fp2.gl.opengl.command.arrays;

import lombok.NonNull;
import net.daporkchop.fp2.gl.command.CommandBufferArrays;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.command.BaseCommandBufferImpl;
import net.daporkchop.fp2.gl.opengl.command.CommandBufferBuilderImpl;
import net.daporkchop.fp2.gl.opengl.draw.DrawBindingImpl;
import net.daporkchop.fp2.gl.opengl.shader.ShaderProgramImpl;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.function.IntPredicate;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class CommandBufferArraysImpl_MultiDraw extends BaseCommandBufferImpl<DrawBindingImpl> implements CommandBufferArrays {
    protected long firstAddr;
    protected long countAddr;

    public CommandBufferArraysImpl_MultiDraw(@NonNull CommandBufferBuilderImpl builder) {
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
    public void clear(int index) {
        PUnsafe.putInt(this.countAddr + checkIndex(this.capacity, index) * (long) INT_SIZE, 0);
    }

    @Override
    public void set(int index, int first, int count) {
        checkIndex(this.capacity, index);

        PUnsafe.putInt(this.firstAddr + index * (long) INT_SIZE, first);
        PUnsafe.putInt(this.countAddr + index * (long) INT_SIZE, count);
    }

    @Override
    public void execute(@NonNull DrawMode mode, @NonNull ShaderProgram shader) {
        ((ShaderProgramImpl) shader).bind(() -> this.binding.bind(() -> {
            this.api.glMultiDrawArrays(GLEnumUtil.from(mode), this.firstAddr, this.countAddr, this.capacity);
        }));
    }

    @Override
    public void execute(@NonNull DrawMode mode, @NonNull ShaderProgram shader, @NonNull IntPredicate selector) {
        int capacity = this.capacity;

        //allocate temporary count buffer
        long countAddr = this.alloc.alloc(capacity * (long) INT_SIZE);
        try {
            //set count to 0 if selector doesn't match
            int idx = 0;
            for (long off = 0L, endOff = capacity * (long) INT_SIZE; off != endOff; off += INT_SIZE, idx++) {
                int count = PUnsafe.getInt(this.countAddr + off);
                PUnsafe.putInt(countAddr + off, count != 0 && selector.test(idx) ? count : 0);
            }

            //actually draw something
            ((ShaderProgramImpl) shader).bind(() -> this.binding.bind(() -> {
                this.api.glMultiDrawArrays(GLEnumUtil.from(mode), this.firstAddr, this.countAddr, this.capacity);
            }));
        } finally {
            this.alloc.free(countAddr);
        }
    }
}
