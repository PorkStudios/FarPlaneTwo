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

package net.daporkchop.fp2.gl.codegen.struct.layout.assignment;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ArrayAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.AttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.MatrixAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.VectorAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.layout.ArrayLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.AttributeLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.LayoutInfo;
import net.daporkchop.fp2.gl.codegen.struct.layout.MatrixLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.VectorLayout;
import net.daporkchop.lib.common.math.PMath;

import java.util.stream.IntStream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implements the {@code std140} layout for GLSL interface blocks.
 *
 * @author DaPorkchop_
 * @see <a href="https://www.khronos.org/registry/OpenGL/specs/gl/glspec45.core.pdf#page=159>OpenGL 4.5, Section 7.6.2.2, page 137</a>
 */
@UtilityClass
public class Std140BlockMemoryLayout {
    public static LayoutInfo computeLayout(StructAttributeType type) {
        return new LayoutInfo(type, layout(type), "std140", true);
    }

    private static AttributeLayout layout(AttributeType type) {
        if (type instanceof StructAttributeType) {
            return layout((StructAttributeType) type);
        } else if (type instanceof ArrayAttributeType) {
            return layout((ArrayAttributeType) type);
        } else if (type instanceof MatrixAttributeType) {
            return layout((MatrixAttributeType) type);
        } else if (type instanceof VectorAttributeType) {
            return layout((VectorAttributeType) type);
        } else {
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    private static StructLayout layout(StructAttributeType type) {
        //9. If the member is a structure, the base alignment of the structure is N , where
        //   N is the largest base alignment value of any of its members, and rounded
        //   up to the base alignment of a vec4. The individual members of this sub-
        //   structure are then assigned offsets by applying this set of rules recursively,
        //   where the base offset of the first member of the sub-structure is equal to the
        //   aligned offset of the structure. The structure may have padding at the end;
        //   the base offset of the member following the sub-structure is rounded up to
        //   the next multiple of the base alignment of the structure.

        AttributeLayout[] fieldLayouts = IntStream.range(0, type.fieldCount())
                .mapToObj(i -> layout(type.fieldType(i)))
                .toArray(AttributeLayout[]::new);

        long[] fieldOffsets = new long[fieldLayouts.length];
        long maxFieldAlignment = 0L;
        long offset = 0L;
        for (int i = 0; i < fieldLayouts.length; i++) {
            AttributeLayout fieldLayout = fieldLayouts[i];

            maxFieldAlignment = Math.max(maxFieldAlignment, fieldLayout.alignment());

            //pad to member alignment
            offset = PMath.roundUp(offset, fieldLayout.alignment());
            fieldOffsets[i] = offset;
            offset += fieldLayout.size();
        }

        //If the member is a structure, the base alignment of the structure is N, where N is the largest base alignment value of any of
        // its members, and rounded up to the base alignment of a vec4.
        long structAlignment = PMath.roundUp(maxFieldAlignment, Float.BYTES * 4);

        //The structure may have padding at the end; the base offset of the member following the sub-structure is rounded up to
        // the next multiple of the base alignment of the structure.
        long size = PMath.roundUp(offset, structAlignment);

        return new StructLayout(size, structAlignment, fieldLayouts, fieldOffsets);
    }

    private static ArrayLayout layout(ArrayAttributeType type) {
        long stride;
        long alignment;

        AttributeType elementType = type.elementType();
        AttributeLayout elementLayout = layout(elementType);

        if (elementType instanceof VectorAttributeType) {
            //4. If the member is an array of scalars or vectors, the base alignment and array
            //   stride are set to match the base alignment of a single array element, according
            //   to rules (1), (2), and (3), and rounded up to the base alignment of a vec4. The
            //   array may have padding at the end; the base offset of the member following
            //   the array is rounded up to the next multiple of the base alignment.

            stride = PMath.roundUp(elementLayout.size(), Float.BYTES * 4);
            alignment = PMath.roundUp(elementLayout.alignment(), Float.BYTES * 4);
        } else if (elementType instanceof MatrixAttributeType) {
            //6. If the member is an array of S column-major matrices with C columns and
            //   R rows, the matrix is stored identically to a row of S × C column vectors
            //   with R components each, according to rule (4).

            //(not implemented)
            //8. If the member is an array of S row-major matrices with C columns and R
            //   rows, the matrix is stored identically to a row of S × R row vectors with C
            //   components each, according to rule (4).

            //matrices are always interpreted as R vectors with C components each, even if they're not used in an array, so we can use
            // the existing alignment and size with no additional changes.

            stride = elementLayout.size();
            alignment = elementLayout.alignment();
        } else if (elementType instanceof ArrayAttributeType) {
            //we can't support this without a hard dependency on ARB_arrays_of_arrays
            throw new UnsupportedOperationException("arrays of arrays are not supported!");
        } else if (elementType instanceof StructAttributeType) {
            //10. If the member is an array of S structures, the S elements of the array are laid
            //    out in order, according to rule (9).

            stride = elementLayout.size();
            alignment = elementLayout.alignment();
        } else {
            throw new IllegalArgumentException(String.valueOf(type));
        }

        long size = stride * type.elementCount();

        /*long[] elementOffsets = new long[type.elementCount()];
        long offset = 0L;
        for (int i = 0; i < elementOffsets.length; i++) {
            //pad to member alignment
            offset = PMath.roundUp(offset, alignment);
            elementOffsets[i] = offset;

            //advance by member size
            offset += stride;
        }*/
        checkState((stride % alignment) == 0L, "stride %s is not a multiple of alignment %s (type: %s)", stride, alignment, type);

        return new ArrayLayout(size, alignment, elementLayout, stride, type.elementCount());
    }

    private static MatrixLayout layout(MatrixAttributeType type) {
        //5. If the member is a column-major matrix with C columns and R rows, the
        //   matrix is stored identically to an array of C column vectors with R compo-
        //   nents each, according to rule (4).

        //(not implemented)
        //7. If the member is a row-major matrix with C columns and R rows, the matrix
        //   is stored identically to an array of R row vectors with C components each,
        //   according to rule (4).

        //matCxR is equivalent to vecR[C]
        //array element alignment+size is always in multiples of vec4
        VectorAttributeType colType = type.colType();
        VectorLayout colLayout = layout(colType);

        long stride = PMath.roundUp(colLayout.size(), Float.BYTES * 4);
        long alignment = PMath.roundUp(colLayout.alignment(), Float.BYTES * 4);
        long size = stride * type.cols();

        /*ShaderPrimitiveType primitiveType = type.colType().componentType().interpretedType();
        long alignment = PMath.roundUp((long) primitiveType.size() * type.rows(), Float.BYTES * 4);
        long size = alignment * type.cols();

        long[] colOffsets = new long[type.cols()];
        long offset = 0L;
        for (int col = 0; col < type.cols(); col++) {
            colOffsets[col] = offset;
            offset = PMath.roundUp(offset + (long) primitiveType.size() * type.rows(), Float.BYTES * 4);
        }*/

        return new MatrixLayout(size, alignment, layout(type.colType()), stride, type.cols());
    }

    private static VectorLayout layout(VectorAttributeType type) {
        long size;
        long alignment;

        //1. If the member is a scalar consuming N basic machine units, the base align-
        //   ment is N.

        //2. If the member is a two- or four-component vector with components consum-
        //   ing N basic machine units, the base alignment is 2N or 4N, respectively.

        //3. If the member is a three-component vector with components consuming N
        //   basic machine units, the base alignment is 4N.

        alignment = size = type.componentType().interpretedType().size() * (type.components() == 3 ? 4L : type.components());

        return new VectorLayout(size, alignment, type.componentType(),
                JavaPrimitiveType.from(type.componentType().interpretedType()),
                type.components());
    }
}
