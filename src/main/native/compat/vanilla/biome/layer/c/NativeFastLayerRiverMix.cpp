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

#include "NativeFastLayer.h"

FP2_JNI(void, NativeFastLayerRiverMix, mix0) (JNIEnv* env, jobject obj,
        jint count, jintArray _biome, jintArray _river) {
    fp2::pinned_int_array biome(env, _biome);
    fp2::pinned_int_array river(env, _river);

    int32_t i = 0;
    for (Vec4i b, r; i < count & ~3; i += 4) {
        b.load(&biome[i]);
        r.load(&river[i]);

        Vec4i riverOut = biomes.MUSHROOM_ISLAND_SHORE;
        riverOut = select((b != biomes.MUSHROOM_ISLAND) & (b != biomes.MUSHROOM_ISLAND_SHORE), r & 0xFF, riverOut);
        riverOut = select(b == biomes.ICE_PLAINS, biomes.FROZEN_RIVER, riverOut);
        
        b = select((b != biomes.OCEAN) & (b != biomes.DEEP_OCEAN) & (r == biomes.RIVER), riverOut, b);
        b.store(&biome[i]);
    }

    for (; i < count; i++) {
        int32_t b = biome[i];
        if (b != biomes.OCEAN && b != biomes.DEEP_OCEAN) {
            int32_t r = river[i];

            if (r == biomes.RIVER) {
                if (b == biomes.ICE_PLAINS) {
                    b = biomes.FROZEN_RIVER;
                } else if (b != biomes.MUSHROOM_ISLAND && b != biomes.MUSHROOM_ISLAND_SHORE) {
                    b = r & 0xFF;
                } else {
                    b = biomes.MUSHROOM_ISLAND_SHORE;
                }
                biome[i] = b;
            }
        }
    }
}
