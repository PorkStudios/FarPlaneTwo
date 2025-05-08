/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.gl.util;

import lombok.NonNull;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.common.annotation.NotThreadSafe;

/**
 * Base class for an OpenGL object.
 *
 * @author DaPorkchop_
 */
@NotThreadSafe
public abstract class GLObject implements AutoCloseable {
    /**
     * The OpenGL context which this object belongs to.
     */
    protected final OpenGL gl;
    private boolean closed;

    /**
     * {@code true} if {@link GLExtension#GL_ARB_direct_state_access GL_ARB_direct_state_access} is supported.
     * <p>
     * This field is hoisted here even though it may not be needed by all subclasses simply to minimize the amount of padding space in other
     * subclasses, and isn't a big deal since initializing it is very cheap.
     */
    protected final boolean dsa;

    protected GLObject(OpenGL gl) {
        this.gl = gl;
        this.dsa = gl.supports(GLExtension.GL_ARB_direct_state_access);
    }

    @Override
    public void close() {
        this.checkOpen();
        this.closed = true;
        this.delete();
    }

    protected abstract void delete();

    //might be neat to use a Cleaner for this instead, but OpenGL objects are typically pretty long-lived so the finalizer should have negligible GC overhead
    @Override
    protected final void finalize() {
        if (!this.closed) {
            new Throwable("OpenGL object " + this + " leaked!").printStackTrace();
        }
    }

    /**
     * Asserts that this OpenGL object is open, i.e. hasn't been {@link #close() closed} yet.
     *
     * @throws IllegalStateException if this object has already been closed
     */
    public final void checkOpen() {
        if (this.closed) {
            throw new IllegalStateException();
        }
    }

    /**
     * Sets this object's debug label to the given value.
     *
     * @param label the label
     * @throws UnsupportedOperationException if the current OpenGL context doesn't support {@link net.daporkchop.fp2.gl.GLExtension#GL_KHR_debug}
     */
    public abstract void setDebugLabel(@NonNull CharSequence label);

    /**
     * @return this object's debug label to the given value
     * @throws UnsupportedOperationException if the current OpenGL context doesn't support {@link net.daporkchop.fp2.gl.GLExtension#GL_KHR_debug}
     */
    public abstract String getDebugLabel();

    /**
     * An ordinary OpenGL object with an integer id.
     *
     * @author DaPorkchop_
     */
    public static abstract class Normal extends GLObject {
        protected final int id;

        public Normal(OpenGL gl, int id) {
            super(gl);
            this.id = id;
        }

        /**
         * @return this OpenGL object's id
         */
        public final int id() {
            super.checkOpen();
            return this.id;
        }

        protected abstract int debugLabelNamespace();

        @Override
        public final void setDebugLabel(@NonNull CharSequence label) {
            this.gl.glObjectLabel(this.debugLabelNamespace(), this.id, label);
        }

        @Override
        public final String getDebugLabel() {
            return this.gl.glGetObjectLabel(this.debugLabelNamespace(), this.id);
        }
    }
}
