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

package net.daporkchop.fp2.gl.opengl.command.state;

import lombok.NonNull;

import java.util.Optional;
import java.util.function.Function;

/**
 * A mapping of {@link StateValueProperty} to their current values.
 *
 * @author DaPorkchop_
 */
public interface State {
    /**
     * @return a duplicate of this state as a {@link CowState}
     */
    CowState immutableSnapshot();

    /**
     * @return a duplicate of this state as a {@link MutableState}
     */
    MutableState mutableSnapshot();

    /**
     * Gets the value which the given {@link StateValueProperty} is set to.
     *
     * @param property the {@link StateValueProperty}
     * @return the value, or an empty {@link Optional} if we don't care what the current value is
     */
    <T> Optional<T> get(@NonNull StateValueProperty<T> property);

    /**
     * Gets the value which the given {@link StateValueProperty} is set to, falling back to the default value if unknown.
     *
     * @param property the {@link StateValueProperty}
     * @return the value, or the property's {@link StateValueProperty#def()} if the property is unset
     */
    default <T> T getOrDef(@NonNull StateValueProperty<T> property) {
        return this.get(property).orElseGet(property::def);
    }

    /**
     * Sets the given {@link StateValueProperty} to the given value.
     *
     * @param property the {@link StateValueProperty}
     * @param value    the value
     * @return a {@link State} with the value set. Note that this may return a new instance
     */
    <T> State set(@NonNull StateValueProperty<T> property, @NonNull T value);

    /**
     * Updates the value of the given {@link StateValueProperty} by applying the given {@link Function} to the current value.
     * <p>
     * If the {@link StateValueProperty} is currently unset, the default value will be used in place of the current value.
     *
     * @param property the {@link StateValueProperty}
     * @param updater  the {@link Function} to use for computing the new value
     * @return a {@link State} with the value set. Note that this may return a new instance
     */
    <T> State update(@NonNull StateValueProperty<T> property, @NonNull Function<T, T> updater);

    /**
     * Unsets the value which the given {@link StateValueProperty} is currently set to.
     *
     * @param property the {@link StateValueProperty}
     * @return a {@link State} with the value unset. Note that this may return a new instance
     */
    State unset(@NonNull StateValueProperty<?> property);
}
