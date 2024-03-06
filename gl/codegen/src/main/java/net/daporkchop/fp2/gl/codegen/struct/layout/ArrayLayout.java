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

package net.daporkchop.fp2.gl.codegen.struct.layout;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
public final class ArrayLayout extends AttributeLayout {
    private final AttributeLayout[] elements;
    private final long size;

    /**
     * @return the number of elements in this array type
     */
    public int elementCount() {
        return this.elements.length;
    }

    /**
     * Gets the layout of the element with the given index.
     *
     * @param index the element index
     * @return the element's layout
     */
    public AttributeLayout elementLayout(int index) {
        return this.elements[index];
    }
}
