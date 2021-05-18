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
import net.daporkchop.fp2.compat.vanilla.biome.layer.AbstractFastLayerWithRiverSource;
import net.daporkchop.fp2.compat.vanilla.biome.layer.IPaddedLayer;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.minecraft.world.gen.layer.GenLayerHills;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;
import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelperCached.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;

/**
 * @author DaPorkchop_
 * @see GenLayerHills
 */
public class JavaFastLayerHills extends AbstractFastLayerWithRiverSource implements IPaddedLayer {
    //this class contains a weird hybrid implementation of IJavaPaddedLayer and IJavaTranslationLayer all mixed together.
    // i decided not to bother abstracting it away since this is the only layer that merges two different source layers while being weird about it

    public JavaFastLayerHills(long seed) {
        super(seed);
    }

    protected int eval0(int x, int z, int center, @NonNull int[] v, int river) {
        int riverSubMod = (river - 2) % 29;

        if (center != 0 && river >= 2 && riverSubMod == 1 && !isMutation(center)) {
            int mutation = getMutationForBiome(center);
            return mutation < 0 ? center : mutation;
        }

        long state = start(this.seed, x, z);
        if (riverSubMod != 0 && nextInt(state, 3) != 0) { //check riverSubMod==0 first to avoid having to yet another expensive modulo
            // computation (rng is updated afterward either way)
            return center;
        }
        state = update(state, this.seed);

        int mutation = center;
        if (center == ID_DESERT) {
            mutation = ID_DESERT_HILLS;
        } else if (center == ID_FOREST) {
            mutation = ID_FOREST_HILLS;
        } else if (center == ID_BIRCH_FOREST) {
            mutation = ID_BIRCH_FOREST_HILLS;
        } else if (center == ID_ROOFED_FOREST) {
            mutation = ID_PLAINS;
        } else if (center == ID_TAIGA) {
            mutation = ID_TAIGA_HILLS;
        } else if (center == ID_REDWOOD_TAIGA) {
            mutation = ID_REDWOOD_TAIGA_HILLS;
        } else if (center == ID_COLD_TAIGA) {
            mutation = ID_COLD_TAIGA_HILLS;
        } else if (center == ID_PLAINS) {
            if (nextInt(state, 3) == 0) {
                mutation = ID_FOREST_HILLS;
            } else {
                mutation = ID_FOREST;
            }
        } else if (center == ID_ICE_PLAINS) {
            mutation = ID_ICE_MOUNTAINS;
        } else if (center == ID_JUNGLE) {
            mutation = ID_JUNGLE_HILLS;
        } else if (center == ID_OCEAN) {
            mutation = ID_DEEP_OCEAN;
        } else if (center == ID_EXTREME_HILLS) {
            mutation = ID_EXTREME_HILLS_WITH_TREES;
        } else if (center == ID_SAVANNA) {
            mutation = ID_SAVANNA_PLATEAU;
        } else if (biomesEqualOrMesaPlateau(center, ID_MESA_ROCK)) {
            mutation = ID_MESA;
        } else if (center == ID_DEEP_OCEAN && nextInt(state, 3) == 0) {
            state = update(state, this.seed);
            if (nextInt(state, 2) == 0) {
                mutation = ID_PLAINS;
            } else {
                mutation = ID_FOREST;
            }
        }

        if (!isValid(mutation)) {
            mutation = -1;
        }

        if (riverSubMod == 0 && mutation != center && (mutation = getMutationForBiome(mutation)) < 0) {
            mutation = center;
        }

        if (mutation == center) {
            return center;
        } else {
            int count = 0; //count the number of neighboring biomes which are the same
            if (biomesEqualOrMesaPlateau(v[0], center)) {
                count++;
            }
            if (biomesEqualOrMesaPlateau(v[1], center)) {
                count++;
            }
            if (biomesEqualOrMesaPlateau(v[2], center)) {
                count++;
            }
            if (biomesEqualOrMesaPlateau(v[3], center)) {
                count++;
            }
            return count >= 3 ? mutation : center;
        }
    }

