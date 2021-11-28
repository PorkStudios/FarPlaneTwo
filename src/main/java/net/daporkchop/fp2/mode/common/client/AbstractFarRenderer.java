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

package net.daporkchop.fp2.mode.common.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.impl.mc.forge1_12_2.ResourceProvider1_12_2;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.core.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.common.client.strategy.IFarRenderStrategy;
import net.daporkchop.fp2.util.annotation.DebugOnly;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockRenderLayer;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarRenderer<POS extends IFarPos, T extends IFarTile> extends AbstractReleasable implements IFarRenderer {
    protected final IFarClientContext<POS, T> context;
    protected final IFarRenderMode<POS, T> mode;

    protected final GL gl;

    protected final BakeManager<POS, T> bakeManager;

    protected final IFarRenderStrategy<POS, T, ?, ?, ?> strategy;

    public AbstractFarRenderer(@NonNull IFarClientContext<POS, T> context) {
        this.context = context;
        this.mode = context.mode();

        this.gl = GL.builder()
                .withResourceProvider(new ResourceProvider1_12_2(MC))
                .wrapCurrent();

        this.strategy = this.strategy0();
        this.bakeManager = this.bakeManager0();
    }

    /**
     * @return the {@link IFarRenderStrategy} used by this renderer
     */
    protected abstract IFarRenderStrategy<POS, T, ?, ?, ?> strategy0();

    /**
     * @return a new {@link BakeManager}
     */
    protected BakeManager<POS, T> bakeManager0() {
        return new BakeManager<>(this, this.context.tileCache());
    }

    @Override
    public void prepare(float partialTicks, @NonNull Minecraft mc, @NonNull IFrustum frustum) {
        checkGLError("pre fp2 select");

        this.gl.runCleanup();
        this.bakeManager.index.select(frustum, partialTicks);

        checkGLError("post fp2 select");
    }

    @Override
    public void render(@NonNull Minecraft mc, @NonNull BlockRenderLayer layer, boolean pre) {
        checkGLError("pre fp2 render");

        this.strategy.render(uncheckedCast(this.bakeManager.index), layer, pre);

        checkGLError("post fp2 render");
    }

    @DebugOnly
    @Override
    public DebugStats.Renderer stats() {
        return this.bakeManager.index.stats();
    }

    @Override
    protected void doRelease() {
        this.strategy.release();
        this.bakeManager.release();
        this.gl.close();
    }
}
