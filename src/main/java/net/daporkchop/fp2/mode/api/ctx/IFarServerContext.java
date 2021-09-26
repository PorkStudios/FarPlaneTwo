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

import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.util.IFarPlayer;
import net.daporkchop.fp2.util.annotation.CalledFromServerThread;

/**
 * A server-side context for a specific {@link IFarPlayer} in a {@link IFarWorldServer} using a specific {@link IFarRenderMode}.
 *
 * @author DaPorkchop_
 */
public interface IFarServerContext<POS extends IFarPos, T extends IFarTile> extends AutoCloseable {
    /**
     * @return the player which this context belongs to
     */
    IFarPlayer player();

    /**
     * @return the vanilla world
     */
    IFarWorldServer world();

    /**
     * @return the render mode
     */
    IFarRenderMode<POS, T> mode();

    /**
     * @return the {@link IFarTileProvider} used in this context
     */
    IFarTileProvider<POS, T> tileProvider();

    /**
     * @return the config currently being used
     */
    FP2Config config();

    /**
     * Called whenever the player's config is changed.
     *
     * @param config the new config
     */
    @CalledFromServerThread
    void notifyConfigChange(@NonNull FP2Config config);

    /**
     * Closes this context, deactivating it if needed and releasing any allocated resources.
     */
    @CalledFromServerThread
    @Override
    void close();

    /**
     * Updates this context.
     * <p>
     * Called once per tick in order to schedule tracking updates, etc.
     */
    @CalledFromServerThread
    void update();
}
