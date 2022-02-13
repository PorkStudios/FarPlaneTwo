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

package net.daporkchop.fp2.gl.opengl.command;

import lombok.NonNull;
import net.daporkchop.fp2.gl.command.BlendFactor;
import net.daporkchop.fp2.gl.command.BlendOp;
import net.daporkchop.fp2.gl.command.CommandBuffer;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.command.Compare;
import net.daporkchop.fp2.gl.command.FramebufferLayer;
import net.daporkchop.fp2.gl.command.StencilOperation;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.list.DrawList;
import net.daporkchop.fp2.gl.draw.list.selected.JavaSelectedDrawList;
import net.daporkchop.fp2.gl.draw.list.selected.ShaderSelectedDrawList;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.FieldHandle;
import net.daporkchop.fp2.gl.opengl.command.methodwriter.MethodWriter;
import net.daporkchop.fp2.gl.opengl.command.state.CowState;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperty;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendFactors;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendOps;
import net.daporkchop.fp2.gl.opengl.command.state.struct.Color4b;
import net.daporkchop.fp2.gl.opengl.command.state.struct.Color4f;
import net.daporkchop.fp2.gl.opengl.command.state.struct.StencilOp;
import net.daporkchop.fp2.gl.opengl.command.uop.CompositeUop;
import net.daporkchop.fp2.gl.opengl.command.uop.ConditionalUop;
import net.daporkchop.fp2.gl.opengl.command.uop.Uop;
import net.daporkchop.fp2.gl.opengl.draw.list.DrawListImpl;
import net.daporkchop.fp2.gl.transform.binding.TransformBinding;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgram;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * OpenGL implementation of {@link CommandBuffer} which generates code using ASM.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractCommandBufferBuilder implements CommandBufferBuilder {
    protected static final String CLASS_NAME = getInternalName(CommandBufferImpl.class) + '0';

    protected final OpenGL gl;

    protected final List<Uop> uops = new ArrayList<>();

    protected CowState state;

    public AbstractCommandBufferBuilder(@NonNull OpenGL gl, @NonNull CowState state) {
        this.gl = gl;
        this.state = state;
    }

    protected abstract String makeField(@NonNull Type type, @NonNull Object value);

    public <T> FieldHandle<T> makeFieldHandle(@NonNull Type type, @NonNull T value) {
        String name = this.makeField(type, value);
        return mv -> {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, CLASS_NAME, name, type.getDescriptor());
        };
    }

    @Override
    public CommandBufferBuilder framebufferClear(@NonNull FramebufferLayer... layers) {
        this.uops.add(new Uop(this.state) {
            @Override
            protected Stream<StateProperty> dependsFirst() {
                return Stream.of(layers).flatMap(layer -> {
                    switch (layer) {
                        case COLOR:
                            return Stream.of(StateProperties.CLEAR_COLOR);
                        case STENCIL:
                            return Stream.of(StateProperties.STENCIL_CLEAR, StateProperties.STENCIL_MASK);
                        case DEPTH:
                            return Stream.of(StateProperties.DEPTH_CLEAR);
                        default:
                            throw new IllegalArgumentException(layer.name());
                    }
                });
            }

            @Override
            public void emitCode(@NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
                writer.write((mv, args) -> {
                    mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                    mv.visitLdcInsn(GLEnumUtil.from(layers));
                    mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glClear", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
                });
            }
        });
        return this;
    }

    @Override
    public CommandBufferBuilder blendEnable() {
        this.state = this.state.set(StateProperties.BLEND, true);
        return this;
    }

    @Override
    public CommandBufferBuilder blendDisable() {
        this.state = this.state.set(StateProperties.BLEND, false);
        return this;
    }

    @Override
    public CommandBufferBuilder blendFunctionSrc(@NonNull BlendFactor rgb, @NonNull BlendFactor a) {
        this.state = this.state.update(StateProperties.BLEND_FACTORS, factors -> new BlendFactors(rgb, a, factors.dstRGB(), factors.dstA()));
        return this;
    }

    @Override
    public CommandBufferBuilder blendFunctionDst(@NonNull BlendFactor rgb, @NonNull BlendFactor a) {
        this.state = this.state.update(StateProperties.BLEND_FACTORS, factors -> new BlendFactors(factors.srcRGB(), factors.srcA(), rgb, a));
        return this;
    }

    @Override
    public CommandBufferBuilder blendOp(@NonNull BlendOp rgb, @NonNull BlendOp a) {
        this.state = this.state.set(StateProperties.BLEND_OPS, new BlendOps(rgb, a));
        return this;
    }

    @Override
    public CommandBufferBuilder blendColor(float r, float g, float b, float a) {
        this.state = this.state.set(StateProperties.BLEND_COLOR, new Color4f(r, g, b, a));
        return this;
    }

    @Override
    public CommandBufferBuilder colorClear(float r, float g, float b, float a) {
        this.state = this.state.set(StateProperties.CLEAR_COLOR, new Color4f(r, g, b, a));
        return this;
    }

    @Override
    public CommandBufferBuilder colorWrite(boolean r, boolean g, boolean b, boolean a) {
        this.state = this.state.set(StateProperties.COLOR_MASK, new Color4b(r, g, b, a));
        return this;
    }

    @Override
    public CommandBufferBuilder cullEnable() {
        this.state = this.state.set(StateProperties.CULL, true);
        return this;
    }

    @Override
    public CommandBufferBuilder cullDisable() {
        this.state = this.state.set(StateProperties.CULL, false);
        return this;
    }

    @Override
    public CommandBufferBuilder depthEnable() {
        this.state = this.state.set(StateProperties.DEPTH, true);
        return this;
    }

    @Override
    public CommandBufferBuilder depthDisable() {
        this.state = this.state.set(StateProperties.DEPTH, false);
        return this;
    }

    @Override
    public CommandBufferBuilder depthClear(double value) {
        this.state = this.state.set(StateProperties.DEPTH_CLEAR, value);
        return this;
    }

    @Override
    public CommandBufferBuilder depthWrite(boolean mask) {
        this.state = this.state.set(StateProperties.DEPTH_WRITE_MASK, mask);
        return this;
    }

    @Override
    public CommandBufferBuilder depthCompare(@NonNull Compare compare) {
        this.state = this.state.set(StateProperties.DEPTH_COMPARE, compare);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilEnable() {
        this.state = this.state.set(StateProperties.STENCIL, true);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilDisable() {
        this.state = this.state.set(StateProperties.STENCIL, false);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilClear(int value) {
        this.state = this.state.set(StateProperties.STENCIL_CLEAR, value);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilWriteMask(int writeMask) {
        this.state = this.state.set(StateProperties.STENCIL_MASK, writeMask);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilCompareMask(int compareMask) {
        this.state = this.state.update(StateProperties.STENCIL_FUNC, func -> func.withMask(compareMask));
        return this;
    }

    @Override
    public CommandBufferBuilder stencilReference(int reference) {
        this.state = this.state.update(StateProperties.STENCIL_FUNC, func -> func.withReference(reference));
        return this;
    }

    @Override
    public CommandBufferBuilder stencilCompare(@NonNull Compare compare) {
        this.state = this.state.update(StateProperties.STENCIL_FUNC, func -> func.withCompare(compare));
        return this;
    }

    @Override
    public CommandBufferBuilder stencilOperation(@NonNull StencilOperation fail, @NonNull StencilOperation pass, @NonNull StencilOperation depthFail) {
        this.state = this.state.set(StateProperties.STENCIL_OP, new StencilOp(fail, pass, depthFail));
        return this;
    }

    @Override
    public CommandBufferBuilder drawArrays(@NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull DrawBinding binding, int first, int count) {
        this.uops.add(new Uop.Draw(this.state, binding, shader, Collections.emptyMap()) {
            @Override
            public void emitCode(@NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
                writer.write((mv, args) -> {
                    mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                    mv.visitLdcInsn(GLEnumUtil.from(mode));
                    mv.visitLdcInsn(first);
                    mv.visitLdcInsn(count);
                    mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDrawArrays", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
                });
            }
        });
        return this;
    }

    @Override
    public CommandBufferBuilder drawList(@NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull DrawList<?> list) {
        this.uops.addAll(((DrawListImpl<?>) list).draw(this.state, shader, GLEnumUtil.from(mode)));
        return this;
    }

    @Override
    public CommandBufferBuilder drawSelectedList(@NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull JavaSelectedDrawList<?> list, @NonNull IntPredicate selector) {
        FieldHandle<IntPredicate> field = this.makeFieldHandle(getType(IntPredicate.class), selector);
        this.uops.addAll(((DrawListImpl.JavaSelected<?>) list).drawSelected(this.state, shader, GLEnumUtil.from(mode), field));
        return this;
    }

    @Override
    public CommandBufferBuilder drawSelectedList(@NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull JavaSelectedDrawList<?> list, @NonNull Supplier<IntPredicate> selectorProvider) {
        FieldHandle<Supplier<IntPredicate>> field = this.makeFieldHandle(getType(Supplier.class), selectorProvider);
        this.uops.addAll(((DrawListImpl.JavaSelected<?>) list).drawSelected(this.state, shader, GLEnumUtil.from(mode), mv -> {
            field.get(mv);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(Supplier.class), "get", getMethodDescriptor(getType(Object.class)), true);
            mv.visitTypeInsn(CHECKCAST, getInternalName(IntPredicate.class));
        }));
        return this;
    }

    @Override
    public CommandBufferBuilder drawSelectedList(@NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull ShaderSelectedDrawList<?> list, @NonNull TransformShaderProgram selectionShader, @NonNull TransformBinding selectionBinding) {
        this.uops.addAll(((DrawListImpl.ShaderSelected<?>) list).drawSelected(this.state, shader, GLEnumUtil.from(mode), selectionShader, selectionBinding));
        return this;
    }

    @Override
    public CommandBufferBuilder transform(@NonNull TransformShaderProgram shader, @NonNull TransformBinding binding, int count) {
        this.uops.add(new Uop.Transform(this.state, binding, shader, Collections.singletonMap(StateProperties.RASTERIZER_DISCARD, true)) {
            @Override
            public void emitCode(@NonNull AbstractCommandBufferBuilder builder, @NonNull MethodWriter<CodegenArgs> writer) {
                //glBeginTransformFeedback(GL_POINTS);
                writer.write((mv, args) -> {
                    mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                    mv.visitLdcInsn(GL_POINTS);
                    mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBeginTransformFeedback", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
                });

                //glDrawArrays(GL_POINTS, 0, count);
                writer.write((mv, args) -> {
                    mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                    mv.visitLdcInsn(GL_POINTS);
                    mv.visitLdcInsn(0);
                    mv.visitLdcInsn(count);
                    mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDrawArrays", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
                });

                //glEndTransformFeedback();
                writer.write((mv, args) -> {
                    mv.visitVarInsn(ALOAD, args.apiLvtIndex());
                    mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glEndTransformFeedback", getMethodDescriptor(VOID_TYPE), true);
                });
            }
        });
        return this;
    }

    @Override
    public CommandBufferBuilder execute(@NonNull CommandBuffer buffer) {
        this.uops.addAll(((CommandBufferImpl) buffer).uops);
        return this;
    }

    @Override
    public CommandBufferBuilder conditional(@NonNull BooleanSupplier condition, @NonNull Consumer<CommandBufferBuilder> callback) {
        CommandBufferBuilderChild childBuilder = new CommandBufferBuilderChild(this);
        callback.accept(childBuilder);
        if (!childBuilder.uops.isEmpty()) {
            this.uops.add(new ConditionalUop(condition, new CompositeUop(childBuilder.uops)));
        }
        return this;
    }

    @Override
    public CommandBufferBuilder barrier() {
        //no-op: the current implementation is always ordered
        return this;
    }
}
