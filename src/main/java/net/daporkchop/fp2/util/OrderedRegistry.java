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

package net.daporkchop.fp2.util;

import io.netty.channel.ChannelPipeline;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import net.daporkchop.lib.common.misc.string.PStrings;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

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
 */
public final class OrderedRegistry<T> implements Iterable<Map.Entry<String, T>> {
    /*
     * Implementation notes:
     *  Entries are stored as a tuple of their name and value in a linked list.
     *  Most operations will require a full traversal of the list, which would in theory be possible to eliminate by
     *  keeping a separate index or similar. However, I'm not expecting this to be used with entry counts higher than
     *  perhaps a few dozen, so there shouldn't be much of a performance issue.
     */

    protected final List<Entry<T>> list = new LinkedList<>();

    /**
     * Adds a new entry at the beginning of the registry.
     *
     * @param name  the name
     * @param value the value
     */
    public OrderedRegistry<T> addFirst(@NonNull String name, @NonNull T value) {
        this.assertUniqueName(name);
        this.list.add(0, new Entry<>(name, value));
        return this;
    }

    /**
     * Adds a new entry at the end of the registry.
     *
     * @param name  the name
     * @param value the value
     */
    public OrderedRegistry<T> addLast(@NonNull String name, @NonNull T value) {
        this.assertUniqueName(name);
        this.list.add(new Entry<>(name, value));
        return this;
    }

    /**
     * Adds a new entry immediately before the entry with the given name.
     *
     * @param targetName the name of the entry that the new entry should be inserted before
     * @param name       the name
     * @param value      the value
     */
    public OrderedRegistry<T> addBefore(@NonNull String targetName, @NonNull String name, @NonNull T value) {
        this.assertUniqueName(name);
        for (ListIterator<Entry<T>> itr = this.list.listIterator(); itr.hasNext(); ) {
            if (itr.next().name.equals(targetName)) {
                itr.previous();
                itr.add(new Entry<>(name, value));
                return this;
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("unable to find entry with name \"%s\"!", targetName));
    }

    /**
     * Adds a new entry immediately before the entry with the given name.
     *
     * @param targetName the name of the entry that the new entry should be inserted before
     * @param name       the name
     * @param value      the value
     */
    public OrderedRegistry<T> addAfter(@NonNull String targetName, @NonNull String name, @NonNull T value) {
        this.assertUniqueName(name);
        for (ListIterator<Entry<T>> itr = this.list.listIterator(); itr.hasNext(); ) {
            if (itr.next().name.equals(targetName)) {
                itr.add(new Entry<>(name, value));
                return this;
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("unable to find entry with name \"%s\"!", targetName));
    }

    /**
     * Replaces the existing entry with the given name.
     *
     * @param targetName the name of the entry that the new entry should be inserted before
     * @param value      the value
     */
    public OrderedRegistry<T> set(@NonNull String targetName, @NonNull T value) {
        return this.set(targetName, targetName, value);
    }

    /**
     * Replaces the existing entry with the given name.
     *
     * @param targetName the name of the entry that the new entry should be inserted before
     * @param newName    the name
     * @param value      the value
     */
    public OrderedRegistry<T> set(@NonNull String targetName, @NonNull String newName, @NonNull T value) {
        if (!targetName.equals(newName)) {
            this.assertUniqueName(newName);
        }
        for (ListIterator<Entry<T>> itr = this.list.listIterator(); itr.hasNext(); ) {
            if (itr.next().name.equals(targetName)) {
                itr.set(new Entry<>(newName, value));
                return this;
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("unable to find entry with name \"%s\"!", targetName));
    }

    /**
     * Removes the existing entry with the given name.
     *
     * @param name the name of the entry to remove
     */
    public OrderedRegistry<T> remove(@NonNull String name) {
        for (Iterator<Entry<T>> itr = this.list.iterator(); itr.hasNext(); ) {
            if (itr.next().name.equals(name)) {
                itr.remove();
                return this;
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("unable to find entry with name \"%s\"!", name));
    }

    /**
     * Removes the existing entry with the given name.
     *
     * @param name the name of the entry to remove
     */
    public T get(@NonNull String name) {
        for (Entry<T> entry : this.list) {
            if (entry.name.equals(name)) {
                return entry.value;
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("unable to find entry with name \"%s\"!", name));
    }

    protected void assertUniqueName(@NonNull String name) {
        this.list.forEach(entry -> checkState(!entry.name.equals(name), "name \"%s\" is already used!", name));
    }

    /**
     * @return a {@link Stream} over all the entries in this registry
     */
    public Stream<Map.Entry<String, T>> stream() {
        return uncheckedCast(this.list.stream());
    }

    @Override
    public Iterator<Map.Entry<String, T>> iterator() {
        return uncheckedCast(this.list.iterator());
    }

    @Override
    public void forEach(@NonNull Consumer<? super Map.Entry<String, T>> action) {
        this.list.forEach(action);
    }

    @Override
    public String toString() {
        return this.list.toString();
    }

    @AllArgsConstructor
    private static class Entry<T> implements Map.Entry<String, T> {
        @NonNull
        protected final String name;
        @NonNull
        protected T value;

        @Override
        public String getKey() {
            return this.name;
        }

        @Override
        public T getValue() {
            return this.value;
        }

        @Override
        public T setValue(@NonNull T value) {
            T old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public String toString() {
            return '{' + this.name + '=' + this.value + '}';
        }
    }
}
