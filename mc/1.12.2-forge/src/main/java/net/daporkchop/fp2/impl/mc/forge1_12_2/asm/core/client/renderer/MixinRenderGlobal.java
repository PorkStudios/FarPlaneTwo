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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.client.renderer;

import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.engine.client.VoxelRenderer;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.client.renderer.IMixinRenderGlobal;
import net.daporkchop.fp2.impl.mc.forge1_12_2.client.TerrainRenderingBlockedTracker1_12_2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        //only create a new renderability tracker if none existed before.
        //  we don't want multiple instances lying around!
        if (this.fp2_vanillaRenderabilityTracker == null) {
            this.fp2_vanillaRenderabilityTracker = new TerrainRenderingBlockedTracker1_12_2();
        }
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/profiler/Profiler;endSection()V"),
            slice = @Slice(
                    from = @At(value = "INVOKE_STRING",
                            target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V",
                            args = "ldc=iteration"),
                    to = @At(value = "INVOKE_STRING",
                            target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V",
                            args = "ldc=captureFrustum")),
            require = 1, allow = 1)
    private void fp2_setupTerrain_updateVanillaRenderability(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        this.mc.profiler.startSection("fp2_vanillaRenderabilityTracker_update");
        this.fp2_vanillaRenderabilityTracker.update(uncheckedCast(this), camera, frameCount);
        this.mc.profiler.endSection();
    }

    @Inject(method = "Lnet/minecraft/client/renderer/RenderGlobal;setupTerrain(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V",
            at = @At(value = "INVOKE_STRING",
                    target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V",
                    args = "ldc=captureFrustum"),
            require = 1, allow = 1)
    private void fp2_setupTerrain_prepare(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        fp2().client().currentPlayer().ifPresent(player -> {
            IFarClientContext context = player.activeContext();
            VoxelRenderer renderer;
            if (context != null && (renderer = context.renderer()) != null) {
                this.mc.profiler.startSection("fp2_prepare");
                renderer.prepare((IFrustum) camera);
                this.mc.profiler.endSection();
            }
        });
    }

    @Override
    public TerrainRenderingBlockedTracker1_12_2 fp2_vanillaRenderabilityTracker() {
        return this.fp2_vanillaRenderabilityTracker;
    }
}
