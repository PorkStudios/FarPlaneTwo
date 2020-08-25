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
import net.daporkchop.fp2.util.math.qef.QefSolver;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.PerlinNoiseEngine;
import net.daporkchop.lib.random.impl.FastPRandom;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.util.vector.Vector3f;

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

    protected static final int MAX_EDGES = 12;
    protected static final int[] edgevmap = {
            0, 4, 1, 5, 2, 6, 3, 7,    // x-axis
            0, 2, 1, 3, 4, 6, 5, 7,    // y-axis
            0, 1, 2, 3, 4, 5, 6, 7     // z-axis
    };

    public static ByteBuf bake(@NonNull VoxelPiece piece) {
        ByteBuf buf = Constants.allocateByteBuf(VOXEL_RENDER_SIZE);

        final int baseX = piece.pos().blockX();
        final int baseY = piece.pos().blockY();
        final int baseZ = piece.pos().blockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        SingleBiomeBlockAccess biomeAccess = new SingleBiomeBlockAccess();

        NoiseSource noise = new PerlinNoiseEngine(new FastPRandom(1234L)).scaled(1d / 16d);

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    double x = baseX + dx;
                    double y = baseY + dy;
                    double z = baseZ + dz;

                    buildVoxel(noise, buf, x, y, z);
                }
            }
        }
        return buf;
    }

    protected static void buildVoxel(NoiseSource noise, ByteBuf buf, double x, double y, double z) {
        int corners = 0;
        for (int i = 0; i < 8; i++) {
            double density = noise.get(x + ((i >> 2) & 1), y + ((i >> 1) & 1), z + (i & 1));
            if (density < 0.0d) {
                corners |= 1 << i;
            }
        }

        if (corners == 0 || corners == 0xFF) {
            buf.writeFloat(0f).writeFloat(0f).writeFloat(0f).writeInt(0);
            return;
        }

        QefSolver qef = new QefSolver();
        Vector3f averageNormal = new Vector3f();
        int edges = 0;
        int edgeMask = 0;

        for (int i = 0; i < 12 && edges < MAX_EDGES; i++) {
            int c0 = edgevmap[i << 1];
            int c1 = edgevmap[(i << 1) | 1];

            if (((corners >> c0) & 1) == ((corners >> c1) & 1)) {
                continue;
            }

            double x0 = x + ((c0 >> 2) & 1);
            double y0 = y + ((c0 >> 1) & 1);
            double z0 = z + (c0 & 1);
            double x1 = x + ((c1 >> 2) & 1);
            double y1 = y + ((c1 >> 1) & 1);
            double z1 = z + (c1 & 1);

            double t = minimize(noise, x0, y0, z0, x1, y1, z1);
            double px = lerp(x0, x1, t);
            double py = lerp(y0, y1, t);
            double pz = lerp(z0, z1, t);

            double dx = noise.get(px + 0.001d, py, pz) - noise.get(px - 0.001d, py, pz);
            double dy = noise.get(px, py + 0.001d, pz) - noise.get(px, py - 0.001d, pz);
            double dz = noise.get(px, py, pz + 0.001d) - noise.get(px, py, pz - 0.001d);
            double length = sqrt(dx * dx + dy * dy + dz * dz);
            float nx = (float) (dx / length);
            float ny = (float) (dy / length);
            float nz = (float) (dz / length);

            qef.add((float) px, (float) py, (float) pz, nx, ny, nz);
            averageNormal.translate(nx, ny, nz);
            edges++;
            edgeMask |= 1 << i;
        }

        Vector3f qefPosition = new Vector3f();
        qef.solve(qefPosition, 0.0001f, 4, 0.0001f);

        buf.writeFloat((float) (qefPosition.x - x)).writeFloat((float) (qefPosition.y - y)).writeFloat((float) (qefPosition.z - z));
        buf.writeInt(edgeMask);
    }

    protected static double minimize(NoiseSource noise, double x0, double y0, double z0, double x1, double y1, double z1) {
        double minValue = Double.MAX_VALUE;
        double t = 0.0d;
        double currentT = 0.0d;

        final double INCREMENT = 1.0d / 8;
        while (currentT <= 1.0d) {
            double v = abs(noise.get(lerp(x0, x1, currentT), lerp(y0, y1, currentT), lerp(z0, z1, currentT)));
            if (v < minValue) {
                minValue = v;
                t = currentT;
            }
            currentT += INCREMENT;
        }
        return t;
    }
}
