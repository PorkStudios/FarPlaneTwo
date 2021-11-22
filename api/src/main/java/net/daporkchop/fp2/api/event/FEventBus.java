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

package net.daporkchop.fp2.api.event;

import lombok.NonNull;

/**
 * Dispatches events to listeners, and provides ways for listeners to register themselves.
 * <p>
 * Event buses are thread-safe, and listeners are expected to be as well. Unless a specific event type is documented otherwise, listeners must be able to deal with handlers
 * being called concurrently from multiple threads.
 *
 * @author DaPorkchop_
 */
public interface FEventBus {
    /**
     * Registers the given event listener instance.
     * <p>
     * The behavior is undefined if the instance has already been registered.
     *
     * @param listener the event listener
     */
    void register(@NonNull Object listener);

    /**
     * Registers the given event listener instance.
     * <p>
     * The behavior is undefined if the instance has already been registered.
     * <p>
     * The instance will be weakly referenced, and will be unregistered automatically from the event bus if garbage collected.
     *
     * @param listener the event listener
     */
    void registerWeak(@NonNull Object listener);

    /**
     * Registers the given event listener class.
     * <p>
     * The behavior is undefined if the class has already been registered.
     *
     * @param listener the event listener class
     */
    void registerStatic(@NonNull Class<?> listener);

    /**
     * Unregisters the given event listener instance.
     * <p>
     * The behavior is undefined if the instance is not registered.
     * <p>
     * Once this method returns, the listener will have been removed from the bus and will not be notified of any subsequent events (unless, of course, it is registered again)
     * and all events fired prior to this method invocation will have completed handling.
     *
     * @param listener the event listener
     */
    void unregister(@NonNull Object listener);

    /**
     * Unregisters the given event listener class.
     * <p>
     * The behavior is undefined if the class is not registered.
     * <p>
     * Once this method returns, the listener will have been removed from the bus and will not be notified of any subsequent events (unless, of course, it is registered again)
     * and all events fired prior to this method invocation will have completed handling.
     *
     * @param listener the event listener class
     */
    void unregisterStatic(@NonNull Class<?> listener);

    /**
     * Dispatches the given event to all registered listeners for the corresponding type.
     *
     * @param event the event
     * @param <T>   the event type
     * @return the event instance
     */
    <T> T fire(@NonNull T event);
}