    @Override
    public int getSingle(@NonNull ArrayAllocator<int[]> alloc, int x, int z) {
        final int inSizeX = 3;
        final int inSizeZ = 3;

        int[] in = alloc.atLeast(inSizeX * inSizeZ);
        int[] v = alloc.atLeast(4);
        try {
            this.child().getGrid(alloc, x - 1, z - 1, inSizeX, inSizeZ, in);

            int[] offsets = IJavaPaddedLayer.offsetsSides(inSizeX, inSizeZ);
            final int inIdx = 1 * inSizeZ + 1;
            for (int i = 0; i < 4; i++) {
                v[i] = in[offsets[i] + inIdx];
            }

            return this.eval0(x, z, in[inIdx], v, this.childRiver.getSingle(alloc, x, z));
        } finally {
            alloc.release(v);
            alloc.release(in);
        }
    }

    @Override
    public void getGrid(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out) {
        this.childRiver.getGrid(alloc, x, z, sizeX, sizeZ, out);

        IPaddedLayer.super.getGrid(alloc, x, z, sizeX, sizeZ, out);
    }

    @Override
    public void getGrid0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int sizeX, int sizeZ, @NonNull int[] out, @NonNull int[] in) {
        final int inSizeX = sizeX + 2;
        final int inSizeZ = sizeZ + 2;

        int[] offsets = IJavaPaddedLayer.offsetsSides(inSizeX, inSizeZ);
        int[] v = new int[4];

        for (int outIdx = 0, dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++, outIdx++) {
                final int inIdx = (dx + 1) * inSizeZ + (dz + 1);
                for (int i = 0; i < 4; i++) {
                    v[i] = in[offsets[i] + inIdx];
                }

                out[outIdx] = this.eval0(x + dx, z + dz, in[inIdx], v, out[outIdx]);
            }
        }
    }

    @Override
    public void multiGetGrids(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out) {
        this.childRiver.multiGetGrids(alloc, x, z, size, dist, depth, count, out);

        IPaddedLayer.super.multiGetGrids(alloc, x, z, size, dist, depth, count, out);
    }

    @Override
    public void multiGetGridsCombined0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        final int inSize = (((dist >> depth) + 1) * count) + 2;
        final int mask = (depth != 0) ? 1 : 0;

        int[] offsets = IJavaPaddedLayer.offsetsSides(inSize, inSize);
        int[] v = new int[4];

        for (int outIdx = 0, gridX = 0; gridX < count; gridX++) {
            for (int gridZ = 0; gridZ < count; gridZ++) {
                final int baseX = mulAddShift(gridX, dist, x, depth);
                final int baseZ = mulAddShift(gridZ, dist, z, depth);
                final int offsetX = mulAddShift(gridX, dist, gridX & mask, depth);
                final int offsetZ = mulAddShift(gridZ, dist, gridZ & mask, depth);

                for (int dx = 0; dx < size; dx++) {
                    for (int dz = 0; dz < size; dz++, outIdx++) {
                        final int inIdx = (offsetX + dx + 1) * inSize + (offsetZ + dz + 1);
                        for (int i = 0; i < 4; i++) {
                            v[i] = in[offsets[i] + inIdx];
                        }

                        out[outIdx] = this.eval0(baseX + dx, baseZ + dz, in[inIdx], v, out[outIdx]);
                    }
                }
            }
        }
    }

    @Override
    public void multiGetGridsIndividual0(@NonNull ArrayAllocator<int[]> alloc, int x, int z, int size, int dist, int depth, int count, @NonNull int[] out, @NonNull int[] in) {
        final int inSize = size + 2;

        int[] offsets = IJavaPaddedLayer.offsetsSides(inSize, inSize);
        int[] v = new int[4];

        for (int outIdx = 0, inBase = 0, gridX = 0; gridX < count; gridX++) {
            for (int gridZ = 0; gridZ < count; gridZ++, inBase += inSize * inSize) {
                final int baseX = mulAddShift(gridX, dist, x, depth);
                final int baseZ = mulAddShift(gridZ, dist, z, depth);

                for (int dx = 0; dx < size; dx++) {
                    for (int dz = 0; dz < size; dz++, outIdx++) {
                        final int inIdx = inBase + (dx + 1) * inSize + (dz + 1);
                        for (int i = 0; i < 4; i++) {
                            v[i] = in[offsets[i] + inIdx];
                        }

                        out[outIdx] = this.eval0(baseX + dx, baseZ + dz, in[inIdx], v, out[outIdx]);
                    }
                }
            }
        }
    }
}
