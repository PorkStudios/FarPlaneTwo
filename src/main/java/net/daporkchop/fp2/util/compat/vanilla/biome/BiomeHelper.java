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

package net.daporkchop.fp2.util.compat.vanilla.biome;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.minecraft.world.biome.BiomeProvider;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Helper class for working with biome generation layers.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class BiomeHelper {
    /**
     * Creates a fast {@link IBiomeProvider} from the given vanilla {@link BiomeProvider}.
     *
     * @param provider the {@link BiomeProvider}
     * @return the created {@link IBiomeProvider}
     */
    public IBiomeProvider from(@NonNull BiomeProvider provider) {
        if (provider.isFixedBiome()) {
            return new FixedBiomeProvider(provider.getFixedBiome());
        } else {
            //don't allow custom subclasses
            checkArg(provider.getClass() == BiomeProvider.class, "unsupported BiomeProvider implementation: %s", provider.getClass());
            return new FastThreadSafeBiomeProvider(provider);
        }
    }

    // stateless GenLayer PRNG emulation

    public static long update(long state, long seed) {
        return state * 6364136223846793005L + 1442695040888963407L + seed;
    }

    public static long start(long seed, long x, long z) {
        long state = seed;
        state = update(state, x);
        state = update(state, z);
        state = update(state, x);
        state = update(state, z);
        return state;
    }

    public static int nextInt(long state, int max) {
        int i = (int) ((state >> 24) % max);
        //equivalent to if (i < 0) { i += max; }
        i += (i >> 31) & max;
        return i;
    }
}
