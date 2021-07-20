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

import lombok.NonNull;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Makes {@link Minecraft} implement {@link ClientThreadExecutor.MixinExecutor}.
 * <p>
 * I do this because {@link Minecraft#addScheduledTask(Runnable)} has significant overhead of allocating a wrapping future, and also suppresses
 * any exceptions thrown by said scheduled tasks.
 *
 * @author DaPorkchop_
 */
@Mixin(Minecraft.class)
@Implements({
        @Interface(iface = ClientThreadExecutor.MixinExecutor.class, prefix = "fp2_executor$", unique = true)
})
public abstract class MixinMinecraft implements ClientThreadExecutor.MixinExecutor {
    @Unique
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    @Shadow
    @Final
    public Profiler profiler;

    @Shadow
    public abstract boolean isCallingFromMinecraftThread();

    @Override
    public void execute(@NonNull Runnable task) {
        if (this.isCallingFromMinecraftThread()) {
            task.run();
        } else {
            this.taskQueue.add(task);
        }
    }

    @Inject(method = "Lnet/minecraft/client/Minecraft;runGameLoop()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE),
            allow = 1)
    public void fp2_runScheduledClientTasks(CallbackInfo ci) {
        this.profiler.startSection("fp2_scheduled_tasks");
        try {
            for (Runnable task; (task = this.taskQueue.poll()) != null; ) {
                task.run();
            }
        } catch (Throwable t0) {
            //we caught an exception... attempt to run as many remaining tasks as possible
            for (Runnable task; (task = this.taskQueue.poll()) != null; ) {
                try {
                    task.run();
                } catch (Throwable t1) {
                    t0.addSuppressed(t1);
                }
            }
            PUnsafe.throwException(t0);
        } finally {
            this.profiler.endSection();
        }
    }
}
