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
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerFuzzyZoom
 */
public class JavaFastLayerFuzzyZoom extends JavaFastLayerZoom {
    public JavaFastLayerFuzzyZoom(long seed) {
        super(seed);
    }

    @Override
    protected int sampleXZLast(ArrayAllocator<int[]> alloc, int lowX, int lowZ) {
        //random
        long state = start(this.seed, lowX << 1, lowZ << 1);
        state = update(state, this.seed);
        state = update(state, this.seed);

        int r = nextInt(state, 4);
        return this.child.getSingle(alloc, lowX + (r & 1), lowZ + ((r >> 1) & 1));
    }

    @Override
    protected int sampleXZLast(long state, @NonNull int[] v) {
        return v[nextInt(state, 4)];
    }
}
