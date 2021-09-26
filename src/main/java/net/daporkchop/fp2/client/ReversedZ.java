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

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.config.FP2Config;
import net.minecraft.client.renderer.GlStateManager;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class ReversedZ {
    public boolean REVERSED = false;

    public void enable() {
        if (FP2Config.global().compatibility().reversedZ()) {
            REVERSED = true;

            GlStateManager.depthFunc(GL_LEQUAL);

            glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);

            GlStateManager.clearDepth(0.0d);
        }
    }

    public void disable() {
        if (REVERSED) {
            REVERSED = false;

            glClipControl(GL_LOWER_LEFT, GL_NEGATIVE_ONE_TO_ONE);
            GlStateManager.depthFunc(GL_LEQUAL);

            GlStateManager.clearDepth(1.0d);
        }
    }

    public int modifyDepthFunc(int func) {
        if (REVERSED) {
            switch (func) {
                case GL_LESS:
                    return GL_GREATER;
                case GL_LEQUAL:
                    return GL_GEQUAL;
                case GL_GREATER:
                    return GL_LESS;
                case GL_GEQUAL:
                    return GL_LEQUAL;
            }
        }
        return func;
    }
}
