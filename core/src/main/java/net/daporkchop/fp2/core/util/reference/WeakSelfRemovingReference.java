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

package net.daporkchop.fp2.core.util.reference;

import lombok.NonNull;
import net.daporkchop.lib.common.reference.HandleableReference;
import net.daporkchop.lib.common.reference.PReferenceHandler;
import net.daporkchop.lib.common.reference.WeakReference;

import java.lang.ref.ReferenceQueue;
import java.util.Map;

/**
 * A {@link WeakReference} which removes itself (using a fixed key) from a {@link Map} when its reference is garbage collected.
 *
 * @author DaPorkchop_
 */
public final class WeakSelfRemovingReference<T> extends WeakReference<T> implements HandleableReference {
    /**
     * Creates a new {@link WeakSelfRemovingReference}.
     *
     * @param referent the value
     * @param key      the value's key in the map
     * @param map      the map
     * @return the reference
     */
    public static <T> WeakReference<T> create(@NonNull T referent, Object key, @NonNull Map<?, ?> map) {
        return PReferenceHandler.createReference(referent, (_referent, queue) -> new WeakSelfRemovingReference<>(_referent, queue, key, map));
    }

    protected final Map<?, ?> map;
    protected final Object key;

    private WeakSelfRemovingReference(@NonNull T referent, @NonNull ReferenceQueue<? super T> queue, Object key, @NonNull Map<?, ?> map) {
        super(referent, queue);

        this.map = map;
        this.key = key;
    }

    @Override
    public void handle() {
        //remove self from map
        this.map.remove(this.key, this);
    }
}
