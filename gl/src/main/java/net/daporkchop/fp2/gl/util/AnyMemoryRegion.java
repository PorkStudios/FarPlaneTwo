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
import lombok.ToString;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.nio.ByteBuffer;

/**
 * Wrapper around a CPU-side memory region which can be used as the source/target for OpenGL data upload/download commands.
 *
 * @author DaPorkchop_
 */
@ToString
public final class AnyMemoryRegion {
    public final @NonNull Object memory;
    public final @NotNegative long size;

    public AnyMemoryRegion(@NonNull ByteBuffer buffer) {
        this.memory = DirectBufferHackery.checkDirect(buffer);
        this.size = buffer.remaining();
    }

    public AnyMemoryRegion(short @NonNull [] array) {
        this.memory = array;
        this.size = (long) array.length * Short.BYTES;
    }

    public AnyMemoryRegion(int @NonNull [] array) {
        this.memory = array;
        this.size = (long) array.length * Integer.BYTES;
    }

    public AnyMemoryRegion(long @NonNull [] array) {
        this.memory = array;
        this.size = (long) array.length * Long.BYTES;
    }

    public AnyMemoryRegion(float @NonNull [] array) {
        this.memory = array;
        this.size = (long) array.length * Float.BYTES;
    }

    public AnyMemoryRegion(double @NonNull [] array) {
        this.memory = array;
        this.size = (long) array.length * Double.BYTES;
    }
}
