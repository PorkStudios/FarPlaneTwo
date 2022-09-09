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
 */

package net.daporkchop.fp2.gl.attribute.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author DaPorkchop_
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface MethodAttribute {
    int sort() default 0;

    String name() default "";

    /**
     * If non-empty, the attribute's component type is set to the vector type.
     * <p>
     * More than one vector component type may not be given.
     * <p>
     * More than one of {@link #typeArray()}, {@link #typeVector()}, {@link #typeMatrix()} and {@link #typeScalar()} may not be set at the same time.
     *
     * @return the array component type
     */
    AArrayType[] typeArray() default {};

    /**
     * If non-empty, the attribute's component type is set to the vector type.
     * <p>
     * More than one vector component type may not be given.
     * <p>
     * More than one of {@link #typeArray()}, {@link #typeVector()}, {@link #typeMatrix()} and {@link #typeScalar()} may not be set at the same time.
     *
     * @return the vector component type
     */
    AVectorType[] typeVector() default {};

    /**
     * If non-empty, the attribute's component type is set to the matrix type.
     * <p>
     * More than one matrix component type may not be given.
     * <p>
     * More than one of {@link #typeArray()}, {@link #typeVector()}, {@link #typeMatrix()} and {@link #typeScalar()} may not be set at the same time.
     *
     * @return the matrix component type
     */
    AMatrixType[] typeMatrix() default {};

    /**
     * If non-empty, the attribute's component type is set to the scalar type.
     * <p>
     * More than one scalar component type may not be given.
     * <p>
     * More than one of {@link #typeArray()}, {@link #typeVector()}, {@link #typeMatrix()} and {@link #typeScalar()} may not be set at the same time.
     *
     * @return the scalar component type
     */
    AScalarType[] typeScalar() default {};
}
