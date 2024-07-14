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
import lombok.val;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.NewAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.attribute.NewAttributeWriter;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.BitSet;

/**
 * Implementation of {@link RenderPosTable} which stores all the tile positions in a single buffer, in no particular order.
 *
 * @author DaPorkchop_
 */
public final class SimpleRenderPosTable extends RenderPosTable {
    private final Object2IntMap<TilePos> positionToIndex = DirectTilePosAccess.newPositionKeyed2IntHashMap();
    private final BitSet indexAlloc = new BitSet();

    private final NewAttributeWriter<VoxelGlobalAttributes> writer;
    private final NewAttributeBuffer<VoxelGlobalAttributes> buffer;

    private final Allocator.GrowFunction growFunction;

    private boolean dirty;

    /**
     * @param growFunction the {@link Allocator.GrowFunction} used to determine the table capacity. Units are in tile positions, not bytes!
     */
    public SimpleRenderPosTable(OpenGL gl, NewAttributeFormat<VoxelGlobalAttributes> format, DirectMemoryAllocator alloc,
                                Allocator.GrowFunction growFunction) {
        this.positionToIndex.defaultReturnValue(-1);
        this.growFunction = growFunction;

        try {
            this.writer = format.createWriter(alloc);
            this.buffer = format.createBuffer();
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(this.writer, this.buffer);
    }

    private void grow(int increment) {
        int oldCapacity = this.writer.size();
        int newCapacity = Math.toIntExact(this.growFunction.grow(oldCapacity, increment));
        assert oldCapacity < newCapacity : oldCapacity + " -> " + newCapacity;

        this.writer.appendUninitialized(newCapacity - oldCapacity);
        assert this.writer.size() == newCapacity : this.writer.size() + " != " + newCapacity;

        //initialize the memory
        //TODO: this isn't necessary yet, but if we do decide to do this we'll need to reset to -1 in remove() as well
        /*for (int i = oldCapacity; i < newCapacity; i++) {
            this.writer.at(i).tilePos(-1, -1, -1, -1).close();
        }*/
    }

    @Override
    public int add(@NonNull TilePos pos) {
        int index = this.positionToIndex.getInt(pos);
        if (index < 0) { //add the position to the table
            index = this.indexAlloc.nextClearBit(0);
            this.indexAlloc.set(index);
            this.positionToIndex.put(pos, index);

            if (index == this.writer.size()) { //the table needs to be grown
                this.grow(1);
            }

            this.writer.at(index).tilePos(pos.x(), pos.y(), pos.z(), pos.level()).close();
            this.dirty = true;
        }
        return index;
    }

    @Override
    public int remove(TilePos pos) {
        int index = this.positionToIndex.removeInt(pos);
        if (index >= 0) { //the position was in the table before, clear the corresponding bit to free up the slot for use by another position
            this.indexAlloc.clear(index);
            //don't need to mark dirty, as we didn't actually modify any data
        }
        return index;
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
