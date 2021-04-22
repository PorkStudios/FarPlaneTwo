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

package net.daporkchop.fp2.mode.voxel.server.gen.rough;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import lombok.NonNull;
import net.daporkchop.fp2.compat.cwg.CWGContext;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;
import net.daporkchop.fp2.mode.voxel.server.gen.AbstractVoxelGenerator;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.math.grid.Grid3d;
import net.daporkchop.lib.math.interpolation.Interpolation;
import net.daporkchop.lib.math.interpolation.LinearInterpolation;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.compat.cwg.CWGContext.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class CWGVoxelGenerator extends AbstractVoxelGenerator<CWGContext> implements IFarGeneratorRough<VoxelPos, VoxelTile> {
    public static final int LOWRES_MIN = DMAP_MIN >> GT_SHIFT;
    public static final int LOWRES_MAX = (DMAP_MAX >> GT_SHIFT) + 2;
    public static final int LOWRES_SIZE = LOWRES_MAX - LOWRES_MIN;
    public static final int LOWRES_SIZE_3 = LOWRES_SIZE * LOWRES_SIZE * LOWRES_SIZE;

    protected final Ref<CWGContext> ctx;

    public CWGVoxelGenerator(@NonNull WorldServer world) {
        super(world);

        this.ctx = ThreadRef.soft(() -> new CWGContext(world, DMAP_SIZE + 7, 2));
    }

    @Override
    public void generate(@NonNull VoxelPos pos, @NonNull VoxelTile tile) {
        int level = pos.level();
        int baseX = pos.blockX();
        int baseY = pos.blockY();
        int baseZ = pos.blockZ();

        CWGContext ctx = this.ctx.get();
        ctx.init(baseX + (DMAP_MIN >> GT_SHIFT << GT_SHIFT << level), baseZ + (DMAP_MIN >> GT_SHIFT << GT_SHIFT << level), level);
        double[][] densityMap = DMAP_CACHE.get();

        //water
        for (int x = DMAP_MIN; x < DMAP_MAX; x++) {
            for (int y = DMAP_MIN; y < DMAP_MAX; y++) {
                for (int z = DMAP_MIN; z < DMAP_MAX; z++) {
                    densityMap[0][densityIndex(x, y, z)] = baseY + (y << level) < this.seaLevel ? -1.0d : 1.0d;
                }
            }
        }

        //blocks
        Grid3d grid = Grid3d.of(new double[LOWRES_SIZE_3], LOWRES_MIN, LOWRES_MIN, LOWRES_MIN, LOWRES_SIZE, LOWRES_SIZE, LOWRES_SIZE);
        for (int x = LOWRES_MIN; x < LOWRES_MAX; x++) {
            for (int y = LOWRES_MIN; y < LOWRES_MAX; y++) {
                for (int z = LOWRES_MIN; z < LOWRES_MAX; z++) {
                    grid.setD(x, y, z, -ctx.get(baseX + (x << GT_SHIFT << level), baseY + (y << GT_SHIFT << level), baseZ + (z << GT_SHIFT << level)));
                }
            }
        }
        Interpolation interp = new LinearInterpolation();
        for (int x = DMAP_MIN; x < DMAP_MAX; x++) {
            for (int y = DMAP_MIN; y < DMAP_MAX; y++) {
                for (int z = DMAP_MIN; z < DMAP_MAX; z++) {
                    densityMap[1][densityIndex(x, y, z)] = interp.getInterpolated(x * 0.25d, y * 0.25d, z * 0.25d, grid);
                }
            }
        }

        this.buildMesh(baseX, baseY, baseZ, level, tile, densityMap, ctx);
    }

    @Override
    protected int getFaceState(int blockX, int blockY, int blockZ, int level, double nx, double ny, double nz, double density0, double density1, int edge, int layer, CWGContext ctx) {
        if (layer == 0) { //layer 0 is always water lol
            return Block.getStateId(Blocks.WATER.getDefaultState());
        }

        double density = -min(density0, density1);

        IBlockState state = Blocks.AIR.getDefaultState();
        for (IBiomeBlockReplacer replacer : ctx.replacersForBiome(ctx.getBiome(blockX, blockZ))) {
            state = replacer.getReplacedBlock(state, blockX, blockY, blockZ, nx, ny, nz, density);
        }

        return Block.getStateId(state);
    }

    @Override
    protected void populateVoxelBlockData(int blockX, int blockY, int blockZ, int level, double nx, double ny, double nz, VoxelData data, CWGContext ctx) {
        blockY++;

        int seaLevel = this.seaLevel >> level << level; //truncate lower bits in order to scale the sea level to the current zoom level
        data.light = packCombinedLight((blockY < seaLevel ? max(15 - (seaLevel - blockY) * 3, 0) : 15) << 20);
        data.biome = ctx.getBiome(blockX, blockZ);
    }

    @Override
    public boolean supportsLowResolution() {
        return true;
    }
}
