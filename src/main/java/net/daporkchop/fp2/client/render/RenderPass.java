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

package net.daporkchop.fp2.client.render;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@Deprecated
public enum RenderPass {
    SOLID {
        @Override
        public void render(@NonNull DrawMode mode, int tileCount) {
            try (ShaderProgram program = mode.shaders.getAndUseShader(mode, this, false)) {
                GlStateManager.disableAlpha();

                mode.draw0(tileCount);

                GlStateManager.enableAlpha();
            }
        }
    },
    CUTOUT {
        @Override
        public void render(@NonNull DrawMode mode, int tileCount) {
            try (ShaderProgram program = mode.shaders.getAndUseShader(mode, this, false)) {
                mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, mc.gameSettings.mipmapLevels > 0);
                GlStateManager.disableCull();

                mode.draw0(tileCount);

                GlStateManager.enableCull();
                mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
            }
        }
    },
    TRANSPARENT {
        @Override
        public void render(@NonNull DrawMode mode, int tileCount) {
            glEnable(GL_STENCIL_TEST);

            try (ShaderProgram program = mode.shaders.getAndUseShader(mode, this, true)) {
                GlStateManager.colorMask(false, false, false, false);

                GlStateManager.clear(GL_STENCIL_BUFFER_BIT);
                glStencilMask(0xFF);
                glStencilFunc(GL_ALWAYS, 1, 0xFF); //always allow all fragments
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

                GlStateManager.depthMask(false);

                mode.draw0(tileCount);

                GlStateManager.depthMask(true);

                GlStateManager.colorMask(true, true, true, true);
            }

            try (ShaderProgram program = mode.shaders.getAndUseShader(mode, this, false)) {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
                GlStateManager.alphaFunc(GL_GREATER, 0.1f);

                glStencilMask(0);
                glStencilFunc(GL_EQUAL, 1, 0xFF);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

                mode.draw0(tileCount);

                GlStateManager.disableBlend();
            }

            glDisable(GL_STENCIL_TEST);
        }
    };

    private static final RenderPass[] VALUES = values();
    public static final int COUNT = VALUES.length;

    public static RenderPass fromOrdinal(int ordinal) {
        return VALUES[ordinal];
    }

    public abstract void render(@NonNull DrawMode mode, int tileCount);
}
