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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.fixes.client.renderer.texture;

import net.minecraft.client.renderer.texture.TextureUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author DaPorkchop_
 */
@Mixin(TextureUtil.class)
public abstract class MixinTextureUtil {
    //original code, with opengl parameters written as constants
    /*public static void allocateTextureImpl(int glTextureId, int mipmapLevels, int width, int height) {
        synchronized (SplashProgress.class) {
            deleteTexture(glTextureId);
            bindTexture(glTextureId);
        }

        if (mipmapLevels >= 0) {
            GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, mipmapLevels);
            GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
            GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, mipmapLevels);
            GlStateManager.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0.0F);
        }

        for (int i = 0; i <= mipmapLevels; ++i) {
            GlStateManager.glTexImage2D(GL_TEXTURE_2D, i, GL_RGBA, width >> i, height >> i, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, null);
        }
    }*/

    /**
     * How in the everloving fuck does this vanilla code work at all, ever? Who came up with the brilliant idea of deleting a texture
     * immediately before using it? Why is only the AMD driver capable of realizing that this is obviously completely wrong?
     */
    @Redirect(method = "Lnet/minecraft/client/renderer/texture/TextureUtil;allocateTextureImpl(IIII)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/TextureUtil;deleteTexture(I)V"),
            allow = 1)
    private static void fp2_allocateTextureImpl_dontDeleteTextureBeforeUpload_wtf(int glTextureId, int mipmapLevels, int width, int height) {
        //no-op
    }
}
