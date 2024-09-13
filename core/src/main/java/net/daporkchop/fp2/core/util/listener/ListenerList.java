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

import com.google.common.collect.ImmutableSet;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.annotation.ThreadSafe;
import net.daporkchop.lib.unsafe.PCleaner;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implements an ordered set of values which implement various event callback methods.
 * <p>
 * A {@link ListenerList} is created for a specific {@link Listener} class, and instances can be {@link #add(Object) added} and
 * removed from it dynamically. Events can then be dispatched by invoking the corresponding method on the
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

    private final ListenerListGenerator<Listener> dispatcherFactory;
    private ImmutableSet<Listener> listenerSet = ImmutableSet.of();

    private volatile Listener dispatcher;

    private ListenerList(@NonNull Class<Listener> listenerClass) {
        this.dispatcherFactory = ListenerListGenerator.get(listenerClass);
        this.dispatcher = this.dispatcherFactory.get(this.listenerSet);
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
     * @return a {@link Handle} which should be closed in order to remove the listener again
     */
    public synchronized Handle add(@NonNull Listener listener) {
        checkState(!this.listenerSet.contains(listener), "duplicate listener: %s", listener);

        ImmutableSet<Listener> newListeners = ImmutableSet.<Listener>builder().addAll(this.listenerSet).add(listener).build();
        Listener newDispatcher = this.dispatcherFactory.get(newListeners);

        Handle handle = new Handle(listener, new Throwable());
        this.listenerSet = newListeners;
        this.dispatcher = newDispatcher;
        return handle;
    }

    /**
     * Removes the given {@link Listener} instance.
     *
     * @param listener the {@link Listener} instance to remove
     */
    synchronized void remove0(@NonNull Listener listener) {
        checkState(this.listenerSet.contains(listener), "unknown listener: %s", listener);

        ImmutableSet<Listener> newListeners = this.listenerSet.stream().filter(l -> !listener.equals(l)).collect(ImmutableSet.toImmutableSet());
        Listener newDispatcher = this.dispatcherFactory.get(newListeners);

        this.listenerSet = newListeners;
        this.dispatcher = newDispatcher;
    }

    /**
     * @author DaPorkchop_
     */
    public final class Handle implements AutoCloseable {
        private final @NonNull Listener listener;
        private final @NonNull Releaser releaser;
        private final @NonNull PCleaner cleaner;

        Handle(@NonNull Listener listener, @NonNull Throwable constructorStackTrace) {
            this.listener = listener;
            this.releaser = new Releaser(listener.getClass(), constructorStackTrace);
            this.cleaner = PCleaner.cleaner(this, this.releaser);
        }

        @Override
        public void close() {
            checkState(!this.releaser.closed);
            this.releaser.closed = true;
            this.cleaner.clean(); //run cleaner now to make the internal PhantomReference collectable

            ListenerList.this.remove0(this.listener);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class Releaser implements Runnable {
        private final @NonNull Class<?> listenerClass;
        private final @NonNull Throwable constructorStackTrace;
        boolean closed;

        @Override
        public void run() {
            if (!this.closed) {
                RuntimeException re = new RuntimeException("an event listener (" + this.listenerClass + ") was not closed!", this.constructorStackTrace);
                re.printStackTrace();
                throw re; //throwing this from the cleaner thread will cause java to exit (although it's broken on Forge)
            }
        }
    }
}
