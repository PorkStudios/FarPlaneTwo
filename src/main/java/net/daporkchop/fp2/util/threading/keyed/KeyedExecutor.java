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

package net.daporkchop.fp2.util.threading.keyed;

import lombok.NonNull;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

/**
 * An executor which organizes submitted tasks based on a key.
 * <p>
 * It is guaranteed that no two tasks with the same key will be executed at once.
 *
 * @author DaPorkchop_
 */
public interface KeyedExecutor<K> extends RefCounted {
    /**
     * Submits a task using the given key.
     *
     * @param key  the key
     * @param task the task
     */
    void submit(@NonNull K key, @NonNull Runnable task);

    /**
     * Submits a task using the given key.
     * <p>
     * This method may cause some number of pending tasks with an identical key to be discarded entirely. As a result, it should be considered unsafe, as
     * it can very easily cause unexpected behavior to occur.
     *
     * @param key  the key
     * @param task the task
     */
    void submitExclusive(@NonNull K key, @NonNull Runnable task);

    /**
     * Cancels a task with the given key.
     * <p>
     * The tasks will be checked for equality using standard {@link Object#equals(Object)} and {@link Object#hashCode()} methods.
     *
     * @param key  the key
     * @param task the task
     */
    void cancel(@NonNull K key, @NonNull Runnable task);

    /**
     * Cancels all the tasks with the given key.
     *
     * @param key the key
     */
    void cancelAll(@NonNull K key);

    /**
     * Updates the execution priority for all previously submitted tasks with the given key.
     * <p>
     * This method may not be supported, in which case it will do nothing and return {@code false}.
     * <p>
     * By default, all tasks are submitted with a priority value of {@link Integer#MAX_VALUE}, and tasks with a lower priority value are executed sooner.
     * <p>
     * The priority change will be executed weakly, meaning that it may not take effect for a variety of reasons (such as a case where some of the tasks have
     * already begun execution).
     * <p>
     * It is undefined whether or not the priority change will affect tasks for the same key which are submitted later.
     *
     * @param key      the key
     * @param priority the key's new priority
     */
    boolean updatePriorityFor(@NonNull K key, int priority);

    @Override
    int refCnt();

    @Override
    KeyedExecutor<K> retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;
}
