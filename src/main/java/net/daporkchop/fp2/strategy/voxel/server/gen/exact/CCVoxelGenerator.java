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
import net.daporkchop.fp2.strategy.common.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.strategy.voxel.VoxelData;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.strategy.voxel.VoxelPos;
import net.daporkchop.fp2.strategy.voxel.server.gen.AbstractVoxelGenerator;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.compat.vanilla.IBlockHeightAccess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import static java.lang.Math.*;
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
        for (int i = 0, dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
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

        for (int dx = DMAP_MIN; dx < DMAP_SIZE + DMAP_MIN; dx++) {
            for (int dy = DMAP_MIN; dy < DMAP_SIZE + DMAP_MIN; dy++) {
                for (int dz = DMAP_MIN; dz < DMAP_SIZE + DMAP_MIN; dz++) {
                    double density = world.getBlockState(pos.setPos(baseX + dx, baseY + dy, baseZ + dz)).isOpaqueCube() ? -1d : 1d;

                    densityMap[((dx - DMAP_MIN) * DMAP_SIZE + dy - DMAP_MIN) * DMAP_SIZE + dz - DMAP_MIN] = density;
                }
            }
        }

        this.buildMesh(baseX, baseY, baseZ, 0, piece, densityMap, world);
    }

    @Override
    protected void populateVoxelBlockData(int blockX, int blockY, int blockZ, VoxelData data, IBlockHeightAccess world, double nx, double ny, double nz) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(blockX, blockY, blockZ);

        Vec3i[] arr = new Vec3i[27];
        for (int i = 0, dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    arr[i++] = new Vec3i(dx, dy, dz);
                }
            }
        }
        Arrays.sort(arr, Comparator.comparingDouble(vec -> {
            double dx = vec.getX() - nx;
            double dy = vec.getY() - ny;
            double dz = vec.getZ() - nz;
            return dx * dx + dy * dy + dz * dz;
        }));

        IBlockState state = world.getBlockState(pos);
        SEARCH:
        if (!state.isOpaqueCube()) {
            for (Vec3i v : arr) {
                if ((state = world.getBlockState(pos.setPos(blockX + v.getX(), blockY + v.getY(), blockZ + v.getZ()))).isOpaqueCube()) {
                    break SEARCH;
                }
            }
        }
        data.state = Block.getStateId(state);
        data.biome = Biome.getIdForBiome(world.getBiome(pos));

        //find neighbor that isn't opaque
        blockX = pos.getX();
        blockY = pos.getY();
        blockZ = pos.getZ();

        for (Vec3i v : arr) {
            if (!world.getBlockState(pos.setPos(blockX + v.getX(), blockY + v.getY(), blockZ + v.getZ())).isOpaqueCube()) {
                data.light = Constants.packCombinedLight(world.getCombinedLight(pos, 0));
                break;
            }
        }
    }
}
