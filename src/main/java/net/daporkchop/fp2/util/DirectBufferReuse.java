/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.system.PlatformInfo;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Allows me to use direct memory directly, and not allocate a new wrapping buffer for every LWJGL method call.
 * <p>
 * Absolutely not thread-safe!
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class DirectBufferReuse {
    static {
        checkState(PlatformInfo.UNALIGNED, "Unaligned memory access not supported?!? (what kind of computer are you running this on lol)");
    }

    public final long BUFFER_CAPACITY_OFFSET = PUnsafe.pork_getOffset(Buffer.class, "capacity");
    public final long BUFFER_ADDRESS_OFFSET = PUnsafe.pork_getOffset(Buffer.class, "address");

    public final ByteBuffer _BYTE = PUnsafe.allocateInstance(PorkUtil.classForName("java.nio.DirectByteBuffer"));
    public final FloatBuffer _FLOAT = PUnsafe.allocateInstance(PorkUtil.classForName("java.nio.DirectFloatBufferU"));
    public final IntBuffer _INT = PUnsafe.allocateInstance(PorkUtil.classForName("java.nio.DirectIntBufferU"));

    public void _resetBuffer(@NonNull Buffer buffer, long address, int capacity) {
        checkState(buffer.isDirect(), "buffer isn't direct! %s", buffer);

        PUnsafe.putInt(buffer, BUFFER_CAPACITY_OFFSET, capacity);
        PUnsafe.putLong(buffer, BUFFER_ADDRESS_OFFSET, address);
        buffer.clear();
    }

    public static ByteBuffer wrapByte(long address, int capacity) {
        ByteBuffer buffer = _BYTE;
        _resetBuffer(buffer, address, capacity);
        return buffer;
    }

    public static FloatBuffer wrapFloat(long address, int capacity) {
        FloatBuffer buffer = _FLOAT;
        _resetBuffer(buffer, address, capacity);
        return buffer;
    }

    public static IntBuffer wrapInt(long address, int capacity) {
        IntBuffer buffer = _INT;
        _resetBuffer(buffer, address, capacity);
        return buffer;
    }
}
