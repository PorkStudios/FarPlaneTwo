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
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.compat.cwg.CWGContext;
import net.daporkchop.fp2.compat.cwg.CWGHelper;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.compat.cwg.CWGContext.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class CWGHeightmapGenerator extends AbstractRoughHeightmapGenerator {
    protected final Ref<CWGContext> ctx;

    public CWGHeightmapGenerator(@NonNull WorldServer world) {
        super(world);

        this.ctx = ThreadRef.soft(() -> new CWGContext(world, T_VOXELS + 1, 2));
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
        ctx.init(baseX, baseZ, level);

        int[] heights = new int[5 * 5];
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                int blockX = baseX + (x << (GT_SHIFT + level));
                int blockZ = baseZ + (z << (GT_SHIFT + level));
                heights[x * 5 + z] = CWGHelper.getHeight(ctx, blockX, blockZ);
            }
        }

        for (int tileX = 0; tileX < GT_COUNT; tileX++) {
            for (int tileZ = 0; tileZ < GT_COUNT; tileZ++) {
                int hxz = heights[tileX * 5 + tileZ];
                int hxZ = heights[tileX * 5 + (tileZ + 1)];
                int hXz = heights[(tileX + 1) * 5 + tileZ];
                int hXZ = heights[(tileX + 1) * 5 + (tileZ + 1)];

                double density = 0.5d;
                double dx = (hXz - hxz) * 0.25d;
                double dy = -1.0d;
                double dz = (hxZ - hxz) * 0.25d;

                for (int subX = 0; subX < GT_SIZE; subX++) {
                    double fx = subX * (1.0d / GT_SIZE);
                    double hz = lerp(hxz, hXz, fx);
                    double hZ = lerp(hxZ, hXZ, fx);

                    for (int subZ = 0; subZ < GT_SIZE; subZ++) {
                        double fz = subZ * (1.0d / GT_SIZE);
                        int height = floorI(lerp(hz, hZ, fz));

                        int x = (tileX << GT_SHIFT) + subX;
                        int z = (tileZ << GT_SHIFT) + subZ;
                        int blockX = baseX + (x << level);
                        int blockZ = baseZ + (z << level);

                        int biome = ctx.biomes[x * ctx.size + z];

                        IBlockState state = Blocks.AIR.getDefaultState();
                        for (IBiomeBlockReplacer replacer : ctx.replacersForBiome(biome)) {
                            state = replacer.getReplacedBlock(state, blockX, height, blockZ, dx, dy, dz, density);
                        }

                        data.height = height;
                        data.state = Block.getStateId(state);
                        data.light = (15 - clamp(this.seaLevel - height, 0, 5) * 3) << 4;
                        data.biome = biome;
                        data.waterLight = packCombinedLight(15 << 20);
                        data.waterBiome = biome;

                        tile.set(x, z, data);
                    }
                }
            }
        }
    }
}
