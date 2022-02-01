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

package net.daporkchop.fp2.gl.opengl.attribute.struct;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.StructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.TextureStructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.StructInfo;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.TextureStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentInterpretation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLBasicType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.concurrent.ExecutionException;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public class StructFormatGenerator {
    private static final boolean WRITE_CLASSES = true;

    protected final Cache<StructLayout<?, ?>, StructFormat<?, ?>> cache = CacheBuilder.newBuilder()
            .weakValues()
            .build();

    @SneakyThrows(ExecutionException.class)
    public <S> InterleavedStructFormat<S> getInterleaved(@NonNull InterleavedStructLayout layout) {
        return uncheckedCast(this.cache.get(layout, () -> this.generateInterleaved(layout)));
    }

    private <S> InterleavedStructFormat<S> generateInterleaved(@NonNull InterleavedStructLayout layout) throws Exception {
        String baseClassName = getInternalName(InterleavedStructFormat.class);
        String className = baseClassName + '$' + layout.layoutName() + '$' + getInternalName(layout.structInfo().clazz()).replace("/", "__");
        String structName = getInternalName(layout.structInfo().clazz());

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        {
            SignatureWriter signature = new SignatureWriter();
            signature.visitSuperclass();
            signature.visitClassType(baseClassName);
            signature.visitTypeArgument('=');
            signature.visitClassType(structName);
            signature.visitEnd();
            signature.visitEnd();

            writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, signature.toString(), baseClassName, null);
        }

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(getInternalName(InterleavedStructLayout.class))), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, baseClassName, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(getInternalName(InterleavedStructLayout.class))), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //this is never actually used...
        //this.generateClone(writer, layout.structInfo());

        //void copy(Object struct, Object dstBase, long dstOffset)
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copy", "(Ljava/lang/Object;Ljava/lang/Object;J)V", null, null);

            //make sure struct can be cast to requested type
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, structName);
            mv.visitVarInsn(ASTORE, 1);

            this.copyStruct2Buf(mv, layout.structProperty(), layout.member(), 1, 2, 3, 5);

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //void copy(Object srcBase, long srcOffset, Object dstBase, long dstOffset)
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copy", "(Ljava/lang/Object;JLjava/lang/Object;J)V", null, null);

            //generate a sequence of instructions emulating a simply memcpy by copying one long at a time, and padding it with ints/shorts/bytes if not an exact multiple
            for (long pos = 0L; pos < layout.stride(); ) {
                //dst
                mv.visitVarInsn(ALOAD, 4);
                mv.visitVarInsn(LLOAD, 5);
                mv.visitLdcInsn(pos);
                mv.visitInsn(LADD);

                //src
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(LLOAD, 2);
                mv.visitLdcInsn(pos);
                mv.visitInsn(LADD);

                //find the biggest integer type <= the remaining size and copy exactly one of it
                if (layout.stride() - pos >= LONG_SIZE) {
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getLong", getMethodDescriptor(LONG_TYPE, getType(Object.class), LONG_TYPE), false);
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putLong", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, LONG_TYPE), false);
                    pos += LONG_SIZE;
                } else if (layout.stride() - pos >= INT_SIZE) {
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getInt", getMethodDescriptor(INT_TYPE, getType(Object.class), LONG_TYPE), false);
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putInt", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, INT_TYPE), false);
                    pos += INT_SIZE;
                } else if (layout.stride() - pos >= SHORT_SIZE) {
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getShort", getMethodDescriptor(SHORT_TYPE, getType(Object.class), LONG_TYPE), false);
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putShort", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, SHORT_TYPE), false);
                    pos += SHORT_SIZE;
                } else if (layout.stride() - pos >= BYTE_SIZE) {
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getByte", getMethodDescriptor(BYTE_TYPE, getType(Object.class), LONG_TYPE), false);
                    mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putByte", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, BYTE_TYPE), false);
                    pos += BYTE_SIZE;
                } else {
                    throw new IllegalArgumentException(String.valueOf(layout.stride() - pos));
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //void configureVAO(GLAPI api, int[] attributeIndices)
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "configureVAO", '(' + Type.getDescriptor(GLAPI.class) + "[I)V", null, null);

            //int i = 0;
            mv.visitLdcInsn(0);
            mv.visitVarInsn(ISTORE, 3);

            this.configureVao(mv, layout, layout.structProperty(), layout.member(), 1, 2, 3);

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        writer.visitEnd();

        if (WRITE_CLASSES) {
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get(className.replace('/', '-') + ".class"), writer.toByteArray());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }

        Class<InterleavedStructFormat<S>> clazz = uncheckedCast(ClassloadingUtils.defineHiddenClass(InterleavedStructFormat.class.getClassLoader(), writer.toByteArray()));
        return clazz.getConstructor(InterleavedStructLayout.class).newInstance(layout);
    }

    private void copyStruct2Buf(@NonNull MethodVisitor mv, @NonNull StructProperty property, @NonNull InterleavedStructLayout.Member member, int structLvtIndexIn, int outputBaseLvtIndex, int outputOffsetLvtIndex, int lvtIndexAllocatorIn) {
        property.with(new StructProperty.PropertyCallback() {
            @Override
            public void withComponents(@NonNull StructProperty.Components componentsProperty) {
                checkArg(componentsProperty.components() == member.components(), "stage %s has %d components, but member %s has only %d!", componentsProperty, componentsProperty.components(), member, member.components());

                componentsProperty.load(mv, structLvtIndexIn, lvtIndexAllocatorIn, (structLvtIndex, lvtIndexAllocator, loader) -> {
                    for (int componentIndex = 0; componentIndex < componentsProperty.components(); componentIndex++) {
                        InterleavedStructLayout.Component component = member.component(componentIndex);

                        mv.visitVarInsn(ALOAD, outputBaseLvtIndex);
                        mv.visitVarInsn(LLOAD, outputOffsetLvtIndex);
                        mv.visitLdcInsn(component.offset());
                        mv.visitInsn(LADD);

                        loader.accept(componentIndex);
                        componentsProperty.componentType().unsafePut(mv);
                    }
                });
            }

            @Override
            public void withElements(@NonNull StructProperty.Elements elementsProperty) {
                checkArg(elementsProperty.elements() == member.children(), "stage %s has %d elements, but member %s has only %d!", elementsProperty, elementsProperty.elements(), member, member.children());

                elementsProperty.load(mv, structLvtIndexIn, lvtIndexAllocatorIn, (structLvtIndex, lvtIndexAllocator) -> {
                    for (int elementIndex = 0; elementIndex < elementsProperty.elements(); elementIndex++) {
                        StructFormatGenerator.this.copyStruct2Buf(mv, elementsProperty.element(elementIndex), member.child(elementIndex), structLvtIndex, outputBaseLvtIndex, outputOffsetLvtIndex, lvtIndexAllocator);
                    }
                });
            }
        });
    }

    private void configureVao(@NonNull MethodVisitor mv, @NonNull InterleavedStructLayout layout, @NonNull StructProperty property, @NonNull InterleavedStructLayout.Member member, int apiLvtIndex, int locationsLvtIndex, int iLvtIndex) {
        property.with(new StructProperty.PropertyCallback() {
            @Override
            public void withComponents(@NonNull StructProperty.Components componentsProperty) {
                checkArg(componentsProperty.components() == member.components(), "stage %s has %d components, but member %s has only %d!", componentsProperty, componentsProperty.components(), member, member.components());

                ComponentInterpretation interpretation = componentsProperty.interpretation();

                int columns = 1;
                int rows = componentsProperty.components();

                /*if (unpackedStage.glslType() instanceof GLSLMatrixType) {
                    GLSLMatrixType mat = (GLSLMatrixType) unpackedStage.glslType();
                    columns = mat.columns();
                    rows = mat.rows();
                }*/

                for (int column = 0; column < columns; column++) {
                    mv.visitVarInsn(ALOAD, apiLvtIndex); //api.<method>(
                    mv.visitVarInsn(ALOAD, locationsLvtIndex); //GLuint index = attributeIndices[i] + column,
                    mv.visitVarInsn(ILOAD, iLvtIndex);
                    mv.visitInsn(IALOAD);
                    mv.visitLdcInsn(column);
                    mv.visitInsn(IADD);

                    mv.visitLdcInsn(rows); //GLint size,
                    mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), "GL_" + interpretation.inputType(), "I"); //GLenum type,

                    if (!interpretation.integer()) { //GLboolean normalized,
                        mv.visitLdcInsn(interpretation.normalized());
                    }

                    mv.visitLdcInsn(toInt(layout.stride(), "stride")); //GLsizei stride,
                    mv.visitLdcInsn(member.component(column * rows).offset()); //const void* pointer);

                    if (interpretation.integer()) { //<method>
                        mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glVertexAttribIPointer", "(IIIIJ)V", true);
                    } else {
                        mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glVertexAttribPointer", "(IIIZIJ)V", true);
                    }
                }

                mv.visitIincInsn(iLvtIndex, 1); //i++
            }

            @Override
            public void withElements(@NonNull StructProperty.Elements elementsProperty) {
                checkArg(elementsProperty.elements() == member.children(), "stage %s has %d elements, but member %s has only %d!", elementsProperty, elementsProperty.elements(), member, member.children());

                for (int elementIndex = 0; elementIndex < elementsProperty.elements(); elementIndex++) {
                    StructFormatGenerator.this.configureVao(mv, layout, elementsProperty.element(elementIndex), member.child(elementIndex), apiLvtIndex, locationsLvtIndex, iLvtIndex);
                }
            }
        });
    }

    @SneakyThrows(ExecutionException.class)
    public <S> TextureStructFormat<S> getTexture(@NonNull TextureStructLayout layout) {
        return uncheckedCast(this.cache.get(layout, () -> this.generateTexture(layout)));
    }

    private <S> TextureStructFormat<S> generateTexture(@NonNull TextureStructLayout layout) throws Exception {
        StructMember<?> member = layout.structInfo().members().get(0);
        StructMember.Stage stage = layout.unpacked() ? member.unpackedStage : member.packedStage;
        StructMember.Stage unpackedStage = member.unpackedStage;

        String baseClassName = getInternalName(TextureStructFormat.class);
        String className = baseClassName + '$' + layout.layoutName() + '$' + getInternalName(layout.structInfo().clazz()).replace("/", "__");
        String structName = getInternalName(layout.structInfo().clazz());

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        {
            SignatureWriter signature = new SignatureWriter();
            signature.visitSuperclass();
            signature.visitClassType(baseClassName);
            signature.visitTypeArgument('=');
            signature.visitClassType(structName);
            signature.visitEnd();
            signature.visitEnd();

            writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, signature.toString(), baseClassName, null);
        }

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(getInternalName(TextureStructLayout.class))), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);

            { //int textureInternalFormat
                String components = "RGBA".substring(0, unpackedStage.components());
                int bitDepth = stage.componentType().stride() * Byte.SIZE;
                String suffix;
                if (stage.componentType().integer()) {
                    suffix = unpackedStage.isNormalizedFloat()
                            ? ((StructMember.ComponentType.Int) stage.componentType()).unsigned() ? "" : "_SNORM"
                            : ((StructMember.ComponentType.Int) stage.componentType()).unsigned() ? "UI" : "I";
                } else {
                    suffix = "F";
                }
                mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), "GL_" + components + bitDepth + suffix, "I");
            }

            { //int textureFormat
                String components = "RGBA".substring(0, unpackedStage.components());
                String suffix = unpackedStage.componentType().integer() ? "_INTEGER" : "";
                mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), "GL_" + components + suffix, "I");
            }

            { //int textureType
                mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), "GL_" + stage.componentType(), "I");
            }

            mv.visitMethodInsn(INVOKESPECIAL, baseClassName, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(getInternalName(TextureStructLayout.class)), Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        this.generateClone(writer, layout.structInfo());

        { //void copy(Object struct, Object dstBase, long dstOffset)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copy", "(Ljava/lang/Object;Ljava/lang/Object;J)V", null, null);

            //make sure struct can be cast to requested type
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, structName);
            mv.visitVarInsn(ASTORE, 1);

            //copy each member type
            member.storeStageOutput(mv, stage, 1, 2, 3, 0L);

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //void copy(Object srcBase, long srcOffset, Object dstBase, long dstOffset)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copy", "(Ljava/lang/Object;JLjava/lang/Object;J)V", null, null);

            //copy each member type
            member.copyStageOutput(mv, stage, 1, 2, 4, 5, 0L);

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //void copyFromARGB(int argb, long srcOffset, Object dstBase, long dstOffset)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copyFromARGB", "(ILjava/lang/Object;J)V", null, null);

            //copy each member type
            for (int componentIndex = 0; componentIndex < stage.components(); componentIndex++) {
                mv.visitVarInsn(ALOAD, 2);
                mv.visitVarInsn(LLOAD, 3);
                mv.visitLdcInsn(componentIndex * (long) stage.componentType().stride());
                mv.visitInsn(LADD);

                mv.visitVarInsn(ILOAD, 1);
                mv.visitLdcInsn((((2 - componentIndex) & 3) << 3));
                mv.visitInsn(ISHR);
                mv.visitLdcInsn(0xFF);
                mv.visitInsn(IAND);
                if (((GLSLBasicType) stage.glslType()).primitive() == GLSLPrimitiveType.FLOAT) {
                    mv.visitInsn(I2F);
                    if (stage.isNormalizedFloat()) {
                        mv.visitLdcInsn(1.0f / 256.0f);
                        mv.visitInsn(FMUL);
                    }
                }

                stage.componentType().unsafePut(mv);
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        writer.visitEnd();

        if (WRITE_CLASSES) {
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get(className.replace('/', '-') + ".class"), writer.toByteArray());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }

        Class<TextureStructFormat<S>> clazz = uncheckedCast(ClassloadingUtils.defineHiddenClass(TextureStructFormat.class.getClassLoader(), writer.toByteArray()));
        return clazz.getConstructor(TextureStructLayout.class).newInstance(layout);
    }

    private <S> void generateClone(@NonNull ClassVisitor cv, @NonNull StructInfo<S> structInfo) {
        String structName = getInternalName(structInfo.clazz());

        { //Object clone(Object struct)
            MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "clone", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);

            //make sure struct can be cast to requested type
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, structName);
            mv.visitVarInsn(ASTORE, 1);

            //allocate new instance
            mv.visitLdcInsn(Type.getObjectType(structName));
            mv.visitMethodInsn(INVOKESTATIC, "net/daporkchop/lib/unsafe/PUnsafe", "allocateInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, structName);
            mv.visitVarInsn(ASTORE, 2);

            //copy each field
            for (StructMember<S> member : structInfo.members()) {
                member.packedStage.cloneStruct(mv, 1, 2);
            }

            mv.visitVarInsn(ALOAD, 2);
            mv.visitInsn(ARETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }
}
