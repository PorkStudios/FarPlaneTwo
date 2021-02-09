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

import net.daporkchop.fp2.client.AllocatedGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.render.IDrawMode;
import net.daporkchop.fp2.client.render.IndirectMultiDrawMode;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * @author DaPorkchop_
 */
public abstract class MultidrawRenderStrategy<POS extends IFarPos, P extends IFarPiece> extends IndexedRenderStrategy<POS, P> {
    protected final VertexArrayObject vao = new VertexArrayObject();
    protected final IDrawMode drawSolid;

    public MultidrawRenderStrategy(int vertexSize, int vertexAttributeCount) {
        super(vertexSize);

        try (VertexArrayObject vao = this.vao.bind()) {
            for (int i = 0; i <= vertexAttributeCount; i++) {
                glEnableVertexAttribArray(i);
            }

            this.drawSolid = new IndirectMultiDrawMode(0, vao);

            try (AllocatedGLBuffer vbo = this.vertices.bind(GL_ARRAY_BUFFER)) {
                this.configureVertexAttributes();
                for (int i = 1; i <= vertexAttributeCount; i++) {
                    vao.putDependency(i, vbo);
                }
            }

            vao.putElementArray(this.indices.bind(GL_ELEMENT_ARRAY_BUFFER));
        } finally {
            for (int i = 0; i <= vertexAttributeCount; i++) {
                glDisableVertexAttribArray(i);
            }

            this.indices.close();
        }
    }

    protected abstract void configureVertexAttributes();

    @Override
    public void render(long refv, int refc) {
        try (IDrawMode output = this.drawSolid.begin()) {
            for (int i = 0; i < refc; i++, refv += LONG_SIZE) {
                this.drawRef(output, PUnsafe.getLong(refv));
            }
        }
    }
}
