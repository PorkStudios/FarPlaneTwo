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
import net.daporkchop.fp2.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import static net.daporkchop.fp2.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class CWGHeightmapGenerator extends AbstractRoughHeightmapGenerator {
    public static final int HMAP_MIN = -1;
    public static final int HMAP_MAX = T_VOXELS + 1;
    public static final int HMAP_SIZE = HMAP_MAX - HMAP_MIN;

    protected static final int[] SEARCH_AROUND_WATER_OFFSETS = {
            heightsIndex(HMAP_MIN - 1, HMAP_MIN - 1),
            heightsIndex(HMAP_MIN - 1, HMAP_MIN + 0),
            heightsIndex(HMAP_MIN - 1, HMAP_MIN + 1),
            heightsIndex(HMAP_MIN + 0, HMAP_MIN - 1),
            heightsIndex(HMAP_MIN + 0, HMAP_MIN + 1),
            heightsIndex(HMAP_MIN + 1, HMAP_MIN - 1),
            heightsIndex(HMAP_MIN + 1, HMAP_MIN + 0),
            heightsIndex(HMAP_MIN + 1, HMAP_MIN + 1)
    };

    protected static int heightsIndex(int x, int z) {
        return (x - HMAP_MIN) * HMAP_SIZE + z - HMAP_MIN;
    }

    protected final Cached<CWGContext> ctx;
    protected final Cached<double[]> hmapCache = Cached.threadLocal(() -> new double[sq(HMAP_SIZE)], ReferenceStrength.WEAK);

    public CWGHeightmapGenerator(@NonNull WorldServer world) {
        super(world);

        this.ctx = Cached.threadLocal(() -> new CWGContext(world, HMAP_SIZE, 2), ReferenceStrength.WEAK);
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
        ctx.init(baseX + (HMAP_MIN << level), baseZ + (HMAP_MIN << level), level);

        double[] hmap = this.hmapCache.get();
        ctx.getHeights(hmap);

        for (int x = 0; x < T_VOXELS; x++) {
            for (int z = 0, inIdx = heightsIndex(x, z); z < T_VOXELS; z++, inIdx++) {
                double height = hmap[inIdx];

                boolean addWater = height < this.seaLevel;
                if (!addWater) { //check surrounding points to see if they're below sea level
                    for (int i = 0, lim = SEARCH_AROUND_WATER_OFFSETS.length; i < lim; i++) {
                        if (hmap[inIdx + SEARCH_AROUND_WATER_OFFSETS[i]] < this.seaLevel) {
                            addWater = true;
                            break;
                        }
                    }
                }

                double density = 0.5d; //TODO: these gradients aren't being computed properly
                double dx = 0.0d;
                double dy = -1.0d;
                double dz = 0.0d;

                this.processSample(ctx, data, tile, baseX + (x << level), baseZ + (z << level), x, z, height + 1.0d, dx, dy, dz, density, addWater);
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
            data.height_frac = HEIGHT_FRAC_LIQUID;
            data.state = Blocks.WATER.getDefaultState();
            data.light = packCombinedLight(15 << 20);
            data.secondaryConnection = WATER_LAYER;
            tile.setLayer(x, z, WATER_LAYER, data);
            data.secondaryConnection = DEFAULT_LAYER;
        }
    }
}
