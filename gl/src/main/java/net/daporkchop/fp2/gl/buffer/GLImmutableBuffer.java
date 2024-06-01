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
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.lib.unsafe.PUnsafe;

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

    public static GLImmutableBuffer create(OpenGL gl, @NonNull ByteBuffer buffer, int flags) {
        return create(gl, buffer.remaining(), PUnsafe.pork_directBufferAddress(buffer) + buffer.position(), flags);
    }

    public static GLImmutableBuffer create(OpenGL gl, long size, int flags) {
        return create(gl, size, 0L, flags);
    }

    public static GLImmutableBuffer create(OpenGL gl, long size, long data, int flags) {
        gl.checkSupported(REQUIRED_EXTENSIONS);
        return new GLImmutableBuffer(gl, size, data, flags);
    }

    private final int flags;

    private GLImmutableBuffer(OpenGL gl, long size, long data, int flags) {
        super(gl);
        this.capacity = size;
        this.flags = flags;

        if (this.dsa) {
            gl.glNamedBufferStorage(this.id, size, data, flags);
        } else {
            this.bind(BufferTarget.ARRAY_BUFFER, target -> {
                this.gl.glBufferStorage(target.id(), size, data, flags);
            });
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
