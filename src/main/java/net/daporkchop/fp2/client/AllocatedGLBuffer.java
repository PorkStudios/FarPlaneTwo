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

package net.daporkchop.fp2.client;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.FixedSizeAllocator;
import net.daporkchop.fp2.util.alloc.VariableSizedAllocator;
import net.daporkchop.lib.common.misc.string.PStrings;

import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * An OpenGL buffer which manages dynamic allocation of data internally.
 *
 * @author DaPorkchop_
 */
public interface AllocatedGLBuffer extends IGLBuffer, Allocator {
    static AllocatedGLBuffer create(@NonNull String name, int usage, long blockSize, boolean variable) {
        abstract class Base extends GLBuffer implements AllocatedGLBuffer, SequentialHeapManager {
            protected final Allocator allocator;
            protected final String name;

            public Base(@NonNull String name, int usage, long blockSize) {
                super(usage);

                this.name = name;

                try (Base base = this.bind(GL_ARRAY_BUFFER)) {
                    this.allocator = this.createAllocator(blockSize);
                }
            }

            protected abstract Allocator createAllocator(long blockSize);

            @Override
            public Base bind(int target) {
                super.bind(target);
                return this;
            }

            @Override
            public long alloc(long size) {
                return this.allocator.alloc(size);
            }

            @Override
            public void free(long address) {
                this.allocator.free(address);
            }

            @Override
            public void brk(long capacity) {
                FP2_LOG.info(PStrings.fastFormat("setting %s buffer capacity to %d bytes (%.2f MiB)", this.name, capacity, capacity / (1024.0d * 1024.0d)));

                //simply set buffer capacity
                this.capacity(capacity);
            }

            @Override
            public void sbrk(long newCapacity) {
                FP2_LOG.info(PStrings.fastFormat("growing %s buffer capacity to %d bytes (%.2f MiB)", this.name, newCapacity, newCapacity / (1024.0d * 1024.0d)));

                this.resize(newCapacity);
            }
        }

        return variable ?
                new Base(name, usage, blockSize) {
                    @Override
                    protected Allocator createAllocator(long blockSize) {
                        return new VariableSizedAllocator(blockSize, this);
                    }
                } :
                new Base(name, usage, blockSize) {
                    @Override
                    protected Allocator createAllocator(long blockSize) {
                        return new FixedSizeAllocator(blockSize, this);
                    }
                };
    }

    @Override
    AllocatedGLBuffer bind(int target);
}
