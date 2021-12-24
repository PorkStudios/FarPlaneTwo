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

package net.daporkchop.fp2.client;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.MatrixHelper;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class ShaderClippingStateHelper {
    private final GLBuffer BUFFER = new GLBuffer(GL_STREAM_DRAW);

    private final int VEC4_SIZE = 4 * FLOAT_SIZE;

    private final int OFFSET_FULLPLANES_OR = 0;
    private final int COUNT_FULLPLANES_OR = 4;
    private final int SIZE_FULLPLANESOR = COUNT_FULLPLANES_OR * VEC4_SIZE;

    private final int OFFSET_FULLPLANES_AND = OFFSET_FULLPLANES_OR + SIZE_FULLPLANESOR;
    private final int COUNT_FULLPLANES_AND = 2;
    private final int SIZE_FULLPLANESAND = COUNT_FULLPLANES_AND * VEC4_SIZE;

    private final int OFFSET_PARTIALCOUNT = OFFSET_FULLPLANES_AND + SIZE_FULLPLANESAND;
    private final int SIZE_PARTIALCOUNT = VEC4_SIZE;

    private final int OFFSET_PARTIALPLANES = OFFSET_PARTIALCOUNT + SIZE_PARTIALCOUNT;
    private final int COUNT_PARTIALPLANES = 10;
    private final int SIZE_PARTIALPLANES = COUNT_PARTIALPLANES * VEC4_SIZE;

    private final int TOTAL_SIZE = OFFSET_PARTIALPLANES + SIZE_PARTIALPLANES + 4096;

    private final long DATA = PUnsafe.allocateMemory(TOTAL_SIZE);

    private final boolean DEBUG_CULLING_FOV = false;

    public void update(@NonNull IFrustum frustum) {
        if (DEBUG_CULLING_FOV) {
            GlStateManager.matrixMode(GL_PROJECTION);
            glPushMatrix();
            GlStateManager.loadIdentity();

            FloatBuffer matrix = BufferUtils.createFloatBuffer(MatrixHelper.MAT4_ELEMENTS);
            MatrixHelper.reversedZ(matrix, 45.0f, (float) Minecraft.getMinecraft().displayWidth / (float) Minecraft.getMinecraft().displayHeight, 0.05F);
            glMultMatrix(matrix);

            GlStateManager.matrixMode(GL_MODELVIEW);
        }

        ClippingHelper clippingHelper = ClippingHelperImpl.getInstance();

        if (DEBUG_CULLING_FOV) {
            GlStateManager.matrixMode(GL_PROJECTION);
            glPopMatrix();
            GlStateManager.matrixMode(GL_MODELVIEW);
        }

        long addr = DATA;
        //vec4 fullPlanesOR[CLIPPING_PLANES_FULL_OR];
        for (int i = 0; i < COUNT_FULLPLANES_OR; i++, addr += VEC4_SIZE) {
            PUnsafe.copyMemory(clippingHelper.frustum[i], PUnsafe.ARRAY_FLOAT_BASE_OFFSET, null, addr, VEC4_SIZE);
        }

        //vec4 fullPlanesAND[CLIPPING_PLANES_FULL_AND];
        for (int i = COUNT_FULLPLANES_OR; i < COUNT_FULLPLANES_OR + COUNT_FULLPLANES_AND; i++, addr += VEC4_SIZE) {
            PUnsafe.copyMemory(clippingHelper.frustum[i], PUnsafe.ARRAY_FLOAT_BASE_OFFSET, null, addr, VEC4_SIZE);
        }

        //uint partialCount;
        PUnsafe.putInt(addr, COUNT_FULLPLANES_OR + COUNT_FULLPLANES_AND);
        addr += VEC4_SIZE;

        //vec4 fullPlanesAND[MAX_CLIPPING_PLANES_PARTIAL];
        for (int i = 0; i < COUNT_FULLPLANES_OR + COUNT_FULLPLANES_AND; i++, addr += VEC4_SIZE) {
            PUnsafe.copyMemory(clippingHelper.frustum[i], PUnsafe.ARRAY_FLOAT_BASE_OFFSET, null, addr, VEC4_SIZE);
        }

        try (GLBuffer buffer = BUFFER.bind(GL_UNIFORM_BUFFER)) { //upload
            buffer.upload(DATA, TOTAL_SIZE);
        }
    }

    public void bind() {
        BUFFER.bindBase(GL_UNIFORM_BUFFER, 2);
    }
}
