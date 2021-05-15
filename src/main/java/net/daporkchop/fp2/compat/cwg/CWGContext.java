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

package net.daporkchop.fp2.compat.cwg;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import lombok.NonNull;
import net.daporkchop.fp2.compat.cwg.noise.CWGNoiseProvider;
import net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper;
import net.daporkchop.fp2.compat.vanilla.biome.IBiomeProvider;
import net.daporkchop.fp2.compat.vanilla.biome.weight.BiomeWeightHelper;
import net.daporkchop.fp2.compat.vanilla.biome.weight.VanillaBiomeWeightHelper;
import net.daporkchop.fp2.util.alloc.DoubleArrayAllocator;
import net.daporkchop.lib.math.grid.Grid3d;
import net.daporkchop.lib.math.interpolation.Interpolation;
import net.daporkchop.lib.math.interpolation.LinearInterpolation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Holds the initialized state for a CubicWorldGen emulation context.
 * <p>
 * Not thread-safe.
 *
 * @author DaPorkchop_
 */
public class CWGContext {
    public final int size;
    public final int[] biomes;

    protected final IBiomeProvider biomeProvider;
    protected final BiomeWeightHelper weightHelper;
    protected final IBiomeBlockReplacer[][] biomeBlockReplacers;

    protected final CWGNoiseProvider.Configured configuredNoiseGen;

    protected final double[] heights;
    protected final double[] variations;
    protected final double[] depth;

    protected final int expectedBaseHeight;

    //current initialization position
    protected int baseX;
    protected int baseZ;
    protected int level = -1;

    protected int cacheLevel;
    protected int cacheSize;
    protected int cacheMask;
    protected int cacheBaseX;
    protected int cacheBaseZ;

    public CWGContext(@NonNull World world, int size, int smoothRadius) {
        this.size = notNegative(size, "size");
        this.biomes = new int[this.size * this.size];

        CustomGeneratorSettings conf = CustomGeneratorSettings.getFromWorld(world);
        BiomeSource biomeSource = new BiomeSource(world, conf.createBiomeBlockReplacerConfig(), new BiomeProvider(world.getWorldInfo()), smoothRadius);

        this.biomeProvider = BiomeHelper.from(CWGHelper.getBiomeGen(biomeSource));
        this.weightHelper = new VanillaBiomeWeightHelper(0.0d, 1.0d, 0.0d, 1.0d, smoothRadius);
        this.biomeBlockReplacers = CWGHelper.blockReplacerMapToArray(CWGHelper.getReplacerMap(biomeSource));

        this.configuredNoiseGen = CWGNoiseProvider.INSTANCE.forSettings(conf, world.getSeed());

        this.heights = new double[this.size * this.size];
        this.variations = new double[this.size * this.size];
        this.depth = new double[this.size * this.size];

        this.expectedBaseHeight = (int) conf.expectedBaseHeight;
    }

    /**
     * Gets the block replacers for the given biome.
     *
     * @param biomeId the {@link Biome}
     * @return an array of {@link IBiomeBlockReplacer}s used by the biome
     */
    public IBiomeBlockReplacer[] replacersForBiome(int biomeId) {
        return this.biomeBlockReplacers[biomeId];
    }

