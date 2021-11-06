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
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.command.DrawCommandArrays;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.buffer.BufferTarget;
import net.daporkchop.fp2.gl.opengl.buffer.GLBufferImpl;
import net.daporkchop.fp2.gl.opengl.command.DrawCommandBufferBuilderImpl;
import net.daporkchop.fp2.gl.opengl.command.DrawCommandBufferImpl;
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
public class DrawCommandBufferArraysImpl_MultiDrawIndirect extends DrawCommandBufferImpl<DrawCommandArrays, DrawBindingImpl> {
    public static final long _COUNT_OFFSET = 0L;
    public static final long _INSTANCECOUNT_OFFSET = _COUNT_OFFSET + INT_SIZE;
    public static final long _FIRST_OFFSET = _INSTANCECOUNT_OFFSET + INT_SIZE;
    public static final long _BASEINSTANCE_OFFSET = _FIRST_OFFSET + INT_SIZE;

    public static final long _SIZE = _BASEINSTANCE_OFFSET + INT_SIZE;

    public static int _count(long cmd) {
        return PUnsafe.getInt(cmd + _COUNT_OFFSET);
    }

    public static void _count(long cmd, int count) {
        PUnsafe.putInt(cmd + _COUNT_OFFSET, count);
    }

    public static int _instanceCount(long cmd) {
        return PUnsafe.getInt(cmd + _INSTANCECOUNT_OFFSET);
    }

    public static void _instanceCount(long cmd, int instanceCount) {
        PUnsafe.putInt(cmd + _INSTANCECOUNT_OFFSET, instanceCount);
    }

    public static int _first(long cmd) {
        return PUnsafe.getInt(cmd + _FIRST_OFFSET);
    }

    public static void _first(long cmd, int first) {
        PUnsafe.putInt(cmd + _FIRST_OFFSET, first);
    }

    public static int _baseInstance(long cmd) {
        return PUnsafe.getInt(cmd + _BASEINSTANCE_OFFSET);
    }

    public static void _baseInstance(long cmd, int baseInstance) {
        PUnsafe.putInt(cmd + _BASEINSTANCE_OFFSET, baseInstance);
    }

    protected final GLBufferImpl buffer;

    protected long commandsAddr;

    public DrawCommandBufferArraysImpl_MultiDrawIndirect(@NonNull DrawCommandBufferBuilderImpl builder) {
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
        checkIndex(this.capacity, index);

        long commandAddr = this.commandsAddr + index * _SIZE;
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
    public void execute(@NonNull DrawMode mode, @NonNull ShaderProgram shader) {
        this.buffer.upload(this.commandsAddr, this.capacity * _SIZE);

        this.buffer.bind(BufferTarget.DRAW_INDIRECT_BUFFER, target -> ((ShaderProgramImpl) shader).bind(() -> this.binding.bind(() -> {
            this.api.glMultiDrawArraysIndirect(GLEnumUtil.from(mode), 0L, this.capacity, 0);
        })));
    }

    @Override
    public void execute(@NonNull DrawMode mode, @NonNull ShaderProgram shader, @NonNull IntPredicate selector) {
        throw new UnsupportedOperationException();
    }
}
