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

package net.daporkchop.fp2.gl.opengl.binding;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.opengl.index.IndexBufferImpl;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class DrawBindingIndexedImpl extends DrawBindingImpl implements DrawBindingIndexed {
    protected final IndexBufferImpl indices;

    public DrawBindingIndexedImpl(@NonNull DrawBindingBuilderImpl builder) {
        super(builder);

        this.indices = builder.indices;

        //bind the elements buffer to the VAO
        int oldVao = this.api.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int oldElementArray = this.api.glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING);
        try {
            this.api.glBindVertexArray(this.vao);
            this.api.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.indices.buffer().id());
        } finally {
            this.api.glBindVertexArray(oldVao);
            this.api.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, oldElementArray);
        }
    }
}
