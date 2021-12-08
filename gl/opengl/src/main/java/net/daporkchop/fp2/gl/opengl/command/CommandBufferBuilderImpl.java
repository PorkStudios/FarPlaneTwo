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

package net.daporkchop.fp2.gl.opengl.command;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.asm.ClassloadingUtils;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.command.BlendFactor;
import net.daporkchop.fp2.gl.command.BlendOp;
import net.daporkchop.fp2.gl.command.CommandBuffer;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.command.Compare;
import net.daporkchop.fp2.gl.command.FramebufferLayer;
import net.daporkchop.fp2.gl.command.StencilOperation;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawMode;
import net.daporkchop.fp2.gl.draw.command.DrawList;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.command.state.CowState;
import net.daporkchop.fp2.gl.opengl.command.state.FixedState;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperties;
import net.daporkchop.fp2.gl.opengl.command.state.StateProperty;
import net.daporkchop.fp2.gl.opengl.command.state.StateValueProperty;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendFactors;
import net.daporkchop.fp2.gl.opengl.command.state.struct.BlendOps;
import net.daporkchop.fp2.gl.opengl.command.state.struct.Color4b;
import net.daporkchop.fp2.gl.opengl.command.state.struct.Color4f;
import net.daporkchop.fp2.gl.opengl.draw.command.DrawListImpl;
import net.daporkchop.fp2.gl.opengl.layout.BaseBindingImpl;
import net.daporkchop.fp2.gl.opengl.shader.BaseShaderProgramImpl;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static net.daporkchop.fp2.common.util.TypeSize.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * OpenGL implementation of {@link CommandBuffer} which generates code using ASM.
 *
 * @author DaPorkchop_
 */
public class CommandBufferBuilderImpl implements CommandBufferBuilder {
    private static final String CLASS_NAME = getInternalName(CommandBufferImpl.class) + '0';

    protected final OpenGL gl;

    protected final ClassWriter writer;

    protected final MethodVisitor ctorVisitor;

    protected final String apiFieldName;

    protected final BitSet lvtAllocationTable = new BitSet();
    protected final List<Object> fieldValues = new ArrayList<>();

    protected final ImmutableList.Builder<Uop> uops = ImmutableList.builder();

    protected CowState state = new CowState();

    @Deprecated
    protected FixedState fixedState = FixedState.DEFAULT_STATE;

    protected BaseBindingImpl binding;
    protected BaseShaderProgramImpl shader;

    public CommandBufferBuilderImpl(@NonNull OpenGL gl) {
        this.gl = gl;

        this.writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        this.writer.visit(V1_8, ACC_PUBLIC | ACC_FINAL, CLASS_NAME, null, getInternalName(CommandBufferImpl.class), null);

        this.ctorVisitor = this.writer.visitMethod(ACC_PUBLIC, "<init>", getMethodDescriptor(VOID_TYPE, getType(List.class), getType(List.class)), null, null);
        this.ctorVisitor.visitCode();
        this.ctorVisitor.visitVarInsn(ALOAD, 0);
        this.ctorVisitor.visitVarInsn(ALOAD, 1);
        this.ctorVisitor.visitMethodInsn(INVOKESPECIAL, getInternalName(CommandBufferImpl.class), "<init>", getMethodDescriptor(VOID_TYPE, getType(List.class)), false);

        this.apiFieldName = this.makeField(getType(GLAPI.class), gl.api());
    }

    protected String makeField(@NonNull Type type, @NonNull Object value) {
        //assign a new field name
        int index = this.fieldValues.size();
        this.fieldValues.add(value);
        String name = PStrings.fastFormat("field_%04x", index);

        //define the field
        this.writer.visitField(ACC_PRIVATE | ACC_FINAL, name, type.getDescriptor(), null, null).visitEnd();

        //set the field value in the constructor
        this.ctorVisitor.visitVarInsn(ALOAD, 0); //this
        this.ctorVisitor.visitVarInsn(ALOAD, 2); //list
        this.ctorVisitor.visitLdcInsn(index); //index
        this.ctorVisitor.visitMethodInsn(INVOKEINTERFACE, getInternalName(List.class), "get", getMethodDescriptor(getType(Object.class), INT_TYPE), true); //list.get(index)
        this.ctorVisitor.visitTypeInsn(CHECKCAST, type.getInternalName()); //(type)
        this.ctorVisitor.visitFieldInsn(PUTFIELD, CLASS_NAME, name, type.getDescriptor()); //this.name = (type) list.get(index);

        return name;
    }

