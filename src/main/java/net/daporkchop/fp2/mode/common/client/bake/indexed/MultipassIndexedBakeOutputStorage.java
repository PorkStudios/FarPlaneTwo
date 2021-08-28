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

package net.daporkchop.fp2.mode.common.client.bake.indexed;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.ElementType;
import net.daporkchop.fp2.client.gl.command.elements.DrawElementsCommand;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuffer;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexLayout;
import net.daporkchop.fp2.mode.common.client.bake.AbstractMultipassBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.bake.IMultipassBakeOutputStorage;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.SequentialFixedSizeAllocator;
import net.daporkchop.fp2.util.alloc.SequentialVariableSizedAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * Implementation of {@link IMultipassBakeOutputStorage} which contains indexed geometry in multiple render passes.
 *
 * @author DaPorkchop_
 */
public class MultipassIndexedBakeOutputStorage extends AbstractMultipassBakeOutputStorage<MultipassIndexedBakeOutput, DrawElementsCommand> {
    /*
     * struct Slot {
     *   int baseVertex;
     *   Pass passes[PASSES];
     * };
     *
     * struct Pass {
     *   int firstIndex;
     *   int count;
     * };
     */

    protected static final long _SLOT_BASEVERTEX_OFFSET = 0L;
    protected static final long _SLOT_PASSES_OFFSET = _SLOT_BASEVERTEX_OFFSET + INT_SIZE;

    protected static final long _PASS_FIRSTINDEX_OFFSET = 0L;
    protected static final long _PASS_COUNT_OFFSET = _PASS_FIRSTINDEX_OFFSET + INT_SIZE;
    protected static final long _PASS_SIZE = _PASS_COUNT_OFFSET + INT_SIZE;

    protected static int _slot_baseVertex(long slot) {
        return PUnsafe.getInt(slot + _SLOT_BASEVERTEX_OFFSET);
    }

    protected static void _slot_baseVertex(long slot, int baseVertex) {
        PUnsafe.putInt(slot + _SLOT_BASEVERTEX_OFFSET, baseVertex);
    }

    protected static long _slot_pass(long slot, int pass) {
        return slot + _SLOT_PASSES_OFFSET + pass * _PASS_SIZE;
    }

    protected static long _SLOT_SIZE(int PASSES) {
        return _SLOT_PASSES_OFFSET + PASSES * _PASS_SIZE;
    }

    protected static int _pass_firstIndex(long pass) {
        return PUnsafe.getInt(pass + _PASS_FIRSTINDEX_OFFSET);
    }

    protected static void _pass_firstIndex(long pass, int firstIndex) {
        PUnsafe.putInt(pass + _PASS_FIRSTINDEX_OFFSET, firstIndex);
    }

    protected static int _pass_count(long pass) {
        return PUnsafe.getInt(pass + _PASS_COUNT_OFFSET);
    }

    protected static void _pass_count(long pass, int count) {
        PUnsafe.putInt(pass + _PASS_COUNT_OFFSET, count);
    }

    protected final Allocator alloc;

    protected final Allocator slotAlloc;
    protected final long slotSize;
    protected long slotsAddr;

    protected final Allocator vertexAlloc;
    protected final IVertexBuffer vertexBuffer;

    protected final int indexSize;
    protected final Allocator[] indexAllocs;
    protected final GLBuffer[] indexBuffers;

    @Getter
    protected final int passes;

    public MultipassIndexedBakeOutputStorage(@NonNull Allocator alloc, @NonNull IVertexLayout vertexLayout, @NonNull ElementType type, int passes) {
        this.alloc = alloc;
        this.passes = positive(passes, "passes");
        this.indexSize = type.size();

        this.slotSize = _SLOT_SIZE(passes);
        this.slotAlloc = new SequentialFixedSizeAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> this.slotsAddr = this.alloc.realloc(this.slotsAddr, capacity * this.slotSize)));

        this.vertexBuffer = vertexLayout.createBuffer();
        this.vertexAlloc = new SequentialVariableSizedAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> this.vertexBuffer.resize(toInt(capacity))));

        this.indexAllocs = new Allocator[passes];
        this.indexBuffers = new GLBuffer[passes];
        for (int pass = 0; pass < passes; pass++) {
            GLBuffer indexBuffer = this.indexBuffers[pass] = new GLBuffer(GL_DYNAMIC_DRAW);
            this.indexAllocs[pass] = new SequentialVariableSizedAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> {
                try (GLBuffer buffer = indexBuffer.bind(GL_ELEMENT_ARRAY_BUFFER)) {
                    buffer.resize(capacity);
                }
            }));
        }
    }

    @Override
    protected void doRelease() {
        this.vertexBuffer.release();
        for (GLBuffer indexBuffer : this.indexBuffers) {
            indexBuffer.delete();
        }
    }

    @Override
    public void configureVAO(@NonNull VertexArrayObject vao, int pass) {
        this.vertexBuffer.configureVAO(vao);
        vao.putElementArray(this.indexBuffers[pass]);
    }

    @Override
    public int add(@NonNull MultipassIndexedBakeOutput output) {
        //allocate a slot
        int handle = toInt(this.slotAlloc.alloc(1L));
        long slot = this.slotsAddr + handle * this.slotSize;

        //allocate and upload vertex data
        int baseVertex = toInt(this.vertexAlloc.alloc(output.verts.size()));
        _slot_baseVertex(slot, baseVertex);
        this.vertexBuffer.set(baseVertex, output.verts);

        //for each pass, allocate and upload index data if needed
        for (int i = 0; i < this.passes; i++) {
            long pass = _slot_pass(slot, i);
            int count = 0;
            int firstIndex = 0;

            ByteBuf indices = output.indices[i];
            if (indices.isReadable()) {
                int readableBytes = indices.readableBytes();
                long offset = this.indexAllocs[i].alloc(readableBytes);

                count = readableBytes / this.indexSize;
                firstIndex = toInt(offset / this.indexSize);

                try (GLBuffer buffer = this.indexBuffers[i].bind(GL_ELEMENT_ARRAY_BUFFER)) {
                    buffer.uploadRange(offset, indices);
                }
            }

            _pass_firstIndex(pass, firstIndex);
            _pass_count(pass, count);
        }

        return handle;
    }

    @Override
    public void delete(int handle) {
        //free slot
        this.slotAlloc.free(handle);
        long slot = this.slotsAddr + handle * this.slotSize;

        //free vertex data
        this.vertexAlloc.free(_slot_baseVertex(slot));

        //for each pass, free index data if needed
        for (int i = 0; i < this.passes; i++) {
            long pass = _slot_pass(slot, i);

            if (_pass_count(pass) > 0) {
                this.indexAllocs[i].free(_pass_firstIndex(pass) * (long) this.indexSize);
            }
        }

        //no need to zero out slot memory or whatever, it'll be initialized later if re-allocated
    }

    @Override
    public void toDrawCommands(int handle, @NonNull DrawElementsCommand[] commands) {
        long slot = this.slotsAddr + handle * this.slotSize;

        int baseVertex = _slot_baseVertex(slot);
        for (int i = 0; i < this.passes; i++) {
            long pass = _slot_pass(slot, i);

            commands[i].baseVertex(baseVertex)
                    .firstIndex(_pass_firstIndex(pass))
                    .count(_pass_count(pass));
        }
    }
}
