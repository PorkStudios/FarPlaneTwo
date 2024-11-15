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

import lombok.NonNull;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.AnyMemoryRegion;
import net.daporkchop.lib.common.annotation.param.NotNegative;
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
     * @param gl the OpenGL context
     * @param capacity the buffer's capacity
     * @param flags the buffer's storage flags
     * @return the created buffer
     */
    public static GLImmutableBuffer create(@NonNull OpenGL gl, @NotNegative long capacity, int flags) {
        gl.checkSupported(REQUIRED_EXTENSIONS);
        return new GLImmutableBuffer(gl, notNegative(capacity, "capacity"), 0L, flags);
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

    private final int flags;

    private GLImmutableBuffer(OpenGL gl, long size, long data, int flags) {
        super(gl);

        try {
            this.capacity = size;
            this.flags = flags;

            if (this.dsa) {
                gl.glNamedBufferStorage(this.id, size, data, flags);
            } else {
                this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                    gl.glBufferStorage(target.id(), size, data, flags);
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
