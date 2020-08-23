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

package net.daporkchop.fp2.strategy.voxel.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.PerlinNoiseEngine;
import net.daporkchop.lib.random.impl.FastPRandom;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class VoxelRenderHelper {
    public static final int VOXEL_RENDER_SIZE = T_VOXELS * T_VOXELS * T_VOXELS * VEC4_SIZE;

    public static ByteBuf bake(@NonNull VoxelPiece piece) {
        ByteBuf buf = Constants.allocateByteBuf(VOXEL_RENDER_SIZE);

        final int baseX = piece.pos().blockX();
        final int baseY = piece.pos().blockY();
        final int baseZ = piece.pos().blockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();

        NoiseSource noise = new PerlinNoiseEngine(new FastPRandom(1234)).scaled(1d / 16d);

        Random random = ThreadLocalRandom.current();
        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    double x = baseX + dx;
                    double y = baseY + dy;
                    double z = baseZ + dz;

                    double v000 = noise.get(x, y, z);
                    double v001 = noise.get(x, y, z + 1);
                    double v010 = noise.get(x, y + 1, z);
                    double v011 = noise.get(x, y + 1, z + 1);
                    double v100 = noise.get(x + 1, y, z);
                    double v101 = noise.get(x + 1, y, z + 1);
                    double v110 = noise.get(x + 1, y + 1, z);
                    double v111 = noise.get(x + 1, y + 1, z + 1);

                    buf.writeFloat(0f).writeFloat(0f).writeFloat(0f);

                    if (signum(v000) != signum(v001))   {
                        buf.writeInt(7);
                    } else {
                        buf.writeInt(0);
                    }
                }
            }
        }
        return buf;
    }
}
