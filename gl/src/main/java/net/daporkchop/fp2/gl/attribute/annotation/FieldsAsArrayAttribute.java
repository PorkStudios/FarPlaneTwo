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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a group of named fields should be grouped together into a single virtual array attribute.
 * <p>
 * This allows defining primitive arrays and vectors without an indirection into an actual Java array.
 *
 * @author DaPorkchop_
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldsAsArrayAttribute {
    /**
     * @return the {@link Attribute} annotation to place on the virtual attribute. {@link Attribute#name()} <strong>must</strong> be set!
     */
    Attribute attribute();

    /**
     * @return the names of the fields to interpret as array components, in order
     */
    String[] names();

    /**
     * An additional {@link ScalarType} annotation to be applied to every array component.
     * <p>
     * This value has no effect if the array's component type is not scalar.
     * <p>
     * If any array component's type is annotated as {@link ScalarType}, that annotation will take priority over this one.
     */
    ScalarType scalarType() default @ScalarType;

    /**
     * @return any additional {@link ArrayTransform}s to apply to the array after it has been assembled
     */
    ArrayTransform[] transform() default {};
}
