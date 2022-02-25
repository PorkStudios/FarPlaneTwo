/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.client.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.impl.mc.forge1_16.client.render.WorldRenderer1_16;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer1_16 {
    @Shadow
    private ClientWorld level;

    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;setupRender(Lnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/culling/ClippingHelper;ZIZ)V",
            at = @At("HEAD"),
            require = 1)
    private void fp2_setupRender_prepare(ActiveRenderInfo info, ClippingHelper clippingHelper, boolean useCapturedFrustum, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        fp2().client().currentPlayer().ifPresent(player -> {
            IFarClientContext<?, ?> context = player.fp2_IFarPlayerClient_activeContext();
            IFarRenderer renderer;
            if (context != null && (renderer = context.renderer()) != null) {
                //TODO: update renderability tracker
                // this.fp2_vanillaRenderabilityTracker.update(uncheckedCast(this));

                this.level.getProfiler().push("fp2_prepare");
                renderer.prepare((IFrustum) clippingHelper); //TODO: this doesn't work
                this.level.getProfiler().pop();
            }
        });
    }

    @Unique
    private int fp2_toLayerIndex(RenderType type) {
        if (type == RenderType.solid()) {
            return IFarRenderer.LAYER_SOLID;
        } else if (type == RenderType.cutout()) {
            return IFarRenderer.LAYER_CUTOUT;
        } else if (type == RenderType.translucent()) {
            return IFarRenderer.LAYER_TRANSPARENT;
        } else {
            return -1;
        }
    }

    @SuppressWarnings("deprecation")
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;setupRenderState()V",
                    shift = At.Shift.AFTER),
            require = 1, allow = 1)
    private void fp2_renderChunkLayer_pre(RenderType type, MatrixStack matrixStack, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        int layer = this.fp2_toLayerIndex(type);
        if (layer < 0) {
            return;
        }

        fp2().client().currentPlayer().ifPresent(player -> {
            IFarClientContext<?, ?> context = player.fp2_IFarPlayerClient_activeContext();
            IFarRenderer renderer;
            if (context != null && (renderer = context.renderer()) != null) {
                this.level.getProfiler().push("fp2_render_pre");
                WorldRenderer1_16.ACTIVE_MATRIX_STACK = matrixStack;

                this.minecraft.getModelManager().getAtlas(AtlasTexture.LOCATION_BLOCKS).setBlurMipmap(false, this.minecraft.options.mipmapLevels > 0);
                renderer.render(layer, true);
                this.minecraft.getModelManager().getAtlas(AtlasTexture.LOCATION_BLOCKS).restoreLastBlurMipmap();

                this.level.getProfiler().pop();
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Inject(method = "Lnet/minecraft/client/renderer/WorldRenderer;renderChunkLayer(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/matrix/MatrixStack;DDD)V",
            at = @At("RETURN"),
            require = 1, allow = 2)
    private void fp2_renderChunkLayer_post(RenderType type, MatrixStack matrixStack, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        int layer = this.fp2_toLayerIndex(type);
        if (layer < 0) {
            return;
        }

        fp2().client().currentPlayer().ifPresent(player -> {
            IFarClientContext<?, ?> context = player.fp2_IFarPlayerClient_activeContext();
            IFarRenderer renderer;
            if (context != null && (renderer = context.renderer()) != null) {
                this.level.getProfiler().push("fp2_render_post");
                WorldRenderer1_16.ACTIVE_MATRIX_STACK = matrixStack;

                this.minecraft.getModelManager().getAtlas(AtlasTexture.LOCATION_BLOCKS).setBlurMipmap(false, this.minecraft.options.mipmapLevels > 0);
                renderer.render(layer, false);
                this.minecraft.getModelManager().getAtlas(AtlasTexture.LOCATION_BLOCKS).restoreLastBlurMipmap();

                this.level.getProfiler().pop();
            }
        });
    }

    @Redirect(method = "Lnet/minecraft/client/renderer/WorldRenderer;renderLevel(Lcom/mojang/blaze3d/matrix/MatrixStack;FJZLnet/minecraft/client/renderer/ActiveRenderInfo;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/util/math/vector/Matrix4f;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;getRenderDistance()F"),
            require = 1, allow = 1)
    private float fp2_renderLevel_increaseFogDistance(GameRenderer renderer) {
        IFarClientContext<?, ?> context = fp2().client().currentPlayer().map(IFarPlayerClient::fp2_IFarPlayerClient_activeContext).orElse(null);
        if (context != null) {
            FP2Config config = context.config();
            return config.effectiveRenderDistanceBlocks();
            //TODO: i need a better system for computing this
        }

        return renderer.getRenderDistance();
    }
}
