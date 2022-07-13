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

package net.daporkchop.fp2.core.util.threading.locks.multi;

/**
 * An asynchronous synchronization operation.
 * <p>
 * This defines a minimal set of methods which should suffice to implement most basic synchronization operations. As the different steps are isolated from each other in separate methods, it is possible for user code
 * to build complex operations without the original implementation needing to have dedicated implementations for them.
 * <p>
 * Instances of {@link SyncOperation} are re-usable; any state required by the operation is kept in a separate {@link STATE state object}.
 *
 * @author DaPorkchop_
 * @see SyncAggregator for static helper methods which can block while awaiting the completion of multiple {@link SyncOperation}s at once
 */
public abstract class SyncOperation<STATE> {
    /**
     * Attempts to quickly complete this operation without having to block the calling thread or allocate any state.
     *
     * @return {@code true} if the operation could be completed, {@code false} otherwise
     */
    protected abstract boolean tryEarly();

    /**
     * Prepares an instance of {@link STATE} to be used for completing this operation in the current thread.
     * <p>
     * Note that if the implementation chooses to re-use {@link STATE} instances, any instance returned by a call to this method may not be returned again by a subsequent call to this method until disposed by
     * {@link #disposeState(Object)}.
     *
     * @return an instance of {@link STATE}
     */
    protected abstract STATE createState();

    /**
     * Tries to complete this operation, given the current state.
     *
     * @param state the current state
     * @return {@code true} if the operation could be completed, {@code false} otherwise
     */
    protected abstract boolean tryComplete(STATE state);

    /**
     * If {@link #tryComplete(Object)} returns {@code false}, this method checks if we can safely park the current thread before trying again.
     *
     * @param state the current state
     * @return {@code true} if it is safe to park, {@code false} if we should immediately try to complete the operation again
     */
    protected abstract boolean shouldParkAfterFailedComplete(STATE state);

    /**
     * Disposes an instance of {@link STATE} once it is no longer needed.
     * <p>
     * This method must be called for every {@link STATE} instance created by {@link #createState()} once it is no longer needed.
     *
     * @param state the {@link STATE} instance to dispose
     */
    protected abstract void disposeState(STATE state);
}
