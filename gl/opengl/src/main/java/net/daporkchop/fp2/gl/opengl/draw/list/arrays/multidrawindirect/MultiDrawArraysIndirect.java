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

import lombok.experimental.UtilityClass;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.common.util.TypeSize.*;

/**
 * Helpers for reading/writing MultiDrawArraysIndirect commands.
 *
 * @author DaPorkchop_
 */
@UtilityClass
class MultiDrawArraysIndirect {
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
}
