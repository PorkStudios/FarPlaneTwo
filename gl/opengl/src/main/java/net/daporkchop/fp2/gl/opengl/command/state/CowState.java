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

package net.daporkchop.fp2.gl.opengl.command.state;

import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Immutable implementation of {@link State} with copy-on-write mutators.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor
public final class CowState implements State {
    final Map<StateValueProperty<?>, Object> values = new IdentityHashMap<>();

    CowState(@NonNull CowState state) {
        this.values.putAll(state.values);
    }

    CowState(@NonNull MutableState state) {
        this.values.putAll(state.values);
    }

    @Override
    public CowState immutableSnapshot() {
        return this;
    }

    @Override
    public MutableState mutableSnapshot() {
        return new MutableState(this);
    }

    @Override
    public <T> Optional<T> get(@NonNull StateValueProperty<T> property) {
        return uncheckedCast(Optional.ofNullable(this.values.get(property)));
    }

    @Override
    public <T> CowState set(@NonNull StateValueProperty<T> property, @NonNull T value) {
        if (!Objects.equals(this.values.get(property), value)) {
            CowState next = new CowState(this);
            next.values.put(property, value);
            return next;
        } else {
            return this;
        }
    }

    @Override
    public <T> CowState update(@NonNull StateValueProperty<T> property, @NonNull Function<T, T> updater) {
        return this.set(property, updater.apply(this.getOrDef(property)));
    }

    @Override
    public CowState unset(@NonNull StateValueProperty<?> property) {
        if (this.values.containsKey(property)) {
            CowState next = new CowState(this);
            next.values.remove(property);
            return next;
        } else {
            return this;
        }
    }

    @Override
    public Stream<StateValueProperty<?>> properties() {
        return this.values.keySet().stream();
    }
}
