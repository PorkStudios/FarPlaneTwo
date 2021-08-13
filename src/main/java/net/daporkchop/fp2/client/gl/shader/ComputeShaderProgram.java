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

package net.daporkchop.fp2.client.gl.shader;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.WorkGroupSize;
import net.daporkchop.lib.common.math.BinMath;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3i;

import java.util.Set;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public final class ComputeShaderProgram extends ShaderProgram<ComputeShaderProgram> {
    protected final WorkGroupSize workGroupSize;
    protected Set<EnumFacing.Axis> globalEnableAxes;

    protected final LoadingCache<Long, Vec3i> computeDispatchSizes;

    protected ComputeShaderProgram(@NonNull String name, Shader vert, Shader geom, Shader frag, Shader comp, String[] xfb_varying, @NonNull WorkGroupSize workGroupSize, @NonNull Set<EnumFacing.Axis> globalEnableAxes) {
        super(name, vert, geom, frag, comp, xfb_varying);

        this.workGroupSize = workGroupSize;
        this.globalEnableAxes = globalEnableAxes;

        this.computeDispatchSizes = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .maximumSize(1024L)
                .build(new CacheLoader<Long, Vec3i>() {
                    @Override
                    public Vec3i load(Long totalInvocations) throws Exception {
                        long totalWorkGroupSize = ComputeShaderProgram.this.workGroupSize.totalSize();
                        checkArg(positive(totalInvocations, "totalInvocations") % totalWorkGroupSize == 0L,
                                "total invocation count %d must be a multiple of work group size %d",
                                totalInvocations, totalWorkGroupSize);
                        long totalWorkGroups = totalInvocations / totalWorkGroupSize;

                        Vec3i size = null;

                        long maxX = ComputeShaderProgram.this.globalEnableAxes.contains(EnumFacing.Axis.X) ? 3 : 1L;
                        long maxY = ComputeShaderProgram.this.globalEnableAxes.contains(EnumFacing.Axis.Y) ? MAX_COMPUTE_WORK_GROUP_COUNT_Y : 1L;
                        long maxZ = ComputeShaderProgram.this.globalEnableAxes.contains(EnumFacing.Axis.Z) ? MAX_COMPUTE_WORK_GROUP_COUNT_Z : 1L;
                        if (totalWorkGroups <= maxX) { //fast-track for cases where we can fit everything on one axis
                            size = new Vec3i(toInt(totalWorkGroups), 1, 1);
                        } else if (totalWorkGroups <= maxY) {
                            size = new Vec3i(1, toInt(totalWorkGroups), 1);
                        } else if (totalWorkGroups <= maxZ) {
                            size = new Vec3i(1, 1, toInt(totalWorkGroups));
                        } else if (BinMath.isPow2(totalInvocations)) { //fast-track for powers of two
                            long total = 1L;
                            int x = 1;
                            int y = 1;
                            int z = 1;

                            if (ComputeShaderProgram.this.globalEnableAxes.contains(EnumFacing.Axis.X)) {
                                while (x < (x << 1) && x < Integer.highestOneBit(MAX_COMPUTE_WORK_GROUP_COUNT_X) && total < totalWorkGroups) {
                                    x <<= 1;
                                    total <<= 1L;
                                }
                            }
                            if (ComputeShaderProgram.this.globalEnableAxes.contains(EnumFacing.Axis.Y)) {
                                while (y < (y << 1) && y < Integer.highestOneBit(MAX_COMPUTE_WORK_GROUP_COUNT_Y) && total < totalWorkGroups) {
                                    y <<= 1;
                                    total <<= 1L;
                                }
                            }
                            if (ComputeShaderProgram.this.globalEnableAxes.contains(EnumFacing.Axis.Z)) {
                                while (z < (z << 1) && z < Integer.highestOneBit(MAX_COMPUTE_WORK_GROUP_COUNT_Z) && total < totalWorkGroups) {
                                    z <<= 1;
                                    total <<= 1L;
                                }
                            }

                            size = new Vec3i(x, y, z);
                        } else { //very slow fallback approach for arbitrary sizes using prime factorization
                            //TODO: this is not perfect and can fail
                            int x = 1;
                            int y = 1;
                            int z = 1;

                            for (long factor : primeFactors(totalWorkGroups)) {
                                if (maxX > factor) {
                                    maxX /= factor;
                                    x = multiplyExact(x, toInt(factor));
                                } else if (maxY > factor) {
                                    maxY /= factor;
                                    y = multiplyExact(y, toInt(factor));
                                } else if (maxZ > factor) {
                                    maxZ /= factor;
                                    z = multiplyExact(z, toInt(factor));
                                } else {
                                    break;
                                }
                            }

                            size = new Vec3i(x, y, z);
                        }

                        checkState(size != null && (long) size.getX() * size.getY() * size.getZ() == totalWorkGroups,
                                "unable to achieve %d compute shader invocations with %s axes enabled, work group size set to %s and work group count limited to (%d,%d,%d)",
                                totalInvocations, ComputeShaderProgram.this.globalEnableAxes, ComputeShaderProgram.this.workGroupSize,
                                MAX_COMPUTE_WORK_GROUP_COUNT_X, MAX_COMPUTE_WORK_GROUP_COUNT_Y, MAX_COMPUTE_WORK_GROUP_COUNT_Z);
                        return size;
                    }
                });
    }

    @Override
    @Deprecated
    protected void reload(Shader vert, Shader geom, Shader frag, Shader comp, String[] xfb_varying) {
        throw new UnsupportedOperationException();
    }

    protected void reload(Shader vert, Shader geom, Shader frag, Shader comp, String[] xfb_varying, @NonNull Set<EnumFacing.Axis> globalEnableAxes) {
        super.reload(vert, geom, frag, comp, xfb_varying);

        this.globalEnableAxes = globalEnableAxes;
        this.computeDispatchSizes.invalidateAll();
    }

    public void dispatch(long totalInvocations) {
        Vec3i dimensions = this.computeDispatchSizes.getUnchecked(totalInvocations);
        glDispatchCompute(dimensions.getX(), dimensions.getY(), dimensions.getZ());
    }
}
