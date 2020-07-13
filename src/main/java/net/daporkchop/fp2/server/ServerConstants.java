/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.server;

import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.CommonConfig;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.util.threading.ServerThreadExecutor;
import net.daporkchop.lib.common.misc.threadfactory.ThreadFactoryBuilder;
import net.daporkchop.lib.common.util.PorkUtil;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class ServerConstants {
    public static EventExecutorGroup GENERATION_WORKERS;
    public static EventExecutorGroup IO_WORKERS;

    public void init() {
        ServerThreadExecutor.INSTANCE.startup();

        GENERATION_WORKERS = new UnorderedThreadPoolEventExecutor(
                CommonConfig.generationThreads,
                new ThreadFactoryBuilder().daemon().collapsingId().formatId().name("FP2 Generation Thread #%d").priority(Thread.MIN_PRIORITY).build());
        IO_WORKERS = new UnorderedThreadPoolEventExecutor(
                CommonConfig.ioThreads,
                new ThreadFactoryBuilder().daemon().collapsingId().formatId().name("FP2 IO Thread #%d").priority(Thread.MIN_PRIORITY).build());
    }

    public void shutdown() {
        Future<?> gen = GENERATION_WORKERS.shutdownGracefully();
        Future<?> io = IO_WORKERS.shutdownGracefully();

        FP2.LOGGER.info("Shutting down generation and IO worker pools...");
        do {
            PorkUtil.sleep(5L);
            ServerThreadExecutor.INSTANCE.workOffQueue();
        } while (!gen.isDone() || !io.isDone());
        FP2.LOGGER.info("Done.");

        GENERATION_WORKERS = null;
        IO_WORKERS = null;

        ServerThreadExecutor.INSTANCE.shutdown();
    }
}
