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
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeWriterImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.LayoutComponentStorage;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.StructMethod;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.StructMethodFactory;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import java.lang.reflect.Method;

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
    protected byte[] generateFormatClass() {
        String superclassName = getInternalName(InterleavedStructFormat.class);
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
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "writer", getMethodDescriptor(getType(InterleavedAttributeWriterImpl.class), getType(AttributeFormatImpl.class)), null, null);

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
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "buffer", getMethodDescriptor(getType(InterleavedAttributeBufferImpl.class), getType(AttributeFormatImpl.class), getType(BufferUsage.class)), null, null);

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

        return this.finish(writer, structName + ' ' + className);
    }

    private void configureVao(@NonNull MethodVisitor mv, @NonNull InterleavedStructLayout layout, @NonNull StructProperty property, @NonNull InterleavedStructLayout.Member member, int apiLvtIndex, int locationsLvtIndex, int iLvtIndex) {
        property.with(new StructProperty.PropertyCallback() {
            @Override
            public void withComponents(@NonNull StructProperty.Components componentsProperty) {
                checkArg(componentsProperty.components()
                         == member.components(), "stage %s has %d components, but member %s has only %d!", componentsProperty, componentsProperty.components(), member, member.components());

                int cols = componentsProperty.cols();
                int rows = componentsProperty.rows();

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
            public void withElements(@NonNull StructProperty.Elements elementsProperty) {
                checkArg(elementsProperty.elements()
                         == member.children(), "stage %s has %d elements, but member %s has only %d!", elementsProperty, elementsProperty.elements(), member, member.children());

                for (int elementIndex = 0; elementIndex < elementsProperty.elements(); elementIndex++) {
                    InterleavedStructFormatClassLoader.this.configureVao(mv, layout, elementsProperty.element(elementIndex), member.child(elementIndex), apiLvtIndex, locationsLvtIndex, iLvtIndex);
                }
            }

            @Override
            public void withFields(@NonNull StructProperty.Fields fieldsProperty) {
                checkArg(fieldsProperty.fields()
                         == member.children(), "stage %s has %d fields, but member %s has only %d!", fieldsProperty, fieldsProperty.fields(), member, member.children());

                for (int fieldIndex = 0; fieldIndex < fieldsProperty.fields(); fieldIndex++) {
                    InterleavedStructFormatClassLoader.this.configureVao(mv, layout, fieldsProperty.fieldProperty(fieldIndex), member.child(fieldIndex), apiLvtIndex, locationsLvtIndex, iLvtIndex);
                }
            }
        });
    }

    @Override
    protected byte[] generateBufferClass() {
        String superclassName = getInternalName(InterleavedAttributeBufferImpl.class);
        String className = this.bufferClassName();
        String structName = getInternalName(this.layout.structInfo().clazz());

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

        return this.finish(writer, structName + ' ' + className);
    }

    @Override
    protected byte[] generateWriterClass() {
        String superclassName = getInternalName(InterleavedAttributeWriterImpl.class);
        String className = this.writerClassName();
        String structName = getInternalName(this.layout.structInfo().clazz());

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, superclassName,
                new String[]{ structName }); //implement the top-level struct type to avoid unnecessary allocations

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class)), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(InterleavedAttributeFormatImpl.class)), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //implement all interface methods
        for (Method method : this.layout.structInfo().clazz().getMethods()) {
            StructMethod structMethod = StructMethodFactory.createFromMethod((StructProperty.Fields) this.layout.structProperty(), method);

            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, method.getName(), getMethodDescriptor(method), null, null);

            int lvtIndex = structMethod.lvtIndexStart();

            int addrLvtIndex = lvtIndex;
            lvtIndex += LONG_TYPE.getSize();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, superclassName, "baseAddr", LONG_TYPE.getDescriptor());
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, superclassName, "index", INT_TYPE.getDescriptor());
            mv.visitLdcInsn(1);
            mv.visitInsn(ISUB);
            mv.visitInsn(I2L);
            mv.visitLdcInsn(this.layout.stride());
            mv.visitInsn(LMUL);
            mv.visitInsn(LADD);
            mv.visitVarInsn(LSTORE, addrLvtIndex);

            ((StructMethod.Setter) structMethod).forEachComponent(mv, lvtIndex, this.layout.member(),
                    (lvtIndexAllocator, component, inputComponentType, loadComponentFromArgs) -> {
                        mv.visitInsn(ACONST_NULL);
                        mv.visitVarInsn(LLOAD, addrLvtIndex);
                        mv.visitLdcInsn(component.offset());
                        mv.visitInsn(LADD);

                        loadComponentFromArgs.accept(lvtIndexAllocator); //load input component
                        component.storage().input2physical(mv, lvtIndexAllocator, inputComponentType);
                        component.storage().physicalStorageType().unsafePut(mv);
                    });

            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return this.finish(writer, structName + ' ' + className);
    }
}
