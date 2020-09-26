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

package net.daporkchop.fp2.strategy.base.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.DirectBufferReuse;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;

import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
public class FarRenderData {
    protected final long gpuVertices;
    protected final long addressVertices;
    protected final int sizeVertices;

    protected final long gpuIndices;
    protected final long addressIndices;
    protected final int sizeIndices;

    protected final PCleaner cleaner;

    public FarRenderData(long gpuVertices, long addressVertices, int sizeVertices, long gpuIndices, long addressIndices, int sizeIndices) {
        this.gpuVertices = gpuVertices;
        this.addressVertices = addressVertices;
        this.sizeVertices = sizeVertices;

        this.gpuIndices = gpuIndices;
        this.addressIndices = addressIndices;
        this.sizeIndices = sizeIndices;

        //prevents memory leaks, but nothing more
        this.cleaner = PCleaner.cleaner(this, new ReleaseCallback(addressVertices, addressIndices));
    }

    public void uploadVertices() {
        glBufferSubData(GL_ARRAY_BUFFER, this.gpuVertices, DirectBufferReuse.wrapByte(this.addressVertices, this.sizeVertices));
    }

    public void uploadIndices() {
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, this.gpuIndices, DirectBufferReuse.wrapByte(this.addressIndices, this.sizeIndices));
    }

    public void release(@NonNull Allocator verticesAlloc, @NonNull Allocator indicesAlloc) {
        this.cleaner.clean();

        verticesAlloc.free(this.gpuVertices);
        indicesAlloc.free(this.gpuIndices);
    }

    @RequiredArgsConstructor
    protected static final class ReleaseCallback implements Runnable {
        protected final long addressVertices;
        protected final long addressIndices;

        @Override
        public void run() {
            PUnsafe.freeMemory(this.addressVertices);
            PUnsafe.freeMemory(this.addressIndices);
        }
    }
}
