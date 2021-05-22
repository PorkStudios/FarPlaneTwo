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
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class CCHeightmapGenerator extends AbstractExactHeightmapGenerator {
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
                    return world.getTopBlockY(blockX, blockZ);
                })
                .distinct() //we don't want a bunch of identical cube Y coordinates
                .filter(cubeY -> cubeY > MINIMUM_CONSIDERED_Y)
                .mapToObj(cubeY -> new Vec3i(pos.flooredChunkX(), cubeY, pos.flooredChunkZ()));
    }
}
