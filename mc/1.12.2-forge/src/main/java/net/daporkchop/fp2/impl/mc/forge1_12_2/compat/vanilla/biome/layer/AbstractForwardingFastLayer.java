/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class AbstractForwardingFastLayer implements IFastLayer {
    protected final IFastLayer delegate;

    @Override
    public void init(@NonNull IFastLayer[] children) {
        this.delegate.init(children);
    }

    @Override
    public boolean shouldResetIntCacheAfterGet() {
        return this.delegate.shouldResetIntCacheAfterGet();
    }

    @Override
    public int getSingle(@NonNull ArrayAllocator<int[]> alloc, int x, int z) {
        return this.delegate.getSingle(alloc, x, z);
    }

    @Override
    public void getGrid(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        this.delegate.getGrid(alloc, x, z, sizeX, sizeZ, out);
    }

    @Override
    public void multiGetGrids(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        this.delegate.multiGetGrids(alloc, x, z, size, dist, depth, count, out);
    }
}
