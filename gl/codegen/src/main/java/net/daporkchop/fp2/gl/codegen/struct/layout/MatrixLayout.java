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
import net.daporkchop.fp2.gl.codegen.struct.attribute.AttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.MatrixAttributeType;

import static net.daporkchop.lib.common.util.PValidation.checkArg;

/**
 * @author DaPorkchop_
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class MatrixLayout extends AttributeLayout {
    @Getter
    private final VectorLayout colLayout;
    private final long[] colOffsets;

    public MatrixLayout(long size, long alignment, VectorLayout colLayout, long[] colOffsets) {
        super(size, alignment);
        int cols = colOffsets.length;
        int rows = colLayout.components();
        checkArg(cols >= 2 && cols <= 4, "illegal matrix column count: %d", cols);
        checkArg(rows >= 2 && rows <= 4, "illegal matrix row count: %d", rows);

        this.colLayout = colLayout;
        this.colOffsets = colOffsets;
    }

    /**
     * @return the number of columns in this struct type
     */
    public int cols() {
        return this.colOffsets.length;
    }

    /**
     * Gets the offset of the column with the given index.
     *
     * @param index the column index
     * @return the column's offset
     */
    public long colOffset(int index) {
        return this.colOffsets[index];
    }

    @Override
    public boolean isCompatible(AttributeType attributeType) {
        if (!(attributeType instanceof MatrixAttributeType)) {
            return false;
        }

        MatrixAttributeType matrixAttributeType = (MatrixAttributeType) attributeType;
        return this.cols() == matrixAttributeType.cols() && this.colLayout.isCompatible(matrixAttributeType.colType());
    }
}
