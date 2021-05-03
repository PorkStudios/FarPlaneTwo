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
import net.daporkchop.fp2.compat.vanilla.biome.layer.AbstractFastLayer;
import net.minecraft.world.gen.layer.GenLayerRiver;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelperCached.*;

/**
 * @author DaPorkchop_
 * @see GenLayerRiver
 */
public class JavaFastLayerRiver extends AbstractFastLayer implements IJavaPaddedLayer {
    private static int riverFilter(int i) {
        return i >= 2 ? 2 + (i & 1) : i;
    }

    public JavaFastLayerRiver(long seed) {
        super(seed);
    }

    @Override
    public int[] offsets(int inSizeX, int inSizeZ) {
        return IJavaPaddedLayer.offsetsSides(inSizeX, inSizeZ);
    }

    @Override
    public int eval0(int x, int z, int center, @NonNull int[] v) {
        center = riverFilter(center);
        if (center == riverFilter(v[0]) && center == riverFilter(v[1]) && center == riverFilter(v[2]) && center == riverFilter(v[3])) {
            return -1;
        } else {
            return ID_RIVER;
        }
    }
}
