/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.MatrixHelper;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.Transform;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

import java.nio.FloatBuffer;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.compat.of.OFHelper.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.minecraft.util.math.MathHelper.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
public class GlStateUniformAttributes {
    @Attribute(
            transform = Transform.Type.ARRAY_TO_MATRIX,
            matrixDimension = @Transform.MatrixDimension(columns = 4, rows = 4))
    public final float[] modelViewProjectionMatrix = new float[16];

    @Attribute(vectorAxes = { "X", "Y", "Z" })
    public int positionFloorX;
    public int positionFloorY;
    public int positionFloorZ;

    @Attribute(vectorAxes = { "X", "Y", "Z" })
    public float positionFracX;
    public float positionFracY;
    public float positionFracZ;

    @Attribute(vectorAxes = { "R", "G", "B", "A" })
    public float fogColorR;
    public float fogColorG;
    public float fogColorB;
    public float fogColorA;

    @Attribute
    public float fogDensity;

    @Attribute
    public float fogStart;

    @Attribute
    public float fogEnd;

    @Attribute
    public float fogScale;

    public GlStateUniformAttributes initFromGlState(float partialTicks, @NonNull Minecraft mc) {
        //optifine compatibility: disable fog if it's turned off, because optifine only does this itself if no vanilla terrain is being rendered
        //  (e.g. it's all being discarded in frustum culling)
        if (OF && (PUnsafe.getInt(mc.gameSettings, OF_FOGTYPE_OFFSET) == OF_OFF && PUnsafe.getBoolean(mc.entityRenderer, OF_ENTITYRENDERER_FOGSTANDARD_OFFSET))) {
            GlStateManager.disableFog();
        }

        { //camera
            this.initModelViewProjectionMatrix();

            Entity entity = mc.getRenderViewEntity();
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

            this.positionFloorX = floorI(x);
            this.positionFloorY = floorI(y);
            this.positionFloorZ = floorI(z);

            this.positionFracX = (float) frac(x);
            this.positionFracY = (float) frac(y);
            this.positionFracZ = (float) frac(z);
        }

        { //fog
            this.initFogColor();

            this.fogDensity = glGetFloat(GL_FOG_DENSITY);
            this.fogStart = glGetFloat(GL_FOG_START);
            this.fogEnd = glGetFloat(GL_FOG_END);
            this.fogScale = 1.0f / (this.fogEnd - this.fogStart);
        }

        return this;
    }

    private void initModelViewProjectionMatrix() {
        ArrayAllocator<float[]> alloc = ALLOC_FLOAT.get();

        float[] modelView = alloc.atLeast(MAT4_ELEMENTS);
        float[] projection = alloc.atLeast(MAT4_ELEMENTS);
        try {
            //load both matrices into arrays
            MatrixHelper.getFloatMatrixFromGL(GL_MODELVIEW_MATRIX, modelView);
            MatrixHelper.getFloatMatrixFromGL(GL_PROJECTION_MATRIX, projection);

            //pre-multiply matrices on CPU to avoid having to do it per-vertex on GPU
            MatrixHelper.multiply4x4(projection, modelView, this.modelViewProjectionMatrix);

            //offset the projected points' depth values to avoid z-fighting with vanilla terrain
            MatrixHelper.offsetDepth(this.modelViewProjectionMatrix, ReversedZ.REVERSED ? -0.00001f : 0.00001f);
        } finally {
            alloc.release(projection);
            alloc.release(modelView);
        }
    }

    private void initFogColor() {
        //buffer needs to fit 16 elements, but only the first 4 will be used
        long addr = PUnsafe.allocateMemory(16 * FLOAT_SIZE);
        try {
            FloatBuffer buffer = DirectBufferHackery.wrapFloat(addr, 16);
            glGetFloat(GL_FOG_COLOR, buffer);

            this.fogColorR = buffer.get(0);
            this.fogColorG = buffer.get(1);
            this.fogColorB = buffer.get(2);
            this.fogColorA = buffer.get(3);
        } finally {
            PUnsafe.freeMemory(addr);
        }
    }
}
