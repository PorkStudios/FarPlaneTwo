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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import static net.daporkchop.lib.common.util.PValidation.checkArg;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public final class MatrixAttributeType extends AttributeType {
    public static MatrixAttributeType createScalar(ComponentType componentType) {
        return new MatrixAttributeType(componentType, 1, 1);
    }

    public static MatrixAttributeType createVector(ComponentType componentType, int components) {
        checkArg(components >= 2 && components <= 4, "illegal vector component count: %d", components);
        return new MatrixAttributeType(componentType, 1, components);
    }

    public static MatrixAttributeType createMatrix(ComponentType componentType, int cols, int rows) {
        checkArg(cols >= 2 && cols <= 4, "illegal matrix column count: %d", cols);
        checkArg(rows >= 2 && rows <= 4, "illegal matrix row count: %d", rows);

        return new MatrixAttributeType(componentType, cols, rows);
    }

    private final ComponentType componentType;

    /**
     * The number of columns this type has. If this type is not a matrix, this value is always {@code 1}
     */
    private final int cols;

    /**
     * The number of rows this property has. If this property is not a matrix, this value is always {@link #totalComponents()}
     */
    private final int rows;

    /**
     * @return the total number of primitive components in this element
     */
    public int totalComponents() {
        return this.cols * this.rows;
    }

    /**
     * @return {@code true} if this type represents a scalar type
     */
    public boolean isScalar() {
        return this.cols == 1 && this.rows == 1;
    }

    /**
     * @return {@code true} if this type represents a vector type
     */
    public boolean isVector() {
        return this.cols == 1 && this.rows >= 2;
    }

    /**
     * @return {@code true} if this type represents a matrix type
     */
    public boolean isMatrix() {
        return this.cols >= 2 && this.rows >= 2;
    }
}
