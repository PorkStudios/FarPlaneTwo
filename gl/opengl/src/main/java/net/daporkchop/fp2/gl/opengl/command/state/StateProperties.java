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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.command.BlendFactor;
import net.daporkchop.fp2.gl.command.BlendOp;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendFactors;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendOps;
import net.daporkchop.fp2.gl.opengl.command.state.struct.Color4f;
import org.objectweb.asm.MethodVisitor;

import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
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
            return Stream.of(BLEND);
        }
    };

    //
    // BLENDING
    //

    public final StateValueProperty<Boolean> BLEND = new BooleanProperty(GL_BLEND) {
        @Override
        protected Stream<StateProperty> dependenciesWhenEnabled() {
            return Stream.of(BLEND_FACTORS, BLEND_OPS);
        }
    };

    public final StateValueProperty<BlendFactors> BLEND_FACTORS = new StateValueProperty<BlendFactors>() {
        @Override
        public BlendFactors def() {
            return new BlendFactors(BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ZERO);
        }

        @Override
        public void generateCode(@NonNull State state, @NonNull MethodVisitor mv, int apiLvtIndex) {
            BlendFactors factors = state.getOrDef(this);

            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GLEnumUtil.from(factors.srcRGB()));
            mv.visitLdcInsn(GLEnumUtil.from(factors.dstRGB()));
            mv.visitLdcInsn(GLEnumUtil.from(factors.srcA()));
            mv.visitLdcInsn(GLEnumUtil.from(factors.dstA()));
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBlendFuncSeparate", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
        }

        @Override
        public Stream<StateProperty> depends(@NonNull State state) {
            return state.getOrDef(this).usesUserColor() ? Stream.of(BLEND_COLOR) : Stream.empty();
        }
    };

    public final StateValueProperty<BlendOps> BLEND_OPS = new StateValueProperty<BlendOps>() {
        @Override
        public BlendOps def() {
            return new BlendOps(BlendOp.ADD, BlendOp.ADD);
        }

        @Override
        public void generateCode(@NonNull State state, @NonNull MethodVisitor mv, int apiLvtIndex) {
            BlendOps ops = state.getOrDef(this);

            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GLEnumUtil.from(ops.rgb()));
            mv.visitLdcInsn(GLEnumUtil.from(ops.a()));
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBlendEquationSeparate", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE), true);
        }

        @Override
        public Stream<StateProperty> depends(@NonNull State state) {
            return Stream.empty();
        }
    };

    public final StateValueProperty<Color4f> BLEND_COLOR = new ColorProperty("glClearColor");

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static abstract class BooleanProperty implements StateValueProperty<Boolean> {
        private final int cap;

        @Override
        public Boolean def() {
            return false;
        }

        @Override
        public void generateCode(@NonNull State state, @NonNull MethodVisitor mv, int apiLvtIndex) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(this.cap);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), state.getOrDef(this) ? "glEnable" : "glDisable", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }

        @Override
        public Stream<StateProperty> depends(@NonNull State state) {
            return state.getOrDef(this)
                    ? this.dependenciesWhenEnabled().flatMap(property -> property.depends(state)).distinct()
                    : Stream.empty();
        }

        protected abstract Stream<StateProperty> dependenciesWhenEnabled();
    }

    /**
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static class ColorProperty implements StateValueProperty<Color4f> {
        @NonNull
        private final String methodName;

        @Override
        public Color4f def() {
            return new Color4f(0.0f, 0.0f, 0.0f, 0.0f);
        }

        @Override
        public void generateCode(@NonNull State state, @NonNull MethodVisitor mv, int apiLvtIndex) {
            Color4f color = state.getOrDef(this);

            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(color.r());
            mv.visitLdcInsn(color.g());
            mv.visitLdcInsn(color.b());
            mv.visitLdcInsn(color.a());
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), this.methodName, getMethodDescriptor(VOID_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE), true);
        }

        @Override
        public Stream<StateProperty> depends(@NonNull State state) {
            return Stream.empty();
        }
    }
}
