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

package net.daporkchop.fp2.common.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.annotation.TransferOwnership;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.pool.recycler.Recycler;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Allows unsafe creation of NIO buffers which wrap a direct memory pointer without a cleaner.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class DirectBufferHackery {
    static {
        PUnsafe.requireUnalignedAccess();
    }

    private final long BUFFER_CAPACITY_OFFSET = PUnsafe.pork_getOffset(Buffer.class, "capacity");
    private final long BUFFER_ADDRESS_OFFSET = PUnsafe.pork_getOffset(Buffer.class, "address");

    private final Class<ByteBuffer> BYTE = PorkUtil.classForName("java.nio.DirectByteBuffer");
    private final Class<IntBuffer> INT = PorkUtil.classForName("java.nio.DirectIntBufferU");
    private final Class<FloatBuffer> FLOAT = PorkUtil.classForName("java.nio.DirectFloatBufferU");
    private final Class<DoubleBuffer> DOUBLE = PorkUtil.classForName("java.nio.DirectDoubleBufferU");

    private static final Cached<Recycler<ByteBuffer>> FAKE_BYTEBUFFER_RECYCLER = Cached.threadLocal(() -> Recycler.unbounded(DirectBufferHackery::emptyByte), ReferenceStrength.SOFT);
    private static final Cached<Recycler<IntBuffer>> FAKE_INTBUFFER_RECYCLER = Cached.threadLocal(() -> Recycler.unbounded(DirectBufferHackery::emptyInt), ReferenceStrength.SOFT);
    private static final Cached<Recycler<FloatBuffer>> FAKE_FLOATBUFFER_RECYCLER = Cached.threadLocal(() -> Recycler.unbounded(DirectBufferHackery::emptyFloat), ReferenceStrength.SOFT);
    private static final Cached<Recycler<DoubleBuffer>> FAKE_DOUBLEBUFFER_RECYCLER = Cached.threadLocal(() -> Recycler.unbounded(DirectBufferHackery::emptyDouble), ReferenceStrength.SOFT);

    public <B extends Buffer> B reset(@NonNull B buffer, long address, int capacity) {
        checkState(buffer.isDirect(), "buffer isn't direct! %s", buffer);

        PUnsafe.putInt(buffer, BUFFER_CAPACITY_OFFSET, capacity);
        PUnsafe.putLong(buffer, BUFFER_ADDRESS_OFFSET, address);
        buffer.clear();
        return buffer;
    }

    public ByteBuffer emptyByte() {
        ByteBuffer buffer = PUnsafe.allocateInstance(BYTE);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    public static ByteBuffer wrapByte(long address, int capacity) {
        ByteBuffer buffer = emptyByte();
        reset(buffer, address, capacity);
        return buffer;
    }

    /**
     * Gets a {@link Recycler} for fake direct {@link ByteBuffer}s whose contents are undefined, but may be redirected to a requested memory address using
     * {@link #reset(Buffer, long, int)}.
     * <p>
     * The returned {@link Recycler} is only valid in the current thread!
     *
     * @return a {@link Recycler} for fake direct buffers
     * @see #reset(Buffer, long, int)
     */
    public static Recycler<ByteBuffer> byteRecycler() {
        return FAKE_BYTEBUFFER_RECYCLER.get();
    }

    public IntBuffer emptyInt() {
        return PUnsafe.allocateInstance(INT);
    }

    public static IntBuffer wrapInt(long address, int capacity) {
        IntBuffer buffer = emptyInt();
        reset(buffer, address, capacity);
        return buffer;
    }

    /**
     * Gets a {@link Recycler} for fake direct {@link IntBuffer}s whose contents are undefined, but may be redirected to a requested memory address using
     * {@link #reset(Buffer, long, int)}.
     * <p>
     * The returned {@link Recycler} is only valid in the current thread!
     *
     * @return a {@link Recycler} for fake direct buffers
     * @see #reset(Buffer, long, int)
     */
    public static Recycler<IntBuffer> intRecycler() {
        return FAKE_INTBUFFER_RECYCLER.get();
    }

    public FloatBuffer emptyFloat() {
        return PUnsafe.allocateInstance(FLOAT);
    }

    public static FloatBuffer wrapFloat(long address, int capacity) {
        FloatBuffer buffer = emptyFloat();
        reset(buffer, address, capacity);
        return buffer;
    }

    /**
     * Gets a {@link Recycler} for fake direct {@link FloatBuffer}s whose contents are undefined, but may be redirected to a requested memory address using
     * {@link #reset(Buffer, long, int)}.
     * <p>
     * The returned {@link Recycler} is only valid in the current thread!
     *
     * @return a {@link Recycler} for fake direct buffers
     * @see #reset(Buffer, long, int)
     */
    public static Recycler<FloatBuffer> floatRecycler() {
        return FAKE_FLOATBUFFER_RECYCLER.get();
    }

    public DoubleBuffer emptyDouble() {
        return PUnsafe.allocateInstance(DOUBLE);
    }

    public static DoubleBuffer wrapDouble(long address, int capacity) {
        DoubleBuffer buffer = emptyDouble();
        reset(buffer, address, capacity);
        return buffer;
    }

    /**
     * Gets a {@link Recycler} for fake direct {@link DoubleBuffer}s whose contents are undefined, but may be redirected to a requested memory address using
     * {@link #reset(Buffer, long, int)}.
     * <p>
     * The returned {@link Recycler} is only valid in the current thread!
     *
     * @return a {@link Recycler} for fake direct buffers
     * @see #reset(Buffer, long, int)
     */
    public static Recycler<DoubleBuffer> doubleRecycler() {
        return FAKE_DOUBLEBUFFER_RECYCLER.get();
    }

    /**
     * Allocates off-heap memory.
     * <p>
     * Allocations <strong>must</strong> be {@link #freeTemporary(long, long) freed} (use try-with-resources) in allocation order!
     *
     * @param size the size of the allocation
     * @return a pointer to the allocation base address
     */
    public static long allocateTemporary(@NotNegative long size) {
        if (size <= 0L) {
            return 0L;
        }

        //TODO: i want to add some kind of thread-local bump allocator for small allocations
        return PUnsafe.allocateMemory(size);
    }

    /**
     * Frees off-heap memory allocated by {@link #allocateTemporary(long)}.
     *
     * @param address the allocation base address, as returned by {@link #allocateTemporary(long)}
     * @param size    the size of the allocation, as given to {@link #allocateTemporary(long)}
     */
    public static void freeTemporary(@TransferOwnership long address, @NotNegative long size) {
        if (size > 0L) {
            PUnsafe.freeMemory(address);
        }
    }
}
