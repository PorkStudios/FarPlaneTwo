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
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static net.daporkchop.fp2.client.compat.OptifineCompat.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class FogHelper {
    protected final UniformBufferObject UBO = new UniformBufferObject();
    protected final ByteBuffer BUFFER = Constants.createByteBuffer(9 * 4);

    public static void _setColor(@NonNull FloatBuffer buffer) {
        PUnsafe.copyMemory(
                PUnsafe.pork_directBufferAddress(buffer) + buffer.position() * 4L,
                PUnsafe.pork_directBufferAddress(BUFFER),
                16L);
    }

    public static void update(@NonNull Minecraft mc) {
        GlStateManager.FogState fogState = GlStateManager.fogState;
        boolean fogEnabled = fogState.fog.currentState
                && (!OF || PUnsafe.getInt(mc.gameSettings, OF_FOGTYPE_OFFSET) != OF_OFF);
        BUFFER.putInt(16, fogEnabled ? fogState.mode : 0)
                .putFloat(16 + 4, fogState.density)
                .putFloat(16 + 8, fogState.start)
                .putFloat(16 + 12, fogState.end)
                .putFloat(16 + 16, 1.0f / (fogState.end - fogState.start));
    }

    public static void upload() {
        try (UniformBufferObject ubo = UBO.bind()) {
            glBufferData(GL_UNIFORM_BUFFER, BUFFER, GL_STATIC_DRAW);
        }
    }

    public static void bind() {
        UBO.bindUBO(1);
    }

    public static void prepare(@NonNull Minecraft mc) {
        update(mc);
        upload();
        bind();
    }
}
