/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.util.function.IntFunction;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Holder class which contains global {@link ArrayAllocator} instances for primitive types.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class GlobalAllocators {
    private static final LoadingCache<Class<?>, Cached<ArrayAllocator<?>>> ALLOCATORS_CACHED_BY_COMPONENT_TYPE = CacheBuilder.newBuilder()
            .weakKeys().weakValues()
            .build(CacheLoader.from(componentType -> makeThreadLocal(() -> ArrayAllocator.pow2(componentType, ReferenceStrength.STRONG, 32))));

    public static final Cached<ArrayAllocator<byte[]>> ALLOC_BYTE = createArrayAllocator(PUnsafe::allocateUninitializedByteArray);
    public static final Cached<ArrayAllocator<int[]>> ALLOC_INT = createArrayAllocator(PUnsafe::allocateUninitializedIntArray);
    public static final Cached<ArrayAllocator<float[]>> ALLOC_FLOAT = createArrayAllocator(PUnsafe::allocateUninitializedFloatArray);
    public static final Cached<ArrayAllocator<double[]>> ALLOC_DOUBLE = createArrayAllocator(PUnsafe::allocateUninitializedDoubleArray);
    public static final Cached<ArrayAllocator<Object[]>> ALLOC_OBJECT = getArrayAllocatorForComponentType(Object.class);

    private static <T> Cached<T> makeThreadLocal(@NonNull Supplier<T> factory) {
        return Cached.threadLocal(factory, ReferenceStrength.WEAK);
    }

    /**
     * Creates a new thread-local {@link ArrayAllocator array allocator} which uses the given {@link IntFunction function} to allocate new arrays.
     *
     * @param allocator the function used to allocate new arrays
     * @return a thread-local array allocator
     */
    private static <T> Cached<ArrayAllocator<T>> createArrayAllocator(@NonNull IntFunction<T> allocator) {
        return makeThreadLocal(() -> ArrayAllocator.pow2(allocator, ReferenceStrength.STRONG, 32));
    }

    /**
     * Creates a new thread-local {@link ArrayAllocator array allocator} for allocating arrays with the given component type.
     *
     * @param componentType the component type class
     * @return a thread-local array allocator
     */
    public static <T> Cached<ArrayAllocator<T[]>> getArrayAllocatorForComponentType(@NonNull Class<T> componentType) {
        return uncheckedCast(ALLOCATORS_CACHED_BY_COMPONENT_TYPE.getUnchecked(componentType));
    }
}
