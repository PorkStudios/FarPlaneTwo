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

package net.daporkchop.fp2.gl.opengl;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.gl.OpenGLConstants;
import net.daporkchop.fp2.gl.OpenGLException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
class ErrorCheckingWrapperGLAPI {
    private final MethodHandle CONSTRUCTOR = generate();

    @SneakyThrows
    private MethodHandle generate() {
        String className = getInternalName(ErrorCheckingWrapperGLAPI.class) + "Impl";

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, className, null, "java/lang/Object", new String[]{
                Type.getInternalName(GLAPI.class),
        });

        writer.visitField(ACC_PRIVATE, "gl", getDescriptor(OpenGL.class), null, null).visitEnd();
        writer.visitField(ACC_PRIVATE, "delegate", getDescriptor(GLAPI.class), null, null).visitEnd();

        { //constructor
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(OpenGL.class), getType(GLAPI.class)), null, null);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, className, "gl", getDescriptor(OpenGL.class));

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 2);
            mv.visitFieldInsn(PUTFIELD, className, "delegate", getDescriptor(GLAPI.class));

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        //automatically generate all other methods
        for (Method method : GLAPI.class.getDeclaredMethods()) {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, method.getName(), getMethodDescriptor(method), null, null);

            //this.gl.ensureOpen();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "gl", getDescriptor(OpenGL.class));
            mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(OpenGL.class), "ensureOpen", getMethodDescriptor(VOID_TYPE), false);

            //proxy to delegate
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, className, "delegate", getDescriptor(GLAPI.class));
            int idx = 1;
            for (Class<?> param : method.getParameterTypes()) {
                Type paramType = getType(param);
                mv.visitVarInsn(paramType.getOpcode(ILOAD), idx);
                idx += paramType.getSize();
            }
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), method.getName(), getMethodDescriptor(method), true);

            //check for opengl error, throw exception if any
            if (!"glGetError".equals(method.getName())) {
                Label tail = new Label();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, className, "delegate", getDescriptor(GLAPI.class));
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetError", getMethodDescriptor(INT_TYPE), true);
                mv.visitInsn(DUP);
                mv.visitFieldInsn(GETSTATIC, getInternalName(OpenGLConstants.class), "GL_NO_ERROR", INT_TYPE.getDescriptor());
                mv.visitJumpInsn(IF_ICMPEQ, tail);
                mv.visitTypeInsn(NEW, getInternalName(OpenGLException.class));
                mv.visitInsn(DUP_X1);
                mv.visitInsn(DUP_X1);
                mv.visitInsn(POP);
                mv.visitMethodInsn(INVOKESPECIAL, getInternalName(OpenGLException.class), "<init>", getMethodDescriptor(VOID_TYPE, INT_TYPE), false);
                mv.visitInsn(ATHROW);
                mv.visitLabel(tail);
                mv.visitInsn(POP);
            }

            mv.visitInsn(getType(method.getReturnType()).getOpcode(IRETURN));

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        writer.visitEnd();

        Class<?> clazz = ClassloadingUtils.defineHiddenClass(ErrorCheckingWrapperGLAPI.class.getClassLoader(), writer.toByteArray());
        return MethodHandles.publicLookup().unreflectConstructor(clazz.getDeclaredConstructor(OpenGL.class, GLAPI.class));
    }

    @SneakyThrows
    public GLAPI wrap(@NonNull OpenGL gl, @NonNull GLAPI api) {
        return (GLAPI) CONSTRUCTOR.invoke(gl, api);
    }
}
