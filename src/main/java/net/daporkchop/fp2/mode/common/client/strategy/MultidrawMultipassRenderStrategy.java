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

package net.daporkchop.fp2.mode.common.client.strategy;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.commandbuffer.IDrawCommandBuffer;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.common.client.IMultipassRenderStrategy;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class MultidrawMultipassRenderStrategy<POS extends IFarPos, P extends IFarPiece> extends MultidrawRenderStrategy<POS, P> implements IMultipassRenderStrategy<POS, P> {
    protected final IDrawCommandBuffer[] passes = new IDrawCommandBuffer[RENDER_PASS_COUNT];

    public MultidrawMultipassRenderStrategy(int vertexSize) {
        super(vertexSize);

        PArrays.fill(this.passes, this::createCommandBuffer);
    }

    @Override
    public void render(long tilev, int tilec) {
        for (IDrawCommandBuffer pass : this.passes) { //begin all passes
            pass.begin();
        }
        try {
            for (long end = tilev + (long) LONG_SIZE * tilec; tilev != end; tilev += LONG_SIZE) { //draw each tile individually
                this.drawTile(this.passes, PUnsafe.getLong(tilev));
            }
        } finally {
            for (IDrawCommandBuffer pass : this.passes) { //finish all passes
                pass.close();
            }
        }

        //render completed passes
        this.renderMultipass(this.passes);
    }

    protected abstract void drawTile(@NonNull IDrawCommandBuffer[] passes, long tile);
}
