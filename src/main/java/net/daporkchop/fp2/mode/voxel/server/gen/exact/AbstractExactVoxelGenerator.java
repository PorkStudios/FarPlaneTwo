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

package net.daporkchop.fp2.mode.voxel.server.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.voxel.VoxelData;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPieceBuilder;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.compat.vanilla.IBlockHeightAccess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import static net.daporkchop.fp2.mode.voxel.VoxelConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactVoxelGenerator implements IFarGeneratorExact<VoxelPos, VoxelPieceBuilder> {

    @Override
    public void init(@NonNull WorldServer world) {
        //no-op
    }

    @Override
    public void generate(@NonNull IBlockHeightAccess world, @NonNull VoxelPos posIn, @NonNull VoxelPieceBuilder builder) {
        final int baseX = posIn.blockX();
        final int baseY = posIn.blockY();
        final int baseZ = posIn.blockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        VoxelData data = new VoxelData();

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    int corners = 0;
                    for (int i = 0; i < 8; i++) {
                        int x = baseX + dx + ((i >> 2) & 1);
                        int y = baseY + dy + ((i >> 1) & 1);
                        int z = baseZ + dz + (i & 1);
                        corners |= type(world.getBlockState(pos.setPos(x, y, z))) << (i << 1);
                    }

                    if (corners == 0 || corners == 0x5555 || corners == 0xAAAA) { //if all corners are the same type, this voxel can be safely skipped
                        continue;
                    }

                    int edgeMask = 0;

                    for (int i = 0; i < EDGEV_COUNT; i++) {
                        int c0 = EDGEVMAP[i << 1] << 1;
                        int c1 = EDGEVMAP[(i << 1) | 1] << 1;

                        if (((corners >> c0) & 3) == ((corners >> c1) & 3)) { //both corners along the current edge are identical, this edge can be skipped
                            continue;
                        }

                        edgeMask |= 1 << i;

                        if (((corners >> c0) & 3) < ((corners >> c1) & 3)) { //the face is facing towards negative coordinates
                            edgeMask |= 1 << EDGEV_COUNT << i;
                        }
                    }

                    data.x = data.y = data.z = .5d;

                    data.edges = edgeMask;

                    int i = EDGEVMAP[(0 << 1) | ((edgeMask >> 3) & 1)];
                    pos.setPos(baseX + dx + ((i >> 2) & 1), baseY + dy + ((i >> 1) & 1), baseZ + dz + (i & 1));
                    data.state0 = Block.getStateId(world.getBlockState(pos));
                    i = EDGEVMAP[(1 << 1) | ((edgeMask >> 4) & 1)];
                    pos.setPos(baseX + dx + ((i >> 2) & 1), baseY + dy + ((i >> 1) & 1), baseZ + dz + (i & 1));
                    data.state1 = Block.getStateId(world.getBlockState(pos));
                    i = EDGEVMAP[(2 << 1) | ((edgeMask >> 5) & 1)];
                    pos.setPos(baseX + dx + ((i >> 2) & 1), baseY + dy + ((i >> 1) & 1), baseZ + dz + (i & 1));
                    data.state2 = Block.getStateId(world.getBlockState(pos));

                    data.biome = Biome.getIdForBiome(world.getBiome(pos));

                    for (i = 0; i < 8; i++) {
                        if (((corners >> (i << 1)) & 3) != 2) {
                            pos.setPos(baseX + dx + ((i >> 2) & 1), baseY + dy + ((i >> 1) & 1), baseZ + dz + (i & 1));
                            break;
                        }
                    }
                    data.light = Constants.packCombinedLight(world.getCombinedLight(pos, 0));

                    builder.set(dx, dy, dz, data);
                }
            }
        }
    }
}
