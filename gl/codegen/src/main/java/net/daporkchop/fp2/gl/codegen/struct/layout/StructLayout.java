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
import lombok.ToString;
import net.daporkchop.fp2.gl.codegen.struct.attribute.AttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;

import static net.daporkchop.lib.common.util.PValidation.checkArg;

/**
 * @author DaPorkchop_
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class StructLayout extends AttributeLayout {
    private final AttributeLayout[] fields;
    private final long[] fieldOffsets;

    public StructLayout(long size, long alignment, AttributeLayout[] fields, long[] fieldOffsets) {
        super(size, alignment);
        checkArg(fields.length == fieldOffsets.length);

        this.fields = fields;
        this.fieldOffsets = fieldOffsets;
    }

    /**
     * @return the number of fields in this struct type
     */
    public int fieldCount() {
        return this.fields.length;
    }

    /**
     * Gets the layout of the field with the given index.
     *
     * @param index the field index
     * @return the field's layout
     */
    public AttributeLayout fieldLayout(int index) {
        return this.fields[index];
    }

    /**
     * Gets the offset of the field with the given index.
     *
     * @param index the field index
     * @return the field's offset
     */
    public long fieldOffset(int index) {
        return this.fieldOffsets[index];
    }

    @Override
    public boolean isCompatible(AttributeType attributeType) {
        if (!(attributeType instanceof StructAttributeType)) {
            return false;
        }

        StructAttributeType structAttributeType = (StructAttributeType) attributeType;
        if (this.fieldCount() != structAttributeType.fieldCount()) {
            return false;
        }

        for (int index = 0; index < this.fieldCount(); index++) {
            if (!this.fieldLayout(index).isCompatible(structAttributeType.fieldType(index))) {
                return false;
            }
        }
        return true;
    }
}
