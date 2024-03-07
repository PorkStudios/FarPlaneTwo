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

package net.daporkchop.fp2.gl.codegen.struct.method.parameter.transform;

import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameter;
import org.objectweb.asm.MethodVisitor;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
public final class Int2ARGBExpansionTransformParameter extends MethodParameter {
    private final MethodParameter parent;
    private final UnpackOrder order;
    private final boolean signed;

    public Int2ARGBExpansionTransformParameter(MethodParameter parent, UnpackOrder order, boolean signed) {
        super(signed ? JavaPrimitiveType.BYTE : JavaPrimitiveType.UNSIGNED_BYTE, order.components);

        checkArg(parent.componentType() == JavaPrimitiveType.INT, "parent component type must be %s (given: %s)", JavaPrimitiveType.INT, parent.componentType());
        checkArg(parent.components() == 1, "parent must have exactly one component (given: %d)", parent.components());

        this.parent = parent;
        this.order = order;
        this.signed = signed;
    }

    @Override
    public void visitLoad(MethodVisitor mv, int[] lvtAlloc, Consumer<IntConsumer> callback) {
        int packedLvtIndex = lvtAlloc[0]++;

        this.parent.visitLoad(mv, lvtAlloc, loader -> {
            //load the 0th component (which is an int) and store it in the LVT
            loader.accept(0);
            mv.visitVarInsn(ISTORE, packedLvtIndex);
            callback.accept(componentIndex -> {
                checkIndex(this.order.components, componentIndex);

                int orderIndex;
                switch (this.order) {
                    case ARGB:
                        //[0, 1, 2, 3] -> [3, 2, 1, 0]
                        orderIndex = componentIndex ^ 3;
                        break;
                    case RGBA:
                        //[0, 1, 2, 3] -> [2, 1, 0, 3]
                        orderIndex = (2 - componentIndex) & 3;
                        break;
                    case RGB:
                        //[0, 1, 2] -> [2, 1, 0]
                        orderIndex = 2 - componentIndex;
                        break;
                    default:
                        throw new UnsupportedOperationException(this.order.name());
                }
                int shift = orderIndex << 3;

                mv.visitVarInsn(ILOAD, packedLvtIndex);
                if (shift != 0) { //unsigned shift right if necessary
                    mv.visitLdcInsn(shift);
                    mv.visitInsn(this.signed ? ISHR : IUSHR);
                }
                if (this.signed) {
                    if (shift != 24) { //truncate and sign-extend if needed (not necessary for component at bits [24, 32), as it is already sign-extended during shifting)
                        mv.visitInsn(I2B);
                    }
                } else {
                    if (shift != 24) { //truncate if needed (not necessary for component at bits [24, 32), as all other bits are already discarded during shifting)
                        mv.visitIntInsn(SIPUSH, 0xFF);
                        mv.visitInsn(IAND);
                    }
                }
            });
        });
    }

    /**
     * The different orders in which the individual color channels may be extracted.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    public enum UnpackOrder {
        ARGB(4),
        RGBA(4),
        RGB(3),
        ;

        public final int components;
    }
}
