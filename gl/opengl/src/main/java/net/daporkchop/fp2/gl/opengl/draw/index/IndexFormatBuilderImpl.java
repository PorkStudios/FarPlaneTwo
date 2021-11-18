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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.draw.index.IndexFormatBuilder;
import net.daporkchop.fp2.gl.draw.index.IndexType;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.fp2.gl.opengl.OpenGL;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class IndexFormatBuilderImpl implements IndexFormatBuilder.TypeSelectionStage {
    @NonNull
    protected final OpenGL gl;

    //
    // TypeSelectionStage
    //

    @Override
    public IndexFormatBuilder type(@NonNull IndexType type) {
        switch (type) {
            case UNSIGNED_BYTE:
                return () -> new IndexFormatImpl(this.gl, type) {
                    @Override
                    public IndexWriter createWriter() {
                        return new IndexWriterImpl.Byte(this);
                    }
                };
            case UNSIGNED_SHORT:
                return () -> new IndexFormatImpl(this.gl, type) {
                    @Override
                    public IndexWriter createWriter() {
                        return new IndexWriterImpl.Short(this);
                    }
                };
            case UNSIGNED_INT:
                return () -> new IndexFormatImpl(this.gl, type) {
                    @Override
                    public IndexWriter createWriter() {
                        return new IndexWriterImpl.Int(this);
                    }
                };
            default:
                throw new IllegalArgumentException(type.name());
        }
    }
}
