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

package net.daporkchop.fp2.gl.buffer.download;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.BufferAccess;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.buffer.GLImmutableBuffer;
import net.daporkchop.fp2.gl.sync.GLFenceSync;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.function.Consumer;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Utility for downloading small bits of data from the GPU asynchronously, without stalling the GPU pipeline.
 *
 * @author DaPorkchop_
 */
public final class AsynchronousSmallBufferDownloader implements AutoCloseable {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_copy_buffer)
            .addAll(GLFenceSync.REQUIRED_EXTENSIONS)
            .add(GLExtension.GL_ARB_shader_image_load_store) //glMemoryBarrier()
            .addAll(GLImmutableBuffer.REQUIRED_EXTENSIONS); //persistently mapped buffers

    private final OpenGL gl;
    private final int maxTransferSize;
    private final int maxConcurrentTransfers;

    private final GLImmutableBuffer cpuBuffer;
    private final GLImmutableBuffer.Mapping cpuBufferMapping;

    private final ArrayDeque<FrameEntry> inFlightFrameEntries = new ArrayDeque<>();
    private int freeStart;
    private int freeCount;

    public AsynchronousSmallBufferDownloader(@NonNull OpenGL gl, @Positive int maxTransferSize, @Positive int maxConcurrentTransfers) {
        gl.checkSupported(REQUIRED_EXTENSIONS);

        try {
            this.gl = gl;
            this.maxTransferSize = positive(maxTransferSize, "maxTransferSize");
            this.maxConcurrentTransfers = positive(maxConcurrentTransfers, "maxConcurrentTransfers");

            this.freeCount = maxConcurrentTransfers;

            int bufferSize = Math.multiplyExact(maxTransferSize, maxConcurrentTransfers);

            this.cpuBuffer = GLImmutableBuffer.create(gl, bufferSize, GL_MAP_READ_BIT | GL_MAP_PERSISTENT_BIT | GL_CLIENT_STORAGE_BIT);
            this.cpuBufferMapping = this.cpuBuffer.mapRange(BufferAccess.READ_ONLY, GL_MAP_PERSISTENT_BIT, 0L, bufferSize);
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    /**
     * Begins an asynchronous download of the data in the given range of the given buffer.
     *
     * @param src       the buffer to download from
     * @param srcOffset the offset of the range inside the buffer (in bytes)
     * @param size      the size of the data (in bytes)
     * @param callback  a function which will be called with a {@link ByteBuffer} pointing to the downloaded data when it arrives. The callback must copy the
     *                  data to a separately allocated buffer if it wishes to keep the data alive once the callback returns.
     */
    public void downloadRange(@NonNull GLBuffer src, @NotNegative long srcOffset, @NotNegative int size, @NonNull Consumer<ByteBuffer> callback) {
        checkState(notNegative(size, "size") <= this.maxTransferSize, "per-frame size limit (%s) exceeded!", this.maxTransferSize);
        checkState(this.freeCount > 0, "render-ahead limit (%s) exceeded!", this.maxConcurrentTransfers);

        int activeSlot = this.freeStart;
        this.freeCount--;
        this.freeStart++;
        if (this.freeStart == this.maxConcurrentTransfers) {
            this.freeStart = 0;
        }

        //ensure updates are complete before downloading the data to the CPU
        this.gl.glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);

        //download data to the CPU-side buffer
        src.copyRange(srcOffset, this.cpuBuffer, (long) activeSlot * this.maxTransferSize, size);

        //place a fence sync so we can be notified when the copy is complete
        this.inFlightFrameEntries.add(new FrameEntry(GLFenceSync.create(this.gl), callback, activeSlot, size));
    }

    /**
     * Should be called periodically (roughly once per frame) to update any internal state
     */
    public void tick() {
        //poll fence sync objects to see if any previously completed entries are available again
        while (!this.inFlightFrameEntries.isEmpty() && this.inFlightFrameEntries.peek().sync.isSignalled()) {
            FrameEntry entry = this.inFlightFrameEntries.remove();
            entry.close();
            this.freeCount++;

            //an entry was completed, so its data is now available! update the current result by reading the data out of the corresponding slot
            //  in the CPU-side buffer.
            //using a persistently mapped buffer means that this is guaranteed to be non-blocking - at least on NVIDIA, glGetBufferSubData() seems to
            //  stall the pipeline even when the buffer region we're accessing is not being modified. we could probably abuse asynchronous pixel transfers
            //  to pull this off without relying on GL_ARB_buffer_storage, but frankly i don't think there's any relevant hardware out there which
            //  supports atomic counters but doesn't support GL_ARB_buffer_storage, so it's probably not worth the hassle.
            entry.callback.accept((ByteBuffer) this.cpuBufferMapping.buffer.clear()
                    .position(entry.slot * this.maxTransferSize)
                    .limit(entry.slot * this.maxTransferSize + entry.size));
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(
                this.cpuBufferMapping,
                this.cpuBuffer,
                PResourceUtil.lazyCloseAll(this.inFlightFrameEntries));
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static final class FrameEntry implements AutoCloseable {
        final GLFenceSync sync;
        final Consumer<ByteBuffer> callback;
        final int slot;
        final int size;

        @Override
        public void close() {
            this.sync.close();
        }
    }
}
