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

package net.daporkchop.fp2.gl.codegen.struct.method;

import net.daporkchop.fp2.gl.codegen.struct.attribute.ArrayAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.MatrixAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.VectorAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.layout.ArrayLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.AttributeLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.MatrixLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.VectorLayout;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.codegen.util.LvtAlloc;
import org.objectweb.asm.MethodVisitor;

import java.util.function.IntConsumer;

/**
 * @author DaPorkchop_
 */
@FunctionalInterface
public interface StructSetter<L extends AttributeLayout> {
    void visit(MethodVisitor mv, LvtAlloc lvtAlloc, L layout, Callback callback);

    /**
     * @author DaPorkchop_
     */
    abstract class Callback {
        /**
         * Recurse into a field of a struct.
         *
         * @param structType   the struct's attribute type
         * @param structLayout the struct's layout
         * @param fieldIndex   the field index
         * @return a {@link Callback} to use during recursion into the field
         */
        public Callback visitStructField(StructAttributeType structType, StructLayout structLayout, int fieldIndex) {
            throw new UnsupportedOperationException(this.getClass().getName());
        }

        /**
         * Recurse into an element of an array which is indexed at compile time.
         *
         * @param arrayType    the array's attribute type
         * @param arrayLayout  the array's layout
         * @param elementIndex the element index
         * @return a {@link Callback} to use during recursion into the array element
         */
        public Callback visitArrayElementConstant(ArrayAttributeType arrayType, ArrayLayout arrayLayout, int elementIndex) {
            throw new UnsupportedOperationException(this.getClass().getName());
        }

        /**
         * Recurse into an element of an array which is indexed at runtime.
         *
         * @param arrayType       the array's attribute type
         * @param arrayLayout     the array's layout
         * @param elementIndexLvt an LVT index containing the element index as an {@code int}
         * @return a {@link Callback} to use during recursion into the array element
         */
        public Callback visitArrayElementIndexed(ArrayAttributeType arrayType, ArrayLayout arrayLayout, int elementIndexLvt) {
            throw new UnsupportedOperationException(this.getClass().getName());
        }

        /**
         * Recurse into an column of an matrix which is indexed at compile time.
         *
         * @param matrixType   the matrix's attribute type
         * @param matrixLayout the matrix's layout
         * @param colIndex     the column index
         * @return a {@link Callback} to use during recursion into the matrix column
         */
        public Callback visitMatrixColumnConstant(MatrixAttributeType matrixType, MatrixLayout matrixLayout, int colIndex) {
            throw new UnsupportedOperationException(this.getClass().getName());
        }

        /**
         * Recurse into an column of an matrix which is indexed at runtime.
         *
         * @param matrixType   the matrix's attribute type
         * @param matrixLayout the matrix's layout
         * @param colIndexLvt  an LVT index containing the column index as an {@code int}
         * @return a {@link Callback} to use during recursion into the matrix column
         */
        public Callback visitMatrixColumnIndexed(MatrixAttributeType matrixType, MatrixLayout matrixLayout, int colIndexLvt) {
            throw new UnsupportedOperationException(this.getClass().getName());
        }

        /**
         * Recurse into a vector.
         *
         * @param vectorType      the vector's attribute type
         * @param vectorLayout    the vector's layout
         * @param parameter       a {@link MethodParameter} for accessing the incoming vector component values
         * @param componentLoader a {@link IntConsumer} used to load specific indexed vector components
         */
        public void visitVector(VectorAttributeType vectorType, VectorLayout vectorLayout, MethodParameter parameter, IntConsumer componentLoader) {
            throw new UnsupportedOperationException(this.getClass().getName());
        }
    }
}
