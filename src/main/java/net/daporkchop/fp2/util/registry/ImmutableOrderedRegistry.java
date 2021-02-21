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

package net.daporkchop.fp2.util.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import lombok.NonNull;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Immutable implementation of {@link OrderedRegistry}.
 *
 * @author DaPorkchop_
 */
public final class ImmutableOrderedRegistry<T> implements OrderedRegistry<T> {
    protected final BiMap<String, T> map;

    public ImmutableOrderedRegistry(@NonNull OrderedRegistry<T> src) {
        ImmutableBiMap.Builder<String, T> builder = ImmutableBiMap.builder();
        src.forEachEntry(builder::put);
        this.map = builder.build();
    }

    @Override
    public OrderedRegistry<T> addFirst(@NonNull String name, @NonNull T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrderedRegistry<T> addLast(@NonNull String name, @NonNull T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrderedRegistry<T> addBefore(@NonNull String targetName, @NonNull String name, @NonNull T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrderedRegistry<T> addAfter(@NonNull String targetName, @NonNull String name, @NonNull T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrderedRegistry<T> set(@NonNull String targetName, @NonNull T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrderedRegistry<T> set(@NonNull String targetName, @NonNull String newName, @NonNull T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrderedRegistry<T> remove(@NonNull String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T get(@NonNull String name) {
        T value = this.map.get(name);
        checkArg(value != null, "unable to find entry with name \"%s\"!", name);
        return value;
    }

    @Override
    public String getName(@NonNull T value) {
        String name = this.map.inverse().get(value);
        checkArg(name != null, "unable to find entry with value \"%s\"!", value);
        return name;
    }

    @Override
    public Stream<Map.Entry<String, T>> stream() {
        return this.map.entrySet().stream();
    }

    @Override
    public Iterator<Map.Entry<String, T>> iterator() {
        return this.map.entrySet().iterator();
    }

    @Override
    public void forEachEntry(@NonNull BiConsumer<String, ? super T> action) {
        this.map.forEach(action);
    }
}
