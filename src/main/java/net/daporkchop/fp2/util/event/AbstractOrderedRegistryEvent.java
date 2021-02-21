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

package net.daporkchop.fp2.util.event;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.OrderedRegistry;
import net.daporkchop.lib.common.util.GenericMatcher;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.lang.reflect.Array;
import java.util.Map;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Fired on {@link MinecraftForge#EVENT_BUS}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractOrderedRegistryEvent<T> extends Event {
    @NonNull
    protected final OrderedRegistry<T> registry;

    /**
     * Fires this event on {@link MinecraftForge#EVENT_BUS}.
     */
    public AbstractOrderedRegistryEvent<T> fire() {
        MinecraftForge.EVENT_BUS.post(this);
        return this;
    }

    /**
     * Collects all the values in the registry into an array.
     *
     * @return the registry values
     */
    public T[] collectValues() {
        Class<?> t = GenericMatcher.find(this.getClass(), AbstractOrderedRegistryEvent.class, "T");
        return this.registry.stream()
                .map(Map.Entry::getValue)
                .toArray(i -> uncheckedCast(Array.newInstance(t, i)));
    }
}
