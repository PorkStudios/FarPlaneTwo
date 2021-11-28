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

package net.daporkchop.fp2.mode.heightmap.server.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.core.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.common.server.gen.AbstractFarGenerator;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.Arrays;

import static net.daporkchop.fp2.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactHeightmapGenerator extends AbstractFarGenerator implements IFarGeneratorExact<HeightmapPos, HeightmapTile> {
    public AbstractExactHeightmapGenerator(@NonNull WorldServer world) {
        super(world);
    }

    protected abstract void computeElevations(@NonNull IBlockHeightAccess world, @NonNull int[] elevations, @NonNull BlockPos.MutableBlockPos pos, int blockX, int blockZ);

    @Override
    public void generate(@NonNull FBlockWorld world, @NonNull HeightmapPos posIn, @NonNull HeightmapTile tile) {
        int tileX = posIn.x();
        int tileZ = posIn.z();

        HeightmapData data = new HeightmapData();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[] elevations = new int[MAX_LAYERS];

        for (int x = 0; x < T_VOXELS; x++) {
            for (int z = 0; z < T_VOXELS; z++) {
                Arrays.fill(elevations, Integer.MIN_VALUE);

                int blockX = tileX * T_VOXELS + x;
                int blockZ = tileZ * T_VOXELS + z;
                this.computeElevations(world, elevations, pos.setPos(blockX, 0, blockZ), blockX, blockZ);

                for (int layer = 0; layer < MAX_LAYERS; layer++) {
                    int elevation = elevations[layer];
                    if (elevation != Integer.MIN_VALUE) {
                        data.state = world.getBlockState(pos.setPos(blockX, elevation, blockZ));
                        data.biome = world.getBiome(pos);
                        pos.setY(data.height_int = pos.getY() + 1);
                        data.light = packCombinedLight(world.getCombinedLight(pos, 0));

                        if (layer == WATER_LAYER) {
                            data.height_frac = HEIGHT_FRAC_LIQUID;
                            data.secondaryConnection = WATER_LAYER;
                        } else {
                            data.height_frac = 0;
                            data.secondaryConnection = DEFAULT_LAYER;
                        }

                        tile.setLayer(x, z, layer, data);
                    }
                }
            }
        }
    }
}
