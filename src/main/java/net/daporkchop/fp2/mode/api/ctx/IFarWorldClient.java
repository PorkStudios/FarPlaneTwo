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

package net.daporkchop.fp2.mode.api.ctx;

import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.util.annotation.CalledFromClientThread;
import net.daporkchop.fp2.util.annotation.CalledFromNetworkThread;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Provides access to {@link IFarClientContext} instances in a {@link WorldClient}.
 *
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public interface IFarWorldClient extends IFarWorld {
    @CalledFromNetworkThread
    void fp2_IFarWorldClient_updateConfig(FP2Config serverConfig, FP2Config mergedConfig);

    FP2Config fp2_IFarWorldClient_serverConfig();
    
    FP2Config fp2_IFarWorldClient_config();

    @CalledFromNetworkThread
    void fp2_IFarWorldClient_beginSession();

    @CalledFromNetworkThread
    void fp2_IFarWorldClient_endSession();

    /**
     * @return the currently active render context, or {@code null} if none are active
     */
    <POS extends IFarPos, T extends IFarTile> IFarClientContext<POS, T> fp2_IFarWorldClient_activeContext();

    @CalledFromClientThread
    @Override
    void fp2_IFarWorld_init();

    @CalledFromClientThread
    @Override
    void fp2_IFarWorld_close();
}
