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

package net.daporkchop.fp2.core.client.player;

import lombok.NonNull;
import net.daporkchop.fp2.api.world.FWorldClient;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;

/**
 * @author DaPorkchop_
 */
public interface IFarPlayerClient extends AutoCloseable {
    FP2Core fp2();

    @CalledFromAnyThread
    void handle(@NonNull Object packet);

    @CalledFromAnyThread
    void send(@NonNull IPacket packet);

    @CalledFromAnyThread
    DebugStats.Tracking debugServerStats();

    @CalledFromClientThread
    void ready();

    FWorldClient world();

    /**
     * @return the server's config, or {@code null} if none/currently unknown
     */
    @CalledFromAnyThread
    FP2Config serverConfig();

    /**
     * @return the current effective config (merged result of client and server config), or {@code null} if none
     */
    @CalledFromAnyThread
    FP2Config config();

    /**
     * @return the currently active render context, or {@code null} if none are active
     */
    @CalledFromAnyThread
    <POS extends IFarPos, T extends IFarTile> IFarClientContext activeContext();

    /**
     * Closes this player, causing the active session (if any) to be closed.
     */
    @CalledFromAnyThread
    @Override
    void close();
}
