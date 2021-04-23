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

package net.daporkchop.fp2.compat.vanilla.biome.layer;

import lombok.NonNull;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.gen.layer.GenLayerRiverMix;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerRiverMix
 */
public class FastLayerRiverMix extends FastLayer {
    protected static final long RIVERPARENT_OFFSET = PUnsafe.pork_getOffset(FastLayerRiverMix.class, "riverParent");

    protected final FastLayer riverParent = null;

    public FastLayerRiverMix(long seed) {
        super(seed);
    }

    @Override
    public void init(@NonNull FastLayer[] children) {
        super.init(children);
        PUnsafe.putObject(this, RIVERPARENT_OFFSET, children[1]);
    }

    @Override
    public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
        int biome = this.parent.getSingle(alloc, x, z);
        if (biome != ID_OCEAN && biome != ID_DEEP_OCEAN) {
            int river = this.riverParent.getSingle(alloc, x, z);
            if (river == ID_RIVER) {
                if (biome == ID_ICE_PLAINS) {
                    return ID_FROZEN_RIVER;
                } else if (biome != ID_MUSHROOM_ISLAND && biome != ID_MUSHROOM_ISLAND_SHORE) {
                    return biome;
                } else {
                    return ID_MUSHROOM_ISLAND_SHORE;
                }
            }
        }
        return biome;
    }
}
