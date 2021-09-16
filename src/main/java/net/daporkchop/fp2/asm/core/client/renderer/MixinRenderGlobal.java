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

package net.daporkchop.fp2.asm.core.client.renderer;

import net.daporkchop.fp2.asm.interfaz.client.renderer.IMixinRenderGlobal;
import net.daporkchop.fp2.client.VanillaRenderabilityTracker;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.util.alloc.DirectMemoryAllocator;
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
    protected VanillaRenderabilityTracker vanillaRenderabilityTracker;

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;setWorldAndLoadRenderers(Lnet/minecraft/client/multiplayer/WorldClient;)V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;viewFrustum:Lnet/minecraft/client/renderer/ViewFrustum;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER),
            require = 1)
    private void fp2_loadRenderers_onDestroyViewFrustum(CallbackInfo ci) {
        this.vanillaRenderabilityTracker.release();
        this.vanillaRenderabilityTracker = null;
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/RenderGlobal;viewFrustum:Lnet/minecraft/client/renderer/ViewFrustum;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.BEFORE),
            require = 1)
    private void fp2_loadRenderers_onCreateViewFrustum(CallbackInfo ci) {
        if (this.vanillaRenderabilityTracker != null) {
            this.vanillaRenderabilityTracker.release();
        }
        this.vanillaRenderabilityTracker = new VanillaRenderabilityTracker(new DirectMemoryAllocator());
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V",
            at = @At("HEAD"))
    private void fp2_setupTerrain_prepare(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        IFarClientContext<?, ?> context = ((IFarWorldClient) this.world).fp2_IFarWorldClient_activeContext();
        if (context != null) {
            this.vanillaRenderabilityTracker.update(uncheckedCast(this));

            this.mc.profiler.startSection("fp2_prepare");
            context.renderer().prepare((float) partialTicks, this.mc, (IFrustum) camera);
            this.mc.profiler.endSection();
        }
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderHelper;disableStandardItemLighting()V",
                    shift = At.Shift.AFTER),
            allow = 1, require = 1)
    private void fp2_renderBlockLayer_pre(BlockRenderLayer layer, double partialTicks, int pass, Entity entity, CallbackInfoReturnable<Integer> ci) {
        IFarClientContext<?, ?> context = ((IFarWorldClient) this.world).fp2_IFarWorldClient_activeContext();
        if (context != null) {
            Minecraft mc = Minecraft.getMinecraft();

            mc.profiler.startSection("fp2_render_pre");
            context.renderer().render(mc, layer, true);
            mc.profiler.endSection();
        }
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "RETURN"),
            allow = 2, require = 1)
    private void fp2_renderBlockLayer_post(BlockRenderLayer layer, double partialTicks, int pass, Entity entity, CallbackInfoReturnable<Integer> ci) {
        IFarClientContext<?, ?> context = ((IFarWorldClient) this.world).fp2_IFarWorldClient_activeContext();
        if (context != null) {
            Minecraft mc = Minecraft.getMinecraft();

            mc.profiler.startSection("fp2_render_post");
            context.renderer().render(mc, layer, false);
            mc.profiler.endSection();
        }
    }

    @Override
    public VanillaRenderabilityTracker fp2_vanillaRenderabilityTracker() {
        return this.vanillaRenderabilityTracker;
    }
}
