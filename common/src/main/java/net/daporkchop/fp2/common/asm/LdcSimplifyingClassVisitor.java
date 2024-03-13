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

package net.daporkchop.fp2.common.asm;

import org.objectweb.asm.ClassVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
public class LdcSimplifyingClassVisitor extends ClassVisitor {
    public LdcSimplifyingClassVisitor(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    /**
     * @author DaPorkchop_
     */
    public static class MethodVisitor extends org.objectweb.asm.MethodVisitor {
        public MethodVisitor(org.objectweb.asm.MethodVisitor mv) {
            super(ASM5, mv);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            int opcode = NOP;

            if (cst instanceof Byte) {
                cst = (int) (byte) cst;
            } else if (cst instanceof Short) {
                cst = (int) (short) cst;
            } else if (cst instanceof Character) {
                cst = (int) (char) cst;
            }

            if (cst instanceof Boolean) {
                opcode = (boolean) cst ? ICONST_1 : ICONST_0;
            } else if (cst instanceof Integer) {
                int i = (int) cst;
                if (i >= -1 && i <= 5) {
                    opcode = ICONST_0 + i;
                } else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE) {
                    opcode = i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE ? BIPUSH : SIPUSH;
                    super.visitIntInsn(opcode, i);
                    return;
                }
            } else if (cst instanceof Long) {
                long l = (long) cst;
                if (l == 0L) {
                    opcode = LCONST_0;
                } else if (l == 1L) {
                    opcode = LCONST_1;
                }
            } else if (cst instanceof Float) {
                int f = Float.floatToRawIntBits((float) cst);
                if (f == Float.floatToRawIntBits(0.0f)) {
                    opcode = FCONST_0;
                } else if (f == Float.floatToRawIntBits(1.0f)) {
                    opcode = FCONST_1;
                } else if (f == Float.floatToRawIntBits(2.0f)) {
                    opcode = FCONST_2;
                }
            } else if (cst instanceof Double) {
                long d = Double.doubleToRawLongBits((double) cst);
                if (d == Double.doubleToRawLongBits(0.0d)) {
                    opcode = DCONST_0;
                } else if (d == Double.doubleToRawLongBits(1.0d)) {
                    opcode = DCONST_1;
                }
            }

            if (opcode != NOP) {
                super.visitInsn(opcode);
            } else {
                super.visitLdcInsn(cst);
            }
        }
    }
}
