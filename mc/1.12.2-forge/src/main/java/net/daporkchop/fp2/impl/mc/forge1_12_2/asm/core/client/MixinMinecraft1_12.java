/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.asm.core.client;

import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.futureexecutor.ClientThreadMarkedFutureExecutor1_12;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;
import org.lwjgl.opengl.PixelFormat;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Makes {@link Minecraft} implement {@link ClientThreadMarkedFutureExecutor1_12.Holder}.
 * <p>
 * I do this because {@link Minecraft#addScheduledTask(Runnable)} suppresses any exceptions thrown by scheduled tasks.
 *
 * @author DaPorkchop_
 */
@Mixin(Minecraft.class)
@SuppressWarnings("deprecation")
public abstract class MixinMinecraft1_12 implements ClientThreadMarkedFutureExecutor1_12.Holder {
    @Shadow
    @Final
    public Profiler profiler;

    @Unique
    private ClientThreadMarkedFutureExecutor1_12 fp2_executor;

    @Inject(method = "Lnet/minecraft/client/Minecraft;<init>*",
            at = @At("RETURN"))
    private void fp2_$init$_constructMarkedExecutor(CallbackInfo ci) {
        this.fp2_executor = new ClientThreadMarkedFutureExecutor1_12(uncheckedCast(this));
    }

    @Override
    public ClientThreadMarkedFutureExecutor1_12 fp2_ClientThreadMarkedFutureExecutor$Holder_get() {
        return this.fp2_executor;
    }

    @Inject(method = "Lnet/minecraft/client/Minecraft;runGameLoop()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE),
            allow = 1)
    private void fp2_runGameLoop_runScheduledClientTasks(CallbackInfo ci) {
        this.profiler.startSection("fp2_scheduled_tasks");
        this.fp2_executor.doAllWork();
        this.profiler.endSection();
    }

    @Inject(
            method = {
                    "Lnet/minecraft/client/Minecraft;loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V",
                    "Lnet/minecraft/client/Minecraft;setDimensionAndSpawnPlayer(I)V"
            },
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;player:Lnet/minecraft/client/entity/EntityPlayerSP;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER))
    private void fp2_notifyContextReady(CallbackInfo ci) { //gross hack to ensure that the client config packet isn't sent until the client is ready
        fp2().client().currentPlayer().ifPresent(IFarPlayerClient::ready);
    }

    @ModifyArg(method = "Lnet/minecraft/client/Minecraft;createDisplay()V",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/Display;create(Lorg/lwjgl/opengl/PixelFormat;)V"),
            index = 0,
            require = 1, allow = 1)
    private PixelFormat fp2_createDisplay_add8bitsOfStencilToPixelFormat(PixelFormat pixelFormat) {
        //make sure we have at least 8 bits of stencil data to work with!
        //  we already change this in MixinFramebuffer, but we want to request it for the default framebuffer as well in case FBOs aren't supported (afaik this would only
        //  be the case on very old OpenGL versions, or if OptiFine's "Fast Render" is enabled)

        return pixelFormat.getStencilBits() < 8
                ? pixelFormat.withStencilBits(8) //request 8 bits of stencil depth
                : pixelFormat; //there are already at least 8 bits of stencil (vanilla doesn't use this, so this would only happen if changed by another mod)
    }

    @Inject(method = "Lnet/minecraft/client/Minecraft;createDisplay()V",
            at = @At(value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Throwable;)V",
                    shift = At.Shift.AFTER),
            require = 1, allow = 1)
    private void fp2_createDisplay_notifySetPixelFormatFailed(CallbackInfo ci) {
        throw new IllegalStateException("LWJGL was unable to set the pixel format!\nThis could cause FarPlaneTwo to break, crashing the game to be safe!");
    }
}
