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

package net.daporkchop.fp2.gl.codegen.struct;

import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.codegen.struct.interleaved.AbstractInterleavedAttributeStruct;
import net.daporkchop.fp2.gl.codegen.struct.interleaved.AbstractInterleavedAttributeWriter;
import net.daporkchop.fp2.gl.codegen.struct.interleaved.AbstractInterleavedUniformBuffer;
import net.daporkchop.fp2.gl.codegen.struct.layout.LayoutInfo;
import net.daporkchop.fp2.gl.codegen.util.LvtAlloc;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;
import java.util.EnumSet;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public class InterleavedStructFormatClassLoader<STRUCT extends AttributeStruct> extends AbstractStructFormatClassLoader<STRUCT> {
    public InterleavedStructFormatClassLoader(EnumSet<AttributeTarget> validTargets, Class<STRUCT> structClass, LayoutInfo layoutInfo) {
        super("Interleaved", validTargets, structClass, layoutInfo);
    }

    @Override
    protected byte[] attributeWriterClass() {
        return generateClass(ACC_PUBLIC | ACC_FINAL, this.attributeWriterClassInternalName, getInternalName(AbstractInterleavedAttributeWriter.class), null, cv -> {
            generatePassthroughCtor(cv, getInternalName(AbstractInterleavedAttributeWriter.class), getType(NewAttributeFormat.class), getType(DirectMemoryAllocator.class));

            //STRUCT handle(long address, int index) { ... }
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "handle", getMethodDescriptor(getType(AttributeStruct.class), LONG_TYPE, INT_TYPE), mv -> {
                mv.visitTypeInsn(NEW, this.handleClassInternalName);
                mv.visitInsn(DUP);
                mv.visitVarInsn(LLOAD, 1); // address + ((long) index * layout.size())
                mv.visitVarInsn(ILOAD, 3);
                mv.visitInsn(I2L);
                mv.visitLdcInsn(this.layoutInfo.rootLayout().size());
                mv.visitInsn(LMUL);
                mv.visitInsn(LADD);
                mv.visitMethodInsn(INVOKESPECIAL, this.handleClassInternalName, "<init>", getMethodDescriptor(VOID_TYPE, LONG_TYPE), false);
                return ARETURN;
            });

            //void grow(int requiredCapacity)
            generateMethod(cv, ACC_PROTECTED | ACC_FINAL, "grow", getMethodDescriptor(VOID_TYPE, INT_TYPE), mv -> {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitLdcInsn(this.layoutInfo.rootLayout().size());
                mv.visitMethodInsn(INVOKEVIRTUAL, this.attributeWriterClassInternalName, "grow", getMethodDescriptor(VOID_TYPE, INT_TYPE, LONG_TYPE), false);
                return RETURN;
            });

            //void copy(int src, int dst)
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "copy", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE), mv -> {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitVarInsn(ILOAD, 2);
                mv.visitLdcInsn(this.layoutInfo.rootLayout().size());
                mv.visitMethodInsn(INVOKEVIRTUAL, this.attributeWriterClassInternalName, "copy", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, LONG_TYPE), false);
                return RETURN;
            });

            //void copySingle(long src, long dst)
            generateMethod(cv, ACC_PROTECTED | ACC_FINAL, "copySingle", getMethodDescriptor(VOID_TYPE, LONG_TYPE, LONG_TYPE), mv -> {
                generateMemcpy(mv, 1, 3, this.layoutInfo.rootLayout().size());
                return RETURN;
            });

            //void copy(int src, int dst, int length)
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "copy", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), mv -> {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ILOAD, 1);
                mv.visitVarInsn(ILOAD, 2);
                mv.visitVarInsn(ILOAD, 3);
                mv.visitLdcInsn(this.layoutInfo.rootLayout().size());
                mv.visitMethodInsn(INVOKEVIRTUAL, this.attributeWriterClassInternalName, "copy", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, LONG_TYPE), false);
                return RETURN;
            });
        });
    }

    @Override
    protected byte[] handleClass() {
        return generateClass(ACC_PUBLIC, this.handleClassInternalName, getInternalName(AbstractInterleavedAttributeStruct.class), new String[]{ getInternalName(this.structClass) }, cv -> {
            generatePassthroughCtor(cv, getInternalName(AbstractInterleavedAttributeStruct.class), LONG_TYPE);

            //implement all interface methods
            this.forEachSetterMethod((method, attributeName) -> {
                String desc = getMethodDescriptor(method);
                MethodVisitor mv = beginMethod(cv, ACC_PUBLIC | ACC_FINAL, method.getName(), desc);

                LvtAlloc lvtAlloc = new LvtAlloc((getArgumentsAndReturnSizes(desc) >> 2) + 1);

                int addrLvtIndex = lvtAlloc.assign(LONG_TYPE);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, this.handleClassInternalName, "address", LONG_TYPE.getDescriptor());
                mv.visitVarInsn(LSTORE, addrLvtIndex);

                //TODO: ((StructMethod.Setter) structMethod).visit(mv, lvtIndex, this.layout.member(), this.visitStructMethod(mv, addrLvtIndex, 0L));

                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(ARETURN);

                finish(mv);
            });
        });
    }

    @Override
    protected byte[] handleUniformClass() {
        return generateClass(ACC_PUBLIC | ACC_FINAL, this.handleUniformClassInternalName, this.handleClassInternalName, null, cv -> {
            cv.visitField(ACC_PUBLIC | ACC_FINAL, "uniformBuffer", getDescriptor(AbstractInterleavedUniformBuffer.class), null, null).visitEnd();

            generatePassthroughCtorWithLocals(cv, this.handleUniformClassInternalName, this.handleClassInternalName,
                    Collections.singletonMap("uniformBuffer", getType(AbstractInterleavedUniformBuffer.class)),
                    LONG_TYPE);

            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "close", getMethodDescriptor(VOID_TYPE), mv -> {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, this.handleUniformClassInternalName, "uniformBuffer", getDescriptor(AbstractInterleavedUniformBuffer.class));
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, this.handleUniformClassInternalName, "address", LONG_TYPE.getDescriptor());
                mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(AbstractInterleavedUniformBuffer.class), "uploadAndRelease", getMethodDescriptor(VOID_TYPE, LONG_TYPE), false);
                return RETURN;
            });
        });
    }

    @Override
    protected byte[] uniformBufferClass() {
        return generateClass(ACC_PUBLIC | ACC_FINAL, this.uniformBufferClassInternalName, getInternalName(AbstractInterleavedUniformBuffer.class), null, cv -> {
            generatePassthroughCtor(cv, getInternalName(AbstractInterleavedUniformBuffer.class), getType(OpenGL.class), getType(NewAttributeFormat.Uniform.class));

            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "update", getMethodDescriptor(getType(AttributeStruct.class), LONG_TYPE), mv -> {
                mv.visitTypeInsn(NEW, this.handleClassInternalName);
                mv.visitInsn(DUP);
                mv.visitVarInsn(LLOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, this.handleClassInternalName, "<init>", getMethodDescriptor(VOID_TYPE, LONG_TYPE), false);
                return ARETURN;
            });
        });
    }
}
