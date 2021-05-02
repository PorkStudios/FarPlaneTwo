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
import net.minecraft.world.gen.layer.GenLayerAddMushroomIsland;

import static net.daporkchop.fp2.compat.vanilla.biome.layer.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerAddMushroomIsland
 */
public class JavaFastLayerAddMushroomIsland extends AbstractFastLayer implements IJavaPaddedLayer {
    public JavaFastLayerAddMushroomIsland(long seed) {
        super(seed);
    }

    @Override
    public int[] offsets(int inSizeX, int inSizeZ) {
        return IJavaPaddedLayer.offsetsCorners(inSizeX, inSizeZ);
    }

    @Override
    public int eval0(int x, int z, int center, @NonNull int[] v) {
        if ((center | v[0] | v[1] | v[2] | v[3]) == 0 && nextInt(start(this.seed, x, z), 100) == 0) {
            return ID_MUSHROOM_ISLAND;
        } else {
            return center;
        }
    }
}
