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

package net.daporkchop.fp2.asm.client.renderer;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@Mixin(GlStateManager.class)
public abstract class MixinGlStateManager {
    /**
     * @author DaPorkchop_
     */
    @Overwrite
    public static void clearDepth(double depth) {
        GL11.glClearDepth(0.0d);
    }

    /**
     * @author DaPorkchop_
     */
    @Overwrite
    public static void depthFunc(int depthFunc) {
        switch (depthFunc)  {
            case GL_LESS:
                depthFunc = GL_GREATER;
                break;
            case GL_GREATER:
                depthFunc = GL_LESS;
                break;
            case GL_LEQUAL:
                depthFunc = GL_GEQUAL;
                break;
            case GL_GEQUAL:
                depthFunc = GL_LEQUAL;
                break;
        }
        GL11.glDepthFunc(depthFunc);
    }
}
