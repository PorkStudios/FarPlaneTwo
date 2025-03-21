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

package net.daporkchop.fp2.gl.buffer;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.util.AnyMemoryRegion;
import net.daporkchop.fp2.gl.util.GLObject;
import net.daporkchop.fp2.gl.util.GLRequires;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An OpenGL buffer.
 *
 * @author DaPorkchop_
 */
public abstract class GLBuffer extends GLObject.Normal {
    /**
     * Creates an OpenGL buffer object with "functionally immutable" storage with the given capacity and uninitialized data.
     * <p>
     * If supported, creates an {@link GLImmutableBuffer immutable buffer}, otherwise falls back to a {@link GLMutableBuffer mutable buffer}. This is intended to be used in situations where a buffer's
     * capacity is known to be constant at construction time.
     *
     * @param gl             the OpenGL context
     * @param capacity       the buffer's capacity
     * @param mutableUsage   the buffer's usage, if it's created with mutable storage
     * @param immutableFlags the buffer's storage flags, if it's created with immutable storage
     * @return the created buffer
     */
    public static GLBuffer createFunctionallyImmutable(@NonNull OpenGL gl, @NotNegative long capacity, @NonNull BufferUsage mutableUsage, int immutableFlags) {
        return gl.supports(GLImmutableBuffer.REQUIRED_EXTENSIONS)
                ? GLImmutableBuffer.create(gl, capacity, immutableFlags)
                : GLMutableBuffer.create(gl, capacity, mutableUsage);
    }

    /**
     * Creates an OpenGL buffer object with "functionally immutable" storage initialized to the given data.
     * <p>
     * If supported, creates an {@link GLImmutableBuffer immutable buffer}, otherwise falls back to a {@link GLMutableBuffer mutable buffer}. This is intended to be used in situations where a buffer's
     * capacity is known to be constant at construction time.
     *
     * @param gl             the OpenGL context
     * @param data           the buffer's initial data, also used to determine the buffer's capacity
     * @param mutableUsage   the buffer's usage, if it's created with mutable storage
     * @param immutableFlags the buffer's storage flags, if it's created with immutable storage
     * @return the created buffer
     */
    public static GLBuffer createFunctionallyImmutable(@NonNull OpenGL gl, @NonNull ByteBuffer data, @NonNull BufferUsage mutableUsage, int immutableFlags) {
        return gl.supports(GLImmutableBuffer.REQUIRED_EXTENSIONS)
                ? GLImmutableBuffer.create(gl, data, immutableFlags)
                : GLMutableBuffer.create(gl, data, mutableUsage);
    }

    /**
     * Creates an OpenGL buffer object with "functionally immutable" storage initialized to the given data.
     * <p>
     * If supported, creates an {@link GLImmutableBuffer immutable buffer}, otherwise falls back to a {@link GLMutableBuffer mutable buffer}. This is intended to be used in situations where a buffer's
     * capacity is known to be constant at construction time.
     *
     * @param gl             the OpenGL context
     * @param data           the buffer's initial data, also used to determine the buffer's capacity
     * @param mutableUsage   the buffer's usage, if it's created with mutable storage
     * @param immutableFlags the buffer's storage flags, if it's created with immutable storage
     * @return the created buffer
     */
    public static GLBuffer createFunctionallyImmutable(@NonNull OpenGL gl, @NonNull AnyMemoryRegion data, @NonNull BufferUsage mutableUsage, int immutableFlags) {
        return gl.supports(GLImmutableBuffer.REQUIRED_EXTENSIONS)
                ? GLImmutableBuffer.create(gl, data, immutableFlags)
                : GLMutableBuffer.create(gl, data, mutableUsage);
    }

    /**
     * Creates an OpenGL buffer object with "functionally immutable" storage initialized to a copy of the given range of the given buffer.
     * <p>
     * This behaves like {@link java.util.Arrays#copyOfRange}, with the difference that the second integer argument is the capacity of
     * the new buffer instead of an upper bound, and that the new buffer is extended with undefined contents at offsets greater than
     * {@code original.capacity() - from}.
     *
     * @param gl             the OpenGL context
     * @param original       the buffer from which a range is to be copied
     * @param from           the initial index of the range to be copied, inclusive
     * @param length         the length of the range to be copied
     * @param mutableUsage   the buffer's usage, if it's created with mutable storage
     * @param immutableFlags the buffer's storage flags, if it's created with immutable storage
     * @return the created buffer
     * @throws UnsupportedOperationException if {@link GLExtension#GL_ARB_copy_buffer ARB_copy_buffer} isn't supported
     * @apiNote requires {@link GLExtension#GL_ARB_copy_buffer GL_ARB_copy_buffer}
     */
    @GLRequires(GLExtension.GL_ARB_copy_buffer)
    public static GLBuffer createFunctionallyImmutableCopyOfRange(@NonNull OpenGL gl, @NonNull GLBuffer original, @NotNegative long from, @NotNegative long length, @NonNull BufferUsage mutableUsage, int immutableFlags) {
        return gl.supports(GLImmutableBuffer.REQUIRED_EXTENSIONS)
                ? GLImmutableBuffer.createCopyOfRange(gl, original, from, length, immutableFlags)
                : GLMutableBuffer.createCopyOfRange(gl, original, from, length, mutableUsage);
    }

    protected final boolean clearBufferObject;
    protected final boolean invalidateSubdata;

