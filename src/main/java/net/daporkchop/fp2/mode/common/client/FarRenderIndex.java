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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.mode.common.client.AbstractFarRenderTree.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class FarRenderIndex {
    protected IntBuffer buffer = Constants.createIntBuffer(256);
    protected int size = 0;

    protected final int indicesSize;
    protected final int vertexSize;

    public int mark() {
        return this.size;
    }

    public void restore(int mark) {
        this.buffer.position(mark * 5);
        this.size = mark;
    }

    @Deprecated
    public boolean add(@NonNull AbstractFarRenderTile tile) {
        if (!tile.rendered) {
            return false;
        }

        if (tile.renderOpaque != null) { //TODO: transparent data as well
            this.ensureWritable(5);

            FarRenderData data = tile.renderOpaque;
            this.buffer.put(data.sizeIndices / this.indicesSize) //count
                    .put(1) //instanceCount
                    .put(toInt(data.gpuIndices / this.indicesSize)) //firstIndex
                    .put(toInt(data.gpuVertices / this.vertexSize)) //baseVertex
                    .put(0); //baseInstance

            this.size++;
        }
        return true;
    }

    public boolean add(AbstractFarRenderTree tree, long node) {
        if (!tree.checkFlags(node, FLAG_RENDERED)) {
            return false;
        }

        if (tree.checkFlags(node, FLAG_OPAQUE)) { //TODO: transparent data as well
            this.ensureWritable(5);

            long data = node + tree.opaque;
            this.buffer.put(PUnsafe.getInt(data + RENDERDATA_INDICES + GPUBUFFER_SIZE)) //count
                    .put(1) //instanceCount
                    .put(PUnsafe.getInt(data + RENDERDATA_INDICES + GPUBUFFER_OFF)) //firstIndex
                    .put(PUnsafe.getInt(data + RENDERDATA_VERTICES + GPUBUFFER_OFF)) //baseVertex
                    .put(0); //baseInstance

            this.size++;
        }
        return true;
    }

    protected void ensureWritable(int count) {
        while (this.buffer.remaining() < count) { //buffer doesn't have enough space, grow it
            IntBuffer bigger = Constants.createIntBuffer(this.buffer.capacity() << 1);
            this.buffer.flip();
            bigger.put(this.buffer);
            PUnsafe.pork_releaseBuffer(this.buffer);
            this.buffer = bigger;
        }
    }

    public void reset() {
        this.buffer.clear();
        this.size = 0;
    }

    public void upload(int slot) {
        if (this.size > 0) {
            this.buffer.flip();
            glBufferData(slot, this.buffer, GL_STREAM_DRAW);
        } else {
            glBufferData(slot, 0L, GL_STREAM_DRAW);
        }
    }
}
