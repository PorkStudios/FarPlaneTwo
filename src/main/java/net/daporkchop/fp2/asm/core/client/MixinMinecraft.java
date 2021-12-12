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

package net.daporkchop.fp2.asm.core.client;

import net.daporkchop.fp2.core.mode.api.player.IFarPlayerClient;
import net.daporkchop.fp2.util.threading.futureexecutor.ClientThreadMarkedFutureExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.profiler.Profiler;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Makes {@link Minecraft} implement {@link ClientThreadMarkedFutureExecutor.Holder}.
 * <p>
 * I do this because {@link Minecraft#addScheduledTask(Runnable)} suppresses any exceptions thrown by scheduled tasks.
 *
 * @author DaPorkchop_
 */
@Mixin(Minecraft.class)
@SuppressWarnings("deprecation")
public abstract class MixinMinecraft implements ClientThreadMarkedFutureExecutor.Holder {
    @Shadow
    @Final
    public Profiler profiler;

    @Shadow
    public EntityPlayerSP player;

    @Unique
    private ClientThreadMarkedFutureExecutor fp2_executor;

    @Inject(method = "Lnet/minecraft/client/Minecraft;<init>*",
            at = @At("RETURN"))
    private void fp2_$init$_constructMarkedExecutor(CallbackInfo ci) {
        this.fp2_executor = new ClientThreadMarkedFutureExecutor(uncheckedCast(this));
    }

    @Override
    public ClientThreadMarkedFutureExecutor fp2_ClientThreadMarkedFutureExecutor$Holder_get() {
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
        if (this.player != null) {
            ((IFarPlayerClient) this.player.connection).fp2_IFarPlayerClient_ready();
        }
    }
}
