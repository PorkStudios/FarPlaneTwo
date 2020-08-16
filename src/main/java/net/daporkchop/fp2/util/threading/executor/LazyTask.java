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

package net.daporkchop.fp2.util.threading.executor;

import io.netty.util.concurrent.Promise;
import lombok.NonNull;
import net.daporkchop.lib.concurrent.PFuture;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
public interface LazyTask<K extends LazyKey<K>, T, R> extends PFuture<R> {
    K key();

    Stream<? extends LazyTask<K, ?, T>> before(@NonNull K key) throws Exception;

    R run(@NonNull List<T> params, @NonNull LazyPriorityExecutor<K> executor) throws Exception;

    /**
     * @see Promise#setSuccess(Object)
     */
    LazyTask<K, T, R> setSuccess(R result);

    /**
     * @see Promise#setFailure(Throwable)
     */
    LazyTask<K, T, R> setFailure(Throwable cause);

    void cancel();
}
