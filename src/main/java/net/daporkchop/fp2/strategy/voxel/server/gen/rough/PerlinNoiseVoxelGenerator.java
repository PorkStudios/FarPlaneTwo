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

package net.daporkchop.fp2.strategy.voxel.server.gen.rough;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.base.server.AbstractFarGenerator;
import net.daporkchop.fp2.strategy.common.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.strategy.voxel.VoxelPiece;
import net.daporkchop.fp2.strategy.voxel.VoxelPos;
import net.daporkchop.fp2.strategy.voxel.server.gen.VoxelGeneratorConstants;
import net.daporkchop.fp2.util.math.Vector3d;
import net.daporkchop.fp2.util.math.qef.QefSolver;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.PerlinNoiseEngine;
import net.daporkchop.lib.random.impl.FastPRandom;
import net.minecraft.world.WorldServer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class PerlinNoiseVoxelGenerator extends AbstractFarGenerator implements IFarGeneratorRough<VoxelPos, VoxelPiece> {
    protected NoiseSource noise;

    @Override
    public void init(@NonNull WorldServer world) {
        super.init(world);

        this.noise = new PerlinNoiseEngine(new FastPRandom(world.getSeed())).scaled(1d / 16d);
    }

    @Override
    public void generate(@NonNull VoxelPiece piece) {
        piece.clear();

        final int baseX = piece.pos().blockX();
        final int baseY = piece.pos().blockY();
        final int baseZ = piece.pos().blockZ();
        final int level = piece.level();

        for (int dx = 0; dx < T_VOXELS; dx++) {
            for (int dy = 0; dy < T_VOXELS; dy++) {
                for (int dz = 0; dz < T_VOXELS; dz++) {
                    double x = baseX + (dx << level);
                    double y = baseY + (dy << level);
                    double z = baseZ + (dz << level);

                    this.buildVoxel(piece, dx, dy, dz, x, y, z);
                }
            }
        }
        piece.markDirty();
    }

    protected void buildVoxel(VoxelPiece piece, int dx, int dy, int dz, double x, double y, double z)   {
        int corners = 0;
        for (int i = 0; i < 8; i++) {
            double density = this.noise.get(x + ((i >> 2) & 1), y + ((i >> 1) & 1), z + (i & 1));
            if (density < 0.0d) {
                corners |= 1 << i;
            }
        }

        if (corners == 0 || corners == 0xFF) {
            return;
        }

        QefSolver qef = new QefSolver();
        int edges = 0;
        int edgeMask = 0;

        for (int i = 0; i < 12; i++) {
            int c0 = VoxelGeneratorConstants.EDGEVMAP[i << 1];
            int c1 = VoxelGeneratorConstants.EDGEVMAP[(i << 1) | 1];

            if (((corners >> c0) & 1) == ((corners >> c1) & 1)) {
                continue;
            }

            double x0 = x + ((c0 >> 2) & 1);
            double y0 = y + ((c0 >> 1) & 1);
            double z0 = z + (c0 & 1);
            double x1 = x + ((c1 >> 2) & 1);
            double y1 = y + ((c1 >> 1) & 1);
            double z1 = z + (c1 & 1);

            double t = this.minimize(x0, y0, z0, x1, y1, z1);
            double px = lerp(x0, x1, t);
            double py = lerp(y0, y1, t);
            double pz = lerp(z0, z1, t);

            double dX = this.noise.get(px + 0.001d, py, pz) - this.noise.get(px - 0.001d, py, pz);
            double dY = this.noise.get(px, py + 0.001d, pz) - this.noise.get(px, py - 0.001d, pz);
            double dZ = this.noise.get(px, py, pz + 0.001d) - this.noise.get(px, py, pz - 0.001d);

            qef.add(px, py, pz, dX, dY, dZ);
            edges++;
            edgeMask |= 1 << i;
        }

        Vector3d qefPosition = new Vector3d();
        qef.solve(qefPosition, 0.0001f, 4, 0.0001f);

        if (qefPosition.x < x || qefPosition.x > x + 1.0d
            || qefPosition.y < y || qefPosition.y > y + 1.0d
            || qefPosition.z < z || qefPosition.z > z + 1.0d) {
            qefPosition.set(qef.massPoint().x, qef.massPoint().y, qef.massPoint().z);
        }

        piece.set(dx, dy, dz, qefPosition.x - x, qefPosition.y - y, qefPosition.z - z, edgeMask);
    }

    protected double minimize(double x0, double y0, double z0, double x1, double y1, double z1) {
        double minValue = Double.MAX_VALUE;
        double t = 0.0d;
        double currentT = 0.0d;

        final double INCREMENT = 1.0d / 8;
        while (currentT <= 1.0d) {
            double v = abs(this.noise.get(lerp(x0, x1, currentT), lerp(y0, y1, currentT), lerp(z0, z1, currentT)));
            if (v < minValue) {
                minValue = v;
                t = currentT;
            }
            currentT += INCREMENT;
        }
        return t;
    }

    @Override
    public boolean supportsLowResolution() {
        return true;
    }

    @Override
    public boolean isLowResolutionInaccurate() {
        return true;
    }
}
