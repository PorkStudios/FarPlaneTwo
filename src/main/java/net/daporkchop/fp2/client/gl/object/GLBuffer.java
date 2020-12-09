/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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
 *
 */

package net.daporkchop.fp2.client.gl.object;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.util.DirectBufferReuse;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
public final class GLBuffer extends GLObject<GLBuffer> {
    protected static final IntSet ACTIVE_TARGETS = new IntOpenHashSet();

    @Getter
    protected long capacity = -1L;

    protected final int usage;
    protected int target = -1;

    public GLBuffer(int usage) {
        super(glGenBuffers());
        this.usage = usage;
    }

    /**
     * Binds this buffer to the given binding target.
     *
     * @param target where the buffer should be bound to
     */
    public GLBuffer bind(int target) {
        checkState(this.target < 0, "buffer id=%s is already bound!", this.id);
        checkState(ACTIVE_TARGETS.add(target), "target id=%s is already active!", target);
        glBindBuffer(this.target = target, this.id);
        return this;
    }

    /**
     * Sets the capacity of this buffer.
     * <p>
     * If the buffer already has the requested capacity, nothing is changed. Otherwise, the buffer is resized and its contents are now undefined.
     *
     * @param capacity the new capacity
     */
    public void capacity(long capacity) {
        checkState(this.target >= 0, "not bound!");
        glBufferData(this.target, this.capacity = notNegative(capacity, "capacity"), this.usage);
    }

    /**
     * Sets the buffer contents.
     *
     * @param addr the base address of the data to upload
     * @param size the size of the data (in bytes)
     */
    public void upload(long addr, long size) {
        checkState(this.target >= 0, "not bound!");
        glBufferData(this.target, DirectBufferReuse.wrapByte(addr, toInt(size, "size")), this.usage);
        this.capacity = size;
    }

    /**
     * Sets the buffer contents.
     *
     * @param data the data to upload
     */
    public void upload(@NonNull ByteBuffer data) {
        checkState(this.target >= 0, "not bound!");
        glBufferData(this.target, data, this.usage);
        this.capacity = data.remaining();
    }

    /**
     * Sets the buffer contents.
     *
     * @param data the data to upload
     */
    public void upload(@NonNull IntBuffer data) {
        checkState(this.target >= 0, "not bound!");
        glBufferData(this.target, data, this.usage);
        this.capacity = data.remaining() * (long) INT_SIZE;
    }

    /**
     * Sets the buffer contents.
     *
     * @param data the data to upload
     */
    public void upload(@NonNull FloatBuffer data) {
        checkState(this.target >= 0, "not bound!");
        glBufferData(this.target, data, this.usage);
        this.capacity = data.remaining() * (long) FLOAT_SIZE;
    }

    /**
     * Sets the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param addr  the base address of the data to upload
     * @param size  the size of the data (in bytes)
     */
    public void uploadRange(long start, long addr, long size) {
        checkState(this.target >= 0, "not bound!");
        checkRangeLen(this.capacity, start, size);
        glBufferSubData(this.target, start, DirectBufferReuse.wrapByte(addr, toInt(size, "size")));
    }

    /**
     * Sets the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the data to upload
     */
    public void uploadRange(long start, @NonNull ByteBuffer data) {
        checkState(this.target >= 0, "not bound!");
        checkRangeLen(this.capacity, start, data.remaining());
        glBufferSubData(this.target, start, data);
    }

    /**
     * Sets the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the data to upload
     */
    public void uploadRange(long start, @NonNull IntBuffer data) {
        checkState(this.target >= 0, "not bound!");
        checkRangeLen(this.capacity, start, data.remaining() * (long) INT_SIZE);
        glBufferSubData(this.target, start, data);
    }

    /**
     * Sets the buffer contents in a certain range.
     *
     * @param start the offset of the range inside the buffer (in bytes)
     * @param data  the data to upload
     */
    public void uploadRange(long start, @NonNull FloatBuffer data) {
        checkState(this.target >= 0, "not bound!");
        checkRangeLen(this.capacity, start, data.remaining() * (long) FLOAT_SIZE);
        glBufferSubData(this.target, start, data);
    }

    @Override
    public void close() {
        checkState(this.target >= 0, "not bound!");
        checkState(ACTIVE_TARGETS.remove(this.target));
        glBindBuffer(this.target, 0);
        this.target = -1;
    }

    @Override
    protected Runnable delete(int id) {
        return () -> glDeleteBuffers(id);
    }

    /**
     * Binds this buffer to a shader buffer binding slot.
     *
     * @param target the binding type
     * @param index  the binding index
     */
    public void bindBase(int target, int index) {
        glBindBufferBase(target, index, this.id);
    }
}
