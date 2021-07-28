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

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.mode.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class CCHeightmapGenerator extends AbstractExactHeightmapGenerator {
    protected static final int Y_LIMIT = Integer.MIN_VALUE + Character.MAX_VALUE; //the minimum Y coordinate that will be considered for heightmap samples

    public CCHeightmapGenerator(@NonNull WorldServer world) {
        super(world);
    }

    @Override
    public Stream<ChunkPos> neededColumns(@NonNull HeightmapPos pos) {
        return Stream.of(pos.flooredChunkPos());
    }

    @Override
    public Stream<Vec3i> neededCubes(@NonNull IBlockHeightAccess world, @NonNull HeightmapPos pos) {
        return IntStream.range(0, T_VOXELS * T_VOXELS)
                .map(packed -> {
                    int blockX = pos.blockX() + (packed & T_MASK);
                    int blockZ = pos.blockZ() + ((packed >> T_SHIFT) & T_MASK);
                    return Coords.blockToCube(world.getTopBlockY(blockX, blockZ));
                })
                .distinct() //we don't want a bunch of identical cube Y coordinates
                .filter(cubeY -> cubeY > Coords.blockToCube(Y_LIMIT))
                .mapToObj(cubeY -> new Vec3i(pos.flooredChunkX(), cubeY, pos.flooredChunkZ()));
    }

    @Override
    protected void computeElevations(@NonNull IBlockHeightAccess world, @NonNull int[] elevations, @NonNull BlockPos.MutableBlockPos pos, int blockX, int blockZ) {
        int y = world.getTopBlockY(blockX, blockZ);
        if (y < Y_LIMIT) { //there are no blocks in this column, therefore nothing to do
            return;
        }

        //cubic chunks worlds are a SERIOUS pain in the ass for heightmap generation. since there's no guarantee that any solid surface even exists, we
        //  need to take the frustratingly inexact and slow approach of using the heightmap to find the highest block, then searching manually from there.
        //  the heightmap, conveniently, seems to be taking water blocks into account. this allows us to simply check the block state at the heightmap, and then:
        //  - if it's opaque, there is a solid surface there so no need to look any further
        //  - otherwise, we iterate down through every block just like in vanilla
        //   this has the unfortunate consequence that it could, theoretically, cause huge numbers of cubes to be generated. honestly, i'm not sure if
        //  there's any way around that without either rewriting CC's surface tracking or somehow taking rough generator output into account.

        int usedExtraLayers = 0;

        pos.setY(y);
        IBlockState state = world.getBlockState(pos);
        if (state.isOpaqueCube()) {
            elevations[DEFAULT_LAYER] = y;
            return;
        } else if (state.getBlock() == Blocks.WATER) {
            elevations[WATER_LAYER] = y;
        } else if (state.getMaterial().isSolid() || state.getMaterial().isLiquid()) {
            elevations[EXTRA_LAYERS[usedExtraLayers++]] = y;
        } else {
            return;
        }

        //first pass: sample blocks in increasingly large intervals, as a quick test to make sure that an opaque surface actually exists
        //TODO: this is inaccurate...
        for (int dy = -1; y + dy < y; dy <<= 1) {
            pos.setY(y + dy);
            if ((state = world.getBlockState(pos)).isOpaqueCube()) {
                break;
            }
        }

        if (!state.isOpaqueCube()) { //we never found a solid surface, abort!
            return;
        }

        for (IBlockState prevState = state; --y >= Y_LIMIT; ) {
            pos.setY(y);
            state = world.getBlockState(pos);

            if (state == prevState) { //skip duplicate block states
                continue;
            }
            prevState = state;

            if (state.isOpaqueCube()) { //solid block: save elevation and immediately return, no other layers will be visible
                elevations[DEFAULT_LAYER] = y;
                return;
            } else if (state.getBlock() == Blocks.WATER) { //water: remember the first Y value we meet it at, discard all other occurrences
                if (elevations[WATER_LAYER] == Integer.MIN_VALUE) {
                    elevations[WATER_LAYER] = y;
                }
            } else if (usedExtraLayers < EXTRA_LAYERS.length && (state.getMaterial().isSolid() || state.getMaterial().isLiquid())) { //all other blocks: put the first few on extra layers, discard everything else
                elevations[EXTRA_LAYERS[usedExtraLayers++]] = y;
            }
        }
    }
}
