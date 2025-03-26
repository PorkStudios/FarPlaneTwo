/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.gl.state;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.daporkchop.fp2.common.util.NIOBufferUtil;
import net.daporkchop.fp2.gl.GLExtension;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.buffer.IndexedBufferTarget;
import net.daporkchop.fp2.gl.util.GLRequires;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.closeable.QuietCloseable;
import net.daporkchop.lib.common.misc.threadlocal.TL;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * Helper methods for binding OpenGL objects.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class MultiBindHelper implements QuietCloseable {
    /**
     * Clears multiple consecutive indexed buffer binding targets to buffer ID {@code 0}.
     *
     * @param gl     the OpenGL context
     * @param target the indexed buffer binding target
     * @param first  the first binding index
     * @param count  the number of binding indices to clear
     */
    public static void unbindIndexedBuffers(@NonNull OpenGL gl, @NonNull IndexedBufferTarget target, @NotNegative int first, @NotNegative int count) {
        checkRangeLen(gl.limits().maxBindings(target), first, count);
        int targetId = target.id();

        if (gl.supports(GLExtension.GL_ARB_multi_bind)) {
            // If possible, use glBindBuffersBase to bind all the buffers to 0
            gl.glBindBuffersBase(targetId, first, count);
        } else {
            // Fall back to using a loop
            for (int i = 0; i < count; i++) {
                gl.glBindBufferBase(targetId, first + i, 0);
            }
        }
    }

    /**
     * Binds the given buffers to multiple consecutive indexed buffer binding targets, starting from the given {@code first} index.
     *
     * @param gl      the OpenGL context
     * @param target  the indexed buffer binding target
     * @param first   the first binding index
     * @param buffers the IDs of the buffers to bind
     */
    public static void bindIndexedBuffers(@NonNull OpenGL gl, @NonNull IndexedBufferTarget target, @NotNegative int first, int @NonNull [] buffers) {
        checkRangeLen(gl.limits().maxBindings(target), first, buffers.length);
        int targetId = target.id();

        if (gl.supports(GLExtension.GL_ARB_multi_bind)) {
            // If possible, use glBindBuffersBase to bind all the buffers to 0
            gl.glBindBuffersBase(targetId, first, buffers);
        } else {
            // Fall back to using a loop
            for (int i = 0; i < buffers.length; i++) {
                gl.glBindBufferBase(targetId, first + i, buffers[i]);
            }
        }
    }

    /**
     * Clears multiple consecutive image texture bindings to texture ID {@code 0}.
     *
     * @param gl    the OpenGL context
     * @param first the first image unit
     * @param count the number of image units to clear
     */
    @GLRequires(GLExtension.GL_ARB_shader_image_load_store)
    public static void unbindImageTextures(@NonNull OpenGL gl, @NotNegative int first, @NotNegative int count) {
        checkRangeLen(gl.limits().maxImageUnits(), first, count);

        if (gl.supports(GLExtension.GL_ARB_multi_bind)) {
            // If possible, use glBindImageTextures to bind all the images to 0
            gl.glBindImageTextures(first, count);
        } else {
            // Fall back to using a loop
            for (int i = 0; i < count; i++) {
                gl.glBindImageTexture(first + i, 0, 0, false, 0, GL_READ_ONLY, GL_R8);
            }
        }
    }

    /**
     * @return a new {@link Builder}
     */
    public static Builder builder(@NonNull OpenGL gl) {
        return new Builder(gl);
    }

    private static final TL<IntBuffer> BUFFERS_INSTANCE = TL.create();

    static IntBuffer getBuffersBuffer(int capacity) {
        IntBuffer buffers = BUFFERS_INSTANCE.get();
        if (buffers == null || buffers.capacity() < capacity) {
            buffers = NIOBufferUtil.allocateDirectNativeInt(capacity);
            BUFFERS_INSTANCE.set(buffers);
        }
        return buffers;
    }

    private final @NonNull MethodHandle dispatcher;
    private final @NonNull MethodHandle unbindDispatcher;

    //TODO: cache generated classes
    private static MultiBindHelper generateBindingClass(List<BindingGroup> bindingGroups) {
        BindingGroup[] inputBindingsArray = new BindingGroup[bindingGroups.stream()
                .flatMapToInt(bindingGroup -> Arrays.stream(bindingGroup.argumentIndices))
                .max().orElse(-1) + 1];

        for (val bindingGroup : bindingGroups) {
            for (int argumentIndex : bindingGroup.argumentIndices) {
                inputBindingsArray[argumentIndex] = bindingGroup;
            }
        }

        int[] baseArgumentLvtIndices = new int[inputBindingsArray.length];
        MethodType dispatchFunctionType;
        {
            List<Class<?>> dispatchFunctionArgumentTypes = new ArrayList<>();
            dispatchFunctionArgumentTypes.add(OpenGL.class);
            for (int lvt = 1, i = 0; i < inputBindingsArray.length; i++) {
                val inputBinding = inputBindingsArray[i];

                baseArgumentLvtIndices[i] = lvt;

                dispatchFunctionArgumentTypes.add(int.class);
                lvt += 1;

                if (inputBinding.bindBufferRange) {
                    dispatchFunctionArgumentTypes.add(long.class);
                    dispatchFunctionArgumentTypes.add(long.class);
                    lvt += 4;
                }
            }
            dispatchFunctionType = MethodType.methodType(void.class, dispatchFunctionArgumentTypes);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC | ACC_FINAL, getInternalName(MultiBindHelper.class) + "$Impl", null, "java/lang/Object", null);

        int largestBindingGroup = 0;
        boolean anyBindBuffersRange = false;
        for (val bindingGroup : bindingGroups) {
            largestBindingGroup = Math.max(largestBindingGroup, bindingGroup.argumentIndices.length);
            anyBindBuffersRange |= bindingGroup.bindBufferRange;
        }
        boolean anyMultiBind = largestBindingGroup > 1;

        { //public static void dispatch(OpenGL gl, ...)
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "dispatch", dispatchFunctionType.toMethodDescriptorString(), null, null);
            mv.visitCode();

            final int glLvtIndex = 0;
            int buffersLvtIndex = -1;

            if (anyMultiBind) {
                buffersLvtIndex = getArgumentsAndReturnSizes(dispatchFunctionType.toMethodDescriptorString()) >> 2;

                mv.visitLdcInsn(largestBindingGroup);
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(MultiBindHelper.class), "getBuffersBuffer", getMethodDescriptor(getType(IntBuffer.class), INT_TYPE), MultiBindHelper.class.isInterface());
                mv.visitVarInsn(ASTORE, buffersLvtIndex);
            }

            for (val bindingGroup : bindingGroups) {
                mv.visitVarInsn(ALOAD, glLvtIndex);
                mv.visitLdcInsn(bindingGroup.target.id());
                mv.visitLdcInsn(bindingGroup.firstIndex);

                if (bindingGroup.argumentIndices.length == 1) {
                    int baseArgumentLvt = baseArgumentLvtIndices[bindingGroup.argumentIndices[0]];
                    mv.visitVarInsn(ILOAD, baseArgumentLvt);

                    if (bindingGroup.bindBufferRange) {
                        mv.visitVarInsn(LLOAD, baseArgumentLvt + 1);
                        mv.visitVarInsn(LLOAD, baseArgumentLvt + 3);
                        mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(OpenGL.class), "glBindBufferRange", "(IIIJJ)V", false);
                    } else {
                        mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(OpenGL.class), "glBindBufferBase", "(III)V", false);
                    }
                } else {
                    assert !bindingGroup.bindBufferRange;

                    mv.visitVarInsn(ALOAD, buffersLvtIndex);
                    mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(IntBuffer.class), "clear", getMethodDescriptor(getType(Buffer.class)), false);
                    mv.visitTypeInsn(CHECKCAST, getInternalName(IntBuffer.class));
                    for (int argumentIndex : bindingGroup.argumentIndices) {
                        mv.visitVarInsn(ILOAD, baseArgumentLvtIndices[argumentIndex]);
                        mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(IntBuffer.class), "put", getMethodDescriptor(getType(IntBuffer.class), INT_TYPE), false);
                    }
                    mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(IntBuffer.class), "flip", getMethodDescriptor(getType(Buffer.class)), false);
                    mv.visitTypeInsn(CHECKCAST, getInternalName(IntBuffer.class));

                    mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(OpenGL.class), "glBindBuffersBase", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, getType(IntBuffer.class)), false);
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        { //static void unbind(OpenGL gl)
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "unbind", getMethodDescriptor(VOID_TYPE, getType(OpenGL.class)), null, null);
            mv.visitCode();

            final int glLvtIndex = 0;

            for (val bindingGroup : bindingGroups) {
                mv.visitVarInsn(ALOAD, glLvtIndex);
                mv.visitLdcInsn(bindingGroup.target.id());
                mv.visitLdcInsn(bindingGroup.firstIndex);

                if (bindingGroup.argumentIndices.length == 1) {
                    mv.visitLdcInsn(0);
                    mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(OpenGL.class), "glBindBufferBase", "(III)V", false);
                } else {
                    mv.visitLdcInsn(bindingGroup.argumentIndices.length);
                    mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(OpenGL.class), "glBindBuffersBase", "(III)V", false);
                }
            }

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        cw.visitEnd();

        MethodHandles.Lookup dispatcherLookup = PUnsafe.defineHiddenClass(MethodHandles.lookup(), true, cw.toByteArray());
        return new MultiBindHelper(
                dispatcherLookup.findStatic(dispatcherLookup.lookupClass(), "dispatch", dispatchFunctionType),
                dispatcherLookup.findStatic(dispatcherLookup.lookupClass(), "unbind", MethodType.methodType(void.class, OpenGL.class)));
    }

    @Override
    public void close() {
        //for now this is a no-op
    }

    /**
     * @return a {@link MethodHandle} which can be invoked with the actual buffer IDs
     */
    public MethodHandle dispatcher() {
        return this.dispatcher;
    }

    /**
     * Resets all binding points affected by this object to {@code 0}, effectively unbinding everything.
     *
     * @param gl the OpenGL context
     */
    public void unbind(@NonNull OpenGL gl) {
        this.unbindDispatcher.invokeExact(gl);
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class Builder {
        final @NonNull OpenGL gl;
        final Set<SingleBinding> singleBindings = new LinkedHashSet<>();

        /**
         * Adds an indexed buffer binding.
         *
         * @param target the binding target
         * @param index  the binding index
         * @return this builder
         */
        public Builder bindBufferBase(@NonNull IndexedBufferTarget target, @NotNegative int index) {
            return this.bindBuffer(target, index, false);
        }

        /**
         * Adds an indexed buffer binding.
         *
         * @param target the binding target
         * @param index  the binding index
         * @return this builder
         */
        public Builder bindBufferRange(@NonNull IndexedBufferTarget target, @NotNegative int index) {
            return this.bindBuffer(target, index, true);
        }

        private Builder bindBuffer(@NonNull IndexedBufferTarget target, @NotNegative int index, boolean range) {
            checkIndex(this.gl.limits().maxBindings(target), index);
            if (!this.singleBindings.add(new SingleBinding(target, index, range, this.singleBindings.size()))) {
                throw new IllegalArgumentException("binding point " + target + " #" + index + " already configured!");
            }
            return this;
        }

        /**
         * Finishes constructing this {@link MultiBindHelper} instance.
         *
         * @return a new {@link MultiBindHelper}
         */
        public MultiBindHelper build() {
            List<SingleBinding> inputBindings = new ArrayList<>(this.singleBindings);

            List<BindingGroup> bindingGroups;
            if (this.gl.supports(GLExtension.GL_ARB_multi_bind)) {
                Map<IndexedBufferTarget, List<SingleBinding>> groupedBindings = inputBindings.stream()
                        .sorted()
                        .collect(Collectors.groupingBy(binding -> binding.target));

                //find sequential runs of binding indices and group them together
                bindingGroups = new ArrayList<>(inputBindings.size());
                for (val group : groupedBindings.values()) {
                    do {
                        SingleBinding first = group.remove(0);

                        List<Integer> argumentIndices = new ArrayList<>();
                        argumentIndices.add(first.argumentIndex);

                        while (!group.isEmpty()) {
                            SingleBinding next = group.get(0);
                            if (first.bindBufferRange || next.bindBufferRange //we don't support glBindBuffersRange() yet
                                    || next.index != first.index + argumentIndices.size()) {
                                break;
                            }

                            group.remove(0);
                            argumentIndices.add(next.argumentIndex);
                        }

                        bindingGroups.add(new BindingGroup(
                                first.target, first.index, first.bindBufferRange,
                                argumentIndices.stream().mapToInt(Integer::intValue).toArray()));
                    } while (!group.isEmpty());
                }
            } else {
                bindingGroups = inputBindings.stream()
                        .sorted()
                        .map(binding -> new BindingGroup(
                                binding.target, binding.index, binding.bindBufferRange,
                                new int[]{binding.argumentIndex}))
                        .collect(Collectors.toList());
            }

            return generateBindingClass(bindingGroups);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    static final class SingleBinding implements Comparable<SingleBinding> {
        final IndexedBufferTarget target;
        final int index;
        final boolean bindBufferRange;

        final int argumentIndex;

        @Override
        public int compareTo(SingleBinding o) {
            int d = this.target.compareTo(o.target);
            if (d == 0) {
                d = Integer.compare(this.index, o.index);
            }
            return d;
        }
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    static final class BindingGroup {
        final IndexedBufferTarget target;
        final int firstIndex;
        final boolean bindBufferRange;

        final int[] argumentIndices;
    }
}
