/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.client.gl.object.DrawIndirectBuffer;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.mode.common.client.AbstractFarRenderTree.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL40.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FarRenderIndex {
    protected IntBuffer bufferOpaque = Constants.createIntBuffer(5);
    protected int sizeOpaque = 0;
    protected IntBuffer bufferTransparent = Constants.createIntBuffer(5);
    protected int sizeTransparent = 0;

    protected final int indicesSize;
    protected final int vertexSize;

    public long mark() {
        return BinMath.packXY(this.sizeOpaque, this.sizeTransparent);
    }

    public void restore(long mark) {
        this.bufferOpaque.position((this.sizeOpaque = BinMath.unpackX(mark)) * 5);
        this.bufferTransparent.position((this.sizeTransparent = BinMath.unpackY(mark)) * 5);
    }

    public boolean add(AbstractFarRenderTree tree, long node) {
        if (!tree.checkFlagsAND(node, FLAG_RENDERED)) {
            return false;
        }

        if (tree.checkFlagsAND(node, FLAG_OPAQUE)) {
            this.ensureOpaqueWritable(5);

            long data = node + tree.data;
            this.bufferOpaque.put(PUnsafe.getInt(data + RENDERDATA_OPAQUE_INDICES + GPUBUFFER_SIZE)) //count
                    .put(1) //instanceCount
                    .put(PUnsafe.getInt(data + RENDERDATA_OPAQUE_INDICES + GPUBUFFER_OFF)) //firstIndex
                    .put(PUnsafe.getInt(data + RENDERDATA_VERTICES + GPUBUFFER_OFF)) //baseVertex
                    .put(0); //baseInstance

            this.sizeOpaque++;
        }
        if (tree.checkFlagsAND(node, FLAG_TRANSPARENT)) {
            this.ensureTransparentWritable(5);

            long data = node + tree.data;
            this.bufferTransparent.put(PUnsafe.getInt(data + RENDERDATA_TRANSPARENT_INDICES + GPUBUFFER_SIZE)) //count
                    .put(1) //instanceCount
                    .put(PUnsafe.getInt(data + RENDERDATA_TRANSPARENT_INDICES + GPUBUFFER_OFF)) //firstIndex
                    .put(PUnsafe.getInt(data + RENDERDATA_VERTICES + GPUBUFFER_OFF)) //baseVertex
                    .put(0); //baseInstance

            this.sizeTransparent++;
        }
        return true;
    }

    protected void ensureOpaqueWritable(int count) {
        while (this.bufferOpaque.remaining() < count) { //buffer doesn't have enough space, grow it
            IntBuffer bigger = Constants.createIntBuffer(this.bufferOpaque.capacity() << 1);
            this.bufferOpaque.flip();
            bigger.put(this.bufferOpaque);
            PUnsafe.pork_releaseBuffer(this.bufferOpaque);
            this.bufferOpaque = bigger;
        }
    }

    protected void ensureTransparentWritable(int count) {
        while (this.bufferTransparent.remaining() < count) { //buffer doesn't have enough space, grow it
            IntBuffer bigger = Constants.createIntBuffer(this.bufferTransparent.capacity() << 1);
            this.bufferTransparent.flip();
            bigger.put(this.bufferTransparent);
            PUnsafe.pork_releaseBuffer(this.bufferTransparent);
            this.bufferTransparent = bigger;
        }
    }

    public void reset() {
        this.bufferOpaque.clear();
        this.sizeOpaque = 0;
        this.bufferTransparent.clear();
        this.sizeTransparent = 0;
    }

    public boolean isEmpty() {
        return this.sizeOpaque == 0 && this.sizeTransparent == 0;
    }

    public long upload(DrawIndirectBuffer drawCommandBufferOpaque, DrawIndirectBuffer drawCommandBufferTransparent) {
        if (!this.isEmpty()) {
            try (DrawIndirectBuffer buffer = drawCommandBufferOpaque.bind()) {
                if (this.sizeOpaque > 0) {
                    this.bufferOpaque.flip();
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, this.bufferOpaque, GL_STREAM_DRAW);
                } else {
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, 0L, GL_STREAM_DRAW);
                }
            }
            try (DrawIndirectBuffer buffer = drawCommandBufferTransparent.bind()) {
                if (this.sizeTransparent > 0) {
                    this.bufferTransparent.flip();
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, this.bufferTransparent, GL_STREAM_DRAW);
                } else {
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, 0L, GL_STREAM_DRAW);
                }
            }
        }
        return this.mark();
    }
}
