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
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ArrayAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.AttributeType;
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

import java.util.BitSet;
import java.util.EnumSet;
import java.util.stream.IntStream;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class PackedInterleavedMemoryLayout {
    public static EnumSet<AttributeTarget> compatibleTargets(OpenGL gl, StructAttributeType type) {
        return EnumSet.of(AttributeTarget.VERTEX_ATTRIBUTE);
    }

    public static LayoutInfo computeLayout(OpenGL gl, StructAttributeType type) {
        return new LayoutInfo(type, layout(type), "packed", true, PackedInterleavedMemoryLayout::compatibleTargets);
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
        AttributeLayout[] fieldLayouts = IntStream.range(0, type.fieldCount())
                .mapToObj(i -> layout(type.fieldType(i)))
                .toArray(AttributeLayout[]::new);

        long[] fieldOffsets = new long[fieldLayouts.length];
        long maxFieldAlignment = 0L;
        long offset = 0L;
        /*for (int i = 0; i < fieldLayouts.length; i++) {
            AttributeLayout fieldLayout = fieldLayouts[i];

            maxFieldAlignment = Math.max(maxFieldAlignment, fieldLayout.alignment());

            //pad to member alignment
            offset = PMath.roundUp(offset, fieldLayout.alignment());
            fieldOffsets[i] = offset;
            offset += fieldLayout.size();
        }*/

        //greedily arrange attributes to minimize padding space
        BitSet remaining = new BitSet(fieldLayouts.length);
        remaining.set(0, fieldLayouts.length);
        while (!remaining.isEmpty()) {
            int minIndex = 0;
            long minPadding = Long.MAX_VALUE;
            for (int i = remaining.nextSetBit(0); i >= 0; i = remaining.nextSetBit(i + 1)) {
                long requiredPadding = PMath.roundUp(offset, fieldLayouts[i].alignment()) - offset;
                if (requiredPadding < minPadding) {
                    minPadding = requiredPadding;
                    minIndex = i;
                }
            }
            remaining.clear(minIndex);

            offset = PMath.roundUp(offset, fieldLayouts[minIndex].alignment());
            fieldOffsets[minIndex] = offset;
            offset += fieldLayouts[minIndex].size();

            maxFieldAlignment = Math.max(maxFieldAlignment, fieldLayouts[minIndex].alignment()); //this assumes that the alignments are always a power of two
        }

        long size = PMath.roundUp(offset, maxFieldAlignment);

        return new StructLayout(size, maxFieldAlignment, fieldLayouts, fieldOffsets);
    }

    private static ArrayLayout layout(ArrayAttributeType type) {
        AttributeLayout elementLayout = layout(type.elementType());
        long stride = PMath.roundUp(elementLayout.size(), elementLayout.alignment());
        return new ArrayLayout(stride * type.elementCount(), elementLayout.alignment(), elementLayout, stride, type.elementCount());
    }

    private static MatrixLayout layout(MatrixAttributeType type) {
        AttributeLayout colLayout = layout(type.colType());
        long stride = PMath.roundUp(colLayout.size(), colLayout.alignment());
        return new MatrixLayout(stride * type.cols(), colLayout.alignment(), layout(type.colType()), stride, type.cols());
    }

    private static VectorLayout layout(VectorAttributeType type) {
        long componentSize = type.componentType().storageType().size();

        long size = type.components() * componentSize;
        long alignment = Math.max(componentSize, 4); //TODO: this is only really necessary on AMD

        return new VectorLayout(size, alignment, type.componentType(),
                type.componentType().storageType(),
                type.components());
    }
}
