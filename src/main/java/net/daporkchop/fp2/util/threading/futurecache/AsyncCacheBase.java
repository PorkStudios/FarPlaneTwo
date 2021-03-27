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

package net.daporkchop.fp2.util.threading.futurecache;

import lombok.NonNull;
import net.daporkchop.fp2.util.reference.WeakSelfRemovingReference;
import net.daporkchop.fp2.util.threading.fj.CompletedForkJoinTask;
import net.daporkchop.fp2.util.threading.fj.ThreadSafeForkJoinSupplier;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.primitive.map.concurrent.ObjObjConcurrentHashMap;

import java.lang.ref.Reference;
import java.util.Map;
import java.util.concurrent.ForkJoinTask;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of {@link IAsyncCache}.
 *
 * @author DaPorkchop_
 */
public abstract class AsyncCacheBase<K, V> implements IAsyncCache<K, V> {
    protected final Map<K, Object> map = new ObjObjConcurrentHashMap<>();

    @Override
    public ForkJoinTask<V> get(@NonNull K key, boolean allowGeneration) {
        class State implements BiFunction<K, Object, Object>, Supplier<ForkJoinTask<V>> {
            Object value;

            @Override
            public Object apply(@NonNull K key, Object value) {
                if (value instanceof Reference) {
                    V dereferenced = PorkUtil.<Reference<V>>uncheckedCast(value).get();

                    if (allowGeneration && dereferenced instanceof CompletedForkJoinTask) { //force generation
                        dereferenced = null;
                    }

                    this.value = dereferenced;
                } else if (value instanceof AsyncCacheBase.LoadTask) {
                    LoadTask task = uncheckedCast(value);

                    if (allowGeneration && !task.allowGeneration) { //force generation
                        task = null;
                    }

                    this.value = task;
                }

                if (this.value != null) { //value or future was obtained successfully, so the currently cached object can be left as-is
                    return value;
                } else { //issue new future load task
                    return this.value = new LoadTask(key, allowGeneration);
                }
            }

            @Override
            public ForkJoinTask<V> get() {
                if (this.value instanceof ForkJoinTask) {
                    return uncheckedCast(this.value);
                } else {
                    return new CompletedForkJoinTask<>(uncheckedCast(this.value));
                }
            }
        }

        State state = new State();
        this.map.compute(key, state);
        return state.get();
    }

    /**
     * Invalidates the cached value for the given key, if any.
     *
     * @param key the key to invalidate
     */
    public void invalidate(@NonNull K key) {
        this.map.remove(key);
    }

    /**
     * Loads the value for the given key.
     *
     * @param key             the key
     * @param allowGeneration whether or not to generate the value if it doesn't already exist
     * @return the value, or {@code null} if the value doesn't exist and {@code allowGeneration} was {@code false}
     */
    protected abstract V load(@NonNull K key, boolean allowGeneration);

    /**
     * A {@link ForkJoinTask} which loads a value for a given key into the cache.
     *
     * @author DaPorkchop_
     */
    protected class LoadTask extends ThreadSafeForkJoinSupplier<V> {
        protected final K key;
        protected final boolean allowGeneration;

        public LoadTask(@NonNull K key, boolean allowGeneration) {
            this.key = key;
            this.allowGeneration = allowGeneration;

            //immediately fork this task
            this.fork();
        }

        @Override
        protected V compute() {
            V value = AsyncCacheBase.this.load(this.key, this.allowGeneration);

            if (value == null) {
                checkState(!this.allowGeneration, "allowGeneration was true, but the value for %s wasn't generated!", this.key);
                value = uncheckedCast(new CompletedForkJoinTask<>(null));
            }

            //replace ForkJoinTask in cache with a weak reference
            AsyncCacheBase.this.map.replace(this.key, this, new WeakSelfRemovingReference<>(value, this.key, AsyncCacheBase.this.map));
            return value;
        }
    }
}
