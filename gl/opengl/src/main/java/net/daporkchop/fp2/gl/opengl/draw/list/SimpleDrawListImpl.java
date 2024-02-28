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

package net.daporkchop.fp2.gl.opengl.draw.list;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.command.CodegenArgs;
import net.daporkchop.fp2.gl.opengl.command.AbstractCommandBufferBuilder;
import net.daporkchop.fp2.gl.opengl.command.uop.AbstractSimpleUop;
import net.daporkchop.fp2.gl.opengl.command.uop.AbstractDrawUop;
import net.daporkchop.fp2.gl.opengl.command.uop.AbstractTransformUop;
import net.daporkchop.fp2.gl.opengl.command.uop.Uop;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.FieldHandle;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.GeneratedSupplier;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.MethodWriter;
import net.daporkchop.fp2.gl.opengl.command.state.State;
import net.daporkchop.fp2.gl.opengl.command.state.StateValueProperty;
import net.daporkchop.fp2.gl.transform.binding.TransformBinding;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgram;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
public interface SimpleDrawListImpl<C extends DrawCommand> extends DrawListImpl<C> {
    Map<StateValueProperty<?>, Object> configureStateForDraw0(@NonNull State state);

    void draw0(GLAPI api, int mode);

    @Override
    default List<Uop> draw(@NonNull State state, @NonNull DrawShaderProgram drawShader, int mode) {
        return Collections.singletonList(new AbstractDrawUop(state, this.binding(), drawShader, this.configureStateForDraw0(state)) {
            @Override
            public void emitCode(@NonNull State effectiveState, @NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
                FieldHandle field = builder.makeFieldHandle(getType(SimpleDrawListImpl.this.getClass()), SimpleDrawListImpl.this);

                //<this>.draw0(api, mode);
                writer.write((mv, args) -> {
                    field.get(mv);
                    mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                    mv.visitLdcInsn(mode);
                    mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(SimpleDrawListImpl.this.getClass()), "draw0", getMethodDescriptor(VOID_TYPE, getType(GLAPI.class), INT_TYPE), false);
                });
            }
        });
    }

    /**
     * @author DaPorkchop_
     */
    interface JavaSelected<C extends DrawCommand> extends DrawListImpl.JavaSelected<C> {
        Map<StateValueProperty<?>, Object> configureStateForDrawSelected0(@NonNull State state);

        void drawSelected0(GLAPI api, int mode, IntPredicate selector);

        @Override
        default List<Uop> drawSelected(@NonNull State state, @NonNull DrawShaderProgram drawShader, int mode, @NonNull GeneratedSupplier<IntPredicate> selector) {
            return Collections.singletonList(new AbstractDrawUop(state, this.binding(), drawShader, this.configureStateForDrawSelected0(state)) {
                @Override
                public void emitCode(@NonNull State effectiveState, @NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
                    FieldHandle<SimpleDrawListImpl.JavaSelected> field = builder.makeFieldHandle(getType(SimpleDrawListImpl.JavaSelected.this.getClass()), SimpleDrawListImpl.JavaSelected.this);

                    //<this>.drawSelected0(api, mode, selector);
                    writer.write((mv, args) -> {
                        field.get(mv);
                        mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                        mv.visitLdcInsn(mode);
                        selector.get(mv);
                        mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(SimpleDrawListImpl.JavaSelected.this.getClass()), "drawSelected0", getMethodDescriptor(VOID_TYPE, getType(GLAPI.class), INT_TYPE, getType(IntPredicate.class)), false);
                    });
                }
            });
        }
    }

    /**
     * @author DaPorkchop_
     */
    interface ShaderSelected<C extends DrawCommand> extends DrawListImpl.ShaderSelected<C> {
        Map<StateValueProperty<?>, Object> configureStateForPrepare0(@NonNull State state);

        void prepare0(GLAPI api, int mode);

        Map<StateValueProperty<?>, Object> configureStateForTransform0(@NonNull State state);

        int transformCount0();

        Map<StateValueProperty<?>, Object> configureStateForDrawSelected0(@NonNull State state);

        void drawSelected0(GLAPI api, int mode);

        @Override
        default List<Uop> drawSelected(@NonNull State state, @NonNull DrawShaderProgram drawShader, int mode, @NonNull TransformShaderProgram selectionShader, @NonNull TransformBinding selectionBinding) {
            return ImmutableList.of(
                    new AbstractSimpleUop(state, this.configureStateForPrepare0(state)) {
                        @Override
                        public void emitCode(@NonNull State effectiveState, @NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
                            FieldHandle<SimpleDrawListImpl.ShaderSelected> field = builder.makeFieldHandle(getType(SimpleDrawListImpl.ShaderSelected.this.getClass()), SimpleDrawListImpl.ShaderSelected.this);

                            //<this>.prepare0(api, mode);
                            writer.write((mv, args) -> {
                                field.get(mv);
                                mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                                mv.visitLdcInsn(mode);
                                mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(SimpleDrawListImpl.ShaderSelected.this.getClass()), "prepare0", getMethodDescriptor(VOID_TYPE, getType(GLAPI.class), INT_TYPE), false);
                            });
                        }
                    },
                    new AbstractTransformUop(state, selectionBinding, selectionShader, this.configureStateForTransform0(state)) {
                        @Override
                        public void emitCode(@NonNull State effectiveState, @NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
                            FieldHandle<SimpleDrawListImpl.ShaderSelected> field = builder.makeFieldHandle(getType(SimpleDrawListImpl.ShaderSelected.this.getClass()), SimpleDrawListImpl.ShaderSelected.this);

                            //glBeginTransformFeedback(GL_POINTS);
                            writer.write((mv, args) -> {
                                mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                                mv.visitLdcInsn(GL_POINTS);
                                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBeginTransformFeedback", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
                            });

                            //api.glDrawArrays(GL_POINTS, 0, <this>.transformCount0());
                            writer.write((mv, args) -> {
                                mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                                mv.visitLdcInsn(GL_POINTS);
                                mv.visitLdcInsn(0);
                                field.get(mv);
                                mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(SimpleDrawListImpl.ShaderSelected.this.getClass()), "transformCount0", getMethodDescriptor(INT_TYPE), false);
                                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDrawArrays", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
                            });

                            //glEndTransformFeedback();
                            writer.write((mv, args) -> {
                                mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glEndTransformFeedback", getMethodDescriptor(VOID_TYPE), true);
                            });
                        }
                    },
                    new AbstractDrawUop(state, this.binding(), drawShader, this.configureStateForDrawSelected0(state)) {
                        @Override
                        public void emitCode(@NonNull State effectiveState, @NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
                            FieldHandle<SimpleDrawListImpl.ShaderSelected> field = builder.makeFieldHandle(getType(SimpleDrawListImpl.ShaderSelected.this.getClass()), SimpleDrawListImpl.ShaderSelected.this);

                            //<this>.drawSelected0(api, mode);
                            writer.write((mv, args) -> {
                                field.get(mv);
                                mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                                mv.visitLdcInsn(mode);
                                mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(SimpleDrawListImpl.ShaderSelected.this.getClass()), "drawSelected0", getMethodDescriptor(VOID_TYPE, getType(GLAPI.class), INT_TYPE), false);
                            });
                        }
                    });
        }
    }
}
