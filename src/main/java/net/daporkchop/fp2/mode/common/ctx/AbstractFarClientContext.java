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
import net.daporkchop.fp2.config.IConfigListener;
import net.daporkchop.fp2.config.ConfigListenerManager;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.mode.api.client.IFarTileCache;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.common.client.FarTileCache;

/**
 * Base implementation of {@link IFarClientContext}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarClientContext<POS extends IFarPos, T extends IFarTile> implements IFarClientContext<POS, T>, IConfigListener {
    protected final IFarTileCache<POS, T> tileCache;
    protected IFarRenderer renderer;
    protected final IFarRenderMode<POS, T> mode;

    public AbstractFarClientContext(@NonNull IFarRenderMode<POS, T> mode) {
        this.mode = mode;

        this.tileCache = this.tileCache0();
        this.configChanged();

        ConfigListenerManager.add(this);
    }

    protected IFarTileCache<POS, T> tileCache0() {
        return new FarTileCache<>();
    }

    protected abstract IFarRenderer renderer0(IFarRenderer old);

    @Override
    public void configChanged() {
        IFarRenderer oldRenderer = this.renderer;
        IFarRenderer newRenderer = this.renderer0(oldRenderer);
        if (oldRenderer != newRenderer) {
            this.renderer = newRenderer;
            if (oldRenderer != null) {
                oldRenderer.close();
            }
        }
    }

    @Override
    public void close() {
        ConfigListenerManager.remove(this);

        IFarClientContext.super.close();
    }
}
