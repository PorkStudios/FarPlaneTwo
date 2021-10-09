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

package net.daporkchop.fp2.mode.common.ctx;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.api.ctx.IFarWorldClient;
import net.daporkchop.fp2.mode.common.client.FarTileCache;
import net.daporkchop.fp2.util.annotation.CalledFromNetworkThread;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of {@link IFarClientContext}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarClientContext<POS extends IFarPos, T extends IFarTile> implements IFarClientContext<POS, T> {
    protected final IFarRenderMode<POS, T> mode;
    protected final IFarWorldClient world;
    protected final IFarTileCache<POS, T> tileCache;

    protected FP2Config config;
    protected IFarRenderer renderer;

    protected boolean closed = false;

    public AbstractFarClientContext(@NonNull IFarWorldClient world, @NonNull FP2Config config, @NonNull IFarRenderMode<POS, T> mode) {
        this.world = world;
        this.mode = mode;
        this.tileCache = this.tileCache0();

        this.notifyConfigChange(config);
    }

    protected IFarTileCache<POS, T> tileCache0() {
        return new FarTileCache<>();
    }

    protected abstract IFarRenderer renderer0(IFarRenderer old, @NonNull FP2Config config);

    @CalledFromNetworkThread
    @Override
    public void notifyConfigChange(@NonNull FP2Config config) {
        checkState(!this.closed, "already closed!");
        this.config = config;

        //check if we need to replace the renderer on the client thread
        this.world.fp2_IFarWorld_scheduleTask(() -> {
            if (this.config != config) { //config has changed since this task was scheduled, skip re-check since we're technically out-of-date
                return;
            }

            IFarRenderer oldRenderer = this.renderer;
            IFarRenderer newRenderer = this.renderer0(oldRenderer, config);
            if (oldRenderer != newRenderer) {
                this.renderer = newRenderer;
                if (oldRenderer != null) {
                    oldRenderer.close();
                }
            }
        });
    }

    @CalledFromNetworkThread
    @Override
    public void close() {
        checkState(!this.closed, "already closed!");
        this.closed = true;

        //do all cleanup on client thread
        this.world.fp2_IFarWorld_scheduleTask(() -> {
            if (this.renderer != null) {
                this.renderer.close();
                this.renderer = null;
            }

            this.tileCache.close();
        });
    }
}
