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

package net.daporkchop.fp2.gl.attribute;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static net.daporkchop.fp2.common.util.TypeSize.*;

/**
 * The different primitive types allowed to be used as a vertex attribute value.
 *
 * @author DaPorkchop_
 */
public interface AttributeType {
    /**
     * @return the size of a single value of this type
     */
    int size();

    /**
     * Gets the size (in bytes) of a vertex attribute using this type with the given number of components.
     *
     * @param components the vertex attribute component count
     * @return the size (in bytes)
     */
    int size(int components);

    /**
     * Integer vertex attribute types.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    enum Integer implements AttributeType {
        BYTE(BYTE_SIZE),
        UNSIGNED_BYTE(BYTE_SIZE),
        SHORT(SHORT_SIZE),
        UNSIGNED_SHORT(SHORT_SIZE),
        INT(INT_SIZE),
        UNSIGNED_INT(INT_SIZE);

        private final int size;

        @Override
        public int size(int components) {
            return this.size * components;
        }
    }

    /**
     * Floating-point vertex attribute types.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    enum Float implements AttributeType {
        FLOAT(FLOAT_SIZE),
        DOUBLE(DOUBLE_SIZE);

        private final int size;

        @Override
        public int size(int components) {
            return this.size * components;
        }
    }
}
