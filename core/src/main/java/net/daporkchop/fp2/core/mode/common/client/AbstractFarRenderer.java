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

package net.daporkchop.fp2.core.mode.common.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.LevelRenderer;
import net.daporkchop.fp2.core.client.render.RenderInfo;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.mode.common.client.strategy.IFarRenderStrategy;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.lib.common.misc.release.AbstractReleasable;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarRenderer extends AbstractReleasable {
    public static final int LAYER_SOLID = 0;
    public static final int LAYER_CUTOUT = LAYER_SOLID + 1;
    public static final int LAYER_TRANSPARENT = LAYER_CUTOUT + 1;

    public static final int LAYERS = LAYER_TRANSPARENT + 1;

    protected final LevelRenderer levelRenderer;
    protected final GL gl;

    protected final IFarClientContext context;
    protected final IFarRenderMode mode;

    protected final BakeManager bakeManager;

    protected final IFarRenderStrategy<?, ?, ?> strategy;

    public AbstractFarRenderer(@NonNull IFarClientContext context) {
        this.context = context;
        this.mode = context.mode();

        this.levelRenderer = context.level().renderer();
        this.gl = this.levelRenderer.gl();

        this.strategy = this.strategy0();
        this.bakeManager = this.bakeManager0();
    }

    /**
     * @return the {@link IFarRenderStrategy} used by this renderer
     */
    protected abstract IFarRenderStrategy<?, ?, ?> strategy0();

    /**
     * @return a new {@link BakeManager}
     */
    protected BakeManager bakeManager0() {
        return new BakeManager(this, this.context.tileCache());
    }

    public void prepare(@NonNull IFrustum frustum) {
        this.gl.runCleanup();
        this.bakeManager.index.select(frustum);
    }

    public void render(@NonNull RenderInfo renderInfo) {
        this.strategy.render(uncheckedCast(this.bakeManager.index), renderInfo);
    }

    public DebugStats.Renderer stats() {
        return this.bakeManager.index.stats();
    }

    @Override
    protected void doRelease() {
        this.strategy.release();
        this.bakeManager.release();
    }
}
