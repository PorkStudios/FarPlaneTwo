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

package net.daporkchop.fp2.strategy.voxel.server.gen.rough;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.common.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.strategy.voxel.VoxelData;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.strategy.voxel.VoxelPos;
import net.daporkchop.fp2.strategy.voxel.server.gen.AbstractVoxelGenerator;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.PerlinNoiseEngine;
import net.daporkchop.lib.random.PRandom;
import net.daporkchop.lib.random.impl.FastPRandom;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStainedGlass;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.world.WorldServer;

import java.util.stream.Stream;

import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class PerlinNoiseVoxelGenerator extends AbstractVoxelGenerator<Void> implements IFarGeneratorRough<VoxelPos, VoxelPiece> {
    private static final int[] STATES = Stream.of(
            Blocks.STONE.getDefaultState(),
            Blocks.GRASS.getDefaultState(),
            Blocks.DIAMOND_ORE.getDefaultState(),
            Blocks.WATER.getDefaultState(),
            Blocks.LAVA.getDefaultState(),
            Blocks.GLASS.getDefaultState(),
            Blocks.STAINED_GLASS.getDefaultState(),
            Blocks.STAINED_GLASS.getDefaultState().withProperty(BlockStainedGlass.COLOR, EnumDyeColor.LIME),
            Blocks.OBSIDIAN.getDefaultState(),
            Blocks.OBSERVER.getDefaultState(),
            Blocks.LOG.getDefaultState(),
            Blocks.LOG.getDefaultState()
    ).mapToInt(Block::getStateId).toArray();

    private static int selectState(double noise)  {
        int i = floorI(noise * STATES.length);
        i ^= (i & 0x80000000) >> 31;
        return STATES[i % STATES.length];
    }

    protected NoiseSource terrainNoise;
    protected NoiseSource blockNoise;
    protected NoiseSource blockLightNoise;
    protected NoiseSource skyLightNoise;
    protected NoiseSource biomeNoise;

    @Override
    public void init(@NonNull WorldServer world) {
        super.init(world);

        PRandom random = new FastPRandom(world.getSeed());
        this.terrainNoise = new PerlinNoiseEngine(random).scaled(1d / 16d);
        this.blockNoise = new PerlinNoiseEngine(random).scaled(1d / 16d);
        this.blockLightNoise = new PerlinNoiseEngine(random).scaled(1d / 16d);
        this.skyLightNoise = new PerlinNoiseEngine(random).scaled(1d / 16d);
        this.biomeNoise = new PerlinNoiseEngine(random).scaled(1d / 16d);
    }

    @Override
    public void generate(@NonNull VoxelPiece piece) {;
        final int baseX = piece.pos().blockX();
        final int baseY = piece.pos().blockY();
        final int baseZ = piece.pos().blockZ();
        final int level = piece.level();

        //populate density map
        double[] densityMap = DMAP_CACHE.get();
        this.terrainNoise.get(densityMap, baseX, baseY, baseZ, 1 << level, 1 << level, 1 << level, DMAP_SIZE, DMAP_SIZE, DMAP_SIZE);

        this.buildMesh(baseX, baseY, baseZ, level, piece, densityMap, null);
    }

    @Override
    protected void populateVoxelBlockData(int blockX, int blockY, int blockZ, VoxelData data, Void param) {
        double x = blockX;
        double y = blockY;
        double z = blockZ;

        data.state = selectState(this.blockNoise.get(x, y, z));
        data.biome = floorI(this.biomeNoise.get(x, y, z) * 8.0d + 8.0d);
        data.light = (floorI(this.skyLightNoise.get(x, y, z) * 7.5d + 7.5d) << 4) | floorI(this.blockLightNoise.get(x, y, z) * 7.5d + 7.5d);
    }

    @Override
    public boolean supportsLowResolution() {
        return true;
    }

    @Override
    public boolean isLowResolutionInaccurate() {
        return true;
    }
}
