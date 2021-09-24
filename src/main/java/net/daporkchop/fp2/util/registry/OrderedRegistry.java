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

import io.netty.channel.ChannelPipeline;
import lombok.NonNull;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A sort of "registry", in which entries are identified by a {@link String} key.
 * <p>
 * Entries maintain a strict order, and can be inserted/removed at either end of the registry, or at positions relative to each other.
 * <p>
 * Not thread-safe.
 * <p>
 * Inspired by Netty's {@link ChannelPipeline}.
 * <p>
 * Will probably be hoisted into PorkLib at some point.
 *
 * @author DaPorkchop_
 * @see LinkedOrderedRegistry
 * @see ImmutableOrderedRegistry
 */
public interface OrderedRegistry<T> extends Iterable<Map.Entry<String, T>> {
    /**
     * Adds a new entry at the beginning of the registry.
     *
     * @param name  the name
     * @param value the value
     */
    OrderedRegistry<T> addFirst(@NonNull String name, @NonNull T value);

    /**
     * Adds a new entry at the end of the registry.
     *
     * @param name  the name
     * @param value the value
     */
    OrderedRegistry<T> addLast(@NonNull String name, @NonNull T value);

    /**
     * Adds a new entry immediately before the entry with the given name.
     *
     * @param targetName the name of the entry that the new entry should be inserted before
     * @param name       the name
     * @param value      the value
     */
    OrderedRegistry<T> addBefore(@NonNull String targetName, @NonNull String name, @NonNull T value);

    /**
     * Adds a new entry immediately before the entry with the given name.
     *
     * @param targetName the name of the entry that the new entry should be inserted before
     * @param name       the name
     * @param value      the value
     */
    OrderedRegistry<T> addAfter(@NonNull String targetName, @NonNull String name, @NonNull T value);

    /**
     * Replaces the existing entry with the given name.
     *
     * @param targetName the name of the entry that the new entry should be inserted before
     * @param value      the value
     */
    OrderedRegistry<T> set(@NonNull String targetName, @NonNull T value);

    /**
     * Replaces the existing entry with the given name.
     *
     * @param targetName the name of the entry that the new entry should be inserted before
     * @param newName    the name
     * @param value      the value
     */
    OrderedRegistry<T> set(@NonNull String targetName, @NonNull String newName, @NonNull T value);

    /**
     * Removes the existing entry with the given name.
     *
     * @param name the name of the entry to remove
     */
    OrderedRegistry<T> remove(@NonNull String name);

    /**
     * Checks if this registry contains an entry with the given name.
     *
     * @param name the name of the entry to check for
     */
    boolean contains(@NonNull String name);

    /**
     * Gets the existing entry with the given name.
     *
     * @param name the name of the entry to get
     */
    T get(@NonNull String name);

    /**
     * Gets the name of the existing entry with the given value.
     *
     * @param value the value
     */
    String getName(@NonNull T value);

    /**
     * @return a {@link Stream} over all the entries in this registry
     */
    Stream<Map.Entry<String, T>> stream();

    @Override
    Iterator<Map.Entry<String, T>> iterator();

    @Override
    @Deprecated
    default void forEach(@NonNull Consumer<? super Map.Entry<String, T>> action) {
        Iterable.super.forEach(action);
    }

    /**
     * Performs the given action for each entry in the registry.
     *
     * @param action the action to perform.
     */
    void forEachEntry(@NonNull BiConsumer<String, ? super T> action);
}
