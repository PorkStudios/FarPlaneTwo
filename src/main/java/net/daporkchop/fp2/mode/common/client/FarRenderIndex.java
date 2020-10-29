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
    protected IntBuffer bufferCutout = Constants.createIntBuffer(5);
    protected int sizeCutout = 0;
    protected IntBuffer bufferTranslucent = Constants.createIntBuffer(5);
    protected int sizeTranslucent = 0;

    protected final int indicesSize;
    protected final int vertexSize;

    public int[] mark() {
        return new int[] {this.sizeOpaque, this.sizeCutout, this.sizeTranslucent};
    }

    public void restore(int[] mark) {
        this.bufferOpaque.position((this.sizeOpaque = mark[0]) * 5);
        this.bufferCutout.position((this.sizeCutout = mark[1]) * 5);
        this.bufferTranslucent.position((this.sizeTranslucent = mark[2]) * 5);
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
        if (tree.checkFlagsAND(node, FLAG_CUTOUT)) {
            this.ensureCutoutWritable(5);

            long data = node + tree.data;
            this.bufferCutout.put(PUnsafe.getInt(data + RENDERDATA_CUTOUT_INDICES + GPUBUFFER_SIZE)) //count
                    .put(1) //instanceCount
                    .put(PUnsafe.getInt(data + RENDERDATA_CUTOUT_INDICES + GPUBUFFER_OFF)) //firstIndex
                    .put(PUnsafe.getInt(data + RENDERDATA_VERTICES + GPUBUFFER_OFF)) //baseVertex
                    .put(0); //baseInstance

            this.sizeCutout++;
        }
        if (tree.checkFlagsAND(node, FLAG_TRANSLUCENT)) {
            this.ensureTranslucentWritable(5);

            long data = node + tree.data;
            this.bufferTranslucent.put(PUnsafe.getInt(data + RENDERDATA_TRANSLUCENT_INDICES + GPUBUFFER_SIZE)) //count
                    .put(1) //instanceCount
                    .put(PUnsafe.getInt(data + RENDERDATA_TRANSLUCENT_INDICES + GPUBUFFER_OFF)) //firstIndex
                    .put(PUnsafe.getInt(data + RENDERDATA_VERTICES + GPUBUFFER_OFF)) //baseVertex
                    .put(0); //baseInstance

            this.sizeTranslucent++;
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

    protected void ensureCutoutWritable(int count) {
        while (this.bufferCutout.remaining() < count) { //buffer doesn't have enough space, grow it
            IntBuffer bigger = Constants.createIntBuffer(this.bufferCutout.capacity() << 1);
            this.bufferCutout.flip();
            bigger.put(this.bufferCutout);
            PUnsafe.pork_releaseBuffer(this.bufferCutout);
            this.bufferCutout = bigger;
        }
    }

    protected void ensureTranslucentWritable(int count) {
        while (this.bufferTranslucent.remaining() < count) { //buffer doesn't have enough space, grow it
            IntBuffer bigger = Constants.createIntBuffer(this.bufferTranslucent.capacity() << 1);
            this.bufferTranslucent.flip();
            bigger.put(this.bufferTranslucent);
            PUnsafe.pork_releaseBuffer(this.bufferTranslucent);
            this.bufferTranslucent = bigger;
        }
    }

    public void reset() {
        this.bufferOpaque.clear();
        this.sizeOpaque = 0;
        this.bufferCutout.clear();
        this.sizeCutout = 0;
        this.bufferTranslucent.clear();
        this.sizeTranslucent = 0;
    }

    public boolean isEmpty() {
        return this.sizeOpaque == 0 && this.sizeCutout == 0 && this.sizeTranslucent == 0;
    }

    public int[] upload(DrawIndirectBuffer drawCommandBufferOpaque, DrawIndirectBuffer drawCommandBufferCutout,  DrawIndirectBuffer drawCommandBufferTranslucent) {
        if (!this.isEmpty()) {
            try (DrawIndirectBuffer buffer = drawCommandBufferOpaque.bind()) {
                if (this.sizeOpaque > 0) {
                    this.bufferOpaque.flip();
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, this.bufferOpaque, GL_STREAM_DRAW);
                } else {
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, 0L, GL_STREAM_DRAW);
                }
            }
            try (DrawIndirectBuffer buffer = drawCommandBufferCutout.bind()) {
                if (this.sizeCutout > 0) {
                    this.bufferCutout.flip();
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, this.bufferCutout, GL_STREAM_DRAW);
                } else {
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, 0L, GL_STREAM_DRAW);
                }
            }
            try (DrawIndirectBuffer buffer = drawCommandBufferTranslucent.bind()) {
                if (this.sizeTranslucent > 0) {
                    this.bufferTranslucent.flip();
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, this.bufferTranslucent, GL_STREAM_DRAW);
                } else {
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, 0L, GL_STREAM_DRAW);
                }
            }
        }
        return this.mark();
    }
}
