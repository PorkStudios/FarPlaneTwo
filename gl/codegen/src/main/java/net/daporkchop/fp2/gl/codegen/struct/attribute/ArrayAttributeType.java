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

package net.daporkchop.fp2.gl.codegen.struct.attribute;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import net.daporkchop.lib.primitive.lambda.IntObjConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public final class ArrayAttributeType extends AttributeType {
    /**
     * This array type's component type.
     */
    private final AttributeType elementType;

    /**
     * The number of elements in this array type.
     */
    private final int elementCount;

    public ArrayAttributeType(AttributeType elementType, int elementCount) {
        this.elementType = elementType;
        this.elementCount = positive(elementCount, "elementCount");
    }

    /**
     * Runs the given action on each of the elements in this array type.
     *
     * @param action the action. For each element in this array type, the action will be invoked once with the element index and the element type
     */
    public void forEachElement(@NonNull IntObjConsumer<AttributeType> action) {
        for (int index = 0; index < this.elementCount; index++) {
            action.accept(index, this.elementType);
        }
    }
}
