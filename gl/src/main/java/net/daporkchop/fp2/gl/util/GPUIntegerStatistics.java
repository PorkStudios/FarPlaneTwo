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

package net.daporkchop.fp2.gl.util;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.BufferAccess;
import net.daporkchop.fp2.gl.buffer.GLImmutableBuffer;
import net.daporkchop.fp2.gl.sync.GLFenceSync;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.common.closeable.PResourceUtil;
import net.daporkchop.lib.common.misc.Tuple;

import java.nio.IntBuffer;
import java.util.ArrayDeque;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A set of GPU-side counters which are asynchronously copied to the CPU.
 *
 * @author DaPorkchop_
 */
public final class GPUIntegerStatistics<UD> implements AutoCloseable {
    public static final GLExtensionSet REQUIRED_EXTENSIONS = GLExtensionSet.empty()
            .add(GLExtension.GL_ARB_shader_atomic_counters)
            .add(GLExtension.GL_ARB_copy_buffer)
            .addAll(GLFenceSync.REQUIRED_EXTENSIONS)
            .add(GLExtension.GL_ARB_shader_image_load_store) //glMemoryBarrier()
            .addAll(GLImmutableBuffer.REQUIRED_EXTENSIONS); //persistently mapped buffers

    private final OpenGL gl;
    private final int intsPerFrame;
    private final int maxRenderahead;

    private final GLImmutableBuffer gpuBuffer;
    private final GLImmutableBuffer cpuBuffer;
    private final GLImmutableBuffer.Mapping cpuBufferMapping;

    private final ArrayDeque<FrameEntry<UD>> inFlightFrameEntries = new ArrayDeque<>();
    private int freeStart;
    private int freeSize;

    private int activeSlot = -1;

    private final int[] currentResult;
    private UD currentResultUserData;

    public GPUIntegerStatistics(@NonNull OpenGL gl, @Positive int intsPerFrame, @Positive int maxRenderahead) {
        gl.checkSupported(REQUIRED_EXTENSIONS);

        try {
            this.gl = gl;
            this.intsPerFrame = positive(intsPerFrame, "intsPerFrame");
            this.maxRenderahead = positive(maxRenderahead, "maxRenderahead");

            this.currentResult = new int[intsPerFrame];
            this.freeSize = maxRenderahead;

            int frameSize = Math.multiplyExact(intsPerFrame, Integer.BYTES);
            int bufferSize = Math.multiplyExact(frameSize, maxRenderahead);

            this.gpuBuffer = GLImmutableBuffer.create(gl, frameSize, 0);
            this.cpuBuffer = GLImmutableBuffer.create(gl, bufferSize, GL_MAP_READ_BIT | GL_MAP_PERSISTENT_BIT | GL_CLIENT_STORAGE_BIT);

            this.cpuBufferMapping = this.cpuBuffer.mapRange(BufferAccess.READ_ONLY, GL_MAP_PERSISTENT_BIT, 0L, bufferSize);
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    /**
     * Prepares to dispatch shader(s) which will write statistics to an atomic counter buffer.
     * <p>
     * Once complete, {@link #endFrame(Object)} must be called to finish the statistics update.
     *
     * @param bindingIndex the index to bind the atomic counter buffer to
     */
    public void beginFrame(@NotNegative int bindingIndex) {
        checkState(this.activeSlot < 0, "a frame is already being rendered!");
        checkState(this.freeSize > 0, "render-ahead limit (%s) exceeded!", this.maxRenderahead);
        checkIndex(this.gl.limits().maxAtomicCounterBufferBindings(), bindingIndex);

        this.activeSlot = this.freeStart;

        //clear the GPU counters to 0
        this.gpuBuffer.clearBufferDataZero();

        this.gl.glBindBufferBase(GL_ATOMIC_COUNTER_BUFFER, bindingIndex, this.gpuBuffer.id());

        this.freeSize--;
        this.freeStart++;
        if (this.freeStart == this.maxRenderahead) {
            this.freeStart = 0;
        }
    }

    /**
     * Finishes accumulating statistics started by a previous call to {@link #beginFrame(int)}.
     *
     * @param userData an arbitrary additional object which will be returned alongside the corresponding statistics when the data becomes available.
     */
    public void endFrame(UD userData) {
        checkState(this.activeSlot >= 0, "no frame is currently being rendered!");

        //ensure updates are complete before downloading the data to the CPU
        this.gl.glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);

        //download data to the CPU-side buffer
        this.gpuBuffer.copyRange(0L, this.cpuBuffer, this.activeSlot * this.gpuBuffer.capacity(), this.gpuBuffer.capacity());

        //place a fence sync so we can be notified when the copy is complete
        this.inFlightFrameEntries.add(new FrameEntry<>(GLFenceSync.create(this.gl), userData, this.activeSlot));

        this.activeSlot = -1;
    }

    /**
     * Should be called periodically (roughly once per frame) to update any internal state
     */
    public void tick() {
        //poll fence sync objects to see if any previously completed entries are available again
        FrameEntry<UD> lastCompletedEntry = null;
        while (!this.inFlightFrameEntries.isEmpty() && this.inFlightFrameEntries.peek().sync.isSignalled()) {
            lastCompletedEntry = this.inFlightFrameEntries.remove();
            lastCompletedEntry.close();
            this.freeSize++;
        }

        if (lastCompletedEntry != null) {
            //an entry was completed, so its data is now available! update the current result by reading the data out of the corresponding slot
            //  in the CPU-side buffer.
            //using a persistently mapped buffer means that this is guaranteed to be non-blocking - at least on NVIDIA, glGetBufferSubData() seems to
            //  stall the pipeline even when the buffer region we're accessing is not being modified. we could probably abuse asynchronous pixel transfers
            //  to pull this off without relying on GL_ARB_buffer_storage, but frankly i don't think there's any relevant hardware out there which
            //  supports atomic counters but doesn't support GL_ARB_buffer_storage, so it's probably not worth the hassle.
            this.currentResultUserData = lastCompletedEntry.userData;
            ((IntBuffer) this.cpuBufferMapping.buffer.asIntBuffer().position(lastCompletedEntry.slot * this.intsPerFrame)).get(this.currentResult);
        }
    }

    /**
     * @return an {@code int[]} containing the values from the most recently completed frame whose data is available. Contains all zeroes if no frames have been completed yet.
     */
    public Tuple<UD, int[]> get() {
        return new Tuple<>(this.currentResultUserData, this.currentResult.clone());
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(
                this.cpuBufferMapping,
                this.cpuBuffer,
                this.gpuBuffer,
                PResourceUtil.lazyCloseAll(this.inFlightFrameEntries));
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static final class FrameEntry<UD> implements AutoCloseable {
        final GLFenceSync sync;
        final UD userData;
        final int slot;

        @Override
        public void close() {
            this.sync.close();
        }
    }
}
