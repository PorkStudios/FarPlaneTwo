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

package net.daporkchop.fp2.core.engine.api.ctx;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.client.AbstractFarRenderer;
import net.daporkchop.fp2.core.engine.client.FarTileCache;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;

/**
 * A client-side context for a rendering session in a {@link IFarLevelClient}.
 *
 * @author DaPorkchop_
 */
public interface IFarClientContext extends AutoCloseable {
    /**
     * @return the world
     */
    IFarLevelClient level();

    /**
     * @return a cache for tiles used by this context
     */
    FarTileCache tileCache();

    /**
     * @return the renderer currently used by this context
     */
    AbstractFarRenderer renderer();

    /**
     * @return the config currently being used
     */
    FP2Config config();

    /**
     * Called whenever the player's config is changed.
     *
     * @param config the new config
     */
    @CalledFromAnyThread
    void notifyConfigChange(@NonNull FP2Config config);

    /**
     * Closes this context, releasing any allocated resources.
     */
    @CalledFromAnyThread
    @Override
    void close();
}
