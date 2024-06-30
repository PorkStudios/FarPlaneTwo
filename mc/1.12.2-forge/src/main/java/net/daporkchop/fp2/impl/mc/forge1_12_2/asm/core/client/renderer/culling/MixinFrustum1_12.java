/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.client.renderer.culling;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.FloatBuffer;

/**
 * Makes {@link Frustum} implement {@link IFrustum}.
 *
 * @author DaPorkchop_
 */
@Mixin(Frustum.class)
public abstract class MixinFrustum1_12 implements IFrustum {
    @Shadow
    @Final
    private ClippingHelper clippingHelper;

    @Override
    public boolean containsPoint(double x, double y, double z) {
        return this.isBoxInFrustum(x, y, z, x, y, z);
    }

    @Override
    public boolean intersectsBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Shadow
    public abstract boolean isBoxInFrustum(double p_78548_1_, double p_78548_3_, double p_78548_5_, double p_78548_7_, double p_78548_9_, double p_78548_11_);

    @Override
    public void configureClippingPlanes(@NonNull ClippingPlanes clippingPlanes) {
        float[][] frustum = this.clippingHelper.frustum;

        clippingPlanes.clippingPlaneCount(frustum.length);
        for (int i = 0; i < frustum.length; i++) {
            clippingPlanes.clippingPlane(i, frustum[i]);
        }
    }

    @Override
    public void configureClippingPlanes(ShaderProgram.UniformSetter uniformSetter, UniformLocations locations) {
        float[][] frustum = this.clippingHelper.frustum;

        uniformSetter.set1ui(locations.u_ClippingPlaneCount, frustum.length);
        long address = PUnsafe.allocateMemory(frustum.length * (4 * Float.BYTES));
        try {
            FloatBuffer buffer = DirectBufferHackery.wrapFloat(address, frustum.length * 4);
            for (float[] plane : frustum) {
                buffer.put(plane);
            }
            buffer.clear();
            uniformSetter.set4f(locations.u_ClippingPlanes, buffer);
        } finally {
            PUnsafe.freeMemory(address);
        }
    }
}
