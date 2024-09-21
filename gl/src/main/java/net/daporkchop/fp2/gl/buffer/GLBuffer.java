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

package net.daporkchop.fp2.gl.buffer;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.fp2.gl.util.GLRequires;
import net.daporkchop.fp2.gl.util.debug.DebugLabel;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An OpenGL buffer.
 *
 * @author DaPorkchop_
 */
public abstract class GLBuffer extends GLObject.Normal {
    protected final boolean dsa;
    protected final boolean invalidateSubdata;

    protected long capacity = -1L;
    protected boolean mapped = false;

    protected GLBuffer(OpenGL gl) {
        super(gl, gl.supports(GLExtension.GL_ARB_direct_state_access) ? gl.glCreateBuffer() : gl.glGenBuffer());
        this.dsa = gl.supports(GLExtension.GL_ARB_direct_state_access);
        this.invalidateSubdata = gl.supports(GLExtension.GL_ARB_invalidate_subdata);
    }

    /**
     * @return this buffer's current capacity
     */
    public final long capacity() {
        return this.capacity;
    }

    @Override
    protected final void delete() {
        this.gl.glDeleteBuffer(this.id);
    }

    @Override
    public void setDebugLabel(@NonNull CharSequence label) {
        this.gl.glObjectLabel(GL_BUFFER, this.id, label);
    }

    @Override
    public String getDebugLabel() {
        return this.gl.glGetObjectLabel(GL_BUFFER, this.id);
    }

