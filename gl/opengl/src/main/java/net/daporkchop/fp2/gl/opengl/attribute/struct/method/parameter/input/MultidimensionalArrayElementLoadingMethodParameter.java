/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.input;

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@Data
public class MultidimensionalArrayElementLoadingMethodParameter implements MethodParameter {
    protected final ComponentType componentType;
    protected final int arrayLvtIndex;

    protected final int[] arrayLengths;
    protected final int[] componentCountsByDimensionIndex;
    protected final int components;

    public MultidimensionalArrayElementLoadingMethodParameter(@NonNull ComponentType componentType, int arrayLvtIndex, @NonNull int[] arrayLengths) {
        checkArg(arrayLengths.length > 0, "array must have at least one dimension");

        this.componentType = componentType;
        this.arrayLvtIndex = arrayLvtIndex;

        int components = 1;
        this.componentCountsByDimensionIndex = new int[arrayLengths.length];
        for (int i = 0; i < arrayLengths.length; i++) {
            components = Math.multiplyExact(components, positive(arrayLengths[i]));
            this.componentCountsByDimensionIndex[i] = components;
        }
        this.components = components;
        this.arrayLengths = arrayLengths.clone();
    }

    protected void checkArrayLength(@NonNull MethodVisitor mv, int dimension) {
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

    @Override
    public void load(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
        this.load0(mv, lvtIndexAllocatorIn, callback, this.arrayLengths.length - 1);
    }

    protected void load0(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn, @NonNull LoadCallback callback, int dimension) {
        if (dimension < 0) { //we've loaded all the array dimensions, we just need to load a reference for the root array
            mv.visitVarInsn(ALOAD, this.arrayLvtIndex);
            this.checkArrayLength(mv, 0);

            callback.accept(lvtIndexAllocatorIn, (lvtIndexAllocator, componentIndex) -> {
                checkIndex(1, componentIndex);
                mv.visitVarInsn(ALOAD, this.arrayLvtIndex);
            });
        } else { //we're at a higher array level
            this.load0(mv, lvtIndexAllocatorIn, (lvtIndexAllocatorFromParent, loader) -> {
                int[] lastParentComponentIndex = { -1 };
                int lastParentArrayLvtIndex = dimension == 0
                        ? -1 //special case for dimension 0: we don't need to allocate an LVT entry, as the dimension 0 array won't be cached
                        : lvtIndexAllocatorFromParent++;

                callback.accept(lvtIndexAllocatorFromParent, (lvtIndexAllocator, componentIndex) -> {
                    checkIndex(this.componentCountsByDimensionIndex[dimension], componentIndex);

                    int parentComponentIndex = componentIndex / this.arrayLengths[dimension];

                    //load a reference to the parent array
                    if (dimension == 0) { //special case for dimension 0: we don't do any caching since the array is already stored in the LVT
                        assert parentComponentIndex == 0 : "parentComponentIndex should be 0";
                        loader.load(lvtIndexAllocator, parentComponentIndex);
                    } else { //other dimensions load and cached a reference to the parent array dimension
                        if (lastParentComponentIndex[0] != parentComponentIndex) { //the next array index differs from the previous one, update cached array reference
                            loader.load(lvtIndexAllocator, lastParentComponentIndex[0] = parentComponentIndex);

                            mv.visitInsn(DUP);
                            mv.visitVarInsn(ASTORE, lastParentArrayLvtIndex);

                            this.checkArrayLength(mv, dimension);
                        }
                        mv.visitVarInsn(ALOAD, lastParentArrayLvtIndex);
                    }


                    mv.visitLdcInsn(componentIndex % this.arrayLengths[dimension]);
                    if (dimension == this.arrayLengths.length - 1) { //this is an array of componentType, load the component type
                        this.componentType.arrayLoad(mv);
                    } else { //this is an array of arrays, so we need to use AALOAD
                        mv.visitInsn(AALOAD);
                    }
                });
            }, dimension - 1);
        }
    }
}
