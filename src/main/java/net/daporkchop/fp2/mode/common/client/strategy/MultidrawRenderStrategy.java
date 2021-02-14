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

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.render.IDrawMode;
import net.daporkchop.fp2.client.render.IndirectMultiDrawMode;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.common.client.BakeOutput;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class MultidrawRenderStrategy<POS extends IFarPos, P extends IFarPiece> extends IndexedRenderStrategy<POS, P> {
    protected final IDrawMode[] layers = new IDrawMode[RENDER_PASS_COUNT];

    public MultidrawRenderStrategy(int vertexSize) {
        super(vertexSize);

        for (int i = 0; i < RENDER_PASS_COUNT; i++) {
            this.layers[i] = new IndirectMultiDrawMode(vao -> {
                this.configureVertexAttributes(this.vertices, vao);
                vao.putElementArray(this.indices);
            });
        }
    }

    protected abstract void configureVertexAttributes(@NonNull IGLBuffer buffer, @NonNull VertexArrayObject vao);

    @Override
    protected abstract void bakeVertsAndIndices(@NonNull POS pos, @NonNull P[] srcs, @NonNull BakeOutput output, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices);

    @Override
    public void render(long tilev, int tilec) {
        for (IDrawMode layer : this.layers) {
            layer.begin();
        }
        try {
            for (int i = 0; i < tilec; i++, tilev += LONG_SIZE) {
                this.drawTile(PUnsafe.getLong(tilev));
            }
        } finally {
            for (IDrawMode layer : this.layers) {
                layer.close();
            }
        }

        this.draw();
    }

    protected abstract void drawTile(long tile);

    protected abstract void draw();
}
