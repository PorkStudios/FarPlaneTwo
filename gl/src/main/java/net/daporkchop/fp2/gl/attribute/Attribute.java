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

package net.daporkchop.fp2.gl.attribute;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author DaPorkchop_
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Attribute {
    int sort() default 0;

    String[] vectorAxes() default {};

    MatrixDimension matrixDimension() default @MatrixDimension(_default = true, columns = -1, rows = -1);

    VectorDimension vectorDimension() default @VectorDimension(_default = true, components = -1);

    Transformation transform() default Transformation.UNCHANGED;

    Conversion[] convert() default {};

    /**
     * @author DaPorkchop_
     */
    enum Transformation {
        /**
         * The source type is interpreted as-is.
         */
        UNCHANGED,
        /**
         * The source type is a single {@code int}.
         * <p>
         * It is interpreted as an ARGB8 color, from which the RGB components are extracted individually, resulting in a 3-component vector of unsigned {@code byte}s.
         */
        INT_ARGB8_TO_BYTE_VECTOR_RGB,
        /**
         * The source type is a single {@code int}.
         * <p>
         * It is interpreted as an ARGB8 color, from which the ARGB components are extracted individually, resulting in a 4-component vector of unsigned {@code byte}s.
         */
        INT_ARGB8_TO_BYTE_VECTOR_RGBA,
        /**
         * The source type is a single primitive array with exactly {@code columns * rows} elements.
         * <p>
         * It is interpreted as column-major matrix, with the matrix dimensions taken from the {@link #matrixDimension()} property (which must be set).
         */
        ARRAY_TO_MATRIX,
        /**
         * The source type is a single primitive array with exactly {@code components} elements.
         * <p>
         * It is interpreted as vector, with the component count taken from the {@link #vectorDimension()} property (which must be set).
         */
        ARRAY_TO_VECTOR;
    }

    /**
     * @author DaPorkchop_
     */
    enum Conversion {
        /**
         * The source value is a 2's compliment signed integer, and is re-interpreted as an unsigned integer.
         */
        TO_UNSIGNED,
        /**
         * The value is an integer type, and is converted to a {@code float}.
         */
        TO_FLOAT,
        /**
         * The value is an integer type, and is converted to a normalized {@code float}.
         * <p>
         * If the value is a 2's compliment signed integer, the resulting {@code float} is normalized to the range {@code [-1, 1)}. If the value is an unsigned integer, the resulting {@code float} is normalized
         * to the range {@code [0, 1]}.
         */
        TO_NORMALIZED_FLOAT;
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface MatrixDimension {
        @Deprecated
        boolean _default() default false;

        /**
         * @return the number of columns. Must be in range {@code [2, 4]}
         */
        int columns();

        /**
         * @return the number of rows. Must be in range {@code [2, 4]}
         */
        int rows();
    }

    /**
     * @author DaPorkchop_
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface VectorDimension {
        @Deprecated
        boolean _default() default false;

        /**
         * @return the number of columns. Must be in range {@code [2, 4]}
         */
        int components();
    }
}
