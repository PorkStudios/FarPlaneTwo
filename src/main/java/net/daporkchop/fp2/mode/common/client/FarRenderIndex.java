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
import net.daporkchop.fp2.client.gl.object.DrawIndirectBuffer;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.common.client.AbstractFarRenderTree.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL40.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class FarRenderIndex {
    protected final IntBuffer[] buffers;
    protected final int[] sizes;

    //serve marks out of a fixed array rather than allocating a new one each time
    //the maximum number of marks is equal to the maximum octree depth, which is fine because there can't be more than that many marks anyway
    protected final int[] marks;
    protected int markIndex;

    protected final int vertexSize;
    protected final int indexSize;
    protected final int passes;

    public FarRenderIndex(AbstractFarRenderCache cache) {
        this.indexSize = cache.indexSize();
        this.vertexSize = cache.vertexSize();
        this.passes = cache.passes();

        this.buffers = new IntBuffer[this.passes];
        for (int i = 0; i < this.passes; i++) {
            this.buffers[i] = Constants.createIntBuffer(5);
        }
        this.sizes = new int[this.passes];

        this.marks = new int[this.passes * DEPTH];
    }

    /**
     * Marks the current index state.
     *
     * @return the mark
     */
    public int mark() {
        int mark = this.markIndex;
        System.arraycopy(this.sizes, 0, this.marks, mark, this.passes);
        this.markIndex = mark + this.passes;
        return mark;
    }

    /**
     * Restores the index state from the given mark.
     * <p>
     * This method will implicitly release the mark.
     *
     * @param mark the mark to reset the index state to
     */
    public void restoreMark(int mark) {
        System.arraycopy(this.marks, this.markIndex = mark, this.sizes, 0, this.passes);
        for (int i = 0; i < this.passes; i++) {
            this.buffers[i].position(this.sizes[i] * 5);
        }
    }

    /**
     * Releases the given mark.
     * <p>
     * Must be called once a mark goes out of scope!
     *
     * @param mark the mark to release
     */
    public void releaseMark(int mark) {
        this.markIndex = mark;
    }

    public boolean add(AbstractFarRenderTree tree, long node) {
        if (!tree.checkFlagsAND(node, FLAG_RENDERED)) {
            return false;
        } else if (!tree.checkFlagsAND(node, FLAG_DATA)) {
            //the node is rendered and has no data, which means it's all air and therefore can be considered to have already
            // been "effectively" added to the render index
            return true;
        }

        long data = node + tree.data;
        int baseVertex = PUnsafe.getInt(data + RENDERDATA_VOFFSET);

        int firstIndex = PUnsafe.getInt(data + RENDERDATA_IOFFSET);
        for (int i = 0; i < this.passes; i++) {
            int indexCount = PUnsafe.getInt(data + RENDERDATA_ICOUNT + i * 4L);
            if (indexCount != 0) {
                this.ensureWritable(i, 5);
                this.buffers[i].put(indexCount) //count
                        .put(1) //instanceCount
                        .put(firstIndex) //firstIndex
                        .put(baseVertex) //baseVertex
                        .put(0); //baseInstance
                this.sizes[i]++;

                firstIndex += indexCount;
            }
        }
        return true;
    }

    protected void ensureWritable(int pass, int count) {
        while (this.buffers[pass].remaining() < count) { //buffer doesn't have enough space, grow it
            IntBuffer bigger = Constants.createIntBuffer(this.buffers[pass].capacity() << 1);
            this.buffers[pass].flip();
            bigger.put(this.buffers[pass]);
            PUnsafe.pork_releaseBuffer(this.buffers[pass]);
            this.buffers[pass] = bigger;
        }
    }

    public void reset() {
        for (int i = 0; i < this.passes; i++) {
            this.buffers[i].clear();
            this.sizes[i] = 0;
        }
    }

    public boolean isEmpty() {
        return or(this.sizes) == 0;
    }

    public int[] upload(@NonNull DrawIndirectBuffer[] drawCommandBuffers) {
        checkGLError("pre upload index");
        for (int i = 0; i < this.passes; i++) {
            try (DrawIndirectBuffer buffer = drawCommandBuffers[i].bind()) {
                if (this.sizes[i] > 0) {
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, (IntBuffer) this.buffers[i].flip(), GL_STREAM_DRAW);
                } else {
                    glBufferData(GL_DRAW_INDIRECT_BUFFER, 0L, GL_STREAM_DRAW);
                }
            }
        }
        checkGLError("post upload index");
        return this.sizes;
    }
}
