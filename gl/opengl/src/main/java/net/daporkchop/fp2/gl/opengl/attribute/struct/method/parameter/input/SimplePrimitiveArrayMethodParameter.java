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
public class SimplePrimitiveArrayMethodParameter implements MethodParameter {
    @NonNull
    protected final ComponentType componentType;
    protected final int arrayLvtIndex;
    protected final int arrayLength;

    @Override
    public ComponentType componentType() {
        return this.componentType;
    }

    @Override
    public int components() {
        return this.arrayLength;
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
        //check array length and throw IllegalArgumentException if it's different than expected
        Label label = new Label();
        mv.visitVarInsn(ALOAD, this.arrayLvtIndex);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitLdcInsn(this.arrayLength);
        mv.visitJumpInsn(IF_ICMPEQ, label);

        mv.visitTypeInsn(NEW, getInternalName(IllegalArgumentException.class));
        mv.visitInsn(DUP);
        mv.visitLdcInsn(": array length must be " + this.arrayLength);
        mv.visitMethodInsn(INVOKESPECIAL, getInternalName(IllegalArgumentException.class), "<init>", getMethodDescriptor(VOID_TYPE, getType(String.class)), false);
        mv.visitInsn(ATHROW);

        mv.visitLabel(label);

        callback.accept(lvtIndexAllocatorIn, (lvtIndexAllocator, componentIndex) -> {
            checkIndex(this.arrayLength, componentIndex);

            mv.visitVarInsn(ALOAD, this.arrayLvtIndex);
            mv.visitLdcInsn(componentIndex);
            this.componentType.arrayLoad(mv);
        });
    }
}
