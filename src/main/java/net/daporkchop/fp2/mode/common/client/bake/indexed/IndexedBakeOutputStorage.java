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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.debug.util.DebugStats;
import net.daporkchop.fp2.gl.attribute.global.GlobalAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.global.GlobalAttributeFormat;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeFormat;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.command.DrawCommandIndexed;
import net.daporkchop.fp2.gl.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.index.IndexBuffer;
import net.daporkchop.fp2.gl.index.IndexFormat;
import net.daporkchop.fp2.gl.index.IndexWriter;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.mode.common.client.bake.AbstractBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutputStorage;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.SequentialFixedSizeAllocator;
import net.daporkchop.fp2.common.util.alloc.SequentialVariableSizedAllocator;
import net.daporkchop.fp2.util.annotation.DebugOnly;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link IBakeOutputStorage} which contains indexed geometry in multiple render passes.
 *
 * @author DaPorkchop_
 */
public class IndexedBakeOutputStorage<SG, SL> extends AbstractBakeOutputStorage<IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> {
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

    protected final GlobalAttributeBuffer globalBuffer;

    protected final Allocator vertexAlloc;
    protected final LocalAttributeBuffer vertexBuffer;

    protected final int indexSize;
    protected final Allocator[] indexAllocs;
    protected final IndexBuffer[] indexBuffers;

    @Getter
    protected final int passes;

    public IndexedBakeOutputStorage(@NonNull Allocator alloc, @NonNull GlobalAttributeFormat<SG> globalFormat, @NonNull LocalAttributeFormat<SL> vertexFormat, @NonNull IndexFormat indexFormat, int passes) {
        this.alloc = alloc;

        this.passes = positive(passes, "passes");
        this.indexSize = indexFormat.size();

        this.globalBuffer = globalFormat.createBuffer(BufferUsage.STATIC_DRAW);

        this.slotSize = _SLOT_SIZE(passes);
        this.slotAlloc = new SequentialFixedSizeAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> {
            this.slotsAddr = this.alloc.realloc(this.slotsAddr, capacity * this.slotSize);
            this.globalBuffer.resize(toInt(capacity));
        }));

        this.vertexBuffer = vertexFormat.createBuffer(BufferUsage.STATIC_DRAW);
        this.vertexAlloc = new SequentialVariableSizedAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> this.vertexBuffer.resize(toInt(capacity))));

        this.indexAllocs = new Allocator[passes];
        this.indexBuffers = new IndexBuffer[passes];
        for (int pass = 0; pass < passes; pass++) {
            IndexBuffer indexBuffer = this.indexBuffers[pass] = indexFormat.createBuffer(BufferUsage.STATIC_DRAW);
            this.indexAllocs[pass] = new SequentialVariableSizedAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> indexBuffer.resize(toInt(capacity))));
        }
    }

    @Override
    protected void doRelease() {
        this.vertexBuffer.close();
        for (IndexBuffer indexBuffer : this.indexBuffers) {
            indexBuffer.close();
        }
    }

    @Override
    public DrawBindingBuilder<DrawBindingIndexed> createDrawBinding(@NonNull DrawLayout layout, int pass) {
        return layout.createBinding()
                .withIndexes(this.indexBuffers[pass])
                .withLocals(this.vertexBuffer)
                .withGlobals(this.globalBuffer);
    }

    @Override
    public int add(@NonNull IndexedBakeOutput output) {
        //allocate a slot
        int handle = toInt(this.slotAlloc.alloc(1L));
        long slotAddr = this.slotsAddr + handle * this.slotSize;

        //upload global data
        this.globalBuffer.set(handle, output.globals);

        //allocate and upload vertex data
        int baseVertex = toInt(this.vertexAlloc.alloc(output.verts.size()));
        _slot_baseVertex(slotAddr, baseVertex);
        this.vertexBuffer.set(baseVertex, output.verts);

        //for each pass, allocate and upload index data if needed
        for (int i = 0; i < this.passes; i++) {
            long passAddr = _slot_pass(slotAddr, i);
            int count = 0;
            int firstIndex = 0;

            IndexWriter indices = output.indices[i];
            if (indices.size() > 0) {
                count = indices.size();
                firstIndex = toInt(this.indexAllocs[i].alloc(indices.size()));

                this.indexBuffers[i].set(firstIndex, indices);
            }

            _pass_firstIndex(passAddr, firstIndex);
            _pass_count(passAddr, count);
        }

        return handle;
    }

    @Override
    public void delete(int handle) {
        //free slot
        this.slotAlloc.free(handle);
        long slotAddr = this.slotsAddr + handle * this.slotSize;

        //free vertex data
        this.vertexAlloc.free(_slot_baseVertex(slotAddr));

        //for each pass, free index data if needed
        for (int i = 0; i < this.passes; i++) {
            long passAddr = _slot_pass(slotAddr, i);

            if (_pass_count(passAddr) > 0) {
                this.indexAllocs[i].free(_pass_firstIndex(passAddr));
            }
        }

        //no need to zero out slot memory or whatever, it'll be initialized later if re-allocated
    }

    @Override
    public DrawCommandIndexed[] toDrawCommands(int handle) {
        long slotAddr = this.slotsAddr + handle * this.slotSize;
        int baseVertex = _slot_baseVertex(slotAddr);

        DrawCommandIndexed[] commands = new DrawCommandIndexed[this.passes];
        for (int i = 0; i < this.passes; i++) {
            long passAddr = _slot_pass(slotAddr, i);
            commands[i] = new DrawCommandIndexed(_pass_firstIndex(passAddr), _pass_count(passAddr), baseVertex);
        }
        return commands;
    }

    @DebugOnly
    @Override
    public DebugStats.Renderer stats() {
        //TODO: this
        //DebugStats.Allocator vertexStats = this.vertexAlloc.stats();
        //DebugStats.Allocator indexStats = Stream.of(this.indexAllocs).map(Allocator::stats).reduce(DebugStats.Allocator.ZERO, DebugStats.Allocator::add);
        DebugStats.Allocator vertexStats = DebugStats.Allocator.ZERO;
        DebugStats.Allocator indexStats = DebugStats.Allocator.ZERO;

        //long vertexSize = this.vertexBuffer.layout().format().vertexSize();
        long vertexSize = 1L;
        long indexSize = this.indexSize;

        return DebugStats.Renderer.builder()
                .allocatedVRAM(vertexStats.allocatedSpace() * vertexSize + indexStats.allocatedSpace())
                .totalVRAM(vertexStats.totalSpace() * vertexSize + indexStats.totalSpace())
                .allocatedIndices(indexStats.allocatedSpace() / indexSize)
                .totalIndices(indexStats.totalSpace() / indexSize)
                .indexSize(indexSize)
                .allocatedVertices(vertexStats.allocatedSpace())
                .totalVertices(vertexStats.totalSpace())
                .vertexSize(vertexSize)
                .build();
    }
}
