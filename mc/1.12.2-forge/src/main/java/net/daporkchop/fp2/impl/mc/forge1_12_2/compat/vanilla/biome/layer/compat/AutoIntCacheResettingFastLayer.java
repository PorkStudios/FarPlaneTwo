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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.compat;

import lombok.NonNull;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.AbstractForwardingFastLayer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.layer.IFastLayer;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.world.gen.layer.IntCache;

/**
 * Implementation of {@link IFastLayer} which automatically resets the {@link IntCache} after every access.
 * <p>
 * This must only be used at the very top of the layer tree, using this as the child of another layer could yield unexpected or incorrect results.
 *
 * @author DaPorkchop_
 */
public class AutoIntCacheResettingFastLayer extends AbstractForwardingFastLayer {
    public AutoIntCacheResettingFastLayer(IFastLayer delegate) {
        super(delegate);
    }

    @Override
    public boolean shouldResetIntCacheAfterGet() {
        return false;
    }

    @Override
    public int getSingle(@NonNull ArrayAllocator<int[]> alloc, int x, int z) {
        int result = super.getSingle(alloc, x, z);
        IntCache.resetIntCache();
        return result;
    }

    @Override
    public void getGrid(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        super.getGrid(alloc, x, z, sizeX, sizeZ, out);
        IntCache.resetIntCache();
    }

    @Override
    public void multiGetGrids(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        super.multiGetGrids(alloc, x, z, size, dist, depth, count, out);
        IntCache.resetIntCache();
    }
}