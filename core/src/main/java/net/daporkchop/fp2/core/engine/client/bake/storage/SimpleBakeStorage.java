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

package net.daporkchop.fp2.core.engine.client.bake.storage;

import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.SequentialVariableSizedAllocator;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.BakeOutput;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.attribute.NewAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.buffer.upload.BufferUploader;
import net.daporkchop.fp2.gl.draw.index.NewIndexBuffer;
import net.daporkchop.fp2.gl.draw.index.NewIndexFormat;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.Map;

import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A simple implementation of {@link BakeStorage} which uses regular mutable OpenGL buffers.
 *
 * @author DaPorkchop_
 */
public final class SimpleBakeStorage<VertexType extends AttributeStruct> extends BakeStorage<VertexType> {
    private final NewAttributeBuffer<VertexType> vertexBuffer;
    private final Allocator vertexAlloc;

    private final NewIndexBuffer indexBuffer;
    private final Allocator indexAlloc;

    private final Map<TilePos, Entry> positionsToEntries = DirectTilePosAccess.newPositionKeyedHashMap();

    public SimpleBakeStorage(OpenGL gl, BufferUploader uploader, NewAttributeFormat<VertexType> vertexFormat, NewIndexFormat indexFormat) {
        super(gl, uploader, vertexFormat, indexFormat);

        try {
            this.vertexBuffer = vertexFormat.createBuffer();
            this.vertexAlloc = new SequentialVariableSizedAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> {
                this.bufferUploader.flush(); //flush the buffer uploader to ensure that any pending uploads are copied along when we resize
                this.vertexBuffer.resize(toInt(capacity), BufferUsage.STATIC_DRAW);
            }));

            this.indexBuffer = indexFormat.createBuffer(gl);
            this.indexAlloc = new SequentialVariableSizedAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> {
                this.bufferUploader.flush(); //flush the buffer uploader to ensure that any pending uploads are copied along when we resize
                this.indexBuffer.resize(toInt(capacity), BufferUsage.STATIC_DRAW);
            }));
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(this.vertexBuffer, this.indexBuffer);
    }

    @Override
    public void update(Map<TilePos, BakeOutput<VertexType>> changes) {
        changes.forEach((pos, output) -> {
            Entry entry = this.positionsToEntries.get(pos);
            if (entry != null) {
                //free all the old allocations
                this.vertexAlloc.free(entry.baseVertex);
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    if (entry.count[pass] > 0) {
                        this.indexAlloc.free(entry.firstIndex[pass]);
                    }
                }
            }

            if (output != null && !output.isEmpty()) { //the tile is being added
                entry = new Entry();

                //allocate space for the new vertex and index data
                entry.vertexCount = output.verts.size();
                entry.baseVertex = toInt(this.vertexAlloc.alloc(entry.vertexCount));
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    int indexCount = output.indicesPerPass[pass].size();
                    entry.count[pass] = indexCount;
                    if (indexCount > 0) {
                        entry.firstIndex[pass] = toInt(this.indexAlloc.alloc(indexCount));
                    }
                }

                //upload vertex and index data
                this.vertexBuffer.setRange(entry.baseVertex, output.verts, this.bufferUploader);
                for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                    if (output.indicesPerPass[pass].size() > 0) {
                        this.indexBuffer.setRange(entry.firstIndex[pass], output.indicesPerPass[pass], this.bufferUploader);
                    }
                }

                this.positionsToEntries.put(pos, entry);
            } else { //the tile is being removed
                if (entry != null) {
                    this.positionsToEntries.remove(pos);
                }
            }
        });
    }

    @Override
    public Location[] find(TilePos pos) {
        Entry entry = this.positionsToEntries.get(pos);
        return entry != null ? entry.locations() : null;
    }

    @Override
    public NewAttributeBuffer<VertexType> vertexBuffer(int level, int pass) {
        checkIndex(RENDER_PASS_COUNT, pass);
        return this.vertexBuffer;
    }

    @Override
    public NewIndexBuffer indexBuffer(int level, int pass) {
        checkIndex(RENDER_PASS_COUNT, pass);
        return this.indexBuffer;
    }

    @Override
    public DebugStats.Renderer stats() {
        Allocator.Stats vertexStats = this.vertexAlloc.stats();
        Allocator.Stats indexStats = this.indexAlloc.stats();

        long vertexSize = this.vertexBuffer.format().size();
        long indexSize = this.indexFormat.size();

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

    /**
     * @author DaPorkchop_
     */
    private static final class Entry {
        public int baseVertex;
        public int vertexCount;

        public final int[] firstIndex = new int[RENDER_PASS_COUNT];
        public final int[] count = new int[RENDER_PASS_COUNT];

        public Location[] locations() {
            Location[] result = new Location[RENDER_PASS_COUNT];
            for (int pass = 0; pass < RENDER_PASS_COUNT; pass++) {
                if (this.count[pass] > 0) {
                    result[pass] = new Location(this.baseVertex, this.vertexCount, this.firstIndex[pass], this.count[pass]);
                }
            }
            return result;
        }
    }
}
