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

package net.daporkchop.fp2.gl.util.type;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * Represents the GLSL {@code vec4} type.
 *
 * @author DaPorkchop_
 */
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public final class GLIVec4 {
    private static final int _X_OFFSET = 0;
    private static final int _Y_OFFSET = _X_OFFSET + Integer.BYTES;
    private static final int _Z_OFFSET = _Y_OFFSET + Integer.BYTES;
    private static final int _W_OFFSET = _Z_OFFSET + Integer.BYTES;

    public static final int BYTES = _W_OFFSET + Integer.BYTES;

    public static GLIVec4 _get(long addr) {
        return _get(addr, new GLIVec4());
    }

    public static GLIVec4 _get(long addr, GLIVec4 vec) {
        vec.x = PUnsafe.getUnalignedInt(addr + _X_OFFSET);
        vec.y = PUnsafe.getUnalignedInt(addr + _Y_OFFSET);
        vec.z = PUnsafe.getUnalignedInt(addr + _Z_OFFSET);
        vec.w = PUnsafe.getUnalignedInt(addr + _W_OFFSET);
        return vec;
    }

    public static void _set(long addr, GLIVec4 vec) {
        PUnsafe.putUnalignedInt(addr + _X_OFFSET, vec.x);
        PUnsafe.putUnalignedInt(addr + _Y_OFFSET, vec.y);
        PUnsafe.putUnalignedInt(addr + _Z_OFFSET, vec.z);
        PUnsafe.putUnalignedInt(addr + _W_OFFSET, vec.w);
    }

    public int x;
    public int y;
    public int z;
    public int w;
}
