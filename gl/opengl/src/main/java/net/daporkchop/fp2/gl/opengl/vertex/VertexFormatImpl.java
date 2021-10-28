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

package net.daporkchop.fp2.gl.opengl.vertex;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.vertex.VertexAttribute;
import net.daporkchop.fp2.gl.vertex.VertexBuffer;
import net.daporkchop.fp2.gl.vertex.VertexFormat;
import net.daporkchop.fp2.gl.vertex.VertexWriter;

import java.util.Map;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public abstract class VertexFormatImpl implements VertexFormat {
    protected final OpenGL gl;

    protected final VertexAttributeImpl[] attribs;
    protected final Map<String, VertexAttributeImpl> attribsByName;

    @Getter
    protected final int size;

    protected VertexFormatImpl(@NonNull VertexFormatBuilderImpl builder) {
        this.gl = builder.gl;

        this.attribsByName = builder.attributes.build();
        this.attribs = this.attribsByName.values().toArray(new VertexAttributeImpl[0]);

        this.size = Stream.of(this.attribs).mapToInt(VertexAttributeImpl::size).sum();
    }

    @Override
    public Map<String, VertexAttribute> attribs() {
        return uncheckedCast(this.attribsByName);
    }

    /**
     * @author DaPorkchop_
     */
    public static class Interleaved extends VertexFormatImpl {
        protected Interleaved(@NonNull VertexFormatBuilderImpl builder) {
            super(builder);
        }

        @Override
        public VertexWriter createWriter() {
            return new VertexWriterImpl.Interleaved(this);
        }

        @Override
        public VertexBuffer createBuffer(@NonNull BufferUsage usage) {
            return new VertexBufferImpl.Interleaved(this, usage);
        }
    }
}