    /**
     * Initializes this context at the given position.
     *
     * @param baseX the base X coordinate (in blocks)
     * @param baseZ the base Z coordinate (in blocks)
     * @param level the detail level
     */
    public void init(int baseX, int baseZ, int level) {
        if (this.baseX == baseX && this.baseZ == baseZ && this.level == level) {
            return; //already initialized to the given position, we don't need to do anything
        }

        this.baseX = baseX;
        this.baseZ = baseZ;
        this.level = level;

        if (level < GT_SHIFT) {
            this.cacheLevel = GT_SHIFT;
            this.cacheSize = (this.size >> (GT_SHIFT - level)) + 3; //+3 is for padding space used for interpolation
            this.cacheMask = ~(GT_MASK >> level);
        } else {
            this.cacheLevel = level;
            this.cacheSize = this.size;
            this.cacheMask = -1;
        }
        this.cacheBaseX = baseX & this.cacheMask;
        this.cacheBaseZ = baseZ & this.cacheMask;

        this.biomeProvider.generateBiomesAndWeightedHeightsVariations(this.cacheBaseX, this.cacheBaseZ, this.cacheLevel, this.cacheSize, this.biomes, this.heights, this.variations, this.weightHelper);

        //convert biome heights/variations to CWG forms
        for (int i = 0; i < sq(this.cacheSize); i++) {
            this.heights[i] = BiomeHelper.biomeHeightVanilla(this.heights[i]);
            this.variations[i] = BiomeHelper.biomeHeightVariationVanilla(this.variations[i]);
        }

        //precompute depth noise
        this.configuredNoiseGen.generateDepth2d(this.depth, this.cacheBaseX, this.cacheBaseZ, 1 << this.cacheLevel, 1 << this.cacheLevel, this.cacheSize, this.cacheSize);
    }

    /**
     * Estimates the terrain height at the given X and Z coordinates.
     *
     * @param x the X coordinate (in blocks)
     * @param z the Z coordinate (in blocks)
     * @return the estimated terrain height value
     */
    public int getHeight(int x, int z) {
        int initialY = this.expectedBaseHeight;

        //find minimum and maximum bounds
        int minY = Integer.MIN_VALUE, maxY = Integer.MAX_VALUE;
        if (this.get(x, initialY, z) > 0.0d) { //initial point is solid
            minY = initialY;
            for (int shift = T_SHIFT; shift < Integer.SIZE; shift++) {
                if (this.get(x, initialY + (1 << shift), z) <= 0.0d) {
                    maxY = initialY + (1 << shift);
                    break;
                } else {
                    minY = initialY + (1 << shift);
                }
            }
        } else {
            maxY = initialY;
            for (int shift = T_SHIFT; shift < Integer.SIZE; shift++) {
                if (this.get(x, initialY - (1 << shift), z) > 0.0d) {
                    minY = initialY - (1 << shift);
                    break;
                } else {
                    maxY = initialY - (1 << shift);
                }
            }
        }

        //binary search
        for (int error = 8 << this.level; maxY - minY > error; ) {
            int middle = (minY + maxY) >>> 1;
            if (this.get(x, middle, z) > 0.0d) { //middle point is solid, move search up
                minY = middle;
            } else {
                maxY = middle;
            }
        }

        double d0 = this.get(x, minY, z);
        double d1 = this.get(x, maxY, z);
        return lerpI(minY, maxY, minimize(d0, d1));
    }

    protected int cacheIndex(int x, int z) {
        return ((x - this.cacheBaseX) >> this.cacheLevel) * this.cacheSize + ((z - this.cacheBaseZ) >> this.cacheLevel);
    }

    public int getBiome(int x, int z) {
        return this.biomes[this.cacheIndex(x, z)];
    }

    public double get(int x, int y, int z) {
        int i = this.cacheIndex(x, z);
        return this.configuredNoiseGen.generateSingle(this.heights[i], this.variations[i], this.depth[i], x, y, z);
    }

    //
    // 3D noisegen
    //

    public void get3d(@NonNull double[] out, int baseY) {
        //CWG generates noise on a 4x8x4 grid and then resamples it with linear interpolation, so we need to do special handling for the cases where the zoom level doesn't contain all samples

        if (this.level < GT_SHIFT) { //noise needs to be generated at low-res and scaled on all three axes
            this.get3d_resampleXYZ(out, baseY);
        } else if (this.level == GT_SHIFT) { //noise can be generated at full resolution, resampling only needs to be done along the Y axis
            this.get3d_resampleY(out, baseY);
        } else { //noise can be generated directly at full resolution without resampling
            this.get3d_noResampling(out, baseY);
        }
    }

