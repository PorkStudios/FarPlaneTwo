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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.OpenGLConstants;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.StructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.TextureStructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.TextureStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentInterpretation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import net.daporkchop.fp2.gl.opengl.attribute.texture.TextureFormat2DImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.codegen.TextureFormat2DClassLoader;
import net.daporkchop.fp2.gl.opengl.attribute.texture.image.PixelFormatImpl;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.concurrent.ExecutionException;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class StructFormatGenerator {
    private static final boolean WRITE_CLASSES = true;

    private static void tryFinally(@NonNull MethodVisitor mv, @NonNull Runnable tryGenerator, @NonNull Runnable finallyGenerator) {
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Label tail = new Label();

        mv.visitTryCatchBlock(start, end, handler, null);

        //try
        mv.visitLabel(start);
        tryGenerator.run();
        mv.visitLabel(end);

        //finally
        finallyGenerator.run();

        //jump to tail
        mv.visitJumpInsn(GOTO, tail);

        //exception handler: finally, then re-throw exception
        mv.visitLabel(handler);
        finallyGenerator.run();
        mv.visitInsn(ATHROW);

        mv.visitLabel(tail);
    }

    protected final Cache<StructLayout<?, ?>, StructFormat<?, ?>> cacheStructInterleaved = CacheBuilder.newBuilder()
            .weakValues()
            .build();

    protected final Cache<PixelFormatImpl, TextureFormat2DImpl> cacheTexture2D = CacheBuilder.newBuilder()
            .weakValues()
            .build();

    @NonNull
    protected final OpenGL gl;

    @SneakyThrows(ExecutionException.class)
    public <S> InterleavedStructFormat<S> getInterleaved(@NonNull InterleavedStructLayout layout) {
        return uncheckedCast(this.cacheStructInterleaved.get(layout, () -> new InterleavedStructFormatClassLoader<S>(this.gl, layout).createFormat()));
    }

    @SneakyThrows(ExecutionException.class)
    public TextureFormat2DImpl getTexture2D(@NonNull PixelFormatImpl pixelFormat) {
        return this.cacheTexture2D.get(pixelFormat, () -> new TextureFormat2DClassLoader(this.gl, pixelFormat).createFormat());
    }

    @Deprecated
    private <S> TextureStructFormat<S> generateTexture(@NonNull TextureStructLayout layout) throws Exception {
        TextureStructLayout.Member member = layout.member();

        StructProperty.Components property = layout.structProperty().with(new StructProperty.TypedPropertyCallback<StructProperty.Components>() {
            @Override
            public StructProperty.Components withComponents(@NonNull StructProperty.Components componentsProperty) {
                throw new UnsupportedOperationException();
            }

            @Override
            public StructProperty.Components withElements(@NonNull StructProperty.Elements elementsProperty) {
                throw new UnsupportedOperationException();
            }

            @Override
            public StructProperty.Components withFields(@NonNull StructProperty.Fields fieldsProperty) {
                return fieldsProperty.fieldProperty(0).with(new StructProperty.TypedPropertyCallback<StructProperty.Components>() {
                    @Override
                    public StructProperty.Components withComponents(@NonNull StructProperty.Components componentsProperty) {
                        return componentsProperty;
                    }

                    @Override
                    public StructProperty.Components withElements(@NonNull StructProperty.Elements elementsProperty) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public StructProperty.Components withFields(@NonNull StructProperty.Fields fieldsProperty) {
                        throw new UnsupportedOperationException();
                    }
                });
            }
        });
        ComponentInterpretation interpretation = property.componentInterpretation();

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
                String components = "RGBA".substring(0, property.components());
                int bitDepth = property.logicalStorageType().size() * Byte.SIZE;
                String suffix;
                if (property.logicalStorageType().integer()) {
                    suffix = !interpretation.outputType().integer() && interpretation.normalized()
                            ? interpretation.outputType().signed() ? "_SNORM" : ""
                            : interpretation.outputType().signed() ? "I" : "UI";
                } else {
                    suffix = "F";
                }
                mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), "GL_" + components + bitDepth + suffix, "I");
            }

            { //int textureFormat
                String components = property.components() == 1
                        ? "GL_RED" //for some reason, if it has a single component it's not GL_R but GL_RED instead
                        : "RGBA".substring(0, property.components());
                String suffix = interpretation.outputType().integer() ? "_INTEGER" : "";
                mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), "GL_" + components + suffix, "I");
            }

            { //int textureType
                mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), "GL_" + property.logicalStorageType(), "I");
            }

            mv.visitMethodInsn(INVOKESPECIAL, baseClassName, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType(getInternalName(TextureStructLayout.class)), Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //void copy(Object struct, Object dstBase, long dstOffset)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copy", "(Ljava/lang/Object;Ljava/lang/Object;J)V", null, null);

            //make sure struct can be cast to requested type
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, structName);
            mv.visitVarInsn(ASTORE, 1);

            //copy each component
            /*property.load(mv, 1, 5, (structLvtIndex, lvtIndexAllocator, loader) -> {
                for (int componentIndex = 0; componentIndex < property.components(); componentIndex++) {
                    TextureStructLayout.Component component = member.component(componentIndex);

                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitVarInsn(LLOAD, 3);
                    mv.visitLdcInsn(component.offset());
                    mv.visitInsn(LADD);

                    loader.load(structLvtIndex, lvtIndexAllocator, componentIndex);
                    property.logicalStorageType().unsafePut(mv);
                }
            });*/ //TODO: this

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //void copy(Object srcBase, long srcOffset, Object dstBase, long dstOffset)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copy", "(Ljava/lang/Object;JLjava/lang/Object;J)V", null, null);

            this.generateMemcpy(mv, 1, 2, 4, 5, layout.stride());
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //void copyFromARGB(int argb, long srcOffset, Object dstBase, long dstOffset)
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "copyFromARGB", "(ILjava/lang/Object;J)V", null, null);

            //copy each member type
            for (int componentIndex = 0; componentIndex < property.components(); componentIndex++) {
                mv.visitVarInsn(ALOAD, 2);
                mv.visitVarInsn(LLOAD, 3);
                mv.visitLdcInsn(componentIndex * (long) property.logicalStorageType().size());
                mv.visitInsn(LADD);

                mv.visitVarInsn(ILOAD, 1);
                mv.visitLdcInsn((((2 - componentIndex) & 3) << 3));
                mv.visitInsn(ISHR);

                if (!property.logicalStorageType().integer() && interpretation.normalized() && interpretation.outputType().signed()) {
                    //the property is stored in memory as a float, which will be normalized while preserving the sign. convert to a byte
                    mv.visitInsn(I2B);
                } else { //truncate to 8 bits
                    mv.visitLdcInsn(0xFF);
                    mv.visitInsn(IAND);
                }

                if (!property.logicalStorageType().integer()) {
                    mv.visitInsn(I2F);
                    if (interpretation.normalized()) {
                        mv.visitLdcInsn(interpretation.outputType().signed() ? (1.0f / 128.0f) : (1.0f / 256.0f));
                        mv.visitInsn(FMUL);
                    }
                }

                property.logicalStorageType().unsafePut(mv);
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

    @SuppressWarnings("SameParameterValue")
    private void generateMemcpy(@NonNull MethodVisitor mv, int srcBaseLvtIndex, int srcOffsetLvtIndex, int dstBaseLvtIndex, int dstOffsetLvtIndex, long size) {
        //generate a sequence of instructions emulating a simply memcpy by copying one long at a time, and padding it with ints/shorts/bytes if not an exact multiple
        for (long pos = 0L; pos < size; ) {
            //dst
            mv.visitVarInsn(ALOAD, dstBaseLvtIndex);
            mv.visitVarInsn(LLOAD, dstOffsetLvtIndex);
            mv.visitLdcInsn(pos);
            mv.visitInsn(LADD);

            //src
            mv.visitVarInsn(ALOAD, srcBaseLvtIndex);
            mv.visitVarInsn(LLOAD, srcOffsetLvtIndex);
            mv.visitLdcInsn(pos);
            mv.visitInsn(LADD);

            //find the biggest integer type <= the remaining size and copy exactly one of it
            if (size - pos >= LONG_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getLong", getMethodDescriptor(LONG_TYPE, getType(Object.class), LONG_TYPE), false);
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putLong", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, LONG_TYPE), false);
                pos += LONG_SIZE;
            } else if (size - pos >= INT_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getInt", getMethodDescriptor(INT_TYPE, getType(Object.class), LONG_TYPE), false);
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putInt", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, INT_TYPE), false);
                pos += INT_SIZE;
            } else if (size - pos >= SHORT_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getShort", getMethodDescriptor(SHORT_TYPE, getType(Object.class), LONG_TYPE), false);
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putShort", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, SHORT_TYPE), false);
                pos += SHORT_SIZE;
            } else if (size - pos >= BYTE_SIZE) {
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getByte", getMethodDescriptor(BYTE_TYPE, getType(Object.class), LONG_TYPE), false);
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "putByte", getMethodDescriptor(VOID_TYPE, getType(Object.class), LONG_TYPE, BYTE_TYPE), false);
                pos += BYTE_SIZE;
            } else {
                throw new IllegalArgumentException(String.valueOf(size - pos));
            }
        }
    }
}
