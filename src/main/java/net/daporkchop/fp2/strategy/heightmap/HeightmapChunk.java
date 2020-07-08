/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.strategy.heightmap;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.util.Constants;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.lib.common.util.PValidation.checkArg;

/**
 * A "chunk" containing the data used by the heightmap rendering strategy.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Accessors(fluent = true)
public class HeightmapChunk {
    public static void checkCoords(int x, int z)    {
        checkArg(x >= 0 && x < HEIGHT_VERTS && z >= 0 && z < HEIGHT_VERTS, "coordinates out of bounds (x=%d, z=%d)", x, z);
    }

    protected final int x;
    protected final int z;

    protected final IntBuffer data = Constants.createIntBuffer(HEIGHT_VERTS * HEIGHT_VERTS << 1); //2 ints per pixel: height followed by color

    public int height(int x, int z) {
        checkCoords(x, z);
        return this.data.get(x * HEIGHT_VERTS + z);
    }

    public int color(int x, int z) {
        checkCoords(x, z);
        return this.data.get(x * HEIGHT_VERTS + z + 1);
    }

    public HeightmapChunk height(int x, int z, int height) {
        checkCoords(x, z);
        this.data.put(x * HEIGHT_VERTS + z, height);
        return this;
    }

    public HeightmapChunk color(int x, int z, int color) {
        checkCoords(x, z);
        this.data.put(x * HEIGHT_VERTS + z + 1, color);
        return this;
    }
}
