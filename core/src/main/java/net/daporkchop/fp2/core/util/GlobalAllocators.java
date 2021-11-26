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

package net.daporkchop.fp2.core.util;

import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.unsafe.PUnsafe;

/**
 * Holder class which contains global {@link ArrayAllocator} instances for primitive types.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class GlobalAllocators {
    public static final Cached<ArrayAllocator<byte[]>> ALLOC_BYTE = Cached.threadLocal(() -> ArrayAllocator.pow2(PUnsafe::allocateUninitializedByteArray, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);
    public static final Cached<ArrayAllocator<int[]>> ALLOC_INT = Cached.threadLocal(() -> ArrayAllocator.pow2(PUnsafe::allocateUninitializedIntArray, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);
    public static final Cached<ArrayAllocator<float[]>> ALLOC_FLOAT = Cached.threadLocal(() -> ArrayAllocator.pow2(PUnsafe::allocateUninitializedFloatArray, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);
    public static final Cached<ArrayAllocator<double[]>> ALLOC_DOUBLE = Cached.threadLocal(() -> ArrayAllocator.pow2(PUnsafe::allocateUninitializedDoubleArray, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);
    public static final Cached<ArrayAllocator<Object[]>> ALLOC_OBJECT = Cached.threadLocal(() -> ArrayAllocator.pow2(Object[]::new, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);
}
