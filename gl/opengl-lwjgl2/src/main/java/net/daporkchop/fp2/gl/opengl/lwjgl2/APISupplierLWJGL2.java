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

package net.daporkchop.fp2.gl.opengl.lwjgl2;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import org.lwjgl.opengl.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class APISupplierLWJGL2 implements Supplier<GLAPI> {
    private static final Cached<Class<? extends GLAPI>> TRANSFORMED_CLASS_CACHE = Cached.global(
            () -> OpenGL.DEBUG ? transformClass() : GLAPILWJGL2.class,
            ReferenceStrength.WEAK);

    /**
     * Injects a call to {@link Util#checkGLError()} immediately before every function return.
     */
    @SneakyThrows(IOException.class)
    private static Class<? extends GLAPI> transformClass() {
        ClassReader reader;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        try (InputStream in = APISupplierLWJGL2.class.getResourceAsStream("GLAPILWJGL2.class")) {
            reader = new ClassReader(in);
        }

        reader.accept(new ClassVisitor(ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == IRETURN || opcode == LRETURN || opcode == FRETURN || opcode == DRETURN || opcode == ARETURN || opcode == RETURN) {
                            super.visitMethodInsn(INVOKESTATIC, "org/lwjgl/opengl/Util", "checkGLError", "()V", false);
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        }, 0);

        return uncheckedCast(ClassloadingUtils.defineHiddenClass(APISupplierLWJGL2.class.getClassLoader(), writer.toByteArray()));
    }

    @Override
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class })
    public GLAPI get() {
        return TRANSFORMED_CLASS_CACHE.get().newInstance();
    }
}
