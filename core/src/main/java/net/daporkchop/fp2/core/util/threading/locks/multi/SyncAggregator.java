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
 *
 */

package net.daporkchop.fp2.core.util.threading.locks.multi;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class SyncAggregator {
    /**
     * Starts multiple {@link SyncOperation synchronization operations} from this thread simultaneously, blocking until one of them is completed. The index of the first operation to complete will be returned; all
     * others will be cancelled.
     *
     * @param operations the {@link SyncOperation synchronization operations} to begin and wait for
     * @return the index of the first operation which was completed
     */
    public static int awaitFirstUninterruptably(@NonNull SyncOperation<?>... operations) {
        int operationsCount = operations.length;
        checkArg(operationsCount > 0, "at least one operation must be given");

        //try to see if any of the operations can succeed without having to create a state
        for (int i = 0; i < operationsCount; i++) {
            if (operations[i].tryEarly()) { //the operation was able to be acquired successfully with tryEarly()!
                return i;
            }
        }

        //allocate array to store all the state objects
        Object[] states = new Object[operationsCount];
        {
            int stateCreationIndex = 0;
            try {
                //create a state instance for every operation
                for (; stateCreationIndex < operationsCount; stateCreationIndex++) {
                    states[stateCreationIndex] = operations[stateCreationIndex].createState();
                }
            } catch (Throwable t) {
                //something went wrong, try to dispose as many state instances as possible
                for (int i = 0; i < stateCreationIndex; i++) {
                    try {
                        operations[i].disposeState(uncheckedCast(states[i]));
                    } catch (Throwable t1) {
                        t.addSuppressed(t1);
                    } finally {
                        Arrays.fill(states, null);
                    }
                }
            }
        }

        boolean interrupted = false;
        try {
            do {
                //try to acquire each running operation
                for (int i = 0; i < operationsCount; i++) {
                    if (operations[i].tryAcquire(uncheckedCast(states[i]))) { //the operation was able to be acquired successfully, return it!
                        return i;
                    }
                }

                //acquire failed! check every running operation to see if we need to park
                boolean shouldPark = true;
                for (int i = 0; i < operationsCount; i++) {
                    if (!operations[i].shouldParkAfterFailedAcquire(uncheckedCast(states[i]))) {
                        //we can only park if EVERY operation reports that it's safe to do so
                        shouldPark = false;
                    }
                }
                if (shouldPark) {
                    LockSupport.park(operations);
                }

                //check if the thread was interrupted, and if so, save it for later
                if (Thread.interrupted()) {
                    interrupted = true;
                }
            } while (true); //spin until successful
        } finally {
            //we were interrupted at some point, restore the old interrupted state
            if (interrupted) {
                Thread.currentThread().interrupt();
            }

            //dispose all the created states
            for (int i = 0; i < operationsCount; i++) {
                operations[i].disposeState(uncheckedCast(states[i]));
            }
            Arrays.fill(states, null);
        }
    }

    /**
     * Starts multiple {@link SyncOperation synchronization operations} from this thread simultaneously, blocking until one of them is completed. The index of the first operation to complete will be returned; all
     * others will be cancelled.
     *
     * @param operations the {@link SyncOperation synchronization operations} to begin and wait for
     * @return the index of the first operation which was completed
     * @throws InterruptedException if the thread was interrupted while waiting
     */
    public static int awaitFirst(@NonNull SyncOperation<?>... operations) throws InterruptedException {
        int operationsCount = operations.length;
        checkArg(operationsCount > 0, "at least one operation must be given");

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        //try to see if any of the operations can succeed without having to create a state
        for (int i = 0; i < operationsCount; i++) {
            if (operations[i].tryEarly()) { //the operation was able to be acquired successfully with tryEarly()!
                return i;
            }
        }

        //allocate array to store all the state objects
        Object[] states = new Object[operationsCount];
        {
            int stateCreationIndex = 0;
            try {
                //create a state instance for every operation
                for (; stateCreationIndex < operationsCount; stateCreationIndex++) {
                    states[stateCreationIndex] = operations[stateCreationIndex].createState();
                }
            } catch (Throwable t) {
                //something went wrong, try to dispose as many state instances as possible
                for (int i = 0; i < stateCreationIndex; i++) {
                    try {
                        operations[i].disposeState(uncheckedCast(states[i]));
                    } catch (Throwable t1) {
                        t.addSuppressed(t1);
                    } finally {
                        Arrays.fill(states, null);
                    }
                }
            }
        }

        try {
            do {
                //try to acquire each running operation
                for (int i = 0; i < operationsCount; i++) {
                    if (operations[i].tryAcquire(uncheckedCast(states[i]))) { //the operation was able to be acquired successfully, return it!
                        return i;
                    }
                }

                //acquire failed! check every running operation to see if we need to park
                boolean shouldPark = true;
                for (int i = 0; i < operationsCount; i++) {
                    if (!operations[i].shouldParkAfterFailedAcquire(uncheckedCast(states[i]))) {
                        //we can only park if EVERY operation reports that it's safe to do so
                        shouldPark = false;
                    }
                }
                if (shouldPark) {
                    LockSupport.park(operations);
                }

                //check if the thread was interrupted, and if so, throw InterruptedException
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            } while (true); //spin until successful
        } finally {
            //dispose all the created states
            for (int i = 0; i < operationsCount; i++) {
                operations[i].disposeState(uncheckedCast(states[i]));
            }
            Arrays.fill(states, null);
        }
    }

    /**
     * Starts multiple {@link SyncOperation synchronization operations} from this thread simultaneously, blocking until one of them is completed. The index of the first operation to complete will be returned; all
     * others will be cancelled.
     *
     * @param operations the {@link SyncOperation synchronization operations} to begin and wait for
     * @return the index of the first operation which was completed
     * @throws InterruptedException if the thread was interrupted while waiting
     */
    public static OptionalInt awaitFirst(long timeout, @NonNull TimeUnit unit, @NonNull SyncOperation<?>... operations) throws InterruptedException {
        int operationsCount = operations.length;
        checkArg(operationsCount > 0, "at least one operation must be given");

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        long nanosTimeout = unit.toNanos(timeout);
        if (nanosTimeout <= 0L) { //timeout has already expired lol
            return OptionalInt.empty();
        }

        //try to see if any of the operations can succeed without having to create a state
        for (int i = 0; i < operationsCount; i++) {
            if (operations[i].tryEarly()) { //the operation was able to be acquired successfully with tryEarly()!
                return OptionalInt.of(i);
            }
        }

        //allocate array to store all the state objects
        Object[] states = new Object[operationsCount];
        {
            int stateCreationIndex = 0;
            try {
                //create a state instance for every operation
                for (; stateCreationIndex < operationsCount; stateCreationIndex++) {
                    states[stateCreationIndex] = operations[stateCreationIndex].createState();
                }
            } catch (Throwable t) {
                //something went wrong, try to dispose as many state instances as possible
                for (int i = 0; i < stateCreationIndex; i++) {
                    try {
                        operations[i].disposeState(uncheckedCast(states[i]));
                    } catch (Throwable t1) {
                        t.addSuppressed(t1);
                    } finally {
                        Arrays.fill(states, null);
                    }
                }
            }
        }

        try {
            long deadline = System.nanoTime() + nanosTimeout;
            do {
                //try to acquire each running operation
                for (int i = 0; i < operationsCount; i++) {
                    if (operations[i].tryAcquire(uncheckedCast(states[i]))) { //the operation was able to be acquired successfully, return it!
                        return OptionalInt.of(i);
                    }
                }

                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L) { //timeout has been reached
                    return OptionalInt.empty();
                }

                //acquire failed! check every running operation to see if we need to park
                boolean shouldPark = true;
                for (int i = 0; i < operationsCount; i++) {
                    if (!operations[i].shouldParkAfterFailedAcquire(uncheckedCast(states[i]))) {
                        //we can only park if EVERY operation reports that it's safe to do so
                        shouldPark = false;
                    }
                }
                if (shouldPark) {
                    LockSupport.parkNanos(operations, nanosTimeout);
                }

                //check if the thread was interrupted, and if so, throw InterruptedException
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            } while (true); //spin until successful
        } finally {
            //dispose all the created states
            for (int i = 0; i < operationsCount; i++) {
                operations[i].disposeState(uncheckedCast(states[i]));
            }
            Arrays.fill(states, null);
        }
    }
}
