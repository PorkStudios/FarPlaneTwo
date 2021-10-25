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

package net.daporkchop.fp2.gl;

import lombok.NonNull;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * A subset of OpenGL functionality.
 * <p>
 * Modules may be disabled if not supported for the context's current version/profile, or if unsupported by the underlying graphics driver.
 * <p>
 * This interface should generally not be implemented or used directly, it's intended for use by sub-interfaces.
 *
 * @author DaPorkchop_
 */
public interface GLModule {
    static <M extends GLModule> M unsupportedImplementation(@NonNull Class<M> clazz) {
        checkArg(GLModule.class.isAssignableFrom(clazz), "not a subclass of %s: %s", GLModule.class, clazz);
        checkArg(clazz.isInterface(), "not an interface: %s", clazz);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        writer.visit(V1_8, ACC_FINAL, (clazz.getTypeName() + "UnsupportedImpl").replace('.', '/'), null, "java/lang/Object", new String[]{ clazz.getTypeName().replace('.', '/') });

        {
            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, "supported", "()Z", null, null);
            mv.visitCode();
            mv.visitLdcInsn(false);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        }

        for (Method method : clazz.getMethods()) {
            if ((method.getModifiers() & Modifier.STATIC) != 0 //don't implement static methods
                || method.getDeclaringClass() == GLModule.class) { //don't implement supported()
                continue;
            }

            MethodVisitor mv = writer.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "()V", false);
            mv.visitInsn(ATHROW);
            mv.visitMaxs(2, 0);
            mv.visitEnd();
        }

        writer.visitEnd();

        return uncheckedCast(PUnsafe.allocateInstance(ClassloadingUtils.defineHiddenClass(clazz.getClassLoader(), writer.toByteArray())));
    }

    /**
     * @return whether or not this module is supported. If {@code false}, all other methods in this instance will throw {@link UnsupportedOperationException}
     */
    boolean supported();
}
