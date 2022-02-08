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

package net.daporkchop.fp2.gl.attribute.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author DaPorkchop_
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ArrayTransform {
    Type value();

    int matrixCols() default -1;

    int matrixRows() default -1;

    int vectorComponents() default -1;

    /**
     * @author DaPorkchop_
     */
    enum Type {
        /**
         * The source type is an array of scalars.
         * <p>
         * It is interpreted as a single column-major matrix, with the matrix dimensions taken from the {@link #matrixCols()} and {@link #matrixRows()} properties (which must be
         * set). The array length must be exactly {@code matrixCols() * matrixRows()}.
         * <p>
         * This transform, if used, must be the final transform in the chain.
         */
        TO_MATRIX,
        /**
         * The source type is an array of scalars.
         * <p>
         * It is interpreted as an array of column-major matrices, with the matrix dimensions taken from the {@link #matrixCols()} and {@link #matrixRows()} properties (which must
         * be set). The array length must be a multiple of {@code matrixCols() * matrixRows()}.
         */
        TO_MATRIX_ARRAY,
        /**
         * The source type is an array of scalars.
         * <p>
         * It is interpreted as a single vector, with the component count taken from the array length.
         * <p>
         * This transform, if used, must be the final transform in the chain.
         */
        TO_VECTOR,
        /**
         * The source type is an array of scalars.
         * <p>
         * It is interpreted as an array of vectors, with the component count taken from the {@link #vectorComponents()} property (which must be set). The array length must
         * be a multiple of the component count.
         */
        TO_VECTOR_ARRAY;
    }
}
