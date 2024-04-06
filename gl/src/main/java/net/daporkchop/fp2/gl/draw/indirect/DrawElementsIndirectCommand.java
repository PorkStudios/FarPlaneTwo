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

package net.daporkchop.fp2.gl.draw.indirect;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.common.util.TypeSize.*;

/**
 * An indirect DrawElements call.
 *
 * @author DaPorkchop_
 * @see net.daporkchop.fp2.gl.OpenGL#glMultiDrawElementsIndirect
 */
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public final class DrawElementsIndirectCommand {
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

    public static DrawElementsIndirectCommand _get(long cmd) {
        return _get(cmd, new DrawElementsIndirectCommand());
    }

    public static DrawElementsIndirectCommand _get(long cmd, DrawElementsIndirectCommand command) {
        command.count = _count(cmd);
        command.instanceCount = _instanceCount(cmd);
        command.firstIndex = _firstIndex(cmd);
        command.baseVertex = _baseVertex(cmd);
        command.baseInstance = _baseInstance(cmd);
        return command;
    }

    public static void _set(long cmd, DrawElementsIndirectCommand command) {
        _count(cmd, command.count);
        _instanceCount(cmd, command.instanceCount);
        _firstIndex(cmd, command.firstIndex);
        _baseVertex(cmd, command.baseVertex);
        _baseInstance(cmd, command.baseInstance);
    }

    public int count;
    public int instanceCount;
    public int firstIndex;
    public int baseVertex;
    public int baseInstance;
}
