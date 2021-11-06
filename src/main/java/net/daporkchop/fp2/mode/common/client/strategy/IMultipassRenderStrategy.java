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

package net.daporkchop.fp2.mode.common.client.strategy;

import lombok.NonNull;
import net.daporkchop.fp2.client.DrawMode;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.gl.command.DrawCommand;
import net.daporkchop.fp2.gl.binding.DrawBinding;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.mode.common.client.index.IRenderIndex;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.BlockRenderLayer;

import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
public interface IMultipassRenderStrategy<POS extends IFarPos, T extends IFarTile, BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends IFarRenderStrategy<POS, T, BO, DB, DC> {
    @Override
    default void render(@NonNull IRenderIndex<POS, BO, DB, DC> index, @NonNull BlockRenderLayer layer, boolean pre) {
        if (layer == BlockRenderLayer.CUTOUT && !pre) {
            ((AbstractTexture) MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)).setBlurMipmapDirect(false, MC.gameSettings.mipmapLevels > 0);

            try (DrawMode mode = DrawMode.SHADER.begin()) {
                this.render(index);
            }

            MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        }
    }

    default void render(@NonNull IRenderIndex<POS, BO, DB, DC> index) {
        this.preRender();

        //in order to properly render overlapping layers while ensuring that low-detail levels always get placed on top of high-detail ones, we'll need to do the following:
        //- for each detail level:
        //  - render both the SOLID and CUTOUT passes at once, using the stencil to ensure that previously rendered SOLID and CUTOUT terrain at higher detail levels is left untouched
        //- render the TRANSPARENT pass at all detail levels at once, using the stencil to not only prevent low-detail from rendering over high-detail, but also fp2 transparent water
        //  from rendering over vanilla water

        for (int level = 0; level < MAX_LODS; level++) {
            if (index.hasAnyTilesForLevel(level)) {
                this.renderSolid(index, level);
                this.renderCutout(index, level);
            }
        }

        this.renderTransparent(index);

        this.postRender();
    }

    default void preRender() {
        if (FP2_DEBUG && !FP2Config.global().debug().backfaceCulling()) {
            GlStateManager.disableCull();
        }

        glEnable(GL_STENCIL_TEST);
        glStencilMask(0xFF);
        glClearStencil(0x7F);
        GlStateManager.clear(GL_STENCIL_BUFFER_BIT);
    }

    default void postRender() {
        glDisable(GL_STENCIL_TEST);

        if (FP2_DEBUG && !FP2Config.global().debug().backfaceCulling()) {
            GlStateManager.enableCull();
        }
    }

    default void renderSolid(@NonNull IRenderIndex<POS, BO, DB, DC> index, int level) {
        GlStateManager.disableAlpha();

        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
        glStencilFunc(GL_LEQUAL, level, 0x7F);
        index.draw(level, 0, this.blockShader());

        GlStateManager.enableAlpha();
    }

    default void renderCutout(@NonNull IRenderIndex<POS, BO, DB, DC> index, int level) {
        MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, MC.gameSettings.mipmapLevels > 0);

        glStencilOp(GL_KEEP, GL_REPLACE, GL_REPLACE);
        glStencilFunc(GL_LEQUAL, level, 0x7F);
        index.draw(level, 1, this.blockShader());

        MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
    }

    default void renderTransparent(@NonNull IRenderIndex<POS, BO, DB, DC> index) {
        this.renderTransparentStencilPass(index);
        this.renderTransparentFragmentPass(index);
    }

    default void renderTransparentStencilPass(@NonNull IRenderIndex<POS, BO, DB, DC> index) {
        GlStateManager.colorMask(false, false, false, false);

        GlStateManager.depthMask(false);

        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
        for (int level = 0; level < MAX_LODS; level++) {
            if (index.hasAnyTilesForLevel(level)) {
                glStencilFunc(GL_GEQUAL, 0x80 | (MAX_LODS - level), 0xFF);
                index.draw(level, 2, this.stencilShader());
            }
        }

        GlStateManager.depthMask(true);

        GlStateManager.colorMask(true, true, true, true);
    }

    default void renderTransparentFragmentPass(@NonNull IRenderIndex<POS, BO, DB, DC> index) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        GlStateManager.alphaFunc(GL_GREATER, 0.1f);

        glStencilMask(0);

        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
        for (int level = 0; level < MAX_LODS; level++) {
            if (index.hasAnyTilesForLevel(level)) {
                glStencilFunc(GL_EQUAL, 0x80 | (MAX_LODS - level), 0xFF);
                index.draw(level, 2, this.blockShaderTransparent());
            }
        }

        GlStateManager.disableBlend();
    }

    /**
     * @return the shader used for rendering blocks
     */
    DrawShaderProgram blockShader();

    /**
     * @return the shader used for rendering blocks
     */
    default DrawShaderProgram blockShaderTransparent() {
        return this.blockShader();
    }

    /**
     * @return the shader used for preparing the stencil buffer
     */
    DrawShaderProgram stencilShader();
}
