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

package net.daporkchop.fp2.gl.opengl.command.state;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.command.BlendFactor;
import net.daporkchop.fp2.gl.command.BlendOp;
import net.daporkchop.fp2.gl.command.Compare;
import net.daporkchop.fp2.gl.command.StencilOperation;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.attribute.texture.TextureTarget;
import net.daporkchop.fp2.gl.opengl.buffer.IndexedBufferTarget;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendFactors;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendOps;
import net.daporkchop.fp2.gl.opengl.command.state.struct.Color4b;
import net.daporkchop.fp2.gl.opengl.command.state.struct.Color4f;
import net.daporkchop.fp2.gl.opengl.command.state.struct.StencilFunc;
import net.daporkchop.fp2.gl.opengl.command.state.struct.StencilOp;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class StateProperties {
    public final StateProperty FIXED_FUNCTION_DRAW_PROPERTIES = new StateProperty() {
        @Override
        public Stream<StateProperty> depends(@NonNull State state) {
            return Stream.of(BLEND, COLOR_MASK, STENCIL).flatMap(property -> Stream.concat(Stream.of(property), property.depends(state)));
        }
    };

    //
    // BLENDING
    //

    public final StateValueProperty<Boolean> BLEND = new FixedFunctionStateEnableProperty(GL_BLEND) {
        @Override
        protected Stream<StateProperty> dependenciesWhenEnabled() {
            return Stream.of(BLEND_FACTORS, BLEND_OPS);
        }
    };

    public final StateValueProperty<BlendFactors> BLEND_FACTORS = new StructContainingMultipleIntegersProperty<BlendFactors>(new BlendFactors(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ZERO),
            "glBlendFuncSeparate",
            GL_BLEND_SRC_RGB, GL_BLEND_SRC_ALPHA, GL_BLEND_DST_RGB, GL_BLEND_DST_ALPHA) {
        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull BlendFactors value) {
            mv.visitLdcInsn(GLEnumUtil.from(value.srcRGB()));
            mv.visitLdcInsn(GLEnumUtil.from(value.dstRGB()));
            mv.visitLdcInsn(GLEnumUtil.from(value.srcA()));
            mv.visitLdcInsn(GLEnumUtil.from(value.dstA()));
        }

        @Override
        public Stream<StateProperty> depends(@NonNull State state) {
            return state.getOrDef(this).usesUserColor() ? Stream.of(BLEND_COLOR) : Stream.empty();
        }
    };

    public final StateValueProperty<BlendOps> BLEND_OPS = new StructContainingMultipleIntegersProperty<BlendOps>(new BlendOps(BlendOp.ADD, BlendOp.ADD),
            "glBlendEquationSeparate",
            GL_BLEND_EQUATION_RGB, GL_BLEND_EQUATION_ALPHA) {
        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull BlendOps value) {
            mv.visitLdcInsn(GLEnumUtil.from(value.rgb()));
            mv.visitLdcInsn(GLEnumUtil.from(value.a()));
        }
    };

    public final StateValueProperty<Color4f> BLEND_COLOR = new ColorProperty("glBlendColor", GL_BLEND_COLOR);

    //
    // COLOR
    //

    public final StateValueProperty<Color4f> CLEAR_COLOR = new ColorProperty("glClearColor", GL_COLOR_CLEAR_VALUE);

    public final StateValueProperty<Color4b> COLOR_MASK = new SimpleStateValueProperty<Color4b>(new Color4b(true, true, true, true)) {
        @Override
        protected void set(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glColorMask", getMethodDescriptor(VOID_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE), true);
        }

        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull Color4b value) {
            mv.visitLdcInsn(value.r());
            mv.visitLdcInsn(value.g());
            mv.visitLdcInsn(value.b());
            mv.visitLdcInsn(value.a());
        }

        @Override
        public void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GL_COLOR_WRITEMASK);
            mv.visitVarInsn(LLOAD, bufferLvtIndex);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetBoolean", getMethodDescriptor(VOID_TYPE, INT_TYPE, LONG_TYPE), true);

            for (int i = 0; i < 4; i++) {
                mv.visitVarInsn(LLOAD, bufferLvtIndex);
                mv.visitLdcInsn((long) i);
                mv.visitInsn(LADD);
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getByte", getMethodDescriptor(BYTE_TYPE, LONG_TYPE), false);
                mv.visitVarInsn(ISTORE, lvtIndexAllocator.getAndIncrement());
            }
        }

        @Override
        protected void loadFromBackup(@NonNull MethodVisitor mv, int bufferLvtIndex, int lvtIndexBase) {
            for (int i = 0; i < 4; i++) {
                mv.visitVarInsn(ILOAD, lvtIndexBase + i);
            }
        }
    };

    //
    // DEPTH
    //

    public final StateValueProperty<Boolean> DEPTH = new FixedFunctionStateEnableProperty(GL_DEPTH_TEST) {
        @Override
        protected Stream<StateProperty> dependenciesWhenEnabled() {
            return Stream.of(DEPTH_COMPARE, DEPTH_WRITE_MASK);
        }
    };

    public final StateValueProperty<Double> DEPTH_CLEAR = new DoubleSimpleStateValueProperty(0.0d, "glClearDepth", GL_DEPTH_CLEAR_VALUE);

    public final StateValueProperty<Compare> DEPTH_COMPARE = new IntegerSimpleStateValueProperty<>(Compare.LESS, GLEnumUtil::from, "glDepthFunc", GL_DEPTH_FUNC);

    public final StateValueProperty<Boolean> DEPTH_WRITE_MASK = new BooleanSimpleStateValueProperty(true, "glDepthMask", GL_DEPTH_WRITEMASK);

    //
    // STENCIL
    //

    public final StateValueProperty<Boolean> STENCIL = new FixedFunctionStateEnableProperty(GL_STENCIL_TEST) {
        @Override
        protected Stream<StateProperty> dependenciesWhenEnabled() {
            return Stream.of(STENCIL_MASK, STENCIL_FUNC, STENCIL_OP);
        }
    };

    public final StateValueProperty<Integer> STENCIL_CLEAR = new IntegerSimpleStateValueProperty<>(0, Function.identity(), "glClearStencil", GL_STENCIL_CLEAR_VALUE);

    public final StateValueProperty<Integer> STENCIL_MASK = new IntegerSimpleStateValueProperty<>(0, Function.identity(), "glStencilMask", GL_STENCIL_WRITEMASK);

    public final StateValueProperty<StencilFunc> STENCIL_FUNC = new StructContainingMultipleIntegersProperty<StencilFunc>(new StencilFunc(Compare.ALWAYS, 0, -1),
            "glStencilFunc",
            GL_STENCIL_FUNC, GL_STENCIL_REF, GL_STENCIL_VALUE_MASK) {
        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull StencilFunc value) {
            mv.visitLdcInsn(GLEnumUtil.from(value.compare()));
            mv.visitLdcInsn(value.reference());
            mv.visitLdcInsn(value.mask());
        }
    };

    public final StateValueProperty<StencilOp> STENCIL_OP = new StructContainingMultipleIntegersProperty<StencilOp>(new StencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP),
            "glStencilOp",
            GL_STENCIL_FAIL, GL_STENCIL_PASS_DEPTH_PASS,  GL_STENCIL_PASS_DEPTH_FAIL) {
        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull StencilOp value) {
            mv.visitLdcInsn(GLEnumUtil.from(value.fail()));
            mv.visitLdcInsn(GLEnumUtil.from(value.pass()));
            mv.visitLdcInsn(GLEnumUtil.from(value.depthFail()));
        }
    };

    //
    // SIMPLE BINDINGS
    //

    public final StateValueProperty<Integer> BOUND_PROGRAM = new IntegerSimpleStateValueProperty<>(0, Function.identity(), "glUseProgram", GL_CURRENT_PROGRAM);

    public final StateValueProperty<Integer> BOUND_VAO = new IntegerSimpleStateValueProperty<>(0, Function.identity(), "glBindVertexArray", GL_VERTEX_ARRAY_BINDING);

    //
    // INDEXED BUFFER BINDINGS
    //

    public final StateValueProperty<Integer>[] BOUND_SSBO = uncheckedCast(PArrays.filledBy(16, StateValueProperty[]::new, index -> new IndexedBufferBindingProperty(IndexedBufferTarget.SHADER_STORAGE_BUFFER, index)));

    public final StateValueProperty<Integer>[] BOUND_UBO = uncheckedCast(PArrays.filledBy(16, StateValueProperty[]::new, index -> new IndexedBufferBindingProperty(IndexedBufferTarget.UNIFORM_BUFFER, index)));

    //
    // TEXTURE BINDINGS
    //

    public final Map<TextureTarget, StateValueProperty<Integer>>[] BOUND_TEXTURE = uncheckedCast(PArrays.filledBy(80, Map[]::new, unit ->
            Stream.of(TextureTarget.values()).collect(ImmutableMap.toImmutableMap(Function.identity(), target -> new TextureBindingProperty(target, unit)))));

    //
    //
    // CLASSES
    //
    //

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static abstract class FixedFunctionStateEnableProperty implements StateValueProperty<Boolean> {
        private final int cap;

        @Override
        public Boolean def() {
            return false;
        }

        @Override
        public void set(@NonNull Boolean value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.cap);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), value ? "glEnable" : "glDisable", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }

        @Override
        public void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.cap);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetInteger", getMethodDescriptor(INT_TYPE, INT_TYPE), true);
            mv.visitVarInsn(ISTORE, lvtIndexAllocator.getAndIncrement());
        }

        @Override
        public void restore(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, int lvtIndexBase) {
            Label l0 = new Label();
            Label lEnd = new Label();

            mv.visitVarInsn(ILOAD, lvtIndexBase);
            mv.visitJumpInsn(IFNE, l0);

            //disabled
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.cap);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDisable", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
            mv.visitJumpInsn(GOTO, lEnd);

            //enabled
            mv.visitLabel(l0);
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.cap);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glEnable", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);

            mv.visitLabel(lEnd);
        }

        @Override
        public Stream<StateProperty> depends(@NonNull State state) {
            return state.getOrDef(this)
                    ? this.dependenciesWhenEnabled().flatMap(property -> Stream.concat(Stream.of(property), property.depends(state))).distinct()
                    : Stream.empty();
        }

        protected abstract Stream<StateProperty> dependenciesWhenEnabled();
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    private static abstract class SimpleStateValueProperty<T> implements StateValueProperty<T> {
        @NonNull
        private final T def;

        @Override
        public void set(@NonNull T value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            this.loadFromValue(mv, value);
            this.set(mv);
        }

        @Override
        public void restore(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, int lvtIndexBase) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            this.loadFromBackup(mv, bufferLvtIndex, lvtIndexBase);
            this.set(mv);
        }

        protected abstract void set(@NonNull MethodVisitor mv);

        protected abstract void loadFromValue(@NonNull MethodVisitor mv, @NonNull T value);

        @Override
        public abstract void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator);

        protected abstract void loadFromBackup(@NonNull MethodVisitor mv, int bufferLvtIndex, int lvtIndexBase);
    }

    //
    // SIMPLE PRIMITIVE PROPERTIES
    //

    /**
     * @author DaPorkchop_
     */
    private static class BooleanSimpleStateValueProperty extends SimpleStateValueProperty<Boolean> {
        private final String func;
        private final int binding;

        public BooleanSimpleStateValueProperty(@NonNull Boolean def, @NonNull String func, int binding) {
            super(def);

            this.func = func;
            this.binding = binding;
        }

        @Override
        protected void set(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), this.func, getMethodDescriptor(VOID_TYPE, BOOLEAN_TYPE), true);
        }

        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull Boolean value) {
            mv.visitLdcInsn(value);
        }

        @Override
        public void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.binding);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetBoolean", getMethodDescriptor(BOOLEAN_TYPE, INT_TYPE), true);
            mv.visitVarInsn(ISTORE, lvtIndexAllocator.getAndIncrement());
        }

        @Override
        protected void loadFromBackup(@NonNull MethodVisitor mv, int bufferLvtIndex, int lvtIndexBase) {
            mv.visitVarInsn(ILOAD, lvtIndexBase);
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static class IntegerSimpleStateValueProperty<T> extends SimpleStateValueProperty<T> {
        private final Function<T, Integer> toInt;
        private final String func;
        private final int binding;

        public IntegerSimpleStateValueProperty(@NonNull T def, @NonNull Function<T, Integer> toInt, @NonNull String func, int binding) {
            super(def);

            this.toInt = toInt;
            this.func = func;
            this.binding = binding;
        }

        @Override
        protected void set(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), this.func, getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }

        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull T value) {
            mv.visitLdcInsn(this.toInt.apply(value));
        }

        @Override
        public void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.binding);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetInteger", getMethodDescriptor(INT_TYPE, INT_TYPE), true);
            mv.visitVarInsn(ISTORE, lvtIndexAllocator.getAndIncrement());
        }

        @Override
        protected void loadFromBackup(@NonNull MethodVisitor mv, int bufferLvtIndex, int lvtIndexBase) {
            mv.visitVarInsn(ILOAD, lvtIndexBase);
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static class DoubleSimpleStateValueProperty extends SimpleStateValueProperty<Double> {
        private final String func;
        private final int binding;

        public DoubleSimpleStateValueProperty(@NonNull Double def, @NonNull String func, int binding) {
            super(def);

            this.func = func;
            this.binding = binding;
        }

        @Override
        protected void set(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), this.func, getMethodDescriptor(VOID_TYPE, DOUBLE_TYPE), true);
        }

        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull Double value) {
            mv.visitLdcInsn(value);
        }

        @Override
        public void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.binding);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetDouble", getMethodDescriptor(DOUBLE_TYPE, INT_TYPE), true);
            mv.visitVarInsn(DSTORE, lvtIndexAllocator.getAndAdd(2));
        }

        @Override
        protected void loadFromBackup(@NonNull MethodVisitor mv, int bufferLvtIndex, int lvtIndexBase) {
            mv.visitVarInsn(DLOAD, lvtIndexBase);
        }
    }

    //
    // MORE COMPLEX PROPERTIES
    //

    /**
     * @author DaPorkchop_
     */
    private static class ColorProperty extends SimpleStateValueProperty<Color4f> {
        private final String func;
        private final int pname;

        public ColorProperty(@NonNull String func, int pname) {
            super(new Color4f(0.0f, 0.0f, 0.0f, 0.0f));

            this.func = func;
            this.pname = pname;
        }

        @Override
        protected void set(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), this.func, getMethodDescriptor(VOID_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE), true);
        }

        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull Color4f value) {
            mv.visitLdcInsn(value.r());
            mv.visitLdcInsn(value.g());
            mv.visitLdcInsn(value.b());
            mv.visitLdcInsn(value.a());
        }

        @Override
        public void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.pname);
            mv.visitVarInsn(LLOAD, bufferLvtIndex);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetFloat", getMethodDescriptor(VOID_TYPE, INT_TYPE, LONG_TYPE), true);

            for (int i = 0; i < 4; i++) {
                mv.visitVarInsn(LLOAD, bufferLvtIndex);
                mv.visitLdcInsn(i * (long) FLOAT_SIZE);
                mv.visitInsn(LADD);
                mv.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "getFloat", getMethodDescriptor(FLOAT_TYPE, LONG_TYPE), false);
                mv.visitVarInsn(FSTORE, lvtIndexAllocator.getAndIncrement());
            }
        }

        @Override
        protected void loadFromBackup(@NonNull MethodVisitor mv, int bufferLvtIndex, int lvtIndexBase) {
            for (int i = 0; i < 4; i++) {
                mv.visitVarInsn(FLOAD, lvtIndexBase + i);
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static class IndexedBufferBindingProperty extends SimpleStateValueProperty<Integer> {
        private final IndexedBufferTarget target;
        private final int index;

        public IndexedBufferBindingProperty(@NonNull IndexedBufferTarget target, int index) {
            super(0);

            this.target = target;
            this.index = index;
        }

        @Override
        protected void set(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindBufferBase", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
        }

        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull Integer value) {
            mv.visitLdcInsn(this.target.id());
            mv.visitLdcInsn(this.index);
            mv.visitLdcInsn(value);
        }

        @Override
        public void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.target.binding());
            mv.visitLdcInsn(this.index);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetInteger", getMethodDescriptor(INT_TYPE, INT_TYPE, INT_TYPE), true);
            mv.visitVarInsn(ISTORE, lvtIndexAllocator.getAndIncrement());
        }

        @Override
        protected void loadFromBackup(@NonNull MethodVisitor mv, int bufferLvtIndex, int lvtIndexBase) {
            mv.visitLdcInsn(this.target.id());
            mv.visitLdcInsn(this.index);
            mv.visitVarInsn(ILOAD, lvtIndexBase);
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static class TextureBindingProperty extends SimpleStateValueProperty<Integer> {
        private final TextureTarget target;
        private final int unit;

        public TextureBindingProperty(@NonNull TextureTarget target, int unit) {
            super(0);

            this.target = target;
            this.unit = unit;
        }

        @Override
        public void set(@NonNull Integer value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GL_TEXTURE0 + this.unit);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glActiveTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);

            super.set(value, mv, apiLvtIndex);
        }

        @Override
        public void restore(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, int lvtIndexBase) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GL_TEXTURE0 + this.unit);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glActiveTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);

            super.restore(mv, apiLvtIndex, bufferLvtIndex, lvtIndexBase);
        }

        @Override
        protected void set(@NonNull MethodVisitor mv) {
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE), true);
        }

        @Override
        protected void loadFromValue(@NonNull MethodVisitor mv, @NonNull Integer value) {
            mv.visitLdcInsn(this.target.target());
            mv.visitLdcInsn(value);
        }

        @Override
        public void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GL_TEXTURE0 + this.unit);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glActiveTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);

            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.target.binding());
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetInteger", getMethodDescriptor(INT_TYPE, INT_TYPE), true);
            mv.visitVarInsn(ISTORE, lvtIndexAllocator.getAndIncrement());
        }

        @Override
        protected void loadFromBackup(@NonNull MethodVisitor mv, int bufferLvtIndex, int lvtIndexBase) {
            mv.visitLdcInsn(this.target.target());
            mv.visitVarInsn(ILOAD, lvtIndexBase);
        }
    }

    /**
     * @author DaPorkchop_
     */
    private abstract static class StructContainingMultipleIntegersProperty<S> extends SimpleStateValueProperty<S> {
        private final String func;
        private final int[] indices;

        public StructContainingMultipleIntegersProperty(@NonNull S def, @NonNull String func, @NonNull int... indices) {
            super(def);

            this.func = func;
            this.indices = indices;
        }

        @Override
        protected void set(@NonNull MethodVisitor mv) {
            Type[] argumentTypes = new Type[this.indices.length];
            Arrays.fill(argumentTypes, INT_TYPE);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), this.func, getMethodDescriptor(VOID_TYPE, argumentTypes), true);
        }

        @Override
        public void backup(@NonNull MethodVisitor mv, int apiLvtIndex, int bufferLvtIndex, @NonNull AtomicInteger lvtIndexAllocator) {
            for (int i : this.indices) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(i);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glGetInteger", getMethodDescriptor(INT_TYPE, INT_TYPE), true);
                mv.visitVarInsn(ISTORE, lvtIndexAllocator.getAndIncrement());
            }
        }

        @Override
        protected void loadFromBackup(@NonNull MethodVisitor mv, int bufferLvtIndex, int lvtIndexBase) {
            for (int i = 0; i < this.indices.length; i++) {
                mv.visitVarInsn(ILOAD, lvtIndexBase + i);
            }
        }
    }
}
