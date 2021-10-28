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

package net.daporkchop.fp2.gl.opengl.index;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.index.IndexWriter;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.common.util.TypeSize.*;

/**
 * @author DaPorkchop_
 */
public abstract class IndexWriterImpl implements IndexWriter {
    @Getter
    protected final IndexFormatImpl format;
    protected final Allocator alloc = new DirectMemoryAllocator();

    protected long addr;

    @Getter
    protected int size;
    protected int capacity;

    public IndexWriterImpl(@NonNull IndexFormatImpl format) {
        this.format = format;

        this.resize(16);
    }

    protected int next(int step) {
        int index = this.size;
        if ((this.size += step) >= this.capacity) { //grow buffer if needed
            this.resize(this.capacity << 1);
        }
        return index;
    }

    protected void resize(int capacity) {
        this.capacity = capacity;
        this.addr = this.alloc.realloc(this.addr, this.capacity * (long) this.format.size());
    }

    @Override
    public void close() {
        this.alloc.free(this.addr);
    }

    /**
     * @author DaPorkchop_
     */
    public static class Byte extends IndexWriterImpl {
        public Byte(@NonNull IndexFormatImpl format) {
            super(format);
        }

        @Override
        public IndexWriter append(int index) {
            PUnsafe.putByte(this.next(1) * (long) BYTE_SIZE + this.addr, (byte) index);
            return this;
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class Short extends IndexWriterImpl {
        public Short(@NonNull IndexFormatImpl format) {
            super(format);
        }

        @Override
        public IndexWriter append(int index) {
            PUnsafe.putShort(this.next(1) * (long) SHORT_SIZE + this.addr, (short) index);
            return this;
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class Int extends IndexWriterImpl {
        public Int(@NonNull IndexFormatImpl format) {
            super(format);
        }

        @Override
        public IndexWriter append(int index) {
            PUnsafe.putInt(this.next(1) * (long) INT_SIZE + this.addr, index);
            return this;
        }
    }
}
