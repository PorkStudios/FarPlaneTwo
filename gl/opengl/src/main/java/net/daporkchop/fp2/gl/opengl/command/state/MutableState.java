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

import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Mutable implementation of {@link State}.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor
public final class MutableState implements State {
    final Map<StateValueProperty<?>, Object> values = new IdentityHashMap<>();

    MutableState(@NonNull CowState state) {
        this.values.putAll(state.values);
    }

    MutableState(@NonNull MutableState state) {
        this.values.putAll(state.values);
    }

    @Override
    public CowState immutableSnapshot() {
        return new CowState(this);
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
    public <T> MutableState set(@NonNull StateValueProperty<T> property, @NonNull T value) {
        this.values.put(property, value);
        return this;
    }

    @Override
    public <T> MutableState update(@NonNull StateValueProperty<T> property, @NonNull Function<T, T> updater) {
        return this.set(property, updater.apply(this.getOrDef(property)));
    }

    @Override
    public MutableState unset(@NonNull StateValueProperty<?> property) {
        this.values.remove(property);
        return this;
    }
}
