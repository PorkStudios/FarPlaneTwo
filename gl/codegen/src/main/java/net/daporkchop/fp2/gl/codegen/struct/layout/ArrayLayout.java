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
import lombok.Getter;
import lombok.ToString;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ArrayAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.AttributeType;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ArrayLayout extends AttributeLayout {
    /**
     * The layout of each element in this array type.
     */
    private final AttributeLayout elementLayout;
    /**
     * The number of elements in this array type.
     */
    private final int elementCount;
    /**
     * The stride between array elements.
     */
    private final long elementStride;

    public ArrayLayout(long size, long alignment, AttributeLayout elementLayout, long elementStride, int elementCount) {
        super(size, alignment);

        this.elementLayout = elementLayout;
        this.elementCount = elementCount;
        this.elementStride = elementStride;
    }

    /**
     * Gets the offset of the element with the given index.
     *
     * @param index the element index
     * @return the element's offset
     */
    public long elementOffset(int index) {
        checkIndex(this.elementCount(), index);
        return this.elementStride * index;
    }

    @Override
    public boolean isCompatible(AttributeType attributeType) {
        if (!(attributeType instanceof ArrayAttributeType)) {
            return false;
        }

        ArrayAttributeType arrayAttributeType = (ArrayAttributeType) attributeType;
        return this.elementCount == arrayAttributeType.elementCount() && this.elementLayout.isCompatible(arrayAttributeType.elementType());
    }
}
