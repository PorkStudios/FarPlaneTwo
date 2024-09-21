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

package net.daporkchop.fp2.gl.buffer.upload;

import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.BufferAccess;
import net.daporkchop.fp2.gl.buffer.GLBuffer;
import net.daporkchop.fp2.gl.buffer.GLImmutableBuffer;
import net.daporkchop.fp2.gl.sync.GLFenceSync;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public final class UnsynchronizedMapBufferUploader extends BufferUploader {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty()
            .addAll(GLImmutableBuffer.REQUIRED_EXTENSIONS)
            .addAll(GLFenceSync.REQUIRED_EXTENSIONS)
            .addAll(ScratchCopyBufferUploader.REQUIRED_EXTENSIONS)
            .add(GLExtension.GL_ARB_copy_buffer)
            .add(GLExtension.GL_ARB_buffer_storage);

    private final OpenGL gl;

    private final int arenaSize;
    private int freeStart;
    private int freeSize;

    private int dirtyStart;

    private final ArrayDeque<PendingCopy> pendingCopies = new ArrayDeque<>();
    private final ArrayDeque<FlushedRegion> flushedRegions = new ArrayDeque<>();

    private final GLImmutableBuffer stagingBuffer;
    private final GLImmutableBuffer.Mapping stagingBufferMapping;

    private final BufferUploader fallback;

    public UnsynchronizedMapBufferUploader(OpenGL gl, int arenaSize) {
        gl.checkSupported(REQUIRED_EXTENSIONS);
        positive(arenaSize, "arenaSize");

        this.gl = gl;
        this.stagingBuffer = GLImmutableBuffer.create(gl, arenaSize, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_CLIENT_STORAGE_BIT);
        this.stagingBufferMapping = this.stagingBuffer.mapRange(BufferAccess.WRITE_ONLY, GL_MAP_PERSISTENT_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_FLUSH_EXPLICIT_BIT, 0L, arenaSize);

        this.fallback = new ScratchCopyBufferUploader(gl);

        this.arenaSize = arenaSize;
        this.freeStart = 0;
        this.freeSize = arenaSize;
        this.dirtyStart = 0;
    }

    @Override
    public void uploadRange(GLBuffer buffer, long offset, long addr, long size) {
        if (size > this.freeSize) { //we don't have enough space available to upload this data, use the fallback uploader
            this.fallback.uploadRange(buffer, offset, addr, size);
            return;
        }

        //delegate to the ByteBuffer overload
        //TODO: would be much cleaner to have the ByteBuffer version delegate to this one instead
        this.uploadRange(buffer, offset, DirectBufferHackery.wrapByte(addr, toInt(size)));
    }

    @Override
    public void uploadRange(GLBuffer buffer, long offset, ByteBuffer data) {
        int size = data.remaining();
        checkRangeLen(buffer.capacity(), offset, size);
        if (size > 0) {
            if (size > this.freeSize) { //we don't have enough space available to upload this data, use the fallback uploader
                this.fallback.uploadRange(buffer, offset, data);
                return;
            }

            int tailSize = this.arenaSize - this.freeStart;
            if (tailSize < size) { //there's not enough data remaining at the tail, we need to wrap around
                this.addCopy(buffer, offset, data, 0, this.freeStart, tailSize);
                this.addCopy(buffer, offset, data, tailSize, 0, size - tailSize);
            } else {
                this.addCopy(buffer, offset, data, 0, this.freeStart, size);
            }

            this.freeSize -= size;
            this.freeStart += size;
            if (this.freeStart >= this.arenaSize) {
                this.freeStart -= this.arenaSize;
            }
        }
    }

    private void addCopy(GLBuffer buffer, long offset, ByteBuffer src, int srcOffset, int dstOffset, int size) {
        copy(src, src.position() + srcOffset, this.stagingBufferMapping.buffer, dstOffset, size);
        this.pendingCopies.add(new PendingCopy(dstOffset, buffer, offset + srcOffset, size));
    }

    private static void copy(ByteBuffer src, int srcOffset, ByteBuffer dst, int dstOffset, int size) {
        src = (ByteBuffer) src.duplicate().position(srcOffset).limit(srcOffset + size);
        dst = (ByteBuffer) dst.duplicate().position(dstOffset).limit(dstOffset + size);
        dst.put(src);
    }

    @Override
    public void flush() {
        this.fallback.flush();

        if (this.pendingCopies.isEmpty()) { //nothing to flush
            return;
        }

        //flush the pending data
        int flushedSize;
        if (this.freeStart < this.dirtyStart) { //we filled up the buffer and wrapped around, need to flush the front and back
            flushedSize = (this.arenaSize - this.dirtyStart) + this.freeStart;
            this.stagingBuffer.flushMappedRange(this.dirtyStart, this.arenaSize - this.dirtyStart);
            this.stagingBuffer.flushMappedRange(0, this.freeStart);
        } else {
            flushedSize = this.freeStart - this.dirtyStart;
            this.stagingBuffer.flushMappedRange(this.dirtyStart, this.freeStart - this.dirtyStart);
        }

        //execute all the pending copies
        for (PendingCopy copy; (copy = this.pendingCopies.poll()) != null; ) {
            this.stagingBuffer.copyRange(copy.srcOffset, copy.dst, copy.dstOffset, copy.size);
        }

        //create a fence sync object which will be signalled when
        this.flushedRegions.add(new FlushedRegion(GLFenceSync.create(this.gl), flushedSize));

        //advance the pointer to the start of the dirty region
        this.dirtyStart = this.freeStart;
    }

    @Override
    public void tick() {
        this.fallback.tick();

        //poll fence sync objects to see if any previously flushed regions are available again
        while (!this.flushedRegions.isEmpty() && this.flushedRegions.peek().sync.isSignalled()) {
            FlushedRegion flushedRegion = this.flushedRegions.remove();
            flushedRegion.sync.close();
            this.freeSize += flushedRegion.size;
        }
    }

    @Override
    public long estimateCapacity() {
        //return the available space in the staging buffer (if exceeded, we'll resort to the fallback uploader)
        return this.freeSize;
    }

    @Override
    public long maximumCapacity() {
        return this.stagingBuffer.capacity();
    }

    @Override
    public void close() {
        super.close();

        for (FlushedRegion region : this.flushedRegions) {
            region.sync.close();
        }

        this.stagingBufferMapping.close();
        this.stagingBuffer.close();
        this.fallback.close();
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class PendingCopy {
        protected final int srcOffset;
        protected final GLBuffer dst;
        protected final long dstOffset;
        protected final int size;
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected static class FlushedRegion {
        protected final GLFenceSync sync;
        protected final int size;
    }
}
