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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.command.BlendFactor;
import net.daporkchop.fp2.gl.command.BlendOp;
import net.daporkchop.fp2.gl.command.Compare;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendFactors;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendOps;
import net.daporkchop.fp2.gl.opengl.command.state.struct.Color4b;
import net.daporkchop.fp2.gl.opengl.command.state.struct.Color4f;
import net.daporkchop.lib.common.util.PArrays;
import org.objectweb.asm.MethodVisitor;

import java.util.stream.Stream;

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
            return Stream.of(BLEND, COLOR_MASK);
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

    public final StateValueProperty<BlendFactors> BLEND_FACTORS = new BaseStateValueProperty<BlendFactors>(new BlendFactors(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ZERO)) {
        @Override
        public void emitCode(@NonNull BlendFactors value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GLEnumUtil.from(value.srcRGB()));
            mv.visitLdcInsn(GLEnumUtil.from(value.dstRGB()));
            mv.visitLdcInsn(GLEnumUtil.from(value.srcA()));
            mv.visitLdcInsn(GLEnumUtil.from(value.dstA()));
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBlendFuncSeparate", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
        }

        @Override
        public Stream<StateProperty> depends(@NonNull State state) {
            return state.getOrDef(this).usesUserColor() ? Stream.of(BLEND_COLOR) : Stream.empty();
        }
    };

    public final StateValueProperty<BlendOps> BLEND_OPS = new BaseStateValueProperty<BlendOps>(new BlendOps(BlendOp.ADD, BlendOp.ADD)) {
        @Override
        public void emitCode(@NonNull BlendOps value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GLEnumUtil.from(value.rgb()));
            mv.visitLdcInsn(GLEnumUtil.from(value.a()));
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBlendEquationSeparate", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE), true);
        }
    };

    public final StateValueProperty<Color4f> BLEND_COLOR = new ColorProperty("glBlendColor");

    //
    // COLOR
    //

    public final StateValueProperty<Color4f> CLEAR_COLOR = new ColorProperty("glClearColor");

    public final StateValueProperty<Color4b> COLOR_MASK = new BaseStateValueProperty<Color4b>(new Color4b(true, true, true, true)) {
        @Override
        public void emitCode(@NonNull Color4b value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(value.r());
            mv.visitLdcInsn(value.g());
            mv.visitLdcInsn(value.b());
            mv.visitLdcInsn(value.a());
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glColorMask", getMethodDescriptor(VOID_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE), true);
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

    public final StateValueProperty<Double> DEPTH_CLEAR = new BaseStateValueProperty<Double>(0.0d) {
        @Override
        public void emitCode(@NonNull Double value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(value);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glClearDepth", getMethodDescriptor(VOID_TYPE, DOUBLE_TYPE), true);
        }
    };

    public final StateValueProperty<Compare> DEPTH_COMPARE = new BaseStateValueProperty<Compare>(Compare.LESS) {
        @Override
        public void emitCode(@NonNull Compare value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GLEnumUtil.from(value));
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDepthFunc", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
    };

    public final StateValueProperty<Boolean> DEPTH_WRITE_MASK = new BaseStateValueProperty<Boolean>(true) {
        @Override
        public void emitCode(@NonNull Boolean value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(value);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDepthMask", getMethodDescriptor(VOID_TYPE, BOOLEAN_TYPE), true);
        }
    };

    //
    // STENCIL
    //

    //
    // SIMPLE BINDINGS
    //

    public final StateValueProperty<Integer> BOUND_PROGRAM = new BaseStateValueProperty<Integer>(0) {
        @Override
        public void emitCode(@NonNull Integer value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(value);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glUseProgram", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
    };

    public final StateValueProperty<Integer> BOUND_VAO = new BaseStateValueProperty<Integer>(0) {
        @Override
        public void emitCode(@NonNull Integer value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(value);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindVertexArray", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
    };

    //
    // BUFFER BINDINGS
    //

    public final StateValueProperty<Integer>[] BOUND_SSBO = uncheckedCast(PArrays.filledBy(16, StateValueProperty[]::new, i -> new IndexedBufferBindingProperty(GL_SHADER_STORAGE_BUFFER, i)));

    public final StateValueProperty<Integer>[] BOUND_UBO = uncheckedCast(PArrays.filledBy(16, StateValueProperty[]::new, i -> new IndexedBufferBindingProperty(GL_UNIFORM_BUFFER, i)));

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @Getter
    private static abstract class BaseStateValueProperty<T> implements StateValueProperty<T> {
        @NonNull
        private final T def;
    }

    /**
     * @author DaPorkchop_
     */
    private static abstract class FixedFunctionStateEnableProperty extends BaseStateValueProperty<Boolean> {
        private final int cap;

        public FixedFunctionStateEnableProperty(int cap) {
            super(false);

            this.cap = cap;
        }

        @Override
        public void emitCode(@NonNull Boolean value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.cap);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), value ? "glEnable" : "glDisable", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
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
    private static class ColorProperty extends BaseStateValueProperty<Color4f> {
        private final String methodName;

        public ColorProperty(@NonNull String methodName) {
            super(new Color4f(0.0f, 0.0f, 0.0f, 0.0f));

            this.methodName = methodName;
        }

        @Override
        public void emitCode(@NonNull Color4f value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(value.r());
            mv.visitLdcInsn(value.g());
            mv.visitLdcInsn(value.b());
            mv.visitLdcInsn(value.a());
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), this.methodName, getMethodDescriptor(VOID_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE), true);
        }
    }

    /**
     * @author DaPorkchop_
     */
    private static class IndexedBufferBindingProperty extends BaseStateValueProperty<Integer> {
        private final int type;
        private final int index;

        public IndexedBufferBindingProperty(int type, int index) {
            super(0);

            this.type = type;
            this.index = index;
        }

        @Override
        public void emitCode(@NonNull Integer value, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.type);
            mv.visitLdcInsn(this.index);
            mv.visitLdcInsn(value);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindBufferBase", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
        }
    }
}
