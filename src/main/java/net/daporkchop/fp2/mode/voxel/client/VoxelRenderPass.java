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

package net.daporkchop.fp2.mode.voxel.client;

import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockRenderLayer;

import static net.daporkchop.fp2.mode.voxel.client.VoxelRenderer.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 * @see BlockRenderLayer
 */
public enum VoxelRenderPass {
    OPAQUE {
        @Override
        public void render(@NonNull Minecraft mc, int count) {
            GlStateManager.disableAlpha();

            super.render(mc, count);

            GlStateManager.enableAlpha();
        }
    },
    CUTOUT {
        @Override
        public void render(@NonNull Minecraft mc, int count) {
            mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, mc.gameSettings.mipmapLevels > 0);
            GlStateManager.disableCull();

            super.render(mc, count);

            GlStateManager.enableCull();
            mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        }
    },
    TRANSLUCENT {
        @Override
        public void render(@NonNull Minecraft mc, int count) {
            glEnable(GL_STENCIL_TEST);

            TRANSLUCENT_STENCIL_SHADER.use();
            {
                GlStateManager.colorMask(false, false, false, false);

                GlStateManager.clear(GL_STENCIL_BUFFER_BIT);
                glStencilMask(0xFF);
                glStencilFunc(GL_ALWAYS, 1, 0xFF); //always allow all fragments
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

                GlStateManager.depthMask(false);

                super.render(mc, count);

                GlStateManager.depthMask(true);

                GlStateManager.colorMask(true, true, true, true);
            }

            SOLID_SHADER.use();
            {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
                GlStateManager.alphaFunc(GL_GREATER, 0.1f);

                glStencilMask(0);
                glStencilFunc(GL_EQUAL, 1, 0xFF);
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

                super.render(mc, count);

                GlStateManager.disableBlend();
            }

            glDisable(GL_STENCIL_TEST);
        }
    };

    public static final VoxelRenderPass[] VALUES = values();
    public static final int COUNT = VALUES.length;

    public void render(@NonNull Minecraft mc, int count) {
        glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, 0L, count, 0);
    }
}
