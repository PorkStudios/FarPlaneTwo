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
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.DelegatingClassLoader;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.attribute.struct.format.StructFormat;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public abstract class StructFormatClassLoader<S, L extends StructLayout<?, ?>, F extends StructFormat<S, L>> extends DelegatingClassLoader {
    protected static final boolean WRITE_CLASSES = StructFormatGenerator.class.desiredAssertionStatus();

    protected final OpenGL gl;
    protected final L layout;

    protected StructFormatClassLoader(@NonNull OpenGL gl, @NonNull L layout) {
        super(OpenGL.class.getClassLoader());

        this.gl = gl;
        this.layout = layout;
    }

    protected String formatClassName() {
        return "StructFormatImpl";
    }

    protected abstract byte[] generateFormatClass();

    protected String bufferClassName() {
        return "AttributeBufferImpl";
    }

    protected abstract byte[] generateBufferClass();

    protected String writerClassName() {
        return "AttributeWriterImpl";
    }

    protected abstract byte[] generateWriterClass();

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (this.formatClassName().equals(name)) {
            return this.defineClass(this.formatClassName(), this.generateFormatClass());
        } else if (this.bufferClassName().equals(name)) {
            return this.defineClass(this.bufferClassName(), this.generateBufferClass());
        } else if (this.writerClassName().equals(name)) {
            return this.defineClass(this.writerClassName(), this.generateWriterClass());
        }

        return super.findClass(name);
    }

    public F createFormat() throws Exception {
        Class<?> formatClass = this.loadClass(this.formatClassName());
        return uncheckedCast(formatClass.getConstructor(OpenGL.class, this.layout.getClass()).newInstance(this.gl, this.layout));
    }

    //
    // CODEGEN UTILITY METHODS
    //

    @SuppressWarnings("SameParameterValue")
    protected void generateMemcpy(@NonNull MethodVisitor mv, int srcBaseLvtIndex, int srcOffsetLvtIndex, int dstBaseLvtIndex, int dstOffsetLvtIndex, long size) {
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

    @SneakyThrows(IOException.class)
    protected byte[] finish(@NonNull ClassWriter writer, @NonNull String fileName) {
        writer.visitEnd();
        byte[] bytecode = writer.toByteArray();

        if (WRITE_CLASSES) {
            Files.write(Paths.get(fileName.replace('/', '-') + ".class"), bytecode);
        }

        return bytecode;
    }
}