    /**
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param addr  the base address of the data to upload
     * @param size  the size of the data (in bytes)
     */
    public final void bufferSubData(long start, long addr, long size) {
        this.checkOpen();
        checkRangeLen(this.capacity, start, size);
        if (this.dsa) {
            this.gl.glNamedBufferSubData(this.id, start, size, addr);
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glBufferSubData(target.id(), start, size, addr);
            });
        }
    }

    /**
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the {@link ByteBuffer} containing the data to upload
     */
    public final void bufferSubData(long start, @NonNull ByteBuffer data) {
        this.checkOpen();
        checkRangeLen(this.capacity, start, data.remaining());
        if (this.dsa) {
            this.gl.glNamedBufferSubData(this.id, start, data);
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glBufferSubData(target.id(), start, data);
            });
        }
    }

    /**
     * Downloads the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param addr  the base address where the data should be stored
     * @param size  the size of the data (in bytes)
     */
    public final void getBufferSubData(long start, long addr, long size) {
        this.checkOpen();
        checkRangeLen(this.capacity, start, size);
        if (this.dsa) {
            this.gl.glGetNamedBufferSubData(this.id, start, size, addr);
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glGetBufferSubData(target.id(), start, size, addr);
            });
        }
    }

    /**
     * Downloads the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the {@link ByteBuffer} where the data should be stored
     */
    public final void getBufferSubData(long start, @NonNull ByteBuffer data) {
        this.checkOpen();
        checkRangeLen(this.capacity, start, data.remaining());
        if (this.dsa) {
            this.gl.glGetNamedBufferSubData(this.id, start, data);
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glGetBufferSubData(target.id(), start, data);
            });
        }
    }

    /**
     * Copies a range of data from the given source buffer into this buffer.
     *
     * @param src       the buffer to copy the data from
     * @param srcOffset the offset in the source buffer to begin copying from
     * @param dstOffset the offset in this buffer to begin copying to
     * @param size      the number of bytes to copy
     * @throws UnsupportedOperationException if {@link GLExtension#GL_ARB_copy_buffer ARB_copy_buffer} isn't supported
     * @apiNote requires {@link GLExtension#GL_ARB_copy_buffer GL_ARB_copy_buffer}
     */
    @GLRequires(GLExtension.GL_ARB_copy_buffer)
    public final void copyRange(@NonNull GLBuffer src, long srcOffset, long dstOffset, long size) {
        this.checkOpen();
        checkRangeLen(src.capacity(), srcOffset, size);
        checkRangeLen(this.capacity(), dstOffset, size);
        if (this.dsa) {
            this.gl.glCopyNamedBufferSubData(src.id, this.id, srcOffset, dstOffset, size);
        } else {
            src.bind(BufferTarget.COPY_READ_BUFFER, srcTarget -> {
                this.bind(BufferTarget.COPY_WRITE_BUFFER, dstTarget -> {
                    this.gl.glCopyBufferSubData(srcTarget.id(), dstTarget.id(), srcOffset, dstOffset, size);
                });
            });
        }
    }

    /**
     * Copies a range of data from this buffer into the given destination buffer.
     *
     * @param srcOffset the offset in this buffer to begin copying from
     * @param dst       the buffer to copy the data to
     * @param dstOffset the offset in the destination buffer to begin copying to
     * @param size      the number of bytes to copy
     * @throws UnsupportedOperationException if {@link GLExtension#GL_ARB_copy_buffer ARB_copy_buffer} isn't supported
     * @apiNote requires {@link GLExtension#GL_ARB_copy_buffer GL_ARB_copy_buffer}
     */
    @GLRequires(GLExtension.GL_ARB_copy_buffer)
    public final void copyRange(long srcOffset, @NonNull GLBuffer dst, long dstOffset, long size) {
        dst.copyRange(this, srcOffset, dstOffset, size);
    }

    /**
     * Hints that the buffer's storage should be invalidated.
     * <p>
     * After invalidation, the buffer contents become undefined.
     * <p>
     * This may do nothing if the OpenGL implementation doesn't support buffer invalidation.
     */
    public void invalidateHint() {
        this.checkOpen();
        if (this.invalidateSubdata) {
            this.gl.glInvalidateBufferData(this.id);
        }
    }

    /**
     * Invalidates this buffer's contents.
     * <p>
     * After invalidation, the buffer contents become undefined.
     *
     * @throws UnsupportedOperationException if {@link GLExtension#GL_ARB_invalidate_subdata GL_ARB_invalidate_subdata} isn't supported
     * @apiNote requires {@link GLExtension#GL_ARB_invalidate_subdata GL_ARB_invalidate_subdata}
     */
    @GLRequires(GLExtension.GL_ARB_invalidate_subdata)
    public final void invalidate() throws UnsupportedOperationException {
        this.checkOpen();
        this.gl.glInvalidateBufferData(this.id);
    }

    /**
     * Executes the given action with this buffer bound to the given {@link BufferTarget buffer binding target}.
     * <p>
     * This will restore the previously bound buffer when the operation completes.
     *
     * @param target   the {@link BufferTarget buffer binding target} to bind the buffer to
     * @param callback the action to run
     */
    public final void bindPreserving(BufferTarget target, Consumer<BufferTarget> callback) {
        this.checkOpen();
        int old = this.gl.glGetInteger(target.binding());
        try {
            this.gl.glBindBuffer(target.id(), this.id);
            callback.accept(target);
        } finally {
            this.gl.glBindBuffer(target.id(), old);
        }
    }

    /**
     * Executes the given action with this buffer bound to the given {@link BufferTarget buffer binding target}.
     * <p>
     * This will restore the previously bound buffer when the operation completes.
     *
     * @param target   the {@link BufferTarget buffer binding target} to bind the buffer to
     * @param callback the action to run
     */
    public final <T> T bindPreserving(BufferTarget target, Function<BufferTarget, T> callback) {
        this.checkOpen();
        int old = this.gl.glGetInteger(target.binding());
        try {
            this.gl.glBindBuffer(target.id(), this.id);
            return callback.apply(target);
        } finally {
            this.gl.glBindBuffer(target.id(), old);
        }
    }

    /**
     * Immediately binds this buffer to the given {@link BufferTarget buffer binding target} in the OpenGL context.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound buffer when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this buffer bound will not cause future issues.
     *
     * @param target the {@link BufferTarget buffer binding target} to bind the buffer to
     */
    public final void bindUnsafe(BufferTarget target) {
        this.checkOpen();
        this.gl.glBindBuffer(target.id(), this.id);
    }

    /**
     * Executes the given action with this buffer bound to the given {@link BufferTarget buffer binding target}.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound buffer when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this buffer bound will not cause future issues.
     *
     * @param target   the {@link BufferTarget buffer binding target} to bind the buffer to
     * @param callback the action to run
     */
    public final void bindUnsafe(BufferTarget target, Consumer<BufferTarget> callback) {
        this.checkOpen();
        this.gl.glBindBuffer(target.id(), this.id);
        callback.accept(target);
    }

    /**
     * Executes the given action with this buffer bound to the given {@link BufferTarget buffer binding target}.
     * <p>
     * This method is unsafe in that it does not provide a mechanism to restore the previously bound buffer when the operation completes. The user is responsible for ensuring
     * that OpenGL state is preserved, or that leaving this buffer bound will not cause future issues.
     *
     * @param target   the {@link BufferTarget buffer binding target} to bind the buffer to
     * @param callback the action to run
     */
    public final <T> T bindUnsafe(BufferTarget target, Function<BufferTarget, T> callback) {
        this.checkOpen();
        this.gl.glBindBuffer(target.id(), this.id);
        return callback.apply(target);
    }

    /**
     * Executes the given action with this buffer bound to the given {@link BufferTarget buffer binding target}.
     *
     * @param target   the {@link BufferTarget buffer binding target} to bind the buffer to
     * @param callback the action to run
     */
    public final void bind(BufferTarget target, Consumer<BufferTarget> callback) {
        if (OpenGL.PRESERVE_REGULAR_BUFFER_BINDINGS_IN_METHODS) {
            this.bindPreserving(target, callback);
        } else {
            this.bindUnsafe(target, callback);
        }
    }

    /**
     * Executes the given action with this buffer bound to the given {@link BufferTarget buffer binding target}.
     *
     * @param target   the {@link BufferTarget buffer binding target} to bind the buffer to
     * @param callback the action to run
     */
    public final <T> T bind(BufferTarget target, Function<BufferTarget, T> callback) {
        if (OpenGL.PRESERVE_REGULAR_BUFFER_BINDINGS_IN_METHODS) {
            return this.bindPreserving(target, callback);
        } else {
            return this.bindUnsafe(target, callback);
        }
    }

    /**
     * Maps this buffer's contents into client address space and passes the mapping address to the given callback function before unmapping the buffer again.
     *
     * @param access   the ways in which the buffer data may be accessed
     * @param callback the callback function
     */
    public final void map(BufferAccess access, LongConsumer callback) {
        this.checkNotMapped();
        ByteBuffer buffer = this.mapRange(0L, this.capacity, access.flags());
        try {
            this.mapped = true;
            callback.accept(PUnsafe.pork_directBufferAddress(buffer) + buffer.position());
        } finally {
            this.mapped = false;
            this.unmap();
        }
    }

    /**
     * Maps this buffer's contents into client address space and passes the mapping address to the given callback function before unmapping the buffer again.
     *
     * @param access   the ways in which the buffer data may be accessed
     * @param callback the callback function
     */
    public final void mapRange(BufferAccess access, int flags, long offset, long length, LongConsumer callback) {
        checkRangeLen(this.capacity, offset, length);
        this.checkNotMapped();
        ByteBuffer buffer = this.mapRange(offset, length, access.flags() | flags);
        try {
            this.mapped = true;
            callback.accept(PUnsafe.pork_directBufferAddress(buffer) + buffer.position());
        } finally {
            this.mapped = false;
            this.unmap();
        }
    }

    /**
     * Maps this buffer's contents into client address space and returns a reference to the mapping.
     *
     * @param access the ways in which the buffer data may be accessed
     * @return a {@link Mapping}
     */
    public final Mapping map(BufferAccess access) {
        this.checkNotMapped();
        this.mapped = true;
        return new Mapping(this.mapRange(0L, this.capacity, access.flags()));
    }

    /**
     * Maps this buffer's contents into client address space and returns a reference to the mapping.
     *
     * @param access the ways in which the buffer data may be accessed
     * @return a {@link Mapping}
     */
    public final Mapping mapRange(BufferAccess access, int flags, long offset, long length) {
        checkRangeLen(this.capacity, offset, length);
        this.checkNotMapped();
        this.mapped = true;
        return new Mapping(this.mapRange(0L, this.capacity, access.flags() | flags));
    }

    protected final ByteBuffer mapRange(long offset, long length, int access) {
        if (this.dsa) {
            return this.gl.glMapNamedBufferRange(this.id, offset, length, access, null);
        } else {
            return this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                return this.gl.glMapBufferRange(target.id(), offset, length, access, null);
            });
        }
    }

    protected final void unmap() {
        if (this.dsa) {
            this.gl.glUnmapNamedBuffer(this.id);
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glUnmapBuffer(target.id());
            });
        }
        this.mapped = false;
    }

    protected final void checkNotMapped() {
        this.checkOpen();
        if (this.mapped) {
            throw new IllegalStateException("this buffer is mapped");
        }
    }

    protected final void checkMapped() {
        this.checkOpen();
        if (!this.mapped) {
            throw new IllegalStateException("this buffer isn't mapped");
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public final class Mapping implements AutoCloseable {
        public final ByteBuffer buffer;

        public long address() {
            return PUnsafe.pork_directBufferAddress(this.buffer);
        }

        @Override
        public void close() {
            GLBuffer.this.checkMapped();
            GLBuffer.this.unmap();
        }
    }
}
