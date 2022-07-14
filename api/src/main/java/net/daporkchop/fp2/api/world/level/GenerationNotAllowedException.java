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

package net.daporkchop.fp2.api.world.level;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Thrown by accessor methods in {@link FBlockLevel} if an attempt is made to access terrain that does not exist and terrain generation is disallowed.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GenerationNotAllowedException extends Exception {
    private static final GenerationNotAllowedException INSTANCE = GenerationNotAllowedException.class.desiredAssertionStatus() ? new GenerationNotAllowedException() : null;

    private static final Consumer<?> THROW_IF_NULL = new Consumer<Object>() {
        @Override
        @SneakyThrows(GenerationNotAllowedException.class)
        public void accept(Object o) {
            if (o == null) {
                throw get();
            }
        }
    };

    /**
     * @return an instance of {@link GenerationNotAllowedException}
     */
    public static GenerationNotAllowedException get() {
        return INSTANCE != null ? INSTANCE : new GenerationNotAllowedException();
    }

    /**
     * @return a {@link Consumer} which will throw {@link GenerationNotAllowedException} if called with a {@code null} argument
     */
    public static <T> Consumer<T> uncheckedThrowIfNull() {
        return uncheckedCast(THROW_IF_NULL);
    }

    /**
     * Throws {@link GenerationNotAllowedException} if the given value is null.
     *
     * @param value the value
     * @return the value
     */
    @SneakyThrows(GenerationNotAllowedException.class)
    public static <T> T uncheckedThrowIfNull(T value) {
        if (value == null) {
            throw get();
        }
        return value;
    }

    /**
     * Throws {@link GenerationNotAllowedException} if the given value is null.
     *
     * @param value the value
     * @return the value
     */
    public static <T> T throwIfNull(T value) throws GenerationNotAllowedException {
        if (value == null) {
            throw get();
        }
        return value;
    }
}
