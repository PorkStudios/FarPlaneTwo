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

package net.daporkchop.fp2.core.client.shader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class ShaderMacros {
    @NonNull
    protected final Map<String, Object> macros;

    /**
     * @return an immutable snapshot of this instance's currently defined macros and values
     */
    public abstract Immutable snapshot();

    protected abstract Stream<ShaderMacros> parentsFlattened();

    /**
     * @author DaPorkchop_
     */
    public static final class Mutable extends ShaderMacros {
        @Getter
        private final List<ShaderMacros> parents;
        private final Set<Mutable> children = Collections.newSetFromMap(new WeakHashMap<>());

        private Immutable cachedSnapshot = null;

        public Mutable(@NonNull ShaderMacros... parents) {
            super(new ObjObjOpenHashMap<>());

            this.parents = ImmutableList.copyOf(parents);

            //register this as a child of all of the parents
            this.parentsFlattened().forEach(parent -> {
                if (parent instanceof Mutable) {
                    ((Mutable) parent).children.add(this);
                }
            });
        }

        @Override
        protected Stream<ShaderMacros> parentsFlattened() {
            return this.parents.stream().flatMap(parent -> Stream.concat(parent.parentsFlattened(), Stream.of(parent)));
        }

        protected void markDirty() {
            if (this.cachedSnapshot != null) { //this instance has been cached
                this.cachedSnapshot = null;

                //recursively mark children as dirty
                this.children.forEach(Mutable::markDirty);
            }
        }

        /**
         * Equivalent to {@code define(key, true)}.
         *
         * @see #define(String, Object)
         */
        public Mutable define(@NonNull String key) {
            return this.define(key, true);
        }

        /**
         * Defines a macro value, replacing any existing macro values for the given key.
         *
         * @param key   the macro key
         * @param value the macro value
         */
        public Mutable define(@NonNull String key, @NonNull Object value) {
            if (!value.equals(this.macros.put(key, value))) { //the existing value for the key didn't match
                this.markDirty();
            }
            return this;
        }

        /**
         * Un-defines a macro value with the given key.
         *
         * @param key the macro key
         */
        public Mutable undefine(@NonNull String key) {
            if (this.macros.remove(key) != null) {
                this.markDirty();
            }
            return this;
        }

        @Override
        public Immutable snapshot() {
            Immutable cachedSnapshot = this.cachedSnapshot;
            if (cachedSnapshot == null) { //re-compute cached snapshot
                ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
                this.parents().forEach(parent -> builder.putAll(parent.snapshot().macros));
                builder.putAll(this.macros);
                this.cachedSnapshot = cachedSnapshot = new Immutable(builder.build());
            }
            return cachedSnapshot;
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static final class Immutable extends ShaderMacros {
        private Immutable(@NonNull ImmutableMap<String, Object> macros) {
            super(macros);
        }

        @Override
        public Immutable snapshot() {
            return this;
        }

        @Override
        protected Stream<ShaderMacros> parentsFlattened() {
            return Stream.empty();
        }

        /**
         * @return a {@link ImmutableMap} containing the defined macros
         */
        public ImmutableMap<String, Object> macros() {
            return (ImmutableMap<String, Object>) this.macros;
        }
    }
}
