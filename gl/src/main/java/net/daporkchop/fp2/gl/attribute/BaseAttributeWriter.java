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

import lombok.NonNull;

/**
 * Base interface which is able to set individual attribute values.
 *
 * @author DaPorkchop_
 */
public interface BaseAttributeWriter<B extends BaseAttributeWriter<B>> {
    /**
     * @return the {@link AttributeFormat} used by this writer
     */
    AttributeFormat format();

    /**
     * Sets the given {@link Attribute.Int1} of the current vertex to the given value.
     *
     * @param attrib the {@link Attribute.Int1}
     * @param v0     the value of the 0th component
     */
    B set(@NonNull Attribute.Int1 attrib, int v0);

    /**
     * Sets the given {@link Attribute.Int2} of the current vertex to the given value.
     *
     * @param attrib the {@link Attribute.Int2}
     * @param v0     the value of the 0th component
     * @param v1     the value of the 1st component
     */
    B set(@NonNull Attribute.Int2 attrib, int v0, int v1);

    /**
     * Sets the given {@link Attribute.Int3} of the current vertex to the given value.
     *
     * @param attrib the {@link Attribute.Int3}
     * @param v0     the value of the 0th component
     * @param v1     the value of the 1st component
     * @param v2     the value of the 2nd component
     */
    B set(@NonNull Attribute.Int3 attrib, int v0, int v1, int v2);

    /**
     * Sets the given {@link Attribute.Int3} of the current vertex to the given value.
     *
     * @param attrib the {@link Attribute.Int3}
     * @param argb   the ARGB8888 value. the 4 color channels correspond to the 3 components as follows:<br>
     *               <ul>
     *                   <li>A {@code ->} <i>discarded</i></li>
     *                   <li>R {@code ->} 0</li>
     *                   <li>G {@code ->} 1</li>
     *                   <li>B {@code ->} 2</li>
     *               </ul>
     */
    B setARGB(@NonNull Attribute.Int3 attrib, int argb);

    /**
     * Sets the given {@link Attribute.Int4} of the current vertex to the given value.
     *
     * @param attrib the {@link Attribute.Int4}
     * @param v0     the value of the 0th component
     * @param v1     the value of the 1st component
     * @param v2     the value of the 2nd component
     * @param v3     the value of the 3rd component
     */
    B set(@NonNull Attribute.Int4 attrib, int v0, int v1, int v2, int v3);

    /**
     * Sets the given {@link Attribute.Int4} of the current vertex to the given value.
     *
     * @param attrib the {@link Attribute.Int4}
     * @param argb   the ARGB8888 value. the 4 color channels correspond to the 4 components as follows:<br>
     *               <ul>
     *                   <li>A {@code ->} 3</li>
     *                   <li>R {@code ->} 0</li>
     *                   <li>G {@code ->} 1</li>
     *                   <li>B {@code ->} 2</li>
     *               </ul>
     */
    B setARGB(@NonNull Attribute.Int4 attrib, int argb);
}
