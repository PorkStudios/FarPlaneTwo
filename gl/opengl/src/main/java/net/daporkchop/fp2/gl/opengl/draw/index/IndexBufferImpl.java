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

package net.daporkchop.fp2.gl.opengl.draw.index;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.draw.index.IndexBuffer;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.fp2.gl.opengl.buffer.GLBuffer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class IndexBufferImpl implements IndexBuffer {
    protected final IndexFormatImpl format;
    protected final GLBuffer buffer;

    protected int capacity;

    public IndexBufferImpl(@NonNull IndexFormatImpl format, @NonNull BufferUsage usage) {
        this.format = format;
        this.buffer = format.gl.createBuffer(usage);
    }

    @Override
    public void resize(int capacity) {
        this.capacity = notNegative(capacity, "capacity");
        this.buffer.resize(this.capacity * (long) this.format.size());
    }

    @Override
    public void close() {
        this.buffer.close();
    }

    @Override
    public void set(int startIndex, @NonNull IndexWriter writerIn) {
        IndexWriterImpl writer = (IndexWriterImpl) writerIn;
        checkArg(writer.format() == this.format, "mismatched index formats!");
        checkRangeLen(this.capacity, startIndex, writer.size());

        long size = this.format.size();
        this.buffer.uploadRange(startIndex * size, writer.addr, writer.size() * size);
    }
}