    protected long capacity = -1L;
    protected boolean mapped = false;

    protected GLBuffer(OpenGL gl) {
        super(gl, gl.supports(GLExtension.GL_ARB_direct_state_access) ? gl.glCreateBuffer() : gl.glGenBuffer());
        this.clearBufferObject = gl.supports(GLExtension.GL_ARB_clear_buffer_object);
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
    protected final int debugLabelNamespace() {
        return GL_BUFFER;
    }

    /**
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param addr  the base address of the data to upload
     * @param size  the size of the data (in bytes)
     */
    public final void bufferSubData(@NotNegative long start, long addr, @NotNegative long size) {
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
     * @param data  the data to upload
     */
    public final void bufferSubData(@NotNegative long start, @NonNull ByteBuffer data) {
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
     * Updates the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the data to upload
     */
    public final void bufferSubData(@NotNegative long start, @NonNull AnyMemoryRegion data) {
        this.checkOpen();
        checkRangeLen(this.capacity, start, data.size);
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
    public final void getBufferSubData(@NotNegative long start, long addr, @NotNegative long size) {
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
     * @param data  where the data should be stored
     */
    public final void getBufferSubData(@NotNegative long start, @NonNull ByteBuffer data) {
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
     * Downloads the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  where the data should be stored
     */
    public final void getBufferSubData(@NotNegative long start, @NonNull AnyMemoryRegion data) {
        this.checkOpen();
        checkRangeLen(this.capacity, start, data.size);
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
    public final void copyRange(@NonNull GLBuffer src, @NotNegative long srcOffset, @NotNegative long dstOffset, @NotNegative long size) {
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
    public final void copyRange(@NotNegative long srcOffset, @NonNull GLBuffer dst, @NotNegative long dstOffset, @NotNegative long size) {
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
     * Clears the buffer contents to zero bytes.
     */
    public final void clearBufferDataZero() {
        this.checkOpen();
        if (this.dsa & this.clearBufferObject) {
            this.gl.glClearNamedBufferData(this.id, GL_R8, GL_RED, GL_UNSIGNED_BYTE, null);
        } else if (this.clearBufferObject) {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glClearBufferData(target.id(), GL_R8, GL_RED, GL_UNSIGNED_BYTE, null);
            });
        } else {
            this.clearBufferSubDataZero(0L, this.capacity);
        }
    }

    /**
     * Clears the buffer contents in a certain range to zero bytes.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param size  the number of bytes to clear
     */
    public final void clearBufferSubDataZero(@NotNegative long start, @NotNegative long size) {
        this.checkOpen();
        checkRangeLen(this.capacity, start, size);
        if (size > 0L) {
            if (this.dsa & this.clearBufferObject) {
                this.gl.glClearNamedBufferSubData(this.id, GL_R8, start, size, GL_RED, GL_UNSIGNED_BYTE, null);
            } else if (this.clearBufferObject) {
                this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                    this.gl.glClearBufferSubData(target.id(), GL_R8, start, size, GL_RED, GL_UNSIGNED_BYTE, null);
                });
            } else {
                //slow fallback approach: map the entire range and fill it with zeroes (invalidating the range in the process)
                //TODO: if this is an immutable buffer this may fail...
                this.mapRange(BufferAccess.WRITE_ONLY, GL_MAP_INVALIDATE_RANGE_BIT, start, size, mapping -> {
                    PUnsafe.setMemory(DirectBufferHackery.address(mapping), size, (byte) 0);
                });
            }
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
    public final void map(BufferAccess access, Consumer<ByteBuffer> callback) {
        this.checkNotMapped();
        ByteBuffer buffer = this.mapRange(0L, this.capacity, access.flags());
        try {
            this.mapped = true;
            callback.accept(buffer);
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
    public final void map(BufferAccess access, int flags, Consumer<ByteBuffer> callback) {
        this.checkNotMapped();
        ByteBuffer buffer = this.mapRange(0L, this.capacity, access.flags() | flags);
        try {
            this.mapped = true;
            callback.accept(buffer);
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
    public final void mapRange(BufferAccess access, int flags, @NotNegative long offset, @NotNegative long length, Consumer<ByteBuffer> callback) {
        checkRangeLen(this.capacity, offset, length);
        this.checkNotMapped();
        ByteBuffer buffer = this.mapRange(offset, length, access.flags() | flags);
        try {
            this.mapped = true;
            callback.accept(buffer);
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
    public final Mapping map(BufferAccess access, int flags) {
        this.checkNotMapped();
        this.mapped = true;
        return new Mapping(this.mapRange(0L, this.capacity, access.flags() | flags));
    }

    /**
     * Maps this buffer's contents into client address space and returns a reference to the mapping.
     *
     * @param access the ways in which the buffer data may be accessed
     * @return a {@link Mapping}
     */
    public final Mapping mapRange(BufferAccess access, int flags, @NotNegative long offset, @NotNegative long length) {
        checkRangeLen(this.capacity, offset, length);
        this.checkNotMapped();
        this.mapped = true;
        return new Mapping(this.mapRange(0L, this.capacity, access.flags() | flags));
    }

    private ByteBuffer mapRange(long offset, long length, int access) {
        checkArg(length <= Integer.MAX_VALUE, "requested mapping length cannot be represented in an int: %s", length);

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

        @Override
        public void close() {
            GLBuffer.this.checkMapped();
            GLBuffer.this.unmap();
        }
    }
}
