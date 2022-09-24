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

package net.daporkchop.fp2.gl.opengl.attribute.texture.codegen;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.common.AttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeBufferImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeFormatImpl;
import net.daporkchop.fp2.gl.opengl.attribute.common.interleaved.InterleavedAttributeWriterImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.InterleavedStructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.InterleavedStructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLSamplerType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLTypeFactory;
import net.daporkchop.fp2.gl.opengl.attribute.texture.TextureFormat2DImpl;
import net.daporkchop.fp2.gl.opengl.attribute.texture.image.PixelFormatImpl;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public class TextureFormat2DClassLoader extends TextureFormatClassLoader<TextureFormat2DImpl> {
    public TextureFormat2DClassLoader(@NonNull OpenGL gl, @NonNull PixelFormatImpl pixelFormat) {
        super(gl, pixelFormat, 2);
    }

    @Override
    protected String dimensionName(int dimension) {
        switch (dimension) {
            case 0:
                return "width";
            case 1:
                return "height";
            default:
                throw new IllegalArgumentException(String.valueOf(dimension));
        }
    }

    @Override
    protected byte[] generateFormatClass() {
        String superclassName = getInternalName(TextureFormat2DImpl.class);
        String className = this.formatClassName();

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, superclassName, null);

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(OpenGL.class), getType(String.class)), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 2);

            {
                String glslPrimitiveTypeName = GLSLPrimitiveType.INVALID.name();
                switch (this.pixelFormat.channelType()) {
                    case FLOATING_POINT:
                        glslPrimitiveTypeName = GLSLPrimitiveType.FLOAT.name();
                        break;
                    case INTEGER:
                        glslPrimitiveTypeName = GLSLPrimitiveType.INT.name();
                        break;
                    case UNSIGNED_INTEGER:
                        glslPrimitiveTypeName = GLSLPrimitiveType.UINT.name();
                        break;
                }

                mv.visitFieldInsn(GETSTATIC, getInternalName(GLSLPrimitiveType.class), glslPrimitiveTypeName, getDescriptor(GLSLPrimitiveType.class));
            }
            mv.visitLdcInsn(this.pixelFormat.channels().size());
            mv.visitMethodInsn(INVOKESTATIC, getInternalName(GLSLTypeFactory.class), "sampler", getMethodDescriptor(getType(GLSLSamplerType.class), getType(GLSLPrimitiveType.class), INT_TYPE), false);

            mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", getMethodDescriptor(VOID_TYPE, getType(OpenGL.class), getType(String.class), getType(GLSLSamplerType.class)), false);
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //long size()
        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "size", getMethodDescriptor(LONG_TYPE), null, null);

            mv.visitLdcInsn((long) this.pixelFormat.internalFormat().sizeBytes());
            mv.visitInsn(LRETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        return this.finish(writer, this.pixelFormat.toString() + ' ' + className);
    }

    @Override
    protected byte[] generateTextureClass() {
        return new byte[0];
    }

    @Override
    protected byte[] generateWriterClass() {
        return new byte[0];
    }
}
