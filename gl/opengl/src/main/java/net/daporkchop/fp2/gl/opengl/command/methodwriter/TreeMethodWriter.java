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

package net.daporkchop.fp2.gl.opengl.command.methodwriter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Implementation of {@link MethodWriter} which groups code into smaller sub-functions every {@code N} calls to {@link MethodWriter#write(WriteCallback)}, and then gathers them together
 * by generating a tree of methods which call their children in order.
 * <p>
 * Example decompiled output, where {@code N = 2} and each {@code userCode*} function is generated in a separate call to {@link MethodWriter#write(WriteCallback)}:
 * <pre>{@code
 * private void gen_0_0(Object arg) {
 *     userCode1();
 *     userCode2(arg);
 * }
 *
 * private void gen_0_1(Object arg) {
 *     userCode3(arg);
 *     userCode4(arg);
 * }
 *
 * private void gen_0_2(Object arg) {
 *     userCode5(arg);
 * }
 *
 * private void gen_1_0(Object arg) {
 *     this.gen_0_0(arg);
 *     this.gen_0_1(arg);
 * }
 *
 * private void gen_1_1(Object arg) {
 *     this.gen_0_2(arg);
 * }
 *
 * private void gen_2_0(Object arg) {
 *     this.gen_1_0(arg);
 *     this.gen_1_1(arg);
 * }
 * }</pre>
 * <p>
 * This prevents the generated method body from becoming too large, and thus can allow the JIT compiler to optimize the code more aggressively.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class TreeMethodWriter<A extends MethodWriter.Args> implements MethodWriter<A> {
    @NonNull
    protected final ClassWriter writer;
    @NonNull
    protected final String className;
    @NonNull
    protected final String methodName;
    @NonNull
    protected final A args;
    protected final int n;
    protected final boolean instance;

    protected int finishedMethods;
    protected int currentMethodWrites;
    protected MethodVisitor mv;

    protected int children;

    @Override
    public void write(@NonNull WriteCallback<A> action) {
        if (this.mv != null && this.currentMethodWrites == this.n) { //current method is full, finish it
            this.finishMethod();
        }
        if (this.mv == null) { //start a new method
            this.startMethod();
        }

        action.accept(this.mv, this.args);
        this.currentMethodWrites++;
    }

    @Override
    public void makeChildAndCall(@NonNull WriteChildCallback<A> callback) {
        TreeMethodWriter<A> childWriter = new TreeMethodWriter<>(this.writer, this.className, this.methodName + "_c" + this.children++, this.args, this.n, this.instance);
        callback.accept(childWriter);
        String childName = childWriter.finish();

        this.write((mv, args) -> {
            this.loadArgs(mv);
            mv.visitMethodInsn(INVOKEVIRTUAL, this.className, childName, getMethodDescriptor(VOID_TYPE, args.argumentTypes()), false);
        });
    }

    protected void startMethod() {
        checkState(this.mv == null, "a method is currently active!");

        this.currentMethodWrites = 0;
        this.mv = this.writer.visitMethod(ACC_PRIVATE, this.methodName + "_0_" + this.finishedMethods, getMethodDescriptor(VOID_TYPE, this.args.argumentTypes()), null, null);
        this.mv.visitCode();
    }

    protected void finishMethod() {
        checkState(this.mv != null, "no method is currently active!");
        checkState(this.currentMethodWrites != 0, "attempted to close a method with 0 writes!");

        this.mv.visitInsn(RETURN);
        this.mv.visitMaxs(0, 0);
        this.mv.visitEnd();
        this.mv = null;

        this.finishedMethods++;
    }

    /**
     * Finishes writing code to this instance.
     *
     * @return the finished method name
     */
    public String finish() {
        if (this.mv != null) {
            this.finishMethod();
        }

        int lastStage = 0;
        int lastMethods = this.finishedMethods;

        while (lastMethods > 1) { //keep grouping methods together until there's only one
            int nextStage = lastStage + 1;
            int nextMethods = 0;

            for (int method = 0; method < lastMethods; nextMethods++) {
                MethodVisitor mv = this.writer.visitMethod(ACC_PRIVATE, this.methodName + '_' + nextStage + '_' + nextMethods, getMethodDescriptor(VOID_TYPE, this.args.argumentTypes()), null, null);
                mv.visitCode();

                for (int i = 0; i < this.n && method < lastMethods; i++, method++) {
                    this.loadArgs(mv);
                    mv.visitMethodInsn(INVOKEVIRTUAL, this.className, this.methodName + '_' + lastStage + '_' + method, getMethodDescriptor(VOID_TYPE, this.args.argumentTypes()), false);
                }
                mv.visitInsn(RETURN);

                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            lastStage = nextStage;
            lastMethods = nextMethods;
        }

        return this.methodName + '_' + lastStage + '_' + (lastMethods - 1);
    }

    protected void loadArgs(MethodVisitor mv) {
        int lvtIndex = 0;
        if (this.instance) {
            mv.visitVarInsn(ALOAD, lvtIndex++);
        }

        for (Type type : this.args.argumentTypes()) {
            mv.visitVarInsn(type.getOpcode(ILOAD), lvtIndex);
            lvtIndex += type.getSize();
        }
    }
}
