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
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.player.IFarPlayerClient;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class ShaderFP2StateHelper {
    private final GLBuffer BUFFER = new GLBuffer(GL_STREAM_DRAW);

    private final int OFFSET_VIEW = 0;
    private final int SIZE_VIEW = 3 * INT_SIZE;

    private final int TOTAL_SIZE = OFFSET_VIEW + SIZE_VIEW;

    private final long DATA = PUnsafe.allocateMemory(TOTAL_SIZE);
    private final long ADDR_VIEW = DATA + OFFSET_VIEW;

    public void update(float partialTicks, @NonNull Minecraft mc) {
        { //view
            long addr = ADDR_VIEW;

            FP2Config config = PorkUtil.fallbackIfNull(((IFarPlayerClient) mc.getConnection()).fp2_IFarPlayerClient_config(), FP2Config.DEFAULT_CONFIG);

            //int renderDistance
            PUnsafe.putInt(addr, config.cutoffDistance() << config.maxLevels() >> 1);
            addr += INT_SIZE;

            //int maxLevels
            PUnsafe.putInt(addr, config.maxLevels());
            addr += INT_SIZE;

            //int levelCutoffDistance
            PUnsafe.putInt(addr, config.cutoffDistance());
            addr += INT_SIZE;
        }

        try (GLBuffer buffer = BUFFER.bind(GL_UNIFORM_BUFFER)) { //upload
            buffer.upload(DATA, TOTAL_SIZE);
        }
    }

    public void bind() {
        BUFFER.bindBase(GL_UNIFORM_BUFFER, 1);
    }
}
