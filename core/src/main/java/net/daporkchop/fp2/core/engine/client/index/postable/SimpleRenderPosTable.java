/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.core.engine.client.index.postable;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.NewAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.attribute.NewAttributeWriter;

import java.util.BitSet;

/**
 * @author DaPorkchop_
 */
public final class SimpleRenderPosTable extends RenderPosTable {
    private final Object2IntMap<TilePos> positionToIndex = DirectTilePosAccess.newPositionKeyed2IntHashMap();
    private final BitSet indexAlloc = new BitSet();

    private final NewAttributeWriter<VoxelGlobalAttributes> writer;
    private final NewAttributeBuffer<VoxelGlobalAttributes> buffer;

    private boolean dirty;

    public SimpleRenderPosTable(OpenGL gl, NewAttributeFormat<VoxelGlobalAttributes> format, DirectMemoryAllocator alloc) {
        this.positionToIndex.defaultReturnValue(-1);

        this.writer = format.createWriter(alloc);
        this.buffer = format.createBuffer();

        this.writer.appendUninitialized(256); //initial capacity
    }

    @Override
    public void close() {
        this.writer.close();
        this.buffer.close();
    }

    @Override
    public int add(@NonNull TilePos pos) {
        int index = this.positionToIndex.getInt(pos);
        if (index < 0) { //add the position to the table
            index = this.indexAlloc.nextClearBit(0);
            this.indexAlloc.set(index);
            this.positionToIndex.put(pos, index);

            if (index == this.writer.size()) { //the table needs to be grown, double the size of the writer
                this.writer.appendUninitialized(this.writer.size());
            }

            this.writer.at(index).tilePos(pos.x(), pos.y(), pos.z(), pos.level()).close();
            this.dirty = true;
        }
        return index;
    }

    @Override
    public void remove(TilePos pos) {
        int index = this.positionToIndex.removeInt(pos);
        if (index >= 0) { //the position was in the table before, clear the corresponding bit to free up the slot for use by another position
            this.indexAlloc.clear(index);
            //don't need to mark dirty, as we didn't actually modify any data
        }
    }

    @Override
    public void flush() {
        if (this.dirty) {
            this.dirty = false;
            this.buffer.set(this.writer, BufferUsage.STATIC_DRAW);
        }
    }

    @Override
    public NewAttributeBuffer<VoxelGlobalAttributes> vertexBuffer(int level) {
        return this.buffer;
    }
}
