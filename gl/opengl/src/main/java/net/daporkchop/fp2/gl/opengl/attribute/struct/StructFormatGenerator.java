/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.StructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.TextureStructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.TextureStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLMatrixType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
public class StructFormatGenerator {
    protected final Cache<StructLayout<?>, StructFormat<?, ?>> cache = CacheBuilder.newBuilder()
            .weakValues()
            .build();

    @SneakyThrows(ExecutionException.class)
    public <S> InterleavedStructFormat<S> getInterleaved(@NonNull InterleavedStructLayout<S> layout) {
        return uncheckedCast(this.cache.get(layout, () -> this.generateInterleaved(layout)));
    }

    private <S> InterleavedStructFormat<S> generateInterleaved(@NonNull InterleavedStructLayout<S> layout) throws Exception {
        String baseClassName = Type.getInternalName(InterleavedStructFormat.class);
        String className = baseClassName + '$' + layout.layoutName() + '$' + Type.getInternalName(layout.structInfo().clazz()).replace("/", "__");
        String structName = Type.getInternalName(layout.structInfo().clazz());

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
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(Type.getInternalName(InterleavedStructLayout.class))), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, baseClassName, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(Type.getInternalName(InterleavedStructLayout.class))), false);
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
            List<StructMember<S>> members = layout.structInfo().members();
            for (int i = 0; i < members.size(); i++) {
                StructMember<S> member = members.get(i);
                StructMember.Stage stage = layout.unpacked() ? member.unpackedStage : member.packedStage;
                member.storeStageOutput(mv, stage, 1, 2, 3, layout.memberOffsets()[i]);
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //void copy(Object srcBase, long srcOffset, Object dstBase, long dstOffset)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copy", "(Ljava/lang/Object;JLjava/lang/Object;J)V", null, null);

            //copy each member type
            List<StructMember<S>> members = layout.structInfo().members();
            for (int i = 0; i < members.size(); i++) {
                StructMember<S> member = members.get(i);
                StructMember.Stage stage = layout.unpacked() ? member.unpackedStage : member.packedStage;
                member.copyStageOutput(mv, stage, 1, 2, 4, 5, layout.memberOffsets()[i]);
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //void configureVAO(GLAPI api, int[] attributeIndices)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "configureVAO", '(' + Type.getDescriptor(GLAPI.class) + "[I)V", null, null);

            //configure attribute for each member
            List<StructMember<S>> members = layout.structInfo().members();
            for (int i = 0; i < members.size(); i++) {
                StructMember<S> member = members.get(i);
                StructMember.Stage srcStage = layout.unpacked() ? member.unpackedStage : member.packedStage;
                StructMember.Stage unpackedStage = member.unpackedStage;

                int columns = 1;
                int rows = unpackedStage.components();

                if (unpackedStage.glslType() instanceof GLSLMatrixType) {
                    GLSLMatrixType mat = (GLSLMatrixType) unpackedStage.glslType();
                    columns = mat.columns();
                    rows = mat.rows();
                }

                for (int column = 0; column < columns; column++) {
                    mv.visitVarInsn(ALOAD, 1); //api.<method>(
                    mv.visitVarInsn(ALOAD, 2); //GLuint index = attributeIndices[i] + column,
                    mv.visitLdcInsn(i);
                    mv.visitInsn(IALOAD);
                    mv.visitLdcInsn(column);
                    mv.visitInsn(IADD);

                    mv.visitLdcInsn(rows); //GLint size,
                    mv.visitFieldInsn(GETSTATIC, Type.getInternalName(OpenGLConstants.class), "GL_" + srcStage.componentType(), "I"); //GLenum type,

                    if (unpackedStage.componentType().floatingPoint()) { //GLboolean normalized,
                        mv.visitLdcInsn(unpackedStage.isNormalizedFloat());
                    }

                    mv.visitLdcInsn(toInt(layout.stride(), "stride")); //GLsizei stride,
                    mv.visitLdcInsn(layout.memberOffsets()[i] + layout.memberComponentOffsets()[i][column * rows]); //const void* pointer);

                    if (unpackedStage.componentType().floatingPoint()) { //<method>
                        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(GLAPI.class), "glVertexAttribPointer", "(IIIZIJ)V", true);
                    } else {
                        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(GLAPI.class), "glVertexAttribIPointer", "(IIIIJ)V", true);
                    }
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        writer.visitEnd();

        if (false) {
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get(className.replace('/', '-') + ".class"), writer.toByteArray());
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }

        Class<InterleavedStructFormat<S>> clazz = uncheckedCast(ClassloadingUtils.defineHiddenClass(InterleavedStructFormat.class.getClassLoader(), writer.toByteArray()));
        return clazz.getConstructor(InterleavedStructLayout.class).newInstance(layout);
    }

    @SneakyThrows(ExecutionException.class)
    public <S> TextureStructFormat<S> getTexture(@NonNull TextureStructLayout<S> layout) {
        return uncheckedCast(this.cache.get(layout, () -> this.generateTexture(layout)));
    }

    private <S> TextureStructFormat<S> generateTexture(@NonNull TextureStructLayout<S> layout) throws Exception {
        StructMember<S> member = layout.structInfo().members().get(0);
        StructMember.Stage stage = layout.unpacked() ? member.unpackedStage : member.packedStage;
        StructMember.Stage unpackedStage = member.unpackedStage;

        String baseClassName = Type.getInternalName(TextureStructFormat.class);
        String className = baseClassName + '$' + layout.layoutName() + '$' + Type.getInternalName(layout.structInfo().clazz()).replace("/", "__");
        String structName = Type.getInternalName(layout.structInfo().clazz());

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
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(Type.getInternalName(TextureStructLayout.class))), null, null);

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
                mv.visitFieldInsn(GETSTATIC, Type.getInternalName(OpenGLConstants.class), "GL_" + components + bitDepth + suffix, "I");
            }

            { //int textureFormat
                String components = "RGBA".substring(0, unpackedStage.components());
                String suffix = unpackedStage.componentType().integer() ? "_INTEGER" : "";
                mv.visitFieldInsn(GETSTATIC, Type.getInternalName(OpenGLConstants.class), "GL_" + components + suffix, "I");
            }

            { //int textureType
                mv.visitFieldInsn(GETSTATIC, Type.getInternalName(OpenGLConstants.class), "GL_" + stage.componentType(), "I");
            }

            mv.visitMethodInsn(INVOKESPECIAL, baseClassName, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(Type.getInternalName(TextureStructLayout.class)), Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE), false);
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

        writer.visitEnd();

        if (false) {
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
        String structName = Type.getInternalName(structInfo.clazz());

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
