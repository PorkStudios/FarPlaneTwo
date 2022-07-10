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

package net.daporkchop.fp2.core.util.datastructure;

import lombok.NonNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * A {@link BlockingQueue blocking queue} which returns elements in priority order, similar to {@link PriorityBlockingQueue}.
 *
 * @author DaPorkchop_
 */
public interface NavigableBlockingQueue<E> extends BlockingQueue<E> {
    /**
     * {@link #poll() Polls} an element from this queue which is strictly less (has a higher priority) than the given element. Returns {@code null} if this queue doesn't contain
     * any elements less than the given element.
     *
     * @param curr the element
     * @return the element which was polled, or {@code null} if none was available
     */
    E pollLess(@NonNull E curr);

    /**
     * {@link #poll() Polls} an element from this queue which is strictly less (has a higher priority) than the given element. If this queue doesn't contain any elements
     * less than the given element, this method will block until one comes available, the given timeout is reached, or the calling thread is interrupted.
     *
     * @param curr    the element
     * @param timeout the timeout duration
     * @param unit    the unit of time in which the timeout duration is given
     * @return the element which was polled, or {@code null} if none became available before the given timeout was reached
     * @throws InterruptedException if the calling thread was interrupted
     */
    default E pollLess(@NonNull E curr, long timeout, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) { //check for interrupt
            throw new InterruptedException();
        }

        long nanosTimeout = unit.toNanos(timeout);
        if (nanosTimeout <= 0L) { //timeout has already expired lol
            return null;
        }

        long deadline = System.nanoTime() + nanosTimeout;
        do { //keep trying to poll a value until the timeout is reached
            E value = this.pollLess(curr);
            if (value != null) {
                return value;
            }

            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L) { //timeout has been reached
                return null;
            }

            //park to avoid spinning CPU at 100%
            LockSupport.parkNanos(this, Math.min(TimeUnit.MILLISECONDS.toNanos(1L), nanosTimeout));
            if (Thread.interrupted()) { //check for interrupt
                throw new InterruptedException();
            }
        } while (true);
    }

    /**
     * {@link #poll() Polls} an element from this queue which is strictly less (has a higher priority) than the given element. If this queue doesn't contain any elements
     * less than the given element, this method will block until one comes available or the calling thread is interrupted.
     *
     * @param curr the element
     * @return the element which was polled
     * @throws InterruptedException if the calling thread was interrupted
     */
    default E takeLess(@NonNull E curr) throws InterruptedException {
        if (Thread.interrupted()) { //check for interrupt
            throw new InterruptedException();
        }

        do { //keep trying to poll a value until the timeout is reached
            E value = this.pollLess(curr);
            if (value != null) {
                return value;
            }

            //park to avoid spinning CPU at 100%
            LockSupport.parkNanos(this, TimeUnit.MILLISECONDS.toNanos(1L));
            if (Thread.interrupted()) { //check for interrupt
                throw new InterruptedException();
            }
        } while (true);
    }
}
