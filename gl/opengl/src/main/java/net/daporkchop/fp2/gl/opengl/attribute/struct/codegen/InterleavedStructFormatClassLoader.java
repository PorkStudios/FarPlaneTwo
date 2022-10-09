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

package net.daporkchop.fp2.gl.opengl.attribute.struct.codegen;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.AttributeArray;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.fp2.gl.opengl.attribute.BaseAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeArrayImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeWriterImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.AttributeType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.ComponentType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.LayoutComponentStorage;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.StructMethod;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.StructMethodFactory;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureWriter;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public class InterleavedStructFormatClassLoader<S> extends StructFormatClassLoader<S, InterleavedStructLayout, InterleavedStructFormat<S>> {
    protected InterleavedStructFormatClassLoader(@NonNull OpenGL gl, @NonNull InterleavedStructLayout layout) {
        super(gl, layout);
    }

    @Override
    protected String generatedClassNamePrefix() {
        return "Interleaved";
    }

    @Override
    protected Class<?> baseFormatClass() {
        return InterleavedStructFormat.class;
    }

    @Override
    protected byte[] generateFormatClass() {
        String superclassName = getInternalName(this.baseFormatClass());
        String className = this.formatClassName();
        String structName = getInternalName(this.layout.structInfo().clazz());

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        {
            SignatureWriter signature = new SignatureWriter();
            signature.visitSuperclass();
            signature.visitClassType(superclassName);
            signature.visitTypeArgument('=');
            signature.visitClassType(structName);
            signature.visitEnd();
            signature.visitEnd();

            writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, signature.toString(), superclassName, null);
        }

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(OpenGL.class), getType(InterleavedStructLayout.class)), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(OpenGL.class), getType(InterleavedStructLayout.class)), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //void copy(Object srcBase, long srcOffset, Object dstBase, long dstOffset)
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copy", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, getType(Object.class), LONG_TYPE), null, null);

            this.generateMemcpy(mv, 1, 2, 4, 5, this.layout.stride());
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //void configureVAO(GLAPI api, int[] attributeIndices)
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "configureVAO", getMethodDescriptor(VOID_TYPE, getType(GLAPI.class), getType(int[].class)), null, null);

            //int i = 0;
            mv.visitLdcInsn(0);
            mv.visitVarInsn(ISTORE, 3);

            this.configureVao(mv, this.layout, this.layout.structProperty(), this.layout.member(), 1, 2, 3);

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //AttributeWriter<S> writer(AttributeFormatImpl attributeFormat)
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "writer", getMethodDescriptor(getType(this.baseWriterClass()), getType(AttributeFormatImpl.class)), null, null);

            //return new ${ this.writerClassName() }(this, (InterleavedAttributeFormatImpl) attributeFormat);
            mv.visitTypeInsn(NEW, this.writerClassName());
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, getInternalName(InterleavedAttributeFormatImpl.class));
            mv.visitMethodInsn(INVOKESPECIAL, this.writerClassName(), "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class)), false);

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //AttributeBuffer<S> buffer(AttributeFormatImpl attributeFormat, BufferUsage usage)
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "buffer", getMethodDescriptor(getType(this.baseBufferClass()), getType(AttributeFormatImpl.class), getType(BufferUsage.class)), null, null);

            //return new ${ this.bufferClassName() }(this, (InterleavedAttributeFormatImpl) attributeFormat, usage);
            mv.visitTypeInsn(NEW, this.bufferClassName());
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, getInternalName(InterleavedAttributeFormatImpl.class));
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, this.bufferClassName(), "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class), getType(BufferUsage.class)), false);

            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return this.finish(writer, className);
    }

    private void configureVao(@NonNull MethodVisitor mv, @NonNull InterleavedStructLayout layout, @NonNull AttributeType property, @NonNull InterleavedStructLayout.Member member, int apiLvtIndex, int locationsLvtIndex, int iLvtIndex) {
        property.with(new AttributeType.Callback() {
            @Override
            public void withComponents(@NonNull AttributeType.Components componentsType) {
                checkArg(componentsType.components()
                         == member.components(), "stage %s has %d components, but member %s has only %d!", componentsType, componentsType.components(), member, member.components());

                int cols = componentsType.cols();
                int rows = componentsType.rows();

                for (int col = 0; col < cols; col++) {
                    //ensure storage is identical for all components
                    LayoutComponentStorage storage = member.component(col * rows).storage();
                    for (int row = 0; row < rows; row++) {
                        LayoutComponentStorage componentStorage = member.component(col * rows + row).storage();
                        checkArg(storage.equals(componentStorage), "invalid storage for property: components in column must all use the same storage");
                    }

                    //ensure component offsets are sequential in memory
                    long offset = member.component(col * rows).offset();
                    for (int row = 0; row < rows; row++) {
                        long componentOffset = member.component(col * rows + row).offset();
                        long expectedOffset = offset + (long) storage.physicalStorageType().size() * row;
                        checkArg(componentOffset == expectedOffset, "invalid offset for property: components in column need to be sequential in memory!");
                    }

                    mv.visitVarInsn(ALOAD, apiLvtIndex); //api.<method>(
                    mv.visitVarInsn(ALOAD, locationsLvtIndex); //GLuint index = attributeIndices[i] + column,
                    mv.visitVarInsn(ILOAD, iLvtIndex);
                    mv.visitInsn(IALOAD);
                    mv.visitLdcInsn(col);
                    mv.visitInsn(IADD);

                    mv.visitLdcInsn(rows); //GLint size,
                    mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), "GL_" + storage.physicalStorageType(), "I"); //GLenum type,

                    if (!storage.glslInterpretedType().integer()) { //GLboolean normalized,
                        mv.visitLdcInsn(storage.interpretation().normalized());
                    }

                    mv.visitLdcInsn(toInt(layout.stride(), "stride")); //GLsizei stride,
                    mv.visitLdcInsn(member.component(col * rows).offset()); //const void* pointer);

                    if (storage.glslInterpretedType().integer()) { //<method>
                        mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glVertexAttribIPointer", "(IIIIJ)V", true);
                    } else {
                        mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glVertexAttribPointer", "(IIIZIJ)V", true);
                    }
                }

                mv.visitIincInsn(iLvtIndex, 1); //i++
            }

            @Override
            public void withElements(@NonNull AttributeType.Elements elementsType) {
                checkArg(elementsType.elements()
                         == member.children(), "stage %s has %d elements, but member %s has only %d!", elementsType, elementsType.elements(), member, member.children());

                for (int elementIndex = 0; elementIndex < elementsType.elements(); elementIndex++) {
                    InterleavedStructFormatClassLoader.this.configureVao(mv, layout, elementsType.componentType(), member.child(elementIndex), apiLvtIndex, locationsLvtIndex, iLvtIndex);
                }
            }

            @Override
            public void withFields(@NonNull AttributeType.Fields fieldsType) {
                checkArg(fieldsType.fields()
                         == member.children(), "stage %s has %d fields, but member %s has only %d!", fieldsType, fieldsType.fields(), member, member.children());

                for (int fieldIndex = 0; fieldIndex < fieldsType.fields(); fieldIndex++) {
                    InterleavedStructFormatClassLoader.this.configureVao(mv, layout, fieldsType.fieldProperty(fieldIndex), member.child(fieldIndex), apiLvtIndex, locationsLvtIndex, iLvtIndex);
                }
            }
        });
    }

    @Override
    protected Class<?> baseBufferClass() {
        return InterleavedAttributeBufferImpl.class;
    }

    @Override
    protected byte[] generateBufferClass() {
        String superclassName = getInternalName(this.baseBufferClass());
        String className = this.bufferClassName();

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, superclassName, null);

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class), getType(BufferUsage.class)), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class), getType(BufferUsage.class)), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //implement all _withStride methods
        this.generateOverridesFor_withStride(writer, this.baseBufferClass());

        { //S setToSingle()
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "setToSingle",
                    getMethodDescriptor(getType(Stream.of(this.baseWriterClass().getTypeParameters())
                            .filter(typeVariable -> "S".equals(typeVariable.getName()))
                            .findAny()
                            .map(typeVariable -> {
                                java.lang.reflect.Type[] bounds = typeVariable.getBounds();
                                return bounds.length == 0 ? Object.class : (Class<?>) bounds[0];
                            })
                            .get())),
                    null, null);

            int baseLvtIndexAllocator = 1;

            int addrLvtIndex = baseLvtIndexAllocator;
            baseLvtIndexAllocator += LONG_TYPE.getSize();

            mv.visitLdcInsn(this.layout.stride());
            mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "allocateMemory", getMethodDescriptor(LONG_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
            mv.visitVarInsn(LSTORE, addrLvtIndex);

            this.generateTryWithCleanupOnException(mv, baseLvtIndexAllocator,
                    innerLvtIndexAllocator -> {
                        mv.visitTypeInsn(NEW, this.handleSetToSingleInternalName());
                        mv.visitInsn(DUP);

                        mv.visitVarInsn(LLOAD, addrLvtIndex);
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESPECIAL, this.handleSetToSingleInternalName(), "<init>",
                                getMethodDescriptor(VOID_TYPE, LONG_TYPE, getType('L' + className.replace('.', '/') + ';')),
                                false);

                        mv.visitInsn(ARETURN);
                    },
                    innerLvtIndexAllocator -> {
                        mv.visitVarInsn(LLOAD, addrLvtIndex);
                        mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "freeMemory", getMethodDescriptor(VOID_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                    });

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //AttributeArray<S> setToMany(int length)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "setToMany", getMethodDescriptor(getType(AttributeArray.class), INT_TYPE), null, null);

            int baseLvtIndexAllocator = 2;

            int addrLvtIndex = baseLvtIndexAllocator;
            baseLvtIndexAllocator += LONG_TYPE.getSize();

            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(I2L);
            mv.visitLdcInsn(this.layout.stride());
            mv.visitInsn(LMUL);
            mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "allocateMemory", getMethodDescriptor(LONG_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
            mv.visitVarInsn(LSTORE, addrLvtIndex);

            this.generateTryWithCleanupOnException(mv, baseLvtIndexAllocator,
                    innerLvtIndexAllocator -> {
                        mv.visitTypeInsn(NEW, this.arraySetToManyInternalName());
                        mv.visitInsn(DUP);

                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKEVIRTUAL, className, "format", getMethodDescriptor(getType(BaseAttributeFormatImpl.class)), false);
                        mv.visitTypeInsn(CHECKCAST, getInternalName(InterleavedAttributeFormatImpl.class));
                        mv.visitVarInsn(LLOAD, addrLvtIndex);
                        mv.visitVarInsn(ILOAD, 1);
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESPECIAL, this.arraySetToManyInternalName(), "<init>",
                                getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class), LONG_TYPE, INT_TYPE, getType('L' + className.replace('.', '/') + ';')),
                                false);

                        mv.visitInsn(ARETURN);
                    },
                    innerLvtIndexAllocator -> {
                        mv.visitVarInsn(LLOAD, addrLvtIndex);
                        mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "freeMemory", getMethodDescriptor(VOID_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                    });

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return this.finish(writer, className);
    }

    @Override
    protected Class<?> baseWriterClass() {
        return InterleavedAttributeWriterImpl.class;
    }

    @Override
    protected byte[] generateWriterClass() {
        String superclassName = getInternalName(this.baseWriterClass());
        String className = this.writerClassName();

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, superclassName, null);

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class)), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class)), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //copyBetweenAddresses(long src, long dst)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copyBetweenAddresses", getMethodDescriptor(VOID_TYPE, LONG_TYPE, LONG_TYPE), null, null);

            this.generateMemcpy(mv, 1, 3, this.layout.stride());
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //implement all _withStride methods
        this.generateOverridesFor_withStride(writer, this.baseWriterClass());

        return this.finish(writer, className);
    }

    @Override
    protected Class<?> baseHandleClass() {
        return Object.class;
    }

    @Override
    protected byte[] generateHandleClass() {
        String superclassName = getInternalName(this.baseHandleClass());
        String className = this.handleClassName();
        String structName = getInternalName(this.layout.structInfo().clazz());

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC, className, null, superclassName,
                new String[]{ structName });

        writer.visitField(ACC_PUBLIC | ACC_FINAL, "address", LONG_TYPE.getDescriptor(), null, null).visitEnd();

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, LONG_TYPE), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE), false);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(LLOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, "address", LONG_TYPE.getDescriptor());

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //close()
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "close", getMethodDescriptor(VOID_TYPE), null, null);

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //implement all interface methods
        for (Method method : this.layout.structInfo().clazz().getDeclaredMethods()) {
            StructMethod structMethod = StructMethodFactory.createFromMethod((AttributeType.Fields) this.layout.structProperty(), method).orElse(null);
            if (structMethod == null) { //no StructMethod could be made from the given method
                continue;
            }

            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC | ACC_FINAL, method.getName(), getMethodDescriptor(method), null, null);

            int lvtIndex = structMethod.lvtIndexStart();

            int addrLvtIndex = lvtIndex;
            lvtIndex += LONG_TYPE.getSize();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "address", LONG_TYPE.getDescriptor());
            mv.visitVarInsn(LSTORE, addrLvtIndex);

            ((StructMethod.Setter) structMethod).visit(mv, lvtIndex, this.layout.member(), this.visitStructMethod(mv, addrLvtIndex, 0L));

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return this.finish(writer, className);
    }

    protected StructMethod.Setter.Callback<InterleavedStructLayout.Member, InterleavedStructLayout.Component> visitStructMethod(@NonNull MethodVisitor mv, int addrLvtIndex, long offsetOffset) {
        return new StructMethod.Setter.Callback<InterleavedStructLayout.Member, InterleavedStructLayout.Component>() {
            @Override
            public void visitComponentFixed(int lvtIndexAllocator, @NonNull InterleavedStructLayout.Member parent, int localComponentIndex, @NonNull InterleavedStructLayout.Component component, @NonNull ComponentType inputComponentType, @NonNull StructMethod.Setter.LoaderCallback componentValueLoader) {
                mv.visitInsn(ACONST_NULL);
                mv.visitVarInsn(LLOAD, addrLvtIndex);
                mv.visitLdcInsn(component.offset() - offsetOffset);
                mv.visitInsn(LADD);

                componentValueLoader.load(lvtIndexAllocator); //load input component
                component.storage().input2physical(mv, lvtIndexAllocator, inputComponentType);
                component.storage().physicalStorageType().unsafePut(mv);
            }

            @Override
            public void visitComponentIndexed(int lvtIndexAllocator, @NonNull InterleavedStructLayout.Member parent, @NonNull BitSet possibleLocalComponentIndices, @NonNull StructMethod.Setter.LoaderCallback localComponentIndexLoader, @NonNull ComponentType inputComponentType, @NonNull StructMethod.Setter.LoaderCallback componentValueLoader) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void visitChildFixed(int lvtIndexAllocator, @NonNull InterleavedStructLayout.Member parent, int localChildIndex, @NonNull InterleavedStructLayout.Member child, @NonNull StructMethod.Setter.ChildCallback<InterleavedStructLayout.Member, InterleavedStructLayout.Component> childCallback) {
                childCallback.visitChild(lvtIndexAllocator, this);
            }

            @Override
            public void visitChildIndexed(int lvtIndexAllocator, @NonNull InterleavedStructLayout.Member parent, @NonNull BitSet possibleLocalChildIndices, @NonNull StructMethod.Setter.LoaderCallback localChildIndexLoader, @NonNull StructMethod.Setter.ChildCallback<InterleavedStructLayout.Member, InterleavedStructLayout.Component> childCallback) {
                int lowestPossibleChildIndex = possibleLocalChildIndices.nextSetBit(0);
                int highestPossibleChildIndex = possibleLocalChildIndices.length() - 1;

                long lowestOffset = InterleavedStructFormatClassLoader.this.computeOffset(parent.child(lowestPossibleChildIndex));
                long highestOffset = InterleavedStructFormatClassLoader.this.computeOffset(parent.child(highestPossibleChildIndex));
                checkState(lowestOffset < highestOffset, "child offsets are out-of-order");
                long stride = (highestOffset - lowestOffset) / (highestPossibleChildIndex - lowestPossibleChildIndex);
                for (int i = lowestPossibleChildIndex; i >= 0; i = possibleLocalChildIndices.nextSetBit(i + 1)) {
                    checkState(lowestOffset + stride * i
                               == InterleavedStructFormatClassLoader.this.computeOffset(parent.child(i)), "child offsets must have uniform stride");
                }

                mv.visitVarInsn(LLOAD, addrLvtIndex);
                mv.visitLdcInsn(lowestOffset);
                mv.visitInsn(LADD);
                localChildIndexLoader.load(lvtIndexAllocator);
                mv.visitInsn(I2L);
                mv.visitLdcInsn(stride);
                mv.visitInsn(LMUL);
                mv.visitInsn(LADD);

                int nextAddrLvtIndex = lvtIndexAllocator++;
                mv.visitVarInsn(LSTORE, nextAddrLvtIndex);

                childCallback.visitChild(lvtIndexAllocator, InterleavedStructFormatClassLoader.this.visitStructMethod(mv, nextAddrLvtIndex, lowestOffset));
            }
        };
    }

    protected long computeOffset(@NonNull InterleavedStructLayout.Member member) {
        long offset = Long.MAX_VALUE;
        for (int i = 0; i < member.children(); i++) {
            offset = Math.min(offset, this.computeOffset(member.child(i)));
        }

        for (int i = 0; i < member.components(); i++) {
            offset = Math.min(offset, member.component(i).offset());
        }

        checkState(offset != Long.MAX_VALUE, "member has no components!");
        return offset;
    }

    @Override
    protected byte[] generateHandleSetToSingleClass() {
        String superclassName = this.handleClassName();
        String className = this.handleSetToSingleInternalName();

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, superclassName, null);

        writer.visitField(ACC_PUBLIC | ACC_FINAL, "buffer", getType('L' + this.bufferClassName().replace('.', '/') + ';').getDescriptor(), null, null).visitEnd();

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>",
                    getMethodDescriptor(VOID_TYPE, LONG_TYPE, getType('L' + this.bufferClassName().replace('.', '/') + ';')),
                    null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(LLOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, LONG_TYPE), false);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 3);
            mv.visitFieldInsn(PUTFIELD, className, "buffer", getType('L' + this.bufferClassName().replace('.', '/') + ';').getDescriptor());

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //close()
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "close", getMethodDescriptor(VOID_TYPE), null, null);

            this.generateTryFinally(mv, 1,
                    lvtIndexAllocator -> {
                        //super.close();
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESPECIAL, superclassName, "close", getMethodDescriptor(VOID_TYPE), false);

                        //this.buffer.setToSingle(this.address);
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, className, "buffer", getType('L' + this.bufferClassName().replace('.', '/') + ';').getDescriptor());
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, superclassName, "address", LONG_TYPE.getDescriptor());
                        mv.visitMethodInsn(INVOKEVIRTUAL, this.bufferClassName(), "setToSingle", getMethodDescriptor(VOID_TYPE, LONG_TYPE), false);
                    },
                    lvtIndexAllocator -> {
                        //PUnsafe.freeMemory(this.address);
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, superclassName, "address", LONG_TYPE.getDescriptor());
                        mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "freeMemory", getMethodDescriptor(VOID_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                    });

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return this.finish(writer, className);
    }

    @Override
    protected Class<?> baseArrayClass() {
        return InterleavedAttributeArrayImpl.class;
    }

    @Override
    protected byte[] generateArrayClass() {
        String superclassName = getInternalName(this.baseArrayClass());
        String className = this.arrayClassName();

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC, className, null, superclassName, null);

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class), LONG_TYPE, INT_TYPE), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(LLOAD, 2);
            mv.visitVarInsn(ILOAD, 4);
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class), LONG_TYPE, INT_TYPE), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //copyBetweenAddresses(long src, long dst)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copyBetweenAddresses", getMethodDescriptor(VOID_TYPE, LONG_TYPE, LONG_TYPE), null, null);

            this.generateMemcpy(mv, 1, 3, this.layout.stride());
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //implement all _withStride methods
        this.generateOverridesFor_withStride(writer, this.baseArrayClass());

        return this.finish(writer, className);
    }

    @Override
    protected byte[] generateArraySetToManyClass() {
        String superclassName = this.arrayClassName();
        String className = this.arraySetToManyInternalName();

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC, className, null, superclassName, null);

        writer.visitField(ACC_PUBLIC | ACC_FINAL, "buffer", getType('L' + this.bufferClassName().replace('.', '/') + ';').getDescriptor(), null, null).visitEnd();

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE,
                    getType(InterleavedAttributeFormatImpl.class), LONG_TYPE, INT_TYPE, getType('L' + this.bufferClassName().replace('.', '/') + ';')),
                    null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(LLOAD, 2);
            mv.visitVarInsn(ILOAD, 4);
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class), LONG_TYPE, INT_TYPE), false);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 5);
            mv.visitFieldInsn(PUTFIELD, className, "buffer", getType('L' + this.bufferClassName().replace('.', '/') + ';').getDescriptor());

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //close()
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "close", getMethodDescriptor(VOID_TYPE), null, null);

            this.generateTryFinally(mv, 1,
                    lvtIndexAllocator -> {
                        //super.close();
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitMethodInsn(INVOKESPECIAL, superclassName, "close", getMethodDescriptor(VOID_TYPE), false);

                        //this.buffer.setToMany(this.baseAddr, this.length);
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, className, "buffer", getType('L' + this.bufferClassName().replace('.', '/') + ';').getDescriptor());
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, superclassName, "baseAddr", LONG_TYPE.getDescriptor());
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, superclassName, "length", INT_TYPE.getDescriptor());
                        mv.visitMethodInsn(INVOKEVIRTUAL, this.bufferClassName(), "setToMany", getMethodDescriptor(VOID_TYPE, LONG_TYPE, INT_TYPE), false);
                    },
                    lvtIndexAllocator -> {
                        //PUnsafe.freeMemory(this.address);
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, superclassName, "baseAddr", LONG_TYPE.getDescriptor());
                        mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "freeMemory", getMethodDescriptor(VOID_TYPE, LONG_TYPE), PUnsafe.class.isInterface());
                    });

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //implement all _withStride methods
        this.generateOverridesFor_withStride(writer, this.baseArrayClass());

        return this.finish(writer, className);
    }

    protected void generateOverridesFor_withStride(@NonNull ClassVisitor writer, @NonNull Class<?> superclass) {
        //generate non-stride variants of all _withStride methods
        for (Method method : superclass.getDeclaredMethods()) {
            if (method.getName().endsWith("_withStride")) {
                //delegate to super method with stride as a constant parameter

                Class<?> returnType = method.getReturnType();
                Class<?>[] parameters = method.getParameterTypes();
                assert parameters[parameters.length - 1] == long.class : method + " must accept long as its final parameter";
                parameters = Arrays.copyOf(parameters, parameters.length - 1);

                MethodVisitor mv = writer.visitMethod(ACC_PUBLIC,
                        method.getName().substring(0, method.getName().length() - "_withStride".length()),
                        getMethodDescriptor(getType(returnType), Stream.of(parameters).map(Type::getType).toArray(Type[]::new)),
                        null, null);

                mv.visitVarInsn(ALOAD, 0);
                { //original parameters
                    int lvtIndex = 1;
                    for (Class<?> parameter : parameters) {
                        Type type = getType(parameter);
                        mv.visitVarInsn(type.getOpcode(ILOAD), lvtIndex);
                        lvtIndex += type.getSize();
                    }
                }
                mv.visitLdcInsn(this.layout.stride()); //stride
                mv.visitMethodInsn(INVOKESPECIAL, getInternalName(superclass), method.getName(), getMethodDescriptor(method), false);
                mv.visitInsn(getType(returnType).getOpcode(IRETURN));

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            } else if (method.getName().endsWith("_withStride_returnHandleFromAddr")) {
                //delegate to super method with stride as a constant parameter, and wrap the returned address in a new handle instance

                assert method.getReturnType() == long.class : method + " must return long";
                Class<?>[] parameters = method.getParameterTypes();
                assert parameters[parameters.length - 1] == long.class : method + " must accept long as its final parameter";
                parameters = Arrays.copyOf(parameters, parameters.length - 1);

                MethodVisitor mv = writer.visitMethod(ACC_PUBLIC,
                        method.getName().substring(0, method.getName().length() - "_withStride_returnHandleFromAddr".length()),
                        getMethodDescriptor(
                                //get the raw type of the struct parameter type
                                getType(Stream.of(this.baseWriterClass().getTypeParameters())
                                        .filter(typeVariable -> "S".equals(typeVariable.getName()))
                                        .findAny()
                                        .map(typeVariable -> {
                                            java.lang.reflect.Type[] bounds = typeVariable.getBounds();
                                            return bounds.length == 0 ? Object.class : (Class<?>) bounds[0];
                                        })
                                        .get()),
                                Stream.of(parameters).map(Type::getType).toArray(Type[]::new)),
                        null, null);

                mv.visitTypeInsn(NEW, this.handleClassName());
                mv.visitInsn(DUP);

                mv.visitVarInsn(ALOAD, 0);
                { //original parameters
                    int lvtIndex = 1;
                    for (Class<?> parameter : parameters) {
                        Type type = getType(parameter);
                        mv.visitVarInsn(type.getOpcode(ILOAD), lvtIndex);
                        lvtIndex += type.getSize();
                    }
                }
                mv.visitLdcInsn(this.layout.stride()); //stride
                mv.visitMethodInsn(INVOKESPECIAL, getInternalName(superclass), method.getName(), getMethodDescriptor(method), false);

                mv.visitMethodInsn(INVOKESPECIAL, this.handleClassName(), "<init>", getMethodDescriptor(VOID_TYPE, LONG_TYPE), false);
                mv.visitInsn(ARETURN);

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }
    }
}
