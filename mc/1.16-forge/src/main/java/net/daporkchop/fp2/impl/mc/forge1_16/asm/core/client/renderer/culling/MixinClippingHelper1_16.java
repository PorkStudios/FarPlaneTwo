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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.client.renderer.culling;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.FloatBuffer;

/**
 * @author DaPorkchop_
 */
@Mixin(ClippingHelper.class)
public abstract class MixinClippingHelper1_16 implements IFrustum {
    @Shadow
    @Final
    private Vector4f[] frustumData;

    @Shadow
    protected abstract boolean cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    @Override
    public boolean containsPoint(double x, double y, double z) {
        return this.cubeInFrustum(x, y, z, x, y, z);
    }

    @Override
    public boolean intersectsBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.cubeInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public void configureClippingPlanes(@NonNull ClippingPlanes clippingPlanes) {
        Vector4f[] frustum = this.frustumData;

        clippingPlanes.clippingPlaneCount(frustum.length);
        for (int i = 0; i < frustum.length; i++) {
            Vector4f plane = frustum[i];
            clippingPlanes.clippingPlane(i, plane.x(), plane.y(), plane.z(), plane.w());
        }
    }

    @Override
    public void configureClippingPlanes(ShaderProgram.UniformSetter uniformSetter, UniformLocations locations) {
        Vector4f[] frustum = this.frustumData;

        uniformSetter.set1ui(locations.u_ClippingPlaneCount, frustum.length);

        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            FloatBuffer buffer = stack.mallocFloat(frustum.length * 4);
            for (Vector4f plane : frustum) {
                buffer.put(plane.x()).put(plane.y()).put(plane.z()).put(plane.w());
            }
            buffer.clear();
            uniformSetter.set4f(locations.u_ClippingPlanes, buffer);
        } finally {
            stack.setPointer(stackPointer);
        }
    }
}
