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

package net.daporkchop.fp2.compat.vanilla.biome.layer.java;

import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.layer.ITranslationLayer;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * Extension of {@link ITranslationLayer} for Java implementations.
 *
 * @author DaPorkchop_
 */
public interface IJavaTranslationLayer extends ITranslationLayer {
    int translate0(int x, int z, int value);

    @Override
    default int getSingle(@NonNull ArrayAllocator<int[]> alloc, int x, int z) {
        return this.translate0(x, z, this.child().getSingle(alloc, x, z));
    }

    @Override
    default void getGrid0(int x, int z, int sizeX, int sizeZ, @NonNull int[] inout) {
        for (int i = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++, i++) {
                inout[i] = this.translate0(x + dx, z + dz, inout[i]);
            }
        }
    }

    @Override
    default void multiGetGrids0(int x, int z, int size, int dist, int depth, int count, @NonNull int[] inout) {
        for (int i = 0, gridX = 0; gridX < count; gridX++) {
            for (int gridZ = 0; gridZ < count; gridZ++) {
                for (int dx = 0; dx < size; dx++) {
                    for (int dz = 0; dz < size; dz++, i++) {
                        inout[i] = this.translate0(mulAddShift(gridX, dist, x, depth) + dx, mulAddShift(gridZ, dist, z, depth) + dz, inout[i]);
                    }
                }
            }
        }
    }
}
