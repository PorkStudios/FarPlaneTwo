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

package net.daporkchop.fp2.mode.heightmap.server.gen.rough;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import lombok.NonNull;
import net.daporkchop.fp2.compat.cwg.CWGContext;
import net.daporkchop.fp2.compat.cwg.CWGHelper;
import net.daporkchop.fp2.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import static java.lang.Math.*;
import static net.daporkchop.fp2.compat.cwg.CWGContext.*;
import static net.daporkchop.fp2.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class CWGHeightmapGenerator extends AbstractRoughHeightmapGenerator {
    protected final Ref<CWGContext> ctx;

    public CWGHeightmapGenerator(@NonNull WorldServer world) {
        super(world);

        this.ctx = ThreadRef.soft(() -> new CWGContext(world, T_VOXELS + 1 + GT_SIZE, 2));
    }

    @Override
    public boolean supportsLowResolution() {
        return true;
    }

    @Override
    public void generate(@NonNull HeightmapPos posIn, @NonNull HeightmapTile tile) {
        int level = posIn.level();
        int baseX = posIn.blockX();
        int baseZ = posIn.blockZ();

        HeightmapData data = new HeightmapData();

        CWGContext ctx = this.ctx.get();
        ctx.init(baseX - GT_SIZE, baseZ - GT_SIZE, level);

        int shift = min(level, GT_SHIFT);
        int scale = shift + GT_SHIFT;

        int hMax = (1 << scale) + 1;
        int hSize = hMax + 1;
        int[] heights = new int[hSize * hSize];
        for (int x = -1; x < hMax; x++) {
            for (int z = -1; z < hMax; z++) {
                int blockX = baseX + (x << (level - shift + GT_SHIFT));
                int blockZ = baseZ + (z << (level - shift + GT_SHIFT));
                heights[(x + 1) * hSize + (z + 1)] = CWGHelper.getHeight(ctx, blockX, blockZ);
            }
        }

        int tileSize = GT_SIZE >> shift;
        double f = 1.0d / tileSize;
        for (int tileX = 0; tileX < hMax - 1; tileX++) {
            for (int tileZ = 0; tileZ < hMax - 1; tileZ++) {
                boolean addWater = false;
                CHECK_ADD_WATER:
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (heights[(tileX + 1 + dx) * hSize + (tileZ + 1 + dz)] < this.seaLevel) {
                            addWater = true;
                            break CHECK_ADD_WATER;
                        }
                    }
                }

                int hxz = heights[(tileX + 1) * hSize + (tileZ + 1)];
                int hxZ = heights[(tileX + 1) * hSize + (tileZ + 2)];
                int hXz = heights[(tileX + 2) * hSize + (tileZ + 1)];
                int hXZ = heights[(tileX + 2) * hSize + (tileZ + 2)];

                double density = 0.5d; //TODO: these gradients aren't being computed properly
                double dx = (hXz - hxz) * f;
                double dy = -1.0d;
                double dz = (hxZ - hxz) * f;

                for (int subX = 0; subX < tileSize; subX++) {
                    double fx = subX * f;
                    double hz = lerp(hxz, hXz, fx);
                    double hZ = lerp(hxZ, hXZ, fx);

                    for (int subZ = 0; subZ < tileSize; subZ++) {
                        double fz = subZ * f;
                        double height = lerp(hz, hZ, fz);

                        int x = (tileX * tileSize) + subX;
                        int z = (tileZ * tileSize) + subZ;

                        this.processSample(ctx, data, tile, baseX + (x << level), baseZ + (z << level), x, z, height, dx, dy, dz, density, addWater);
                    }
                }
            }
        }
    }

    protected void processSample(CWGContext ctx, HeightmapData data, HeightmapTile tile, int blockX, int blockZ, int x, int z, double height, double dx, double dy, double dz, double density, boolean addWater) {
        int heightI = floorI(height);
        int heightF = clamp(floorI((height - heightI) * 255.0d), 0, 255);

        int biome = ctx.getBiome(blockX, blockZ);

        IBlockState state = STATE_AIR;
        for (IBiomeBlockReplacer replacer : ctx.replacersForBiome(biome)) {
            state = replacer.getReplacedBlock(state, blockX, heightI, blockZ, dx, dy, dz, density);
        }

        data.height_int = heightI;
        data.height_frac = heightF;
        data.state = state;
        data.light = (15 - clamp(this.seaLevel - heightI, 0, 5) * 3) << 4;
        data.biome = Biome.getBiomeForId(biome);
        tile.setLayer(x, z, DEFAULT_LAYER, data);

        if (addWater) { //set water
            data.height_int = this.seaLevel - 1;
            data.height_frac = 224; //256 * 7/8
            data.state = Blocks.WATER.getDefaultState();
            data.light = packCombinedLight(15 << 20);
            data.secondaryConnection = 1;
            tile.setLayer(x, z, 1, data);
            data.secondaryConnection = DEFAULT_LAYER;
        }
    }
}
