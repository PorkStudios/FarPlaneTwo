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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author DaPorkchop_
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ScalarConvert {
    Type value();

    boolean normalized() default false;

    /**
     * @author DaPorkchop_
     */
    enum Type {
        /**
         * The source value is a 2's compliment signed integer, and is re-interpreted as an unsigned integer without modification.
         * <p>
         * If used, this conversion must be the first element in {@link #convert()}.
         */
        TO_UNSIGNED,
        /**
         * The source value is an integer, and is converted to a {@code float}.
         * <p>
         * If {@link #normalized()} is {@code true}, the resulting values will be normalized. The range to which they are normalized depends on the source type:
         * <ul>
         *     <li>If the value is a 2's compliment signed integer, the resulting {@code float} is normalized to the range {@code [-1, 1)}.</li>
         *     <li>If the value is an unsigned integer, the resulting {@code float} is normalized to the range {@code [0, 1]}.</li>
         * </ul>
         */
        TO_FLOAT;
    }
}
