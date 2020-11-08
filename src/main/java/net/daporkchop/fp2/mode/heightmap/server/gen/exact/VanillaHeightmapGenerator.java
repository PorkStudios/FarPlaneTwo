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

package net.daporkchop.fp2.mode.heightmap.server.gen.exact;

import lombok.NonNull;
import net.daporkchop.fp2.mode.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.mode.common.server.gen.AbstractFarGenerator;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPiece;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapSample;
import net.daporkchop.fp2.mode.heightmap.piece.HeightmapPieceBuilder;
import net.daporkchop.fp2.util.compat.vanilla.IBlockHeightAccess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;

import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class VanillaHeightmapGenerator extends AbstractExactHeightmapGenerator {
    @Override
    public Stream<ChunkPos> neededColumns(@NonNull HeightmapPos pos) {
        return Stream.of(pos.flooredChunkPos());
    }

    @Override
    public Stream<Vec3i> neededCubes(@NonNull IBlockHeightAccess world, @NonNull HeightmapPos pos) {
        return Stream.empty(); //assume that all relevant data is loaded with the chunk
    }

    @Override
    protected void generateHeightmap(@NonNull IBlockHeightAccess world, @NonNull HeightmapPos posIn, @NonNull HeightmapPiece piece) {
        int pieceX = posIn.x();
        int pieceZ = posIn.z();

        HeightmapSample sample = new HeightmapSample();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < T_VOXELS; x++) {
            for (int z = 0; z < T_VOXELS; z++) {
                int height = world.getTopBlockY(pieceX * T_VOXELS + x, pieceZ * T_VOXELS + z);
                pos.setPos(pieceX * T_VOXELS + x, height, pieceZ * T_VOXELS + z);

                IBlockState state = world.getBlockState(pos);
                while (state.getMaterial().isLiquid()) {
                    pos.setY(--height);
                    state = world.getBlockState(pos);
                }

                pos.setY(sample.height = ++height);
                sample.state = Block.getStateId(state);
                sample.light = packCombinedLight(world.getCombinedLight(pos, 0));
                sample.biome = Biome.getIdForBiome(world.getBiome(pos));
                pos.setY(this.seaLevel + 1);
                sample.waterLight = packCombinedLight(world.getCombinedLight(pos, 0));
                sample.waterBiome = Biome.getIdForBiome(world.getBiome(pos));

                piece.set(x, z, sample);
            }
        }
    }
}
