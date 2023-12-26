/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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
 */

package net.daporkchop.fp2.core.mode.common.client.strategy;

import lombok.NonNull;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.daporkchop.fp2.core.mode.common.client.AbstractFarRenderer;
import net.daporkchop.fp2.gl.command.BlendFactor;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.command.Compare;
import net.daporkchop.fp2.gl.command.FramebufferLayer;
import net.daporkchop.fp2.gl.command.StencilOperation;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.core.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.core.mode.common.client.index.IRenderIndex;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.debug.FP2Debug.*;

/**
 * @author DaPorkchop_
 */
public interface IMultipassRenderStrategy<BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends IFarRenderStrategy<BO, DB, DC> {
    /*@Override
    default void render(@NonNull IRenderIndex<POS, BO, DB, DC> index, int layer, boolean pre) {
        if (layer == IFarRenderer.LAYER_CUTOUT && !pre) {
            //((AbstractTexture) MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)).setBlurMipmapDirect(false, MC.gameSettings.mipmapLevels > 0);

            try (DrawMode mode = DrawMode.SHADER.begin()) {
                this.render(index);
            }

            //MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        }
    }*/

    default void render(@NonNull CommandBufferBuilder builder, @NonNull IRenderIndex<BO, DB, DC> index) {
        this.preRender(builder);

        //in order to properly render overlapping layers while ensuring that low-detail levels always get placed on top of high-detail ones, we'll need to do the following:
        //- for each detail level:
        //  - render both the SOLID and CUTOUT passes at once, using the stencil to ensure that previously rendered SOLID and CUTOUT terrain at higher detail levels is left untouched
        //- render the TRANSPARENT pass at all detail levels at once, using the stencil to not only prevent low-detail from rendering over high-detail, but also fp2 transparent water
        //  from rendering over vanilla water

        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            this.renderSolid(builder, index, level);
            this.renderCutout(builder, index, level);
        }

        this.renderTransparent(builder, index);

        this.postRender(builder);
    }

    default void preRender(@NonNull CommandBufferBuilder builder) {
        if (!FP2_DEBUG || fp2().globalConfig().debug().backfaceCulling()) {
            builder.cullEnable();
        }

        builder.depthEnable();
        builder.depthCompare(fp2().globalConfig().compatibility().reversedZ() ? Compare.GREATER : Compare.LESS);

        builder.stencilEnable();
        builder.stencilWriteMask(0xFF);
        builder.stencilClear(0x7F).framebufferClear(FramebufferLayer.STENCIL);
    }

    default void postRender(@NonNull CommandBufferBuilder builder) {
    }

    default void renderSolid(@NonNull CommandBufferBuilder builder, @NonNull IRenderIndex<BO, DB, DC> index, int level) {
        //GlStateManager.disableAlpha();

        builder.stencilOperation(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.REPLACE)
                .stencilCompare(Compare.LESS_OR_EQUAL)
                .stencilReference(level)
                .stencilCompareMask(0x7F);
        index.draw(builder, level, 0, this.blockShader(level, AbstractFarRenderer.LAYER_SOLID));

        //GlStateManager.enableAlpha();
    }

    default void renderCutout(@NonNull CommandBufferBuilder builder, @NonNull IRenderIndex<BO, DB, DC> index, int level) {
        //MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, MC.gameSettings.mipmapLevels > 0);

        builder.stencilOperation(StencilOperation.KEEP, StencilOperation.REPLACE, StencilOperation.REPLACE)
                .stencilCompare(Compare.LESS_OR_EQUAL)
                .stencilReference(level)
                .stencilCompareMask(0x7F);
        index.draw(builder, level, 1, this.blockShader(level, AbstractFarRenderer.LAYER_CUTOUT));

        //MC.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
    }

    default void renderTransparent(@NonNull CommandBufferBuilder builder, @NonNull IRenderIndex<BO, DB, DC> index) {
        this.renderTransparentStencilPass(builder, index);

        //don't want fragments to be rendered before the stencil pass can complete
        builder.barrier();

        this.renderTransparentFragmentPass(builder, index);
    }

    default void renderTransparentStencilPass(@NonNull CommandBufferBuilder builder, @NonNull IRenderIndex<BO, DB, DC> index) {
        builder.colorWrite(false, false, false, false);
        builder.depthWrite(false);

        builder.stencilOperation(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.REPLACE)
                .stencilCompare(Compare.GREATER_OR_EQUAL)
                .stencilCompareMask(0xFF);
        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            builder.stencilReference(0x80 | (EngineConstants.MAX_LODS - level));
            index.draw(builder, level, 2, this.stencilShader(level, AbstractFarRenderer.LAYER_TRANSPARENT));
        }

        builder.depthWrite(true);
        builder.colorWrite(true, true, true, true);
    }

    default void renderTransparentFragmentPass(@NonNull CommandBufferBuilder builder, @NonNull IRenderIndex<BO, DB, DC> index) {
        builder.blendEnable();
        builder.blendFunctionSrc(BlendFactor.SRC_ALPHA, BlendFactor.ONE).blendFunctionDst(BlendFactor.ONE_MINUS_SRC_ALPHA, BlendFactor.ZERO);

        //GlStateManager.alphaFunc(GL_GREATER, 0.1f);

        builder.stencilWriteMask(0);
        builder.stencilOperation(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP)
                .stencilCompare(Compare.EQUAL)
                .stencilCompareMask(0xFF);
        for (int level = 0; level < EngineConstants.MAX_LODS; level++) {
            builder.stencilReference(0x80 | (EngineConstants.MAX_LODS - level));
            index.draw(builder, level, 2, this.blockShader(level, AbstractFarRenderer.LAYER_TRANSPARENT));
        }

        builder.blendDisable();
    }

    /**
     * @return the shader used for rendering blocks
     */
    DrawShaderProgram blockShader(int level, int layer);

    /**
     * @return the shader used for preparing the stencil buffer
     */
    DrawShaderProgram stencilShader(int level, int layer);
}
