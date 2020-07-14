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

package net.daporkchop.fp2.strategy.flat.cc;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.flat.FlatGenerator;
import net.daporkchop.fp2.strategy.flat.FlatPiece;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import static net.daporkchop.fp2.strategy.flat.FlatConstants.*;

/**
 * @author DaPorkchop_
 */
public class CCFlatGenerator implements FlatGenerator {
    @Override
    public void init(@NonNull WorldServer world) {
    }

    @Override
    public void generateRough(@NonNull CachedBlockAccess world, @NonNull FlatPiece piece) {
        this.generateExact(world, piece);
    }

    @Override
    public void generateExact(@NonNull CachedBlockAccess world, @NonNull FlatPiece piece) {
        int pieceX = piece.x();
        int pieceZ = piece.z();
        world.prefetch(new AxisAlignedBB(
                pieceX * FLAT_VOXELS, 0, pieceZ * FLAT_VOXELS,
                (pieceX + 1) * FLAT_VOXELS, 0, (pieceZ + 1) * FLAT_VOXELS), true);
        for (int x = 0; x < FLAT_VERTS; x++) {
            for (int z = 0; z < FLAT_VERTS; z++) {
                int height = world.getTopBlockY(pieceX * FLAT_VOXELS + x, pieceZ * FLAT_VOXELS + z);
                BlockPos pos = new BlockPos(pieceX * FLAT_VOXELS + x, height, pieceZ * FLAT_VOXELS + z);
                IBlockState state = world.getBlockState(pos);

                while (height <= 63 && state.getMaterial() == Material.WATER) {
                    pos = new BlockPos(pos.getX(), --height, pos.getZ());
                    state = world.getBlockState(pos);
                }

                Biome biome = world.getBiome(pos);
                MapColor color = state.getMapColor(world, pos);
                piece.height(x, z, height)
                        .color(x, z, color.colorIndex)
                        .biome(x, z, Biome.getIdForBiome(biome))
                        .block(x, z, Block.getStateId(state))
                        .light(x, z, world.getCombinedLight(pos.add(0, 1, 0), 0) >> 4);
            }
        }
    }
}
