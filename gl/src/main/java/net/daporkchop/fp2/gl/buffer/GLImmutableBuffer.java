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

import lombok.NonNull;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.AnyMemoryRegion;
import net.daporkchop.fp2.gl.util.GLRequires;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.nio.ByteBuffer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * An OpenGL buffer with immutable storage.
 *
 * @author DaPorkchop_
 */
public final class GLImmutableBuffer extends GLBuffer {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_buffer_storage);

    /**
     * Creates a new immutable buffer with the given capacity and uninitialized data.
     *
     * @param gl       the OpenGL context
     * @param capacity the buffer's capacity
     * @param flags    the buffer's storage flags
     * @return the created buffer
     */
    public static GLImmutableBuffer create(@NonNull OpenGL gl, @Positive long capacity, int flags) {
        gl.checkSupported(REQUIRED_EXTENSIONS);
        return new GLImmutableBuffer(gl, positive(capacity, "capacity"), 0L, flags);
    }

    /**
     * Creates a new immutable buffer initialized to the given data.
     *
     * @param gl    the OpenGL context
     * @param data  the buffer's initial data, also used to determine the buffer's capacity
     * @param flags the buffer's storage flags
     * @return the created buffer
     */
    public static GLImmutableBuffer create(@NonNull OpenGL gl, @NonNull ByteBuffer data, int flags) {
        gl.checkSupported(REQUIRED_EXTENSIONS);
        return new GLImmutableBuffer(gl, data.remaining(), DirectBufferHackery.address(data), flags);
    }

    /**
     * Creates a new immutable buffer initialized to the given data.
     *
     * @param gl    the OpenGL context
     * @param data  the buffer's initial data, also used to determine the buffer's capacity
     * @param flags the buffer's storage flags
     * @return the created buffer
     */
    public static GLImmutableBuffer create(@NonNull OpenGL gl, @NonNull AnyMemoryRegion data, int flags) {
        gl.checkSupported(REQUIRED_EXTENSIONS);
        return new GLImmutableBuffer(gl, data, flags);
    }

    /**
     * Creates a new immutable buffer  initialized to a copy of the given range of the given buffer.
     * <p>
     * This behaves like {@link java.util.Arrays#copyOfRange}, with the difference that the second integer argument is the capacity of
     * the new buffer instead of an upper bound, and that the new buffer is extended with undefined contents at offsets greater than
     * {@code original.capacity() - from}.
     *
     * @param gl       the OpenGL context
     * @param original the buffer from which a range is to be copied
     * @param from     the initial index of the range to be copied, inclusive
     * @param length   the length of the range to be copied
     * @param flags    the buffer's storage flags
     * @return the created buffer
     * @throws UnsupportedOperationException if {@link GLExtension#GL_ARB_copy_buffer ARB_copy_buffer} isn't supported
     * @apiNote requires {@link GLExtension#GL_ARB_copy_buffer GL_ARB_copy_buffer}
     */
    @GLRequires(GLExtension.GL_ARB_copy_buffer)
    public static GLImmutableBuffer createCopyOfRange(@NonNull OpenGL gl, @NonNull GLBuffer original, @NotNegative long from, @NotNegative long length, int flags) {
        GLImmutableBuffer result = create(gl, length, flags);
        try {
            result.copyRange(original, from, 0L, Math.min(original.capacity() - from, length));
            return result;
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, result);
        }
    }

    private final int flags;

    private GLImmutableBuffer(OpenGL gl, long capacity, long data, int flags) {
        super(gl);

        try {
            this.capacity = positive(capacity, "capacity");
            this.flags = flags;

            if (this.dsa) {
                gl.glNamedBufferStorage(this.id, capacity, data, flags);
            } else {
                this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                    gl.glBufferStorage(target.id(), capacity, data, flags);
                });
            }
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    private GLImmutableBuffer(OpenGL gl, AnyMemoryRegion data, int flags) {
        super(gl);

        try {
            this.capacity = data.size;
            this.flags = flags;

            if (this.dsa) {
                gl.glNamedBufferStorage(this.id, data, flags);
            } else {
                this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                    gl.glBufferStorage(target.id(), data, flags);
                });
            }
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    /**
     * Flushes the given range of this buffer, assuming that it's currently mapped with {@link net.daporkchop.fp2.gl.OpenGLConstants#GL_MAP_FLUSH_EXPLICIT_BIT}.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param size  the size of the range to flush (in bytes)
     */
    public void flushMappedRange(long start, long size) {
        checkRangeLen(this.capacity, start, size);
        this.checkOpen();
        this.checkMapped();

        if (this.dsa) {
            this.gl.glFlushMappedNamedBufferRange(this.id, start, size);
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glFlushMappedBufferRange(target.id(), start, size);
            });
        }
    }
}
