/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.event;

import lombok.Getter;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.event.generic.FRegisterEvent;
import net.daporkchop.fp2.api.util.OrderedRegistry;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.util.registry.ImmutableOrderedRegistry;
import net.daporkchop.fp2.core.util.registry.LinkedOrderedRegistry;
import net.daporkchop.lib.common.util.GenericMatcher;

import java.lang.reflect.Array;
import java.util.Map;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Implementation of {@link FRegisterEvent}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractRegisterEvent<T> implements FRegisterEvent<T> {
    protected final OrderedRegistry<T> registry = new LinkedOrderedRegistry<>();

    /**
     * Fires this event on {@link FP2Core#fp2()}'s {@link FEventBus}.
     */
    public AbstractRegisterEvent<T> fire() {
        return fp2().eventBus().fire(this);
    }

    /**
     * @return an immutable snapshot of the registry
     */
    public OrderedRegistry<T> immutableRegistry() {
        return new ImmutableOrderedRegistry<>(this.registry);
    }

    /**
     * Collects all the values in the registry into an array.
     *
     * @return the registry values
     */
    public T[] collectValues() {
        Class<?> t = GenericMatcher.find(this.getClass(), AbstractRegisterEvent.class, "T");
        return this.registry.stream()
                .map(Map.Entry::getValue)
                .toArray(i -> uncheckedCast(Array.newInstance(t, i)));
    }
}
