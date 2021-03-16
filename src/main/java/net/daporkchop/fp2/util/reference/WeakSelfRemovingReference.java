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

package net.daporkchop.fp2.util.reference;

import lombok.NonNull;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * A {@link WeakReference} which removes itself (using a fixed key) from a {@link Map} when its reference is garbage collected.
 *
 * @author DaPorkchop_
 */
public class WeakSelfRemovingReference<T> extends WeakReference<T> implements Runnable {
    protected final Map<?, ?> map;
    protected final Object key;

    public WeakSelfRemovingReference(@NonNull T referent, Object key, @NonNull Map<?, ?> map) {
        super(referent, ReferenceHandlerThread.queue());

        this.map = map;
        this.key = key;
    }

    @Override
    public void run() {
        //remove self from map
        this.map.remove(this.key, this);
    }
}
