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
import net.daporkchop.fp2.strategy.heightmap.gen.HeightmapGenerator;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class VanillaHeightmapGenerator implements HeightmapGenerator {
    @Override
    public void init(@NonNull WorldServer world) {
    }

    @Override
    public void generate(@NonNull CachedBlockAccess world, @NonNull HeightmapPiece piece) {
        int pieceX = piece.x();
        int pieceZ = piece.z();

        world.prefetch(new AxisAlignedBB(
                pieceX * HEIGHTMAP_VOXELS - 1, 0, pieceZ * HEIGHTMAP_VOXELS - 1,
                (pieceX + 1) * HEIGHTMAP_VOXELS, 0, (pieceZ + 1) * HEIGHTMAP_VOXELS), true);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < HEIGHTMAP_VOXELS; x++) {
            for (int z = 0; z < HEIGHTMAP_VOXELS; z++) {
                int height = world.getTopBlockY(pieceX * HEIGHTMAP_VOXELS + x, pieceZ * HEIGHTMAP_VOXELS + z);
                pos.setPos(pieceX * HEIGHTMAP_VOXELS + x, height, pieceZ * HEIGHTMAP_VOXELS + z);

                IBlockState state = world.getBlockState(pos);

                while (state.getMaterial().isLiquid()) {
                    pos.setY(--height);
                    state = world.getBlockState(pos);
                }

                pos.setY(height + 1);
                int light = world.getCombinedLight(pos, 0);

                piece.set(x, z, height, state, packCombinedLight(light));
            }
        }

        for (int x = -1; x <= HEIGHTMAP_VOXELS; x++) {
            for (int z = -1; z <= HEIGHTMAP_VOXELS; z++) {
                pos.setPos(pieceX * HEIGHTMAP_VOXELS + x, 0, pieceZ * HEIGHTMAP_VOXELS + z);
                piece.setBiome(x, z, world.getBiome(pos));
            }
        }
    }
}
