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

package net.daporkchop.fp2.config;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.util.reference.ReferenceHandlerThread;
import net.daporkchop.fp2.util.reference.WeakEqualityForwardingReference;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Manages global config listeners.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class ConfigListenerManager {
    private final Set<ListenerWrapper> ACTIVE_LISTENERS = new CopyOnWriteArraySet<>();

    private final Consumer<ListenerWrapper> FIRE_CALLBACK = wrapper -> {
        IConfigListener listener = wrapper.get();
        if (listener != null) {
            listener.configChanged();
        }
    };

    /**
     * Adds a new {@link IConfigListener}.
     *
     * @param listener the {@link IConfigListener}
     */
    public void add(@NonNull IConfigListener listener) {
        ACTIVE_LISTENERS.add(new ListenerWrapper(listener));
    }

    /**
     * Removes a previously added {@link IConfigListener}.
     *
     * @param listener the {@link IConfigListener}
     */
    public void remove(@NonNull IConfigListener listener) {
        ACTIVE_LISTENERS.removeIf(wrapper -> wrapper == listener);
    }

    /**
     * Fires the config changed event to all active {@link IConfigListener}s.
     */
    public void fire() {
        ACTIVE_LISTENERS.forEach(FIRE_CALLBACK);
    }

    /**
     * Weak-referencing wrapper around an {@link IConfigListener}.
     *
     * @author DaPorkchop_
     */
    private static class ListenerWrapper extends WeakEqualityForwardingReference<IConfigListener> implements Runnable, Predicate<ListenerWrapper> {
        public ListenerWrapper(@NonNull IConfigListener referent) {
            super(referent, ReferenceHandlerThread.queue());
        }

        @Override
        public void run() { //called by the reference handler thread after the listener is garbage collected
            ACTIVE_LISTENERS.removeIf(this);
        }

        /**
         * @deprecated internal API, do not touch!
         */
        @Override
        @Deprecated
        public boolean test(ListenerWrapper wrapper) { //used by #run() to remove this wrapper from ACTIVE_LISTENERS by its identity
            return wrapper == this;
        }
    }
}
