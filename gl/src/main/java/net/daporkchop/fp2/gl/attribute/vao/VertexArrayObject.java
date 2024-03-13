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

package net.daporkchop.fp2.gl.attribute.vao;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.OpenGL;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class VertexArrayObject implements AutoCloseable {
    public static VertexArrayObject create(OpenGL gl) {
        return new Basic(gl);
    }

    protected final OpenGL gl;
    protected final int id;

    /**
     * @return the ID of the corresponding OpenGL Vertex Array Object
     */
    public final int id() {
        return this.id;
    }

    public abstract void setFAttrib(int index, int size, int type, boolean normalized, int stride, long pointer, int divisor);

    public abstract void setIAttrib(int index, int size, int type, int stride, long pointer, int divisor);

    @Override
    public void close() {
        this.gl.glDeleteVertexArray(this.id);
    }

    /**
     * @author DaPorkchop_
     */
    private static final class Basic extends VertexArrayObject {
        public Basic(OpenGL gl) {
            super(gl, gl.glGenVertexArray());
        }

        @Override
        public void setFAttrib(int index, int size, int type, boolean normalized, int stride, long pointer, int divisor) {
            this.bind(() -> {
                this.gl.glEnableVertexAttribArray(index);
                this.gl.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
                this.gl.glVertexAttribDivisor(index, divisor); //TODO: throw exception if divisors aren't supported
            });
        }

        @Override
        public void setIAttrib(int index, int size, int type, int stride, long pointer, int divisor) {
            this.bind(() -> {
                this.gl.glEnableVertexAttribArray(index);
                this.gl.glVertexAttribIPointer(index, size, type, stride, pointer);
                this.gl.glVertexAttribDivisor(index, divisor);
            });
        }

        private void bind(Runnable action) {
            int old = this.gl.glGetInteger(GL_VERTEX_ARRAY_BINDING);
            try {
                this.gl.glBindVertexArray(this.id);
                action.run();
            } finally {
                this.gl.glBindVertexArray(old);
            }
        }
    }
}
