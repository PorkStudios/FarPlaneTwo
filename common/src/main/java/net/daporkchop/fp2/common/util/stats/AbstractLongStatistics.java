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

package net.daporkchop.fp2.common.util.stats;

import lombok.NonNull;
import net.daporkchop.lib.unsafe.PUnsafe;

import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation for a {@link Statistics} type consisting exclusively of {@code long} fields.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractLongStatistics<S extends AbstractLongStatistics<S>> implements Statistics<S> {
    private static final ClassValue<long[]> OFFSETS_CACHE = new ClassValue<long[]>() {
        @Override
        protected long[] computeValue(Class<?> type) {
            checkArg(type.getSuperclass() == AbstractLongStatistics.class, "%s doesn't extend from %s", type, AbstractLongStatistics.class);

            return Stream.of(type.getDeclaredFields())
                    .filter(field -> (field.getModifiers() & Modifier.STATIC) == 0)
                    .peek(field -> checkState(field.getType() == long.class, "field type must be long: %s", field))
                    .mapToLong(PUnsafe::objectFieldOffset)
                    .sorted()
                    .toArray();
        }
    };

    @Override
    public final S add(@NonNull S other) {
        S out = uncheckedCast(PUnsafe.allocateInstance(this.getClass()));
        for (long offset : OFFSETS_CACHE.get(this.getClass())) {
            PUnsafe.putLong(out, offset, Math.addExact(PUnsafe.getLong(this, offset), PUnsafe.getLong(other, offset)));
        }
        return out;
    }

    @Override
    public final S sub(@NonNull S other) {
        S out = uncheckedCast(PUnsafe.allocateInstance(this.getClass()));
        for (long offset : OFFSETS_CACHE.get(this.getClass())) {
            PUnsafe.putLong(out, offset, Math.subtractExact(PUnsafe.getLong(this, offset), PUnsafe.getLong(other, offset)));
        }
        return out;
    }
}
