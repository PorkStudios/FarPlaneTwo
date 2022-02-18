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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.client;

import net.daporkchop.fp2.impl.mc.forge1_16.util.threading.futureexecutor.ClientThreadMarkedFutureExecutor1_16;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.IProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Mixin(Minecraft.class)
@SuppressWarnings("deprecation")
public abstract class MixinMinecraft1_16 implements ClientThreadMarkedFutureExecutor1_16.Holder {
    @Shadow
    private IProfiler profiler;

    @Unique
    private ClientThreadMarkedFutureExecutor1_16 fp2_executor;

    @Inject(method = "Lnet/minecraft/client/Minecraft;<init>*",
            at = @At("RETURN"),
            require = 1)
    private void fp2_$init$_constructMarkedExecutor(CallbackInfo ci) {
        this.fp2_executor = new ClientThreadMarkedFutureExecutor1_16(uncheckedCast(this));
    }

    @Override
    public ClientThreadMarkedFutureExecutor1_16 fp2_ClientThreadMarkedFutureExecutor1_16$Holder_get() {
        return this.fp2_executor;
    }

    @Inject(method = "Lnet/minecraft/client/Minecraft;runTick(Z)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/MouseHelper;turnPlayer()V",
                    shift = At.Shift.BEFORE),
            require = 1, allow = 1)
    private void fp2_runTick_runScheduledClientTasks(CallbackInfo ci) {
        this.profiler.push("fp2_scheduled_tasks");
        this.fp2_executor.doAllWork();
        this.profiler.pop();
    }
}
