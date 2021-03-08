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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Predicate;

/**
 * Manages global config listeners.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class ConfigListenerManager {
    private final List<Reference<ConfigListener>> ACTIVE_LISTENERS = new ObjectArrayList<>();

    private final Predicate<Reference<ConfigListener>> CLEANUP_FILTER = ref -> ref.get() == null;
    private final Predicate<Reference<ConfigListener>> FIRE_FILTER = ref -> {
        ConfigListener listener = ref.get();
        if (listener != null) {
            listener.configChanged();
            return false;
        } else { //the listener was GC'd, remove it
            return true;
        }
    };

    /**
     * Adds a new {@link ConfigListener}.
     *
     * @param listener the {@link ConfigListener}
     */
    public void add(@NonNull ConfigListener listener) {
        ACTIVE_LISTENERS.removeIf(CLEANUP_FILTER);
        ACTIVE_LISTENERS.add(new WeakReference<>(listener));
    }

    /**
     * Removes a previously added {@link ConfigListener}.
     *
     * @param listener the {@link ConfigListener}
     */
    public void remove(@NonNull ConfigListener listener) {
        ACTIVE_LISTENERS.removeIf(ref -> {
            ConfigListener deRef = ref.get();
            return deRef == null || deRef == listener;
        });
    }

    /**
     * Fires the config changed event to all active {@link ConfigListener}s.
     */
    public void fire() {
        ACTIVE_LISTENERS.removeIf(FIRE_FILTER);
    }
}
