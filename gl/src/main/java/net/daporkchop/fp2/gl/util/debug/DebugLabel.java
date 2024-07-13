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

package net.daporkchop.fp2.gl.util.debug;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.util.GLObject;

/**
 * Utility for constructing a label for objects to be used by the {@link net.daporkchop.fp2.gl.GLExtension#GL_KHR_debug} extension.
 * <p>
 * We use this extra abstraction instead of directly concatenating strings because it allows us to avoid expensive string concatenation when
 * {@link net.daporkchop.fp2.gl.GLExtension#GL_KHR_debug} is unavailable and the labels would therefore not be used.
 *
 * @author DaPorkchop_
 */
public abstract class DebugLabel {
    /**
     * @return a {@link DebugLabel} which will ignore all strings pushed to it and not set the object's label
     */
    public static DebugLabel ignore() {
        return new DebugLabel() {
            @Override
            public DebugLabel push(@NonNull String suffix) {
                return this;
            }

            @Override
            public void configure(@NonNull GLObject object) {
                //no-op
            }
        };
    }

    /**
     * Gets a {@link DebugLabel} which will concatenate all strings pushed to it.
     *
     * @param initial the initial string
     * @return a {@link DebugLabel} which will concatenate all strings pushed to it
     */
    public static DebugLabel concat(@NonNull String initial) {
        return new Concat(initial);
    }

    /**
     * Gets a label with the given additional suffix appended to it.
     *
     * @param suffix the suffix
     * @return a label
     */
    public abstract DebugLabel push(@NonNull String suffix);

    /**
     * Sets the given OpenGL object's label to this value.
     *
     * @param object the object
     */
    public abstract void configure(@NonNull GLObject object);

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    private static final class Concat extends DebugLabel {
        private final String value;

        @Override
        public DebugLabel push(@NonNull String suffix) {
            return new Concat(this.value + " -> " + suffix);
        }

        @Override
        public void configure(@NonNull GLObject object) {
            object.setDebugLabel(this.value);
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
