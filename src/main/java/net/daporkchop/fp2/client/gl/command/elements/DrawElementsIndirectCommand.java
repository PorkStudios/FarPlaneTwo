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

package net.daporkchop.fp2.client.gl.command.elements;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.daporkchop.fp2.client.gl.command.IDrawIndirectCommand;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;

import static net.daporkchop.fp2.client.gl.OpenGL.*;

/**
 * A draw command used by {@link GL40#glDrawElementsIndirect(int, int, long)} and {@link GL43#glMultiDrawElementsIndirect(int, int, long, int, int)}.
 *
 * @author DaPorkchop_
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public final class DrawElementsIndirectCommand implements IDrawIndirectCommand {
    public static final long _COUNT_OFFSET = 0L;
    public static final long _INSTANCECOUNT_OFFSET = _COUNT_OFFSET + INT_SIZE;
    public static final long _FIRSTINDEX_OFFSET = _INSTANCECOUNT_OFFSET + INT_SIZE;
    public static final long _BASEVERTEX_OFFSET = _FIRSTINDEX_OFFSET + INT_SIZE;
    public static final long _BASEINSTANCE_OFFSET = _BASEVERTEX_OFFSET + INT_SIZE;

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

    public static int _firstIndex(long cmd) {
        return PUnsafe.getInt(cmd + _FIRSTINDEX_OFFSET);
    }

    public static void _firstIndex(long cmd, int firstIndex) {
        PUnsafe.putInt(cmd + _FIRSTINDEX_OFFSET, firstIndex);
    }

    public static int _baseVertex(long cmd) {
        return PUnsafe.getInt(cmd + _BASEVERTEX_OFFSET);
    }

    public static void _baseVertex(long cmd, int baseVertex) {
        PUnsafe.putInt(cmd + _BASEVERTEX_OFFSET, baseVertex);
    }

    public static int _baseInstance(long cmd) {
        return PUnsafe.getInt(cmd + _BASEINSTANCE_OFFSET);
    }

    public static void _baseInstance(long cmd, int baseInstance) {
        PUnsafe.putInt(cmd + _BASEINSTANCE_OFFSET, baseInstance);
    }

    protected int count;
    protected int instanceCount;
    protected int firstIndex;
    protected int baseVertex;
    protected int baseInstance;

    @Override
    public long size() {
        return _SIZE;
    }

    @Override
    public void load(long addr) {
        this.count = _count(addr);
        this.instanceCount = _instanceCount(addr);
        this.firstIndex = _firstIndex(addr);
        this.baseVertex = _baseVertex(addr);
        this.baseInstance = _baseInstance(addr);
    }

    @Override
    public void store(long addr) {
        _count(addr, this.count);
        _instanceCount(addr, this.instanceCount);
        _firstIndex(addr, this.firstIndex);
        _baseVertex(addr, this.baseVertex);
        _baseInstance(addr, this.baseInstance);
    }

    @Override
    public boolean isEmpty() {
        return this.count == 0;
    }

    @Override
    public void clear() {
        this.count = 0;
        this.instanceCount = 0;
        this.firstIndex = 0;
        this.baseVertex = 0;
        this.baseInstance = 0;
    }
}
