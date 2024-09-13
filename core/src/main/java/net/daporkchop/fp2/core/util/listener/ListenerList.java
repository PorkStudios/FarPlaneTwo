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

package net.daporkchop.fp2.core.util.listener;

import lombok.NonNull;
import net.daporkchop.lib.common.annotation.ThreadSafe;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

/**
 * Implements an ordered set of values which implement various event callback methods.
 * <p>
 * A {@link ListenerList} is created for a specific {@link Listener} class, and instances can be {@link #add(Object) added} and
 * {@link #remove(Object) removed} from it dynamically. Events can then be dispatched by invoking the corresponding method on the
 * {@link Listener} instance returned by {@link #dispatcher()}, which will then invoke the method on each of the listeners in turn.
 *
 * @author DaPorkchop_
 */
@ThreadSafe
public final class ListenerList<Listener> {
    /**
     * Creates a new {@link ListenerList} for the given listener class.
     *
     * @param listenerClass the listener class
     * @param <Listener>    the listener type
     * @return a new {@link ListenerList}
     */
    public static <Listener> ListenerList<Listener> create(@NonNull Class<Listener> listenerClass) {
        return new ListenerList<>(listenerClass);
    }

    private final CopyOnWriteArraySet<Listener> listenerSet = new CopyOnWriteArraySet<>();
    private final Function<Set<Listener>, Listener> dispatcherFactory;

    private volatile Listener dispatcher;

    private ListenerList(@NonNull Class<Listener> listenerClass) {
        this.dispatcherFactory = ListenerListGenerator.generateInstanceFactory(listenerClass);
        this.dispatcher = this.dispatcherFactory.apply(this.listenerSet);
    }

    /**
     * Gets the {@link Listener} instance used to dispatch events to the registered listeners.
     * <p>
     * Note that this is only guaranteed to dispatch events to the listeners which where registered at the time this
     * method was called. Listeners which were added/removed during or after the call to this method may be skipped/wrongly
     * included.
     *
     * @return the {@link Listener} instance used to dispatch events
     */
    public Listener dispatcher() {
        return this.dispatcher;
    }

    /**
     * Adds the given {@link Listener} instance.
     *
     * @param listener the {@link Listener} instance to add
     * @return {@code true} if the {@link Listener} was added
     */
    public synchronized boolean add(@NonNull Listener listener) {
        boolean res = this.listenerSet.add(listener);
        if (res) {
            try {
                this.dispatcher = this.dispatcherFactory.apply(this.listenerSet);
            } catch (Throwable t) {
                this.listenerSet.remove(listener);
                throw t;
            }
        }
        return res;
    }

    /**
     * Removes the given {@link Listener} instance.
     *
     * @param listener the {@link Listener} instance to remove
     * @return {@code true} if the {@link Listener} was previously added and was successfully removed
     */
    public synchronized boolean remove(@NonNull Listener listener) {
        boolean res = this.listenerSet.remove(listener);
        if (res) {
            try {
                this.dispatcher = this.dispatcherFactory.apply(this.listenerSet);
            } catch (Throwable t) {
                this.listenerSet.add(listener);
                throw t;
            }
        }
        return res;
    }

    /**
     * Removes all previously added {@link Listener} instances.
     *
     * @return {@code true} if any {@link Listener}s were removed
     */
    public synchronized boolean clear() {
        if (this.listenerSet.isEmpty()) {
            return false;
        }

        this.listenerSet.clear();
        this.dispatcher = this.dispatcherFactory.apply(this.listenerSet);
        return true;
    }
}
