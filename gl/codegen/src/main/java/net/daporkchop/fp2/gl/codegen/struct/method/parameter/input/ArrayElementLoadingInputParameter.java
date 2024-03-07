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

package net.daporkchop.fp2.gl.codegen.struct.method.parameter.input;

import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameter;
import net.daporkchop.lib.common.util.PValidation;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public final class ArrayElementLoadingInputParameter extends MethodParameter {
    private final int arrayLvtIndex;

    private final int[] arrayLengths;
    private final int[] componentCountsByDimensionIndex;

    public ArrayElementLoadingInputParameter(JavaPrimitiveType componentType, int arrayLvtIndex, int[] arrayLengths) {
        super(componentType, Arrays.stream(arrayLengths).peek(PValidation::positive).reduce(1, Math::multiplyExact));
        checkArg(arrayLengths.length > 0, "array must have at least one dimension");

        this.arrayLvtIndex = arrayLvtIndex;
        this.arrayLengths = arrayLengths.clone();

        this.componentCountsByDimensionIndex = new int[arrayLengths.length];
        this.componentCountsByDimensionIndex[0] = arrayLengths[0];
        for (int i = 1; i < arrayLengths.length; i++) {
            this.componentCountsByDimensionIndex[i] = this.componentCountsByDimensionIndex[i - 1] * arrayLengths[i];
        }
    }

    @Override
    public void visitLoad(MethodVisitor mv, int[] lvtAlloc, Consumer<IntConsumer> callback) {
        this.load0(mv, lvtAlloc, callback, this.arrayLengths.length - 1);
    }

    private void load0(MethodVisitor mv, int[] lvtAlloc, Consumer<IntConsumer> callback, int dimension) {
        if (dimension < 0) { //we've loaded all the array dimensions, we just need to load a reference for the root array
            mv.visitVarInsn(ALOAD, this.arrayLvtIndex);
            this.checkArrayLength(mv, 0);

            callback.accept(componentIndex -> {
                checkIndex(1, componentIndex);
                mv.visitVarInsn(ALOAD, this.arrayLvtIndex);
            });
        } else { //we're at a higher array level
            int lastParentArrayLvtIndex = dimension == 0
                    ? -1 //special case for dimension 0: we don't need to allocate an LVT entry, as the dimension 0 array won't be cached
                    : lvtAlloc[0]++;

            this.load0(mv, lvtAlloc, loader -> {
                int[] lastParentComponentIndex = {-1};

                callback.accept(componentIndex -> {
                    checkIndex(this.componentCountsByDimensionIndex[dimension], componentIndex);

                    int parentComponentIndex = componentIndex / this.arrayLengths[dimension];

                    //load a reference to the parent array
                    if (dimension == 0) { //special case for dimension 0: we don't do any caching since the array is already stored in the LVT
                        assert parentComponentIndex == 0 : "parentComponentIndex should be 0";
                        loader.accept(parentComponentIndex);
                    } else { //other dimensions load and cached a reference to the parent array dimension
                        if (lastParentComponentIndex[0] != parentComponentIndex) { //the next array index differs from the previous one, update cached array reference
                            loader.accept(lastParentComponentIndex[0] = parentComponentIndex);

                            mv.visitInsn(DUP);
                            mv.visitVarInsn(ASTORE, lastParentArrayLvtIndex);

                            this.checkArrayLength(mv, dimension);
                        }
                        mv.visitVarInsn(ALOAD, lastParentArrayLvtIndex);
                    }

                    mv.visitLdcInsn(componentIndex % this.arrayLengths[dimension]);
                    if (dimension == this.arrayLengths.length - 1) { //this is an array of componentType, load the component type
                        this.componentType().arrayLoad(mv);
                    } else { //this is an array of arrays, so we need to use AALOAD
                        mv.visitInsn(AALOAD);
                    }
                });
            }, dimension - 1);
        }
    }

    private void checkArrayLength(MethodVisitor mv, int dimension) {
        //check array length and throw IllegalArgumentException if it's different than expected
        Label label = new Label();
        mv.visitInsn(ARRAYLENGTH);
        mv.visitLdcInsn(this.arrayLengths[dimension]);
        mv.visitJumpInsn(IF_ICMPEQ, label);

        mv.visitTypeInsn(NEW, getInternalName(IllegalArgumentException.class));
        mv.visitInsn(DUP);
        mv.visitLdcInsn("array length must be " + this.arrayLengths[dimension]);
        mv.visitMethodInsn(INVOKESPECIAL, getInternalName(IllegalArgumentException.class), "<init>", getMethodDescriptor(VOID_TYPE, getType(String.class)), false);
        mv.visitInsn(ATHROW);

        mv.visitLabel(label);
    }
}
