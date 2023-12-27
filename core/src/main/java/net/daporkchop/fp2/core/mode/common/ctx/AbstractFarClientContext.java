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

package net.daporkchop.fp2.core.mode.common.ctx;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.client.VoxelRenderer;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.engine.client.FarTileCache;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of {@link IFarClientContext}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarClientContext implements IFarClientContext {
    protected final IFarLevelClient level;
    protected final FarTileCache tileCache;

    protected FP2Config config;
    protected VoxelRenderer renderer;

    protected boolean closed = false;

    public AbstractFarClientContext(@NonNull IFarLevelClient level, @NonNull FP2Config config) {
        this.level = level;
        this.tileCache = new FarTileCache();

        this.notifyConfigChange(config);
    }

    protected abstract VoxelRenderer renderer0(VoxelRenderer old, @NonNull FP2Config config);

    @CalledFromAnyThread
    @Override
    public synchronized void notifyConfigChange(@NonNull FP2Config config) {
        checkState(!this.closed, "already closed!");
        this.config = config;

        //check if we need to replace the renderer on the client thread
        this.level.workerManager().rootExecutor().execute(() -> {
            if (this.config != config) { //config has changed since this task was scheduled, skip re-check since we're technically out-of-date
                return;
            }

            VoxelRenderer oldRenderer = this.renderer;
            VoxelRenderer newRenderer = this.renderer0(oldRenderer, config);
            if (oldRenderer != newRenderer) {
                this.renderer = newRenderer;
                if (oldRenderer != null) {
                    oldRenderer.close();
                }
            }
        });
    }

    @CalledFromAnyThread
    @Override
    public synchronized void close() {
        checkState(!this.closed, "already closed!");
        this.closed = true;

        //do all cleanup on client thread
        this.level.workerManager().rootExecutor().execute(() -> {
            //try-with-resources to make sure everything gets cleaned up
            try (FarTileCache tileCache = this.tileCache;
                 VoxelRenderer renderer = this.renderer) {
                this.renderer = null;
            }
        });
    }
}
