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
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ComponentType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;

import static net.daporkchop.lib.common.util.PValidation.checkArg;

/**
 * @author DaPorkchop_
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public final class MatrixLayout extends AttributeLayout {
    private final JavaPrimitiveType physicalStorageType;

    private final long[][] componentOffsets;
    private final int[] bufferIndicesPerCol;

    private final int cols;
    private final int rows;

    public MatrixLayout(JavaPrimitiveType physicalStorageType, int cols, int rows, long[][] componentOffsets, int[] bufferIndices) {
        checkArg(cols >= 2 && cols <= 4, "illegal matrix column count: %d", cols);
        checkArg(rows >= 2 && rows <= 4, "illegal matrix row count: %d", rows);

        checkArg(componentOffsets.length == cols && bufferIndices.length == cols);
        for (long[] row : componentOffsets) {
            checkArg(row.length == rows);
        }

        this.physicalStorageType = physicalStorageType;

        this.componentOffsets = componentOffsets;
        this.bufferIndicesPerCol = bufferIndices;

        this.cols = cols;
        this.rows = rows;
    }

    public int cols() {
        return this.cols;
    }

    public int rows() {
        return this.rows;
    }
}
