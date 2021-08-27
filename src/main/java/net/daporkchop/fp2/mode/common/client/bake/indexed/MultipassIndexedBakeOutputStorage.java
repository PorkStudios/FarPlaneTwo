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
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.ElementType;
import net.daporkchop.fp2.client.gl.command.elements.DrawElementsCommand;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuffer;
import net.daporkchop.fp2.client.gl.vertex.buffer.interleaved.InterleavedVertexBuffer;
import net.daporkchop.fp2.mode.common.client.bake.AbstractBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutputStorage;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.SequentialFixedSizeAllocator;
import net.daporkchop.fp2.util.alloc.SequentialVariableSizedAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * Implementation of {@link IBakeOutputStorage} which contains indexed geometry in multiple render passes.
 *
 * @author DaPorkchop_
 */
public class MultipassIndexedBakeOutputStorage extends AbstractBakeOutputStorage<MultipassIndexedBakeOutput, DrawElementsCommand> {
    protected final Allocator alloc;
    protected final VertexFormat format;
    protected final ElementType type;

    /*
     * struct Slot {
     *   int verts_base;
     *   int indices_base[PASSES];
     *   int indices_counts[PASSES];
     * };
     */
    protected final Allocator slotAlloc;
    protected final long slotStride;
    protected long slotsAddr;

    protected final Allocator vertexAlloc;
    protected final IVertexBuffer vertexBuffer;

    protected final Allocator[] indexAllocs;
    protected final GLBuffer[] indexBuffers;

    protected final int passes;

    public MultipassIndexedBakeOutputStorage(@NonNull Allocator alloc, @NonNull VertexFormat format, @NonNull ElementType type, int passes) {
        this.alloc = alloc;
        this.format = format;
        this.type = type;
        this.passes = positive(passes, "passes");

        this.slotStride = (passes * 2L + 1L) * Integer.BYTES;
        this.slotAlloc = new SequentialFixedSizeAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> this.slotsAddr = this.alloc.realloc(this.slotsAddr, capacity * this.slotStride)));

        this.vertexBuffer = new InterleavedVertexBuffer(alloc, format);
        this.vertexAlloc = new SequentialVariableSizedAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> this.vertexBuffer.resize(toInt(capacity))));

        this.indexAllocs = new Allocator[passes];
        this.indexBuffers = new GLBuffer[passes];
        for (int pass = 0; pass < passes; pass++) {
            GLBuffer indexBuffer = this.indexBuffers[pass] = new GLBuffer(GL_DYNAMIC_DRAW);
            this.indexAllocs[pass] = new SequentialVariableSizedAllocator(1L, Allocator.SequentialHeapManager.unified(capacity -> indexBuffer.resize(toInt(capacity))));
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
    public int add(@NonNull MultipassIndexedBakeOutput output) {
        //allocate a slot
        int slot = toInt(this.slotAlloc.alloc(1L));
        long slotAddr = this.slotsAddr + slot * this.slotStride;

        //allocate and upload vertex data
        int vertsBase = toInt(this.vertexAlloc.alloc(output.verts.size()));
        this.vertexBuffer.set(vertsBase, output.verts);
        PUnsafe.putInt(slotAddr, vertsBase);

        //for each pass, allocate and upload index data if needed
        for (int pass = 0; pass < this.passes; pass++) {
            ByteBuf indices = output.indices[pass];
            if (indices.isReadable()) {
                int count = indices.readableBytes() / this.type.size();
                int indexBase = toInt(this.indexAllocs[pass].alloc(count));
                this.indexBuffers[pass].uploadRange(indexBase + (long) this.type.size(), indices);
                PUnsafe.putInt(slotAddr + (pass + 1L) * Integer.BYTES, indexBase);
                PUnsafe.putInt(slotAddr + (pass + 1L + this.passes) * Integer.BYTES, count);
            } else {
                PUnsafe.putInt(slotAddr + (pass + 1L) * Integer.BYTES, 0);
                PUnsafe.putInt(slotAddr + (pass + 1L + this.passes) * Integer.BYTES, 0);
            }
        }

        return slot;
    }

    @Override
    public void delete(int handle) {
        //free slot
        this.slotAlloc.free(handle);
        long slotAddr = this.slotsAddr + handle * this.slotStride;

        //free vertex data
        this.vertexAlloc.free(PUnsafe.getInt(slotAddr));

        //for each pass, free index data if needed
        for (int pass = 0; pass < this.passes; pass++) {
            if (PUnsafe.getInt(slotAddr + (pass + 1L + this.passes) * Integer.BYTES) > 0) {
                this.indexAllocs[pass].free(PUnsafe.getInt(slotAddr + (pass + 1L) * Integer.BYTES));
            }
        }
    }

    @Override
    public void toDrawCommands(int handle, @NonNull DrawElementsCommand[] commands) {
        long slotAddr = this.slotsAddr + handle * this.slotStride;

        int baseVertex = PUnsafe.getInt(slotAddr);
        for (int pass = 0; pass < this.passes; pass++) {
            int indexBase = PUnsafe.getInt(slotAddr + (pass + 1L) * Integer.BYTES);
            int count = PUnsafe.getInt(slotAddr + (pass + 1L + this.passes) * Integer.BYTES);
            if (count > 0) {
                commands[pass].baseVertex(baseVertex).firstIndex(indexBase).count(count);
            }
        }
    }
}
