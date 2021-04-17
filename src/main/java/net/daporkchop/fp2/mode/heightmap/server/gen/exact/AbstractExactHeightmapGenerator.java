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

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactHeightmapGenerator extends AbstractFarGenerator implements IFarGeneratorExact<HeightmapPos, HeightmapTile> {
    public AbstractExactHeightmapGenerator(@NonNull WorldServer world) {
        super(world);
    }

    @Override
    public void generate(@NonNull IBlockHeightAccess world, @NonNull HeightmapPos posIn, @NonNull HeightmapTile tile) {
        int tileX = posIn.x();
        int tileZ = posIn.z();

        HeightmapData data = new HeightmapData();
        data.waterHeight = this.seaLevel; //TODO: this should be calculated individually for each block
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < T_VOXELS; x++) {
            for (int z = 0; z < T_VOXELS; z++) {
                int height = world.getTopBlockY(tileX * T_VOXELS + x, tileZ * T_VOXELS + z);
                pos.setPos(tileX * T_VOXELS + x, height, tileZ * T_VOXELS + z);

                IBlockState state = world.getBlockState(pos);
                while (state.getMaterial().isLiquid()) {
                    pos.setY(--height);
                    state = world.getBlockState(pos);
                }

                pos.setY(data.height = ++height);
                data.state = Block.getStateId(state);
                data.light = packCombinedLight(world.getCombinedLight(pos, 0));
                data.biome = Biome.getIdForBiome(world.getBiome(pos));
                pos.setY(this.seaLevel + 1);
                data.waterLight = packCombinedLight(world.getCombinedLight(pos, 0));
                data.waterBiome = Biome.getIdForBiome(world.getBiome(pos));

                tile.set(x, z, data);
            }
        }
    }
}
