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
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.common.server.gen.AbstractFarGenerator;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapData;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import static net.daporkchop.fp2.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactHeightmapGenerator extends AbstractFarGenerator implements IFarGeneratorExact<HeightmapPos, HeightmapTile> {
    protected static final int MINIMUM_CONSIDERED_Y = Integer.MIN_VALUE + Character.MAX_VALUE; //the minimum Y coordinate that will be considered for heightmap samples

    public AbstractExactHeightmapGenerator(@NonNull WorldServer world) {
        super(world);
    }

    @Override
    public void generate(@NonNull IBlockHeightAccess world, @NonNull HeightmapPos posIn, @NonNull HeightmapTile tile) {
        int tileX = posIn.x();
        int tileZ = posIn.z();

        HeightmapData data = new HeightmapData();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < T_VOXELS; x++) {
            for (int z = 0; z < T_VOXELS; z++) {
                int height = world.getTopBlockY(tileX * T_VOXELS + x, tileZ * T_VOXELS + z);
                pos.setPos(tileX * T_VOXELS + x, height, tileZ * T_VOXELS + z);

                IBlockState state = null;
                while (height > MINIMUM_CONSIDERED_Y && (state = world.getBlockState(pos)).getMaterial().isLiquid()) {
                    pos.setY(--height);
                }

                if (height <= MINIMUM_CONSIDERED_Y) { //discard points that are too low
                    continue;
                }

                pos.setY(data.height_int = ++height);
                data.state = state;
                data.light = packCombinedLight(world.getCombinedLight(pos, 0));
                data.biome = world.getBiome(pos);
                tile.setLayer(x, z, DEFAULT_LAYER, data);

                /*pos.setY(this.seaLevel + 1);
                data.waterLight = packCombinedLight(world.getCombinedLight(pos, 0));
                data.waterBiome = world.getBiome(pos);
                tile.setLayer(x, z, 1, data);*/
            }
        }
    }
}
