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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.client.renderer;

import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.client.renderer.IMixinRenderGlobal;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.TerrainRenderingBlockedTracker1_12_2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal implements IMixinRenderGlobal {
    @Shadow
    public WorldClient world;

    @Shadow
    @Final
    public Minecraft mc;

    @Unique
    protected TerrainRenderingBlockedTracker1_12_2 fp2_vanillaRenderabilityTracker;

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;setWorldAndLoadRenderers(Lnet/minecraft/client/multiplayer/WorldClient;)V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;viewFrustum:Lnet/minecraft/client/renderer/ViewFrustum;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER),
            require = 1)
    private void fp2_loadRenderers_onDestroyViewFrustum(CallbackInfo ci) {
        this.fp2_vanillaRenderabilityTracker.release();
        this.fp2_vanillaRenderabilityTracker = null;
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;viewFrustum:Lnet/minecraft/client/renderer/ViewFrustum;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.BEFORE),
            require = 1)
    private void fp2_loadRenderers_onCreateViewFrustum(CallbackInfo ci) {
        if (this.fp2_vanillaRenderabilityTracker != null) {
            this.fp2_vanillaRenderabilityTracker.release();
        }
        this.fp2_vanillaRenderabilityTracker = new TerrainRenderingBlockedTracker1_12_2();
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V",
            at = @At("HEAD"))
    private void fp2_setupTerrain_prepare(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        fp2().client().currentPlayer().ifPresent(player -> {
            IFarClientContext<?, ?> context = player.fp2_IFarPlayerClient_activeContext();
            IFarRenderer renderer;
            if (context != null && (renderer = context.renderer()) != null) {
                this.fp2_vanillaRenderabilityTracker.update(uncheckedCast(this));

                this.mc.profiler.startSection("fp2_prepare");
                renderer.prepare((IFrustum) camera);
                this.mc.profiler.endSection();
            }
        });
    }

    @Unique
    private int toLayerIndex(BlockRenderLayer layer) {
        int ordinal = layer.ordinal();
        return ordinal + ((-ordinal) >> 31);
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderHelper;disableStandardItemLighting()V",
                    shift = At.Shift.AFTER),
            allow = 1, require = 1)
    private void fp2_renderBlockLayer_pre(BlockRenderLayer layer, double partialTicks, int pass, Entity entity, CallbackInfoReturnable<Integer> ci) {
        if (layer == BlockRenderLayer.CUTOUT_MIPPED) {
            return;
        }

        fp2().client().currentPlayer().ifPresent(player -> {
            IFarClientContext<?, ?> context = player.fp2_IFarPlayerClient_activeContext();
            IFarRenderer renderer;
            if (context != null && (renderer = context.renderer()) != null) {
                this.mc.profiler.startSection("fp2_render_pre");
                renderer.render(this.toLayerIndex(layer), true);
                this.mc.profiler.endSection();
            }
        });
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "RETURN"),
            allow = 2, require = 1)
    private void fp2_renderBlockLayer_post(BlockRenderLayer layer, double partialTicks, int pass, Entity entity, CallbackInfoReturnable<Integer> ci) {
        if (layer == BlockRenderLayer.CUTOUT_MIPPED) {
            return;
        }

        fp2().client().currentPlayer().ifPresent(player -> {
            IFarClientContext<?, ?> context = player.fp2_IFarPlayerClient_activeContext();
            IFarRenderer renderer;
            if (context != null && (renderer = context.renderer()) != null) {
                this.mc.profiler.startSection("fp2_render_post");

                //TODO: reduce this down to a single call - the implementation shouldn't have to be aware of which vanilla render passes have completed
                this.mc.textureMapBlocks.setBlurMipmapDirect(false, this.mc.gameSettings.mipmapLevels > 0);
                renderer.render(this.toLayerIndex(layer), false);
                this.mc.textureMapBlocks.restoreLastBlurMipmap();

                this.mc.profiler.endSection();
            }
        });
    }

    @Override
    public TerrainRenderingBlockedTracker1_12_2 fp2_vanillaRenderabilityTracker() {
        return this.fp2_vanillaRenderabilityTracker;
    }
}
