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

import lombok.SneakyThrows;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.attribute.NewAttributeWriter;
import net.daporkchop.fp2.gl.attribute.NewUniformBuffer;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ArrayAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.MatrixAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.VectorAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.interleaved.AbstractInterleavedAttributeFormat;
import net.daporkchop.fp2.gl.codegen.struct.interleaved.AbstractInterleavedAttributeStruct;
import net.daporkchop.fp2.gl.codegen.struct.interleaved.AbstractInterleavedAttributeWriter;
import net.daporkchop.fp2.gl.codegen.struct.interleaved.AbstractInterleavedUniformBuffer;
import net.daporkchop.fp2.gl.codegen.struct.layout.ArrayLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.LayoutInfo;
import net.daporkchop.fp2.gl.codegen.struct.layout.MatrixLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.VectorLayout;
import net.daporkchop.fp2.gl.codegen.struct.method.StructSetter;
import net.daporkchop.fp2.gl.codegen.struct.method.StructSetterFactory;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.codegen.util.LvtAlloc;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public class InterleavedStructFormatClassLoader<STRUCT extends AttributeStruct> extends AbstractStructFormatClassLoader<STRUCT> {
    public InterleavedStructFormatClassLoader(Class<STRUCT> structClass, LayoutInfo layoutInfo) {
        super("Interleaved", structClass, layoutInfo);
    }

    @Override
    protected void registerClassGenerators(BiConsumer<String, Supplier<byte[]>> registerGenerator, Consumer<Class<?>> registerClass) {
        super.registerClassGenerators(registerGenerator, registerClass);

        registerClass.accept(AbstractInterleavedAttributeFormat.class);
        registerClass.accept(AbstractInterleavedAttributeStruct.class);
        registerClass.accept(AbstractInterleavedAttributeWriter.class);
        registerClass.accept(AbstractInterleavedUniformBuffer.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    @SneakyThrows
    public NewAttributeFormat<STRUCT> createAttributeFormat(OpenGL gl) {
        return (NewAttributeFormat<STRUCT>) MethodHandles.publicLookup().findConstructor(this.loadClass(this.attributeFormatClassInternalName.replace('/', '.')), MethodType.methodType(void.class, OpenGL.class, LayoutInfo.class))
                .invoke(gl, this.layoutInfo);
    }

    @Override
    protected byte[] attributeFormatClass() {
        return generateClass(ACC_PUBLIC | ACC_FINAL, this.attributeFormatClassInternalName, getInternalName(AbstractInterleavedAttributeFormat.class), null, cv -> {
            generatePassthroughCtor(cv, getInternalName(AbstractInterleavedAttributeFormat.class), getType(OpenGL.class), getType(LayoutInfo.class));

            //AttributeBuffer createBuffer()
            /*generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "createBuffer", getMethodDescriptor(getType(NewAttributeBuffer.class), getType(OpenGL.class)), mv -> {
                mv.visitTypeInsn(NEW, this.attributeWriterClassInternalName);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, this.attributeWriterClassInternalName, "<init>", getMethodDescriptor(VOID_TYPE, getType(NewAttributeFormat.class), getType(DirectMemoryAllocator.class)), false);
                return ARETURN;
            });*/

            //AttributeWriter createWriter(DirectMemoryAllocator alloc)
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "createWriter", getMethodDescriptor(getType(NewAttributeWriter.class), getType(DirectMemoryAllocator.class)), mv -> {
                mv.visitTypeInsn(NEW, this.attributeWriterClassInternalName);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKESPECIAL, this.attributeWriterClassInternalName, "<init>", getMethodDescriptor(VOID_TYPE, getType(NewAttributeFormat.class), getType(DirectMemoryAllocator.class)), false);
                return ARETURN;
            });

            //int occupiedVertexAttributes()
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "occupiedVertexAttributes", getMethodDescriptor(INT_TYPE), mv -> {
                generateSupportedCheck(mv, this.attributeFormatClassInternalName, AttributeTarget.VERTEX_ATTRIBUTE);
                mv.visitLdcInsn(this.layoutInfo.rootType().occupiedVertexAttributes());
                return IRETURN;
            });

            //UniformBuffer createUniformBuffer()
            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "createUniformBuffer", getMethodDescriptor(getType(NewUniformBuffer.class)), mv -> {
                generateSupportedCheck(mv, this.attributeFormatClassInternalName, AttributeTarget.UBO);
                mv.visitTypeInsn(NEW, this.uniformBufferClassInternalName);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, this.uniformBufferClassInternalName, "<init>", getMethodDescriptor(VOID_TYPE, getType(NewAttributeFormat.class)), false);
                return ARETURN;
            });
        });
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

                StructSetterFactory.createFromMethod(this.layoutInfo.rootType(), method, attributeName)
                        .visit(mv, lvtAlloc, this.layoutInfo.rootLayout(), visitSetter(mv, lvtAlloc, addrLvtIndex, 0L));

                mv.visitVarInsn(ALOAD, 0);
                mv.visitInsn(ARETURN);

                finish(mv);
            });
        });
    }

    static StructSetter.Callback visitSetter(MethodVisitor mv, LvtAlloc lvtAlloc, int addressLvt, long constantOffset) {
        return new StructSetter.Callback() {
            @Override
            public StructSetter.Callback visitStructField(StructAttributeType structType, StructLayout structLayout, int fieldIndex) {
                return visitSetter(mv, lvtAlloc, addressLvt, constantOffset + structLayout.fieldOffset(fieldIndex));
            }

            @Override
            public StructSetter.Callback visitArrayElementConstant(ArrayAttributeType arrayType, ArrayLayout arrayLayout, int elementIndex) {
                return visitSetter(mv, lvtAlloc, addressLvt, constantOffset + arrayLayout.elementOffset(elementIndex));
            }

            @Override
            public StructSetter.Callback visitMatrixColumnConstant(MatrixAttributeType matrixType, MatrixLayout matrixLayout, int colIndex) {
                return visitSetter(mv, lvtAlloc, addressLvt, constantOffset + matrixLayout.colOffset(colIndex));
            }

            private int visitIndexed(int elementIndexLvt, long elementStride) {
                int nextAddressLvt = lvtAlloc.assign(LONG_TYPE);
                mv.visitVarInsn(LLOAD, addressLvt);
                mv.visitVarInsn(ILOAD, elementIndexLvt);
                mv.visitInsn(I2L);
                mv.visitLdcInsn(elementStride);
                mv.visitInsn(LMUL);
                if (constantOffset != 0L) {
                    mv.visitLdcInsn(constantOffset);
                    mv.visitInsn(LADD);
                }
                mv.visitInsn(LADD);
                mv.visitVarInsn(LSTORE, nextAddressLvt);
                return nextAddressLvt;
            }

            @Override
            public StructSetter.Callback visitArrayElementIndexed(ArrayAttributeType arrayType, ArrayLayout arrayLayout, int elementIndexLvt) {
                return visitSetter(mv, lvtAlloc, this.visitIndexed(elementIndexLvt, arrayLayout.elementStride()), 0L);
            }

            @Override
            public StructSetter.Callback visitMatrixColumnIndexed(MatrixAttributeType matrixType, MatrixLayout matrixLayout, int colIndexLvt) {
                return visitSetter(mv, lvtAlloc, this.visitIndexed(colIndexLvt, matrixLayout.colStride()), 0L);
            }

            @Override
            public void visitVector(VectorAttributeType vectorType, VectorLayout vectorLayout, MethodParameter parameter, IntConsumer componentLoader) {
                for (int componentIndex = 0; componentIndex < vectorType.components(); componentIndex++) {
                    long componentOffset = constantOffset + vectorLayout.componentOffset(componentIndex);

                    mv.visitInsn(ACONST_NULL);
                    mv.visitVarInsn(LLOAD, addressLvt);
                    if (componentOffset != 0L) {
                        mv.visitLdcInsn(componentOffset);
                        mv.visitInsn(LADD);
                    }

                    componentLoader.accept(componentIndex);
                    convertComponent(mv, parameter.componentType(), vectorLayout.componentType(), vectorLayout.physicalStorageType());
                    vectorLayout.physicalStorageType().unsafePut(mv);
                }
            }
        };
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
            generatePassthroughCtor(cv, getInternalName(AbstractInterleavedUniformBuffer.class), getType(NewAttributeFormat.class));

            generateMethod(cv, ACC_PUBLIC | ACC_FINAL, "update", getMethodDescriptor(getType(AttributeStruct.class), LONG_TYPE), mv -> {
                mv.visitTypeInsn(NEW, this.handleUniformClassInternalName);
                mv.visitInsn(DUP);
                mv.visitVarInsn(LLOAD, 1);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, this.handleUniformClassInternalName, "<init>", getMethodDescriptor(VOID_TYPE, LONG_TYPE, getType(AbstractInterleavedUniformBuffer.class)), false);
                return ARETURN;
            });
        });
    }
}