    protected void get3d_resampleXYZ(@NonNull double[] out, int baseY) {
        DoubleArrayAllocator alloc = DoubleArrayAllocator.DEFAULT.get();

        int cacheBaseY = baseY & (this.cacheMask << 1);
        int cacheHeight = asrCeil(this.cacheSize, 1) + 2;

        double[] tmp = alloc.get(sq(this.cacheSize) * cacheHeight);
        try {
            //generate 3d noise at low resolution
            this.configuredNoiseGen.generate3d(this.heights, this.variations, this.depth, tmp, this.cacheBaseX, cacheBaseY, this.cacheBaseZ, 1 << GT_SHIFT, 2 << GT_SHIFT, 1 << GT_SHIFT, this.cacheSize, cacheHeight, this.cacheSize);

            //resample noise values
            double scaleXZ = 1.0d / (1 << (GT_SHIFT - this.level));
            double scaleY = scaleXZ * 0.5d;

            Grid3d grid = Grid3d.of(tmp, this.cacheBaseX >> (GT_SHIFT - this.level), cacheBaseY >> ((GT_SHIFT - this.level) + 1), this.cacheBaseZ >> (GT_SHIFT - this.level), this.cacheSize, cacheHeight, this.cacheSize);
            Interpolation interp = new LinearInterpolation();
            for (int i = 0, dx = 0; dx < this.size; dx++) {
                for (int dy = 0; dy < this.size; dy++) {
                    for (int dz = 0; dz < this.size; dz++, i++) {
                        out[i] = interp.getInterpolated((this.baseX + dx) * scaleXZ, (baseY + dy) * scaleY, (this.baseZ + dz) * scaleXZ, grid);
                    }
                }
            }
        } finally {
            alloc.release(tmp);
        }
    }

    protected void get3d_resampleY(@NonNull double[] out, int baseY) {
        DoubleArrayAllocator alloc = DoubleArrayAllocator.DEFAULT.get();

        int cacheBaseY = baseY & (~GT_MASK << 1);
        int cacheHeight = asrCeil(this.cacheSize, 1);

        double[] tmp = alloc.get(sq(this.cacheSize) * cacheHeight);
        try {
            //generate 3d noise at full resolution
            this.configuredNoiseGen.generate3d(this.heights, this.variations, this.depth, tmp, this.cacheBaseX, cacheBaseY, this.cacheBaseZ, 1 << GT_SHIFT, 2 << GT_SHIFT, 1 << GT_SHIFT, this.cacheSize, cacheHeight, this.cacheSize);

            //resample noise values
            for (int i = 0, dx = 0; dx < this.size; dx++) {
                for (int dy = 0; dy < this.size; dy++) {
                    if (((dy + (baseY >> GT_SHIFT)) & 1) == 0) {
                        for (int dz = 0, lowIdx = (dx * cacheHeight + (dy >> 1)) * this.size, highIdx = (dx * cacheHeight + (dy >> 1) + 1) * this.size; dz < this.size; dz++, lowIdx++, highIdx++, i++) {
                            out[i] = (tmp[lowIdx] + tmp[highIdx]) * 0.5d;
                        }
                    } else {
                        System.arraycopy(tmp, (dx * cacheHeight + (dy >> 1)) * this.size, out, i, this.size);
                        i += this.size;
                    }
                }
            }
        } finally {
            alloc.release(tmp);
        }
    }

    protected void get3d_noResampling(@NonNull double[] out, int baseY) {
        checkState(this.cacheSize == this.size, "cacheSize (%d) != size (%d)", this.cacheSize, this.size);

        //generate 3d noise directly at full resolution, no resampling required
        this.configuredNoiseGen.generate3d(this.heights, this.variations, out, this.baseX, baseY, this.baseZ, 1 << this.cacheLevel, 1 << this.cacheLevel, 1 << this.cacheLevel, this.size, this.size, this.size);
    }
}
