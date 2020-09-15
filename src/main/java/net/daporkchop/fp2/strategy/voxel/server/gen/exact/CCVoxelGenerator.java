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

package net.daporkchop.fp2.strategy.voxel.server.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.server.AbstractFarGenerator;
import net.daporkchop.fp2.strategy.common.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.strategy.voxel.VoxelData;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.strategy.voxel.VoxelPos;
import net.daporkchop.fp2.strategy.voxel.server.gen.AbstractVoxelGenerator;
import net.daporkchop.fp2.util.compat.vanilla.IBlockHeightAccess;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;

import java.util.List;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class CCVoxelGenerator extends AbstractVoxelGenerator<IBlockHeightAccess> implements IFarGeneratorExact<VoxelPos, VoxelPiece> {
    @Override
    public Stream<ChunkPos> neededColumns(@NonNull VoxelPos pos) {
        return Stream.empty();
    }

    @Override
    public Stream<Vec3i> neededCubes(@NonNull IBlockHeightAccess world, @NonNull VoxelPos pos) {
        Vec3i[] arr = new Vec3i[27];
        for (int i = 0, dx = -1; dx <= 1; dx++)    {
            for (int dy = -1; dy <= 1; dy++)    {
                for (int dz = -1; dz <= 1; dz++)    {
                    arr[i++] = new Vec3i(pos.x() + dx, pos.y() + dy, pos.z() + dz);
                }
            }
        }
        return Stream.of(arr);
    }

    @Override
    public void generate(@NonNull IBlockHeightAccess world, @NonNull VoxelPiece piece) {
        final int baseX = piece.pos().blockX();
        final int baseY = piece.pos().blockY();
        final int baseZ = piece.pos().blockZ();

        double[] densityMap = DMAP_CACHE.get();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = DMAP_MIN; dx < DMAP_SIZE - 1; dx++)  {
            for (int dy = DMAP_MIN; dy < DMAP_SIZE - 1; dy++)  {
                for (int dz = DMAP_MIN; dz < DMAP_SIZE - 1; dz++)  {
                    double density = 0.0d;

                    /*for (int i = 0; i < 8; i++) {
                       pos.setPos(baseX + dx + ((i >> 2) & 1), baseY + dy + ((i >> 1) & 1), baseZ + dz + (i & 1));
                       density += world.getBlockState(pos).isOpaqueCube() ? -1.0d : 1.0d;
                    }*/

                    density += world.getBlockState(pos.setPos(baseX + dx, baseY + dy, baseZ + dz)).isOpaqueCube() ? -1d : 0.0d;

                    /*density += (world.getBlockState(pos.setPos(baseX + dx, baseY + dy, baseZ + dz + 1)).isOpaqueCube() ? -1.0d : 1.0d) * 0.05d;
                    density += (world.getBlockState(pos.setPos(baseX + dx, baseY + dy, baseZ + dz - 1)).isOpaqueCube() ? -1.0d : 1.0d) * 0.05d;
                    density += (world.getBlockState(pos.setPos(baseX + dx, baseY + dy + 1, baseZ + dz)).isOpaqueCube() ? -1.0d : 1.0d) * 0.05d;
                    density += (world.getBlockState(pos.setPos(baseX + dx, baseY + dy - 1, baseZ + dz)).isOpaqueCube() ? -1.0d : 1.0d) * 0.05d;
                    density += (world.getBlockState(pos.setPos(baseX + dx + 1, baseY + dy, baseZ + dz)).isOpaqueCube() ? -1.0d : 1.0d) * 0.05d;
                    density += (world.getBlockState(pos.setPos(baseX + dx - 1, baseY + dy, baseZ + dz)).isOpaqueCube() ? -1.0d : 1.0d) * 0.05d;*/

                    densityMap[((dx - DMAP_MIN) * DMAP_SIZE + dy - DMAP_MIN) * DMAP_SIZE + dz - DMAP_MIN] = clamp(density, -1.0d, 1.0d);
                }
            }
        }

        this.buildMesh(baseX, baseY, baseZ, 0, piece, densityMap, world);
    }

    @Override
    protected void populateVoxelBlockData(int blockX, int blockY, int blockZ, VoxelData data, IBlockHeightAccess world) {
        data.light = 0xFF;
        data.state = Block.getStateId(world.getBlockState(new BlockPos(blockX, blockY, blockZ)));
    }
}
