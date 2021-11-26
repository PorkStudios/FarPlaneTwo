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

package net.daporkchop.fp2.util.threading.futureexecutor;

import lombok.NonNull;
import net.daporkchop.fp2.core.util.threading.futureexecutor.AbstractMarkedFutureExecutor;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.WorldWorkerManager;

/**
 * @author DaPorkchop_
 */
public class ServerThreadMarkedFutureExecutor extends AbstractMarkedFutureExecutor implements WorldWorkerManager.IWorker {
    /**
     * Gets the {@link ServerThreadMarkedFutureExecutor} for the given {@link MinecraftServer} instance.
     *
     * @param server the {@link MinecraftServer} instance
     * @return the corresponding {@link ServerThreadMarkedFutureExecutor}
     */
    public static ServerThreadMarkedFutureExecutor getFor(@NonNull MinecraftServer server) {
        return ((Holder) server).fp2_ServerThreadMarkedFutureExecutor$Holder_get();
    }

    /**
     * @deprecated internal API, do not touch!
     */
    @Deprecated
    public ServerThreadMarkedFutureExecutor(@NonNull MinecraftServer server) {
        super(server.serverThread);
        this.start();
    }

    @Override
    protected synchronized void start() {
        super.start();
        WorldWorkerManager.addWorker(this);
    }

    @Override
    public boolean hasWork() {
        return this.running;
    }

    @Override
    public boolean doWork() {
        return this.hasWork() && super.doWork();
    }

    /**
     * @author DaPorkchop_
     * @deprecated internal API, do not touch!
     */
    @Deprecated
    public interface Holder {
        ServerThreadMarkedFutureExecutor fp2_ServerThreadMarkedFutureExecutor$Holder_get();
    }
}
