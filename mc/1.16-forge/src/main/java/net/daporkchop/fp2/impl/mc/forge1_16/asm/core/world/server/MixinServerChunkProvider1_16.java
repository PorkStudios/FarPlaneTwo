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

package net.daporkchop.fp2.impl.mc.forge1_16.asm.core.world.server;

import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.server.IMixinServerWorld1_16;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@Mixin(ServerChunkProvider.class)
public abstract class MixinServerChunkProvider1_16 {
    @Unique
    private static boolean FP2_WORKERTHREAD_GETCHUNK_WARNING = false;

    @Shadow
    @Final
    public ServerWorld level;

    @Redirect(method = "Lnet/minecraft/world/server/ServerChunkProvider;getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/IChunk;",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"),
            require = 1, allow = 1)
    private CompletableFuture<IChunk> fp2_getChunk_redirectToFP2Executor(Supplier<IChunk> supplier, Executor executor) {
        //redirect tasks to the fp2 worker manager if current thread belongs to an fp2 worker group
        WorkerManager workerManager = ((IMixinServerWorld1_16) this.level).fp2_farWorldServer().fp2_IFarWorld_workerManager();
        if (workerManager.belongsToWorkerGroup(Thread.currentThread())) {
            //print warning if not already printed
            if (!FP2_WORKERTHREAD_GETCHUNK_WARNING) {
                synchronized (ServerChunkProvider.class) {
                    if (!FP2_WORKERTHREAD_GETCHUNK_WARNING) {
                        FP2_WORKERTHREAD_GETCHUNK_WARNING = true;

                        fp2().log().warn("ServerChunkProvider#getChunk() was accessed from an fp2 worker thread! This is bad, and likely indicates a bug.", new RuntimeException().fillInStackTrace());
                    }
                }
            }

            return workerManager.workExecutor().supply(supplier);
        } else {
            return CompletableFuture.supplyAsync(supplier, executor);
        }
    }
}
