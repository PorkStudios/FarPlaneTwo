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
import net.daporkchop.fp2.strategy.voxel.server.gen.VoxelGeneratorConstants;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.daporkchop.fp2.util.math.Vector3d;
import net.daporkchop.fp2.util.math.qef.QefSolver;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.PerlinNoiseEngine;
import net.daporkchop.lib.random.impl.FastPRandom;
import net.minecraft.util.math.BlockPos;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;

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

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    buf.writeFloat((float) piece.dx(dx, dy, dz))
                            .writeFloat((float) piece.dy(dx, dy, dz))
                            .writeFloat((float) piece.dz(dx, dy, dz))
                            .writeInt(piece.edgeMask(dx, dy, dz));
                }
            }
        }
        return buf;
    }
}