    protected void releaseLocalVariable(int lvtIndex) {
        this.lvtAllocationTable.clear(lvtIndex);
    }

    @Override
    public CommandBufferBuilder framebufferClear(@NonNull FramebufferLayer... layers) {
        this.uops.add(new Uop(this.state) {
            @Override
            protected Stream<StateProperty> dependsFirst() {
                return Stream.of(layers).map(layer -> {
                    switch (layer) {
                        case COLOR:
                            return StateProperties.CLEAR_COLOR;
                        case STENCIL:
                            throw new UnsupportedOperationException(); //TODO
                        case DEPTH:
                            return StateProperties.DEPTH_CLEAR;
                        default:
                            throw new IllegalArgumentException(layer.name());
                    }
                });
            }

            @Override
            public void emitCode(@NonNull CommandBufferBuilderImpl builder, @NonNull MethodVisitor mv, int apiLvtIndex) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(layers));
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glClear", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
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
    public CommandBufferBuilder colorMask(boolean r, boolean g, boolean b, boolean a) {
        this.state = this.state.set(StateProperties.COLOR_MASK, new Color4b(r, g, b, a));
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
        this.fixedState = this.fixedState.withStencil(true);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilDisable() {
        this.fixedState = this.fixedState.withStencil(false);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilClear(int value) {
        this.fixedState = this.fixedState.withStencilClearValue(value);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilWriteMask(int writeMask) {
        this.fixedState = this.fixedState.withStencilWriteMask(writeMask);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilCompareMask(int compareMask) {
        this.fixedState = this.fixedState.withStencilCompareMask(compareMask);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilReference(int reference) {
        this.fixedState = this.fixedState.withStencilReference(reference);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilCompare(@NonNull Compare compare) {
        this.fixedState = this.fixedState.withStencilCompare(compare);
        return this;
    }

    @Override
    public CommandBufferBuilder stencilOperation(@NonNull StencilOperation fail, @NonNull StencilOperation pass, @NonNull StencilOperation depthFail) {
        this.fixedState = this.fixedState.withStencilOperationFail(fail).withStencilOperationPass(pass).withStencilOperationDepthFail(depthFail);
        return this;
    }

    @Override
    public CommandBufferBuilder drawArrays(@NonNull DrawBinding binding, @NonNull DrawShaderProgram shader, @NonNull DrawMode mode, int first, int count) {
        this.uops.add(new Uop.Draw(this.state, binding, shader) {
            @Override
            public void emitCode(@NonNull CommandBufferBuilderImpl builder, @NonNull MethodVisitor mv, int apiLvtIndex) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(mode));
                mv.visitLdcInsn(first);
                mv.visitLdcInsn(count);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDrawArrays", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
            }
        });
        return this;
    }

    @Override
    public CommandBufferBuilder drawList(@NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull DrawList<?> _list) {
        DrawListImpl<?, ?> list = (DrawListImpl<?, ?>) _list;

        this.uops.add(new Uop.Draw(this.state, list.binding(), shader) {
            @Override
            public void emitCode(@NonNull CommandBufferBuilderImpl builder, @NonNull MethodVisitor mv, int apiLvtIndex) {
                String fieldName = builder.makeField(getType(DrawListImpl.class), list);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, CLASS_NAME, fieldName, getDescriptor(DrawListImpl.class));
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(mode));
                mv.visitMethodInsn(INVOKEVIRTUAL, getInternalName(DrawListImpl.class), "draw0", getMethodDescriptor(VOID_TYPE, getType(GLAPI.class), INT_TYPE), false);
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
    public CommandBufferBuilder barrier() {
        //no-op: the current implementation is always ordered
        return this;
    }

    @Override
    @SneakyThrows({ IllegalAccessException.class, InstantiationException.class, InvocationTargetException.class, NoSuchMethodException.class })
    public CommandBuffer build() {
        MethodVisitor entryVisitor = this.writer.visitMethod(ACC_PUBLIC | ACC_FINAL, "execute", getMethodDescriptor(VOID_TYPE), null, null);
        entryVisitor.visitCode();

        entryVisitor.visitVarInsn(ALOAD, 0);
        entryVisitor.visitFieldInsn(GETFIELD, CLASS_NAME, this.apiFieldName, getDescriptor(GLAPI.class));
        entryVisitor.visitVarInsn(ASTORE, 1);

        MethodVisitor codeVisitor = this.writer.visitMethod(ACC_PRIVATE | ACC_FINAL, "execute", getMethodDescriptor(VOID_TYPE, getType(GLAPI.class)), null, null);
        codeVisitor.visitCode();

        List<Uop> uops = this.uops.build();
        Map<StateValueProperty<?>, Object> state = new IdentityHashMap<>();
        for (Uop uop : uops) {
            uop.depends()
                    .filter(property -> property instanceof StateValueProperty)
                    .map(property -> (StateValueProperty<?>) property)
                    .distinct()
                    .forEach(property -> {
                        uop.state().get(property).ifPresent(value -> {
                            if (!Objects.equals(value, state.put(property, value))) {
                                property.set(value, codeVisitor, 1);
                            }
                        });
                    });

            uop.emitCode(this, codeVisitor, 1);
        }

        if (this.gl.preserveInputGlState()) { //we want to preserve the current OpenGL state
            AtomicInteger lvtIndexAllocator = new AtomicInteger(2);
            Queue<Integer> baseLvts = new ArrayDeque<>();

            //allocate buffer for temporary data to/from glGet()s
            String bufferFieldName = this.makeField(getType(ByteBuffer.class), ByteBuffer.allocateDirect(16 * DOUBLE_SIZE));
            entryVisitor.visitVarInsn(ALOAD, 0);
            entryVisitor.visitFieldInsn(GETFIELD, CLASS_NAME, bufferFieldName, getDescriptor(ByteBuffer.class));
            entryVisitor.visitMethodInsn(INVOKESTATIC, getInternalName(PUnsafe.class), "pork_directBufferAddress", getMethodDescriptor(LONG_TYPE, getType(Buffer.class)), false);
            int bufferLvtIndex = lvtIndexAllocator.getAndAdd(2);
            entryVisitor.visitVarInsn(LSTORE, bufferLvtIndex);

            //back up all affected OpenGL properties into local variables
            state.keySet().forEach(property -> {
                baseLvts.add(lvtIndexAllocator.get());
                property.backup(entryVisitor, 1, bufferLvtIndex, lvtIndexAllocator);
            });

            //invoke execute(GLAPI)
            entryVisitor.visitVarInsn(ALOAD, 0);
            entryVisitor.visitVarInsn(ALOAD, 1);
            entryVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME, "execute", getMethodDescriptor(VOID_TYPE, getType(GLAPI.class)), false);

            //restore all affected OpenGL properties from their saved values
            state.keySet().forEach(property -> {
                property.restore(entryVisitor, 1, bufferLvtIndex, baseLvts.poll());
            });
        } else {
            //invoke execute(GLAPI)
            entryVisitor.visitVarInsn(ALOAD, 0);
            entryVisitor.visitVarInsn(ALOAD, 1);
            entryVisitor.visitMethodInsn(INVOKEVIRTUAL, CLASS_NAME, "execute", getMethodDescriptor(VOID_TYPE, getType(GLAPI.class)), false);

            //reset all properties to their default values
            state.forEach((property, value) -> {
                if (!Objects.equals(value, property.def())) {
                    property.set(uncheckedCast(property.def()), entryVisitor, 1);
                }
            });
        }

        this.ctorVisitor.visitInsn(RETURN);
        this.ctorVisitor.visitMaxs(0, 0);
        this.ctorVisitor.visitEnd();

        codeVisitor.visitInsn(RETURN);
        codeVisitor.visitMaxs(0, 0);
        codeVisitor.visitEnd();

        entryVisitor.visitInsn(RETURN);
        entryVisitor.visitMaxs(0, 0);
        entryVisitor.visitEnd();

        this.writer.visitEnd();

        if (true) {
            try {
                Files.write(Paths.get(CLASS_NAME.substring(CLASS_NAME.lastIndexOf('/') + 1) + ".class"), this.writer.toByteArray());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Class<? extends CommandBuffer> clazz = uncheckedCast(ClassloadingUtils.defineHiddenClass(CommandBufferBuilderImpl.class.getClassLoader(), this.writer.toByteArray()));
        return clazz.getDeclaredConstructor(List.class, List.class).newInstance(uops, this.fieldValues);
    }
}
