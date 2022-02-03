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
 *
 */

package net.daporkchop.fp2.gl.opengl.attribute.struct.property.input;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructPropertyFactory;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLBasicType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLTypeFactory;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class SimplePrimitiveArrayInputProperty implements StructProperty.Elements {
    private final StructPropertyFactory.Options options;

    private final ComponentType componentType;

    private final Field field;
    private final int length;

    public SimplePrimitiveArrayInputProperty(@NonNull StructPropertyFactory.Options options, @NonNull Field field, int length) {
        this.options = options;
        this.field = field;
        this.length = length;

        this.componentType = ComponentType.from(field.getType().getComponentType());
    }

    @Override
    public int elements() {
        return this.length;
    }

    @Override
    public StructProperty element(int elementIndex) {
        checkIndex(this.length, elementIndex);

        return new StructProperty.Components() {
            @Override
            public ComponentType componentType() {
                return SimplePrimitiveArrayInputProperty.this.componentType;
            }

            @Override
            public GLSLBasicType glslType() {
                return GLSLTypeFactory.vec(this.componentType().glslPrimitive(), this.components());
            }

            @Override
            public int cols() {
                return 1;
            }

            @Override
            public int rows() {
                return 1;
            }

            @Override
            public void load(@NonNull MethodVisitor mv, int structLvtIndexIn, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
                callback.accept(structLvtIndexIn, lvtIndexAllocatorIn, (structLvtIndex, lvtIndexAllocator, componentIndex) -> {
                    checkIndex(1, componentIndex);

                    //array is stored at structLvtIndex, we want to load the elementIndex-th element from it
                    mv.visitVarInsn(ALOAD, structLvtIndex);
                    mv.visitLdcInsn(elementIndex);
                    this.componentType().arrayLoad(mv);
                });
            }
        };
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int structLvtIndex, int lvtIndexAllocator, @NonNull LoadCallback callback) {
        int arrayLvtIndex = lvtIndexAllocator++;

        //load array from struct into a local variable
        mv.visitVarInsn(ALOAD, structLvtIndex);
        mv.visitFieldInsn(GETFIELD, getInternalName(this.field.getDeclaringClass()), this.field.getName(), getDescriptor(this.field.getType()));
        mv.visitVarInsn(ASTORE, arrayLvtIndex);

        //check array length and throw IllegalArgumentException if it's different than expected
        Label label = new Label();
        mv.visitVarInsn(ALOAD, arrayLvtIndex);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitLdcInsn(this.length);
        mv.visitJumpInsn(IF_ICMPEQ, label);

        mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(this.field + ": array length must be " + this.length);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);

        mv.visitLabel(label);

        callback.accept(arrayLvtIndex, lvtIndexAllocator);
    }
}
