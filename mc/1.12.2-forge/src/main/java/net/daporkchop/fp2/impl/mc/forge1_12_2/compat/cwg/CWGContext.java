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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg;

import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomCubicWorldType;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomGeneratorSettings;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.builder.BiomeSource;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.replacer.IBiomeBlockReplacer;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cwg.noise.CWGNoiseProvider;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.FastRegistry;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.BiomeHelper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.IBiomeProvider;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.weight.BiomeWeightHelper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.biome.weight.VanillaBiomeWeightHelper;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.math.grid.Grid3d;
import net.daporkchop.lib.math.interpolation.Interpolation;
import net.daporkchop.lib.math.interpolation.LinearInterpolation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.VanillaTerrainGenConstants.*;
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
    private static final MethodHandle BIOMESOURCE_CTOR;
    private static final MethodHandle V6_REPLACER_GETREPLACEDBLOCK_IMPL;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            if (CWGHelper.CWG_V6) {
                Class<?> iBiomeBlockReplacerClass = Class.forName("io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer");
                Class<?> biomeBlockReplacerConfigClass = Class.forName("io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.BiomeBlockReplacerConfig");

                BIOMESOURCE_CTOR = MethodHandles.filterArguments(
                        lookup.findConstructor(BiomeSource.class,
                                MethodType.methodType(void.class, World.class, biomeBlockReplacerConfigClass, BiomeProvider.class, int.class)),
                        1,
                        lookup.findVirtual(CustomGeneratorSettings.class, "createBiomeBlockReplacerConfig",
                                MethodType.methodType(biomeBlockReplacerConfigClass)));

                V6_REPLACER_GETREPLACEDBLOCK_IMPL = MethodHandles.explicitCastArguments(
                        lookup.findVirtual(iBiomeBlockReplacerClass, "getReplacedBlock",
                                MethodType.methodType(IBlockState.class, IBlockState.class, int.class, int.class, int.class, double.class, double.class, double.class, double.class)),
                        MethodType.methodType(IBlockState.class, Object.class, IBlockState.class, int.class, int.class, int.class, double.class, double.class, double.class, double.class));
            } else {
                BIOMESOURCE_CTOR = MethodHandles.filterArguments(
                        lookup.findConstructor(BiomeSource.class,
                                MethodType.methodType(void.class, World.class, List.class, BiomeProvider.class, int.class)),
                        1,
                        lookup.findGetter(CustomGeneratorSettings.class, "replacers", List.class));

                V6_REPLACER_GETREPLACEDBLOCK_IMPL = null;
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public final int size;
    public final int[] biomes;

    protected final FGameRegistry registry;

    protected final IBiomeProvider biomeProvider;
    protected final BiomeWeightHelper weightHelper;

    protected final Object[] biomeBlockReplacers;
    protected final long[][] biomeBlockReplacerFlags;

    protected final CWGNoiseProvider.Configured configuredNoiseGen;

    protected final double[] heights;
    protected final double[] variations;
    protected final double[] depth;

    protected final int expectedBaseHeight;

    protected final int tileShift;

    //current initialization position
    protected int baseX;
    protected int baseZ;
    protected int level = -1;

    protected int cacheLevel;
    protected int cacheSize;
    protected int cacheMask;
    protected int cacheBaseX;
    protected int cacheBaseZ;

    @SneakyThrows
    public CWGContext(@NonNull FGameRegistry registry, @NonNull World world, int size, int smoothRadius, int tileShift) {
        this.size = notNegative(size, "size");
        this.biomes = new int[this.size * this.size];

        this.registry = registry;
        this.tileShift = tileShift;

        CustomGeneratorSettings conf = CustomGeneratorSettings.getFromWorld(world);
        BiomeSource biomeSource = (BiomeSource) BIOMESOURCE_CTOR.invokeExact(world, conf, CustomCubicWorldType.makeBiomeProvider(world, conf), smoothRadius);

        this.biomeProvider = BiomeHelper.from(CWGHelper.getBiomeGen(biomeSource));
        this.weightHelper = new VanillaBiomeWeightHelper(0.0d, 1.0d, 0.0d, 1.0d, smoothRadius);

        if (CWGHelper.CWG_V6) {
            this.biomeBlockReplacers = CWGHelper.getReplacerMapToArray_V6(registry, biomeSource);
            this.biomeBlockReplacerFlags = null;
        } else {
            this.biomeBlockReplacers = conf.replacers.stream()
                    .map(replacerConfig -> IBiomeBlockReplacer.create(world.getSeed(), replacerConfig))
                    .toArray(IBiomeBlockReplacer[]::new);
            this.biomeBlockReplacerFlags = CWGHelper.getReplacerFlagsToArray_V7(registry, biomeSource);
        }
        this.configuredNoiseGen = CWGNoiseProvider.INSTANCE.forSettings(conf, world.getSeed());

        this.heights = new double[this.size * this.size];
        this.variations = new double[this.size * this.size];
        this.depth = new double[this.size * this.size];

        this.expectedBaseHeight = (int) conf.expectedBaseHeight;
    }

    /**
     * Gets the block at the given position based on the given biome and density values.
     *
     * @see IBiomeBlockReplacer#getReplacedBlock(IBlockState, Biome, int, int, int, double, double, double, double)
     */
    public IBlockState getReplacedBlockInBiome(int biomeId, int blockX, int blockY, int blockZ, double nx, double ny, double nz, double density) {
        return CWGHelper.CWG_V6
                ? getReplacedBlockInBiome_v6(((Object[][]) this.biomeBlockReplacers)[biomeId], Blocks.AIR.getDefaultState(), blockX, blockY, blockZ, nx, ny, nz, density)
                : getReplacedBlockInBiome_v7(this.biomeBlockReplacerFlags[biomeId], Blocks.AIR.getDefaultState(), Biome.getBiomeForId(biomeId), blockX, blockY, blockZ, nx, ny, nz, density, (IBiomeBlockReplacer[]) this.biomeBlockReplacers);
    }

    @SneakyThrows
    private static IBlockState getReplacedBlockInBiome_v6(Object[] replacers, IBlockState state, int blockX, int blockY, int blockZ, double nx, double ny, double nz, double density) {
        for (Object replacer : replacers) {
            state = (IBlockState) V6_REPLACER_GETREPLACEDBLOCK_IMPL.invokeExact(replacer, state, blockX, blockY, blockZ, nx, ny, nz, density);
        }
        return state;
    }

    private static IBlockState getReplacedBlockInBiome_v7(long[] biomeBlockReplacerFlags, IBlockState state, Biome biome, int blockX, int blockY, int blockZ, double nx, double ny, double nz, double density, IBiomeBlockReplacer[] replacers) {
        for (int wordIndex = 0; wordIndex < biomeBlockReplacerFlags.length; wordIndex++) {
            long flags = biomeBlockReplacerFlags[wordIndex];
            while (flags != 0L) {
                int bitIndex = Long.numberOfTrailingZeros(flags);
                flags &= ~(1L << bitIndex);

                state = replacers[(wordIndex << 6) | bitIndex].getReplacedBlock(state, biome, blockX, blockY, blockZ, nx, ny, nz, density);
            }
        }
        return state;
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

        if (level < GTH_SHIFT) {
            this.cacheLevel = GTH_SHIFT;
            this.cacheSize = (this.size >> (GTH_SHIFT - level)) + 3; //+3 is for padding space used for interpolation

            this.cacheBaseX = baseX & ~GTH_MASK;
            this.cacheBaseZ = baseZ & ~GTH_MASK;
        } else {
            this.cacheLevel = level;
            this.cacheSize = this.size;

            this.cacheBaseX = baseX;
            this.cacheBaseZ = baseZ;
        }

        this.biomeProvider.generateBiomesAndWeightedHeightsVariations(this.cacheBaseX, this.cacheBaseZ, this.cacheLevel, this.cacheSize, this.biomes, this.heights, this.variations, this.weightHelper);

        //convert biome IDs to fp2 IDs
        //TODO: eliminate this
        for (int i = 0; i < sq(this.cacheSize); i++) {
            this.biomes[i] = this.registry.biome2id(FastRegistry.getBiome(this.biomes[i]));
        }

        //convert biome heights/variations to CWG forms
        for (int i = 0; i < sq(this.cacheSize); i++) {
            this.heights[i] = BiomeHelper.biomeHeightVanilla(this.heights[i]);
            this.variations[i] = BiomeHelper.biomeHeightVariationVanilla(this.variations[i]);
        }

        //precompute depth noise
        this.configuredNoiseGen.generateDepth2d(this.depth, this.cacheBaseX, this.cacheBaseZ, 1 << this.cacheLevel, 1 << this.cacheLevel, this.cacheSize, this.cacheSize);
    }

    protected int cacheIndex(int x, int z) {
        return ((x - this.cacheBaseX) >> this.cacheLevel) * this.cacheSize + ((z - this.cacheBaseZ) >> this.cacheLevel);
    }

    public int getBiome(int x, int z) {
        return this.biomes[this.cacheIndex(x, z)];
    }

    //
    // 3D noisegen (sample)
    //

    public double get(int x, int y, int z) {
        //CWG generates noise on a 4x8x4 grid and then resamples it with linear interpolation, so we need to do special handling for the cases where the zoom level doesn't contain all samples

        int cacheIndex = this.cacheIndex(x, z);
        if (this.level < GTH_SHIFT) { //noise needs to be generated at low-res and scaled on all three axes
            return this.get_resampleXYZ(x, y, z, cacheIndex);
        } else if (this.level < GTV_SHIFT) { //noise can be generated at full resolution, resampling only needs to be done along the Y axis
            return this.get_resampleY(x, y, z, cacheIndex);
        } else {
            return this.get_resampleNone(x, y, z, cacheIndex);
        }
    }

    protected double get_resampleXYZ(int x, int y, int z, int cacheIndex) {
        int xFracI = x & GTH_MASK;
        int yFracI = y & GTV_MASK;
        int zFracI = z & GTH_MASK;

        if (xFracI == 0 && yFracI == 0 && zFracI == 0) { //the sample is perfectly grid-aligned, no resampling needed
            return this.get_resampleNone(x, y, z, cacheIndex);
        }

        x &= ~GTH_MASK;
        y &= ~GTV_MASK;
        z &= ~GTH_MASK;

        double xFrac = xFracI * (1.0d / GTH_SIZE);
        double yFrac = yFracI * (1.0d / GTV_SIZE);
        double zFrac = zFracI * (1.0d / GTH_SIZE);

        double v000 = this.get(x + 0 * GTH_SIZE, y + 0 * GTV_SIZE, z + 0 * GTH_SIZE);
        double v001 = this.get(x + 0 * GTH_SIZE, y + 0 * GTV_SIZE, z + 1 * GTH_SIZE);
        double v010 = this.get(x + 0 * GTH_SIZE, y + 1 * GTV_SIZE, z + 0 * GTH_SIZE);
        double v011 = this.get(x + 0 * GTH_SIZE, y + 1 * GTV_SIZE, z + 1 * GTH_SIZE);
        double v100 = this.get(x + 1 * GTH_SIZE, y + 0 * GTV_SIZE, z + 0 * GTH_SIZE);
        double v101 = this.get(x + 1 * GTH_SIZE, y + 0 * GTV_SIZE, z + 1 * GTH_SIZE);
        double v110 = this.get(x + 1 * GTH_SIZE, y + 1 * GTV_SIZE, z + 0 * GTH_SIZE);
        double v111 = this.get(x + 1 * GTH_SIZE, y + 1 * GTV_SIZE, z + 1 * GTH_SIZE);

        double v000_001 = lerp(v000, v001, zFrac);
        double v010_011 = lerp(v010, v011, zFrac);
        double v100_101 = lerp(v100, v101, zFrac);
        double v110_111 = lerp(v110, v111, zFrac);

        double v000_001__010_011 = lerp(v000_001, v010_011, yFrac);
        double v100_101__110_111 = lerp(v100_101, v110_111, yFrac);

        return lerp(v000_001__010_011, v100_101__110_111, xFrac);
    }

    protected double get_resampleY(int x, int y, int z, int cacheIndex) {
        int yFracI = y & GTV_MASK;

        if (yFracI == 0) { //the sample is perfectly grid-aligned, no resampling needed
            return this.get_resampleNone(x, y, z, cacheIndex);
        }

        y &= ~GTV_MASK;
        double yFrac = yFracI * (1.0d / GTV_SIZE);

        double v0 = this.get(x, y + 0 * GTV_SIZE, z);
        double v1 = this.get(x, y + 1 * GTV_SIZE, z);

        return lerp(v0, v1, yFrac);
    }

    protected double get_resampleNone(int x, int y, int z, int cacheIndex) {
        return this.configuredNoiseGen.generateSingle(this.heights[cacheIndex], this.variations[cacheIndex], this.depth[cacheIndex], x, y, z);
    }

    //
    // 3D noisegen (grid)
    //

    /**
     * Generates a 3d grid of noise.
     * <p>
     * The generated region will be a cube with a side length of {@link #size}, and the lowest point is at ({@link #baseX}, {@code baseY}, {@link #baseZ}).
     *
     * @param out   the array to write the generated noise values to
     * @param baseY the base Y coordinate
     */
    public void get3d(@NonNull double[] out, int baseY) {
        //CWG generates noise on a 4x8x4 grid and then resamples it with linear interpolation, so we need to do special handling for the cases where the zoom level doesn't contain all samples

        if (this.level < GTH_SHIFT) { //noise needs to be generated at low-res and scaled on all three axes
            this.get3d_resampleXYZ(out, baseY);
        } else if (this.level < GTV_SHIFT) { //noise can be generated at full resolution, resampling only needs to be done along the Y axis
            this.get3d_resampleY(out, baseY);
        } else { //noise can be generated directly at full resolution without resampling
            this.get3d_resampleNone(out, baseY);
        }
    }

    protected void get3d_resampleXYZ(@NonNull double[] out, int baseY) {
        ArrayAllocator<double[]> alloc = GlobalAllocators.ALLOC_DOUBLE.get();

        int cacheBaseY = baseY & ~GTV_MASK;
        int cacheHeight = asrCeil(this.cacheSize, 1) + 2;

        double[] tmp = alloc.atLeast(sq(this.cacheSize) * cacheHeight);
        try {
            //generate 3d noise at low resolution
            this.configuredNoiseGen.generate3d(this.heights, this.variations, this.depth, tmp, this.cacheBaseX, cacheBaseY, this.cacheBaseZ, GTH_SIZE, GTV_SIZE, GTH_SIZE, this.cacheSize, cacheHeight, this.cacheSize);

            //resample noise values
            double scaleH = 1.0d / (1 << (GTH_SHIFT - this.level));
            double scaleV = 1.0d / (1 << (GTV_SHIFT - this.level));

            Grid3d grid = Grid3d.of(tmp, this.cacheBaseX >> (GTH_SHIFT - this.level), cacheBaseY >> (GTV_SHIFT - this.level), this.cacheBaseZ >> (GTH_SHIFT - this.level), this.cacheSize, cacheHeight, this.cacheSize);
            Interpolation interp = new LinearInterpolation();
            for (int i = 0, dx = 0; dx < this.size; dx++) {
                for (int dy = 0; dy < this.size; dy++) {
                    for (int dz = 0; dz < this.size; dz++, i++) {
                        out[i] = interp.getInterpolated((this.baseX + dx) * scaleH, (baseY + dy) * scaleV, (this.baseZ + dz) * scaleH, grid);
                    }
                }
            }
        } finally {
            alloc.release(tmp);
        }
    }

    protected void get3d_resampleY(@NonNull double[] out, int baseY) {
        checkState(this.cacheSize == this.size, "cacheSize (%d) != size (%d)", this.cacheSize, this.size);

        ArrayAllocator<double[]> alloc = GlobalAllocators.ALLOC_DOUBLE.get();

        int cacheBaseY = baseY & ~GTV_MASK;
        int cacheHeight = asrCeil(this.size, 1);

        double[] tmp = alloc.atLeast(sq(this.size) * cacheHeight);
        try {
            //generate 3d noise at full resolution
            this.configuredNoiseGen.generate3d(this.heights, this.variations, this.depth, tmp, this.cacheBaseX, cacheBaseY, this.cacheBaseZ, GTH_SIZE, GTV_SIZE, GTH_SIZE, this.size, cacheHeight, this.size);

            //resample noise values
            for (int i = 0, dx = 0; dx < this.size; dx++) {
                for (int dy = 0; dy < this.size; dy++) {
                    if (((dy + (baseY >> GTH_SHIFT)) & 1) != 0) {
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

    protected void get3d_resampleNone(@NonNull double[] out, int baseY) {
        checkState(this.cacheSize == this.size, "cacheSize (%d) != size (%d)", this.cacheSize, this.size);

        //generate 3d noise directly at full resolution, no resampling required
        this.configuredNoiseGen.generate3d(this.heights, this.variations, out, this.baseX, baseY, this.baseZ, 1 << this.level, 1 << this.level, 1 << this.level, this.size, this.size, this.size);
    }

    //
    // Height generation
    //

    /**
     * Estimates the terrain height at the given X and Z coordinates.
     *
     * @param x the X coordinate (in blocks)
     * @param z the Z coordinate (in blocks)
     * @return the estimated terrain height value
     */
    public double getHeight(int x, int z) {
        int initialY = this.expectedBaseHeight;

        //find minimum and maximum bounds
        int minY = Integer.MIN_VALUE, maxY = Integer.MAX_VALUE;
        if (this.get(x, initialY, z) > 0.0d) { //initial point is solid
            minY = initialY;
            for (int shift = this.tileShift; shift < Integer.SIZE; shift++) {
                if (this.get(x, initialY + (1 << shift), z) <= 0.0d) {
                    maxY = initialY + (1 << shift);
                    break;
                } else {
                    minY = initialY + (1 << shift);
                }
            }
        } else {
            maxY = initialY;
            for (int shift = this.tileShift; shift < Integer.SIZE; shift++) {
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
            int middle = (minY + maxY) >> 1;
            if (this.get(x, middle, z) > 0.0d) { //middle point is solid, move search up
                minY = middle;
            } else {
                maxY = middle;
            }
        }

        double d0 = this.get(x, minY, z);
        double d1 = this.get(x, maxY, z);
        return lerp(minY, maxY, minimize(d0, d1));
    }

    /**
     * Generates a 2d grid of estimated terrain heights.
     * <p>
     * The generated region will be a square with a side length of {@link #size}, and the lowest point is at ({@link #baseX}, {@link #baseZ}).
     *
     * @param out the array to write the estimated height values to
     */
    public void getHeights(@NonNull double[] out) {
        for (int i = 0, dx = 0; dx < this.size; dx++) {
            for (int dz = 0; dz < this.size; dz++, i++) {
                out[i] = this.getHeight(this.baseX + (dx << this.level), this.baseZ + (dz << this.level));
            }
        }
    }
}
