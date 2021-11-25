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

package net.daporkchop.fp2.mode.api.player;

import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.core.util.annotation.CalledFromNetworkThread;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public interface IFarPlayerClient {
    @CalledFromNetworkThread
    void fp2_IFarPlayerClient_handle(@NonNull Object packet);

    @CalledFromAnyThread
    void fp2_IFarPlayerClient_send(@NonNull IPacket packet);

    @CalledFromAnyThread
    DebugStats.Tracking fp2_IFarPlayerClient_debugServerStats();

    @CalledFromClientThread
    void fp2_IFarPlayerClient_ready();

    /**
     * @return the server's config, or {@code null} if none/currently unknown
     */
    @CalledFromAnyThread
    FP2Config fp2_IFarPlayerClient_serverConfig();

    /**
     * @return the current effective config (merged result of client and server config), or {@code null} if none
     */
    @CalledFromAnyThread
    FP2Config fp2_IFarPlayerClient_config();

    /**
     * @return the currently active render context, or {@code null} if none are active
     */
    @CalledFromAnyThread
    <POS extends IFarPos, T extends IFarTile> IFarClientContext<POS, T> fp2_IFarPlayerClient_activeContext();
}
