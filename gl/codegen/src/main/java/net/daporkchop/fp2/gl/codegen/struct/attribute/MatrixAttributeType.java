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
import lombok.ToString;

import static net.daporkchop.lib.common.util.PValidation.checkArg;

/**
 * @author DaPorkchop_
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public final class MatrixAttributeType extends AttributeType {
    /**
     * The type of each column in this matrix type.
     */
    private final VectorAttributeType colType;

    /**
     * The number of columns this matrix type has.
     */
    private final int cols;

    public MatrixAttributeType(VectorAttributeType colType, int cols) {
        int rows = colType.components();
        checkArg(cols >= 2 && cols <= 4, "illegal matrix column count: %d", cols);
        checkArg(rows >= 2 && rows <= 4, "illegal matrix row count: %d", rows);

        this.colType = colType;
        this.cols = cols;
    }

    /**
     * @return the number of rows this matrix type has
     */
    public int rows() {
        return this.colType.components();
    }
}
