/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.ctx;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.engine.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.engine.client.FarTileCache;
import net.daporkchop.fp2.core.engine.client.AbstractFarRenderer;
import net.daporkchop.fp2.core.util.annotation.CalledFromAnyThread;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class ClientContext implements IFarClientContext {
    protected final IFarLevelClient level;
    protected final long sessionId;

    protected final FarTileCache tileCache;

    protected FP2Config config;
    protected AbstractFarRenderer renderer;

    protected boolean closed = false;

    public ClientContext(@NonNull IFarLevelClient level, @NonNull FP2Config config, long sessionId) {
        this.level = level;
        this.sessionId = sessionId;
        this.tileCache = new FarTileCache();

        this.notifyConfigChange(config);
    }

    private AbstractFarRenderer renderer0(AbstractFarRenderer old, @NonNull FP2Config config) {
        /*if (OFHelper.of_Config_isShaders()) {
            return old; //TODO: transform feedback renderer
        } else {*/
        return old instanceof AbstractFarRenderer.ShaderMultidraw ? old : new AbstractFarRenderer.ShaderMultidraw(this);
        //}
    }

    @Override
    public synchronized void reloadRenderer() {
        checkState(!this.closed, "already closed!");

        this.level.workerManager().rootExecutor().execute(() -> {
            AbstractFarRenderer oldRenderer = this.renderer;
            this.renderer = null;
            oldRenderer.close();

            this.renderer = this.renderer0(null, this.config);
        });
    }

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

            AbstractFarRenderer oldRenderer = this.renderer;
            AbstractFarRenderer newRenderer = this.renderer0(oldRenderer, config);
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
                 AbstractFarRenderer renderer = this.renderer) {
                this.renderer = null;
            }
        });
    }
}
