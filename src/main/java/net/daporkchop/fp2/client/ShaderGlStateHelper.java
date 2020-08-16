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

package net.daporkchop.fp2.client;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.client.gl.object.UniformBufferObject;
import net.daporkchop.fp2.util.DirectBufferReuse;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

import java.nio.FloatBuffer;

import static net.daporkchop.fp2.util.compat.of.OFHelper.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class ShaderGlStateHelper {
    private final UniformBufferObject UBO = new UniformBufferObject();

    private final int OFFSET_CAMERA = 0;
    private final int SIZE_CAMERA = 2 * MAT4_SIZE + DVEC3_SIZE;

    private final int OFFSET_FOG = PMath.roundUp(OFFSET_CAMERA + SIZE_CAMERA, VEC4_SIZE);
    private final int SIZE_FOG = VEC4_SIZE + INT_SIZE + 4 * FLOAT_SIZE;

    private final int TOTAL_SIZE = OFFSET_FOG + SIZE_FOG;

    private final long DATA = PUnsafe.allocateMemory(TOTAL_SIZE);
    private final long ADDR_CAMERA = DATA + OFFSET_CAMERA;
    private final long ADDR_FOG = DATA + OFFSET_FOG;

    public void updateAndBind(float partialTicks, @NonNull Minecraft mc) {
        { //camera
            glGetFloat(GL_PROJECTION_MATRIX, DirectBufferReuse.wrapFloat(ADDR_CAMERA, MAT4_ELEMENTS));
            glGetFloat(GL_MODELVIEW_MATRIX, DirectBufferReuse.wrapFloat(ADDR_CAMERA + MAT4_SIZE, MAT4_ELEMENTS));

            Entity entity = mc.getRenderViewEntity();
            PUnsafe.putDouble(ADDR_CAMERA + 2 * MAT4_SIZE + 0 * DOUBLE_SIZE,
                    entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks);
            PUnsafe.putDouble(ADDR_CAMERA + 2 * MAT4_SIZE + 1 * DOUBLE_SIZE,
                    entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks);
            PUnsafe.putDouble(ADDR_CAMERA + 2 * MAT4_SIZE + 2 * DOUBLE_SIZE,
                    entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks);
        }

        { //fog
            GlStateManager.FogState fogState = GlStateManager.fogState;
            boolean fogEnabled = fogState.fog.currentState
                    && (!OF || PUnsafe.getInt(mc.gameSettings, OF_FOGTYPE_OFFSET) != OF_OFF);

            PUnsafe.putFloat(ADDR_FOG + VEC4_SIZE + 0 * FLOAT_SIZE, fogState.density);
            PUnsafe.putFloat(ADDR_FOG + VEC4_SIZE + 1 * FLOAT_SIZE, fogState.start);
            PUnsafe.putFloat(ADDR_FOG + VEC4_SIZE + 2 * FLOAT_SIZE, fogState.end);
            PUnsafe.putFloat(ADDR_FOG + VEC4_SIZE + 3 * FLOAT_SIZE, 1.0f / (fogState.end - fogState.start));
            PUnsafe.putInt(ADDR_FOG + VEC4_SIZE + 4 * FLOAT_SIZE, fogEnabled ? fogState.mode : 0);
        }

        try (UniformBufferObject ubo = UBO.bind()) { //upload
            glBufferData(GL_UNIFORM_BUFFER, DirectBufferReuse.wrapByte(DATA, TOTAL_SIZE), GL_STATIC_DRAW);
        }
        UBO.bindUBO(0);
    }

    public void updateFogColor(@NonNull FloatBuffer buffer)  {
        PUnsafe.copyMemory(PUnsafe.pork_directBufferAddress(buffer) + buffer.position() * FLOAT_SIZE, ADDR_FOG, VEC4_SIZE);
    }
}
