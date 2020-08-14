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

package net.daporkchop.fp2.strategy.heightmap.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.gen.AbstractHeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.gen.HeightmapGenerator;
import net.daporkchop.fp2.util.threading.cachedblockaccess.CachedBlockAccess;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class CCHeightmapGenerator extends AbstractHeightmapGenerator {
    @Override
    public void generate(@NonNull CachedBlockAccess world, @NonNull HeightmapPiece piece) {
        int pieceX = piece.pos().x();
        int pieceZ = piece.pos().z();

        //prefetch columns, but not cubes because we really can't know where they are
        world.prefetch(new AxisAlignedBB(
                pieceX * T_VOXELS, 0, pieceZ * T_VOXELS,
                (pieceX + 1) * T_VOXELS - 1, 0, (pieceZ + 1) * T_VOXELS - 1), true);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < T_VOXELS; x++) {
            for (int z = 0; z < T_VOXELS; z++) {
                int height = world.getTopBlockY(pieceX * T_VOXELS + x, pieceZ * T_VOXELS + z);
                pos.setPos(pieceX * T_VOXELS + x, height, pieceZ * T_VOXELS + z);
                IBlockState state = world.getBlockState(pos);

                while (height <= 63 && state.getMaterial() == Material.WATER) {
                    pos.setY(--height);
                    state = world.getBlockState(pos);
                }

                pos.setY(height + 1);
                int light = world.getCombinedLight(pos, 0);
                Biome biome = world.getBiome(pos);

                pos.setY(this.seaLevel);
                int waterLight = world.getCombinedLight(pos, 0);
                Biome waterBiome = world.getBiome(pos);
                piece.set(x, z, height, state, packCombinedLight(light), biome, packCombinedLight(waterLight), waterBiome);
            }
        }
    }

    @Override
    public boolean supportsLowResolution() {
        return false;
    }

    @Override
    public boolean isLowResolutionInaccurate() {
        return false;
    }
}
