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

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.With;
import net.daporkchop.fp2.gl.command.Comparison;
import net.daporkchop.fp2.gl.command.StencilOperation;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.buffer.BufferTarget;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Data
@With
public class State {
    public static final State DEFAULT_STATE = new State();

    private static <K, V> Map<K, V> cowImmutableMapPut(@NonNull Map<K, V> map, K key, V value) {
        if (Objects.equals(value, map.get(key)) && map.containsKey(key)) { //already present, do nothing
            return map;
        }

        Map<K, V> tmp = new HashMap<>(map);
        tmp.put(key, value);
        return ImmutableMap.copyOf(tmp);
    }

    public static void generateStateChange(@NonNull MethodVisitor mv, int apiLvtIndex, @NonNull State prevState, @NonNull State nextState) {
        //stencil
        if (prevState.stencil != nextState.stencil) { //toggle stencil test if it changed
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GL_STENCIL_TEST);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), nextState.stencil ? "glEnable" : "glDisable", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
        if (nextState.stencil) { //stencil is enabled, update the state
            if (prevState.stencilCompare != nextState.stencilCompare || prevState.stencilReference != nextState.stencilReference || prevState.stencilCompareMask != nextState.stencilCompareMask) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(nextState.stencilCompare));
                mv.visitLdcInsn(nextState.stencilReference);
                mv.visitLdcInsn(nextState.stencilCompareMask);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glStencilFunc", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
            }
            if (prevState.stencilWriteMask != nextState.stencilWriteMask) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(nextState.stencilWriteMask);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glStencilMask", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
            }
            if (prevState.stencilOperationFail != nextState.stencilOperationFail || prevState.stencilOperationPass != nextState.stencilOperationPass || prevState.stencilOperationDepthFail != nextState.stencilOperationDepthFail) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(nextState.stencilOperationFail));
                mv.visitLdcInsn(GLEnumUtil.from(nextState.stencilOperationPass));
                mv.visitLdcInsn(GLEnumUtil.from(nextState.stencilOperationDepthFail));
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glStencilOp", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
            }
        }

        //bindings
        if (prevState.program != nextState.program) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(nextState.program);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glUseProgram", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
        if (prevState.vao != nextState.vao) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(nextState.vao);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindVertexArray", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
        nextState.shaderStorageBufferBindings.forEach((index, buffer) -> {
            if (!Objects.equals(prevState.shaderStorageBufferBindings.get(index), buffer)) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GL_SHADER_STORAGE_BUFFER);
                mv.visitLdcInsn(index);
                mv.visitLdcInsn(buffer);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindBufferBase", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
            }
        });
        nextState.uniformBufferBindings.forEach((index, buffer) -> {
            if (!Objects.equals(prevState.uniformBufferBindings.get(index), buffer)) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GL_UNIFORM_BUFFER);
                mv.visitLdcInsn(index);
                mv.visitLdcInsn(buffer);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindBufferBase", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
            }
        });
    }

    //stencil
    private final boolean stencil;
    private final int stencilWriteMask;
    private final int stencilCompareMask;
    private final int stencilReference;
    private final Comparison stencilCompare;
    private final StencilOperation stencilOperationFail;
    private final StencilOperation stencilOperationPass;
    private final StencilOperation stencilOperationDepthFail;

    //bindings
    private final int program;
    private final int vao;
    @With(AccessLevel.PRIVATE)
    private final Map<Integer, Integer> shaderStorageBufferBindings;
    @With(AccessLevel.PRIVATE)
    private final Map<Integer, Integer> uniformBufferBindings;

    private State() {
        this.stencil = false;
        this.stencilWriteMask = -1;
        this.stencilCompareMask = -1;
        this.stencilReference = 0;
        this.stencilCompare = Comparison.NEVER;
        this.stencilOperationFail = StencilOperation.KEEP;
        this.stencilOperationPass = StencilOperation.KEEP;
        this.stencilOperationDepthFail = StencilOperation.KEEP;

        this.program = 0;
        this.vao = 0;
        this.shaderStorageBufferBindings = ImmutableMap.of();
        this.uniformBufferBindings = ImmutableMap.of();
    }

    /**
     * Sets the current GL state to the state described by this instance.
     *
     * @param api the {@link GLAPI} instance
     */
    public void set(@NonNull GLAPI api) {
        if (this.stencil) {
            api.glEnable(GL_STENCIL_TEST);
        } else {
            api.glDisable(GL_STENCIL_TEST);
        }
        api.glStencilFunc(GLEnumUtil.from(this.stencilCompare), this.stencilReference, this.stencilCompareMask);
        api.glStencilMask(this.stencilWriteMask);
        api.glStencilOp(GLEnumUtil.from(this.stencilOperationFail), GLEnumUtil.from(this.stencilOperationPass), GLEnumUtil.from(this.stencilOperationDepthFail));

        api.glUseProgram(this.program);

        this.shaderStorageBufferBindings.forEach((index, buffer) -> api.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, buffer));
        this.uniformBufferBindings.forEach((index, buffer) -> api.glBindBufferBase(GL_UNIFORM_BUFFER, index, buffer));
    }

    public State withShaderStorageBuffer(int index, int buffer) {
        return this.withShaderStorageBufferBindings(cowImmutableMapPut(this.shaderStorageBufferBindings, index, buffer));
    }

    public State withUniformBuffer(int index, int buffer) {
        return this.withUniformBufferBindings(cowImmutableMapPut(this.uniformBufferBindings, index, buffer));
    }
}
