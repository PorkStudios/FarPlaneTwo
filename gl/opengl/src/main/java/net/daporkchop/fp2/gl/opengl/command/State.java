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
import net.daporkchop.fp2.gl.command.BlendFactor;
import net.daporkchop.fp2.gl.command.BlendOp;
import net.daporkchop.fp2.gl.command.Comparison;
import net.daporkchop.fp2.gl.command.StencilOperation;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.fp2.gl.opengl.GLEnumUtil;
import net.daporkchop.fp2.gl.opengl.attribute.texture.TextureTarget;
import net.daporkchop.fp2.gl.opengl.buffer.BufferTarget;
import net.daporkchop.lib.common.misc.Tuple;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

        Map<K, V> tmp = new LinkedHashMap<>(map);
        tmp.put(key, value);
        return ImmutableMap.copyOf(tmp);
    }

    public static void generateStateChange(@NonNull MethodVisitor mv, int apiLvtIndex, @NonNull State prevState, @NonNull State nextState) {
        //blend
        if (prevState.blend != nextState.blend) { //toggle blend if it changed
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GL_BLEND);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), nextState.stencil ? "glEnable" : "glDisable", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
        if (nextState.blend) { //blend is enabled, update the state
            if (prevState.blendFactorSrcRGB != nextState.blendFactorSrcRGB || prevState.blendFactorSrcA != nextState.blendFactorSrcA || prevState.blendFactorDstRGB != nextState.blendFactorDstRGB || prevState.blendFactorDstA != nextState.blendFactorDstA) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(nextState.blendFactorSrcRGB));
                mv.visitLdcInsn(GLEnumUtil.from(nextState.blendFactorDstRGB));
                mv.visitLdcInsn(GLEnumUtil.from(nextState.blendFactorSrcA));
                mv.visitLdcInsn(GLEnumUtil.from(nextState.blendFactorDstA));
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBlendFuncSeparate", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
            }
            if (prevState.blendOpRGB != nextState.blendOpRGB || prevState.blendOpA != nextState.blendOpA) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(nextState.blendOpRGB));
                mv.visitLdcInsn(GLEnumUtil.from(nextState.blendOpA));
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBlendEquationSeparate", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE), true);
            }
            if (prevState.blendColorR != nextState.blendColorR || prevState.blendColorG != nextState.blendColorG || prevState.blendColorB != nextState.blendColorB || prevState.blendColorA != nextState.blendColorA) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(nextState.blendColorR);
                mv.visitLdcInsn(nextState.blendColorG);
                mv.visitLdcInsn(nextState.blendColorB);
                mv.visitLdcInsn(nextState.blendColorA);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBlendColor", getMethodDescriptor(VOID_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE), true);
            }
        }

        //color
        if (prevState.colorClearR != nextState.colorClearR || prevState.colorClearG != nextState.colorClearG || prevState.colorClearB != nextState.colorClearB || prevState.colorClearA != nextState.colorClearA) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(nextState.colorClearR);
            mv.visitLdcInsn(nextState.colorClearG);
            mv.visitLdcInsn(nextState.colorClearB);
            mv.visitLdcInsn(nextState.colorClearA);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glClearColor", getMethodDescriptor(VOID_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE, FLOAT_TYPE), true);
        }
        if (prevState.colorMask != nextState.colorMask) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn((nextState.colorMask & 1) != 0);
            mv.visitLdcInsn((nextState.colorMask & 2) != 0);
            mv.visitLdcInsn((nextState.colorMask & 4) != 0);
            mv.visitLdcInsn((nextState.colorMask & 8) != 0);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glColorMask", getMethodDescriptor(VOID_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE, BOOLEAN_TYPE), true);
        }

        //depth
        if (prevState.depth != nextState.depth) { //toggle depth test if it changed
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GL_DEPTH_TEST);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), nextState.stencil ? "glEnable" : "glDisable", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
        if (prevState.depthClearValue != nextState.depthClearValue) { //depth clear value can be used even if depth testing is disabled
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(nextState.depthClearValue);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glClearDepth", getMethodDescriptor(VOID_TYPE, DOUBLE_TYPE), true);
        }
        if (nextState.depth) { //depth test is enabled, update the state
            if (prevState.depthCompare != nextState.depthCompare) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(nextState.depthCompare));
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDepthFunc", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
            }
            if (prevState.depthWriteMask != nextState.depthWriteMask) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(nextState.depthWriteMask);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glDepthMask", getMethodDescriptor(VOID_TYPE, BOOLEAN_TYPE), true);
            }
        }

        //stencil
        if (prevState.stencil != nextState.stencil) { //toggle stencil test if it changed
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(GL_STENCIL_TEST);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), nextState.stencil ? "glEnable" : "glDisable", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
        if (prevState.stencilClearValue != nextState.stencilClearValue) { //stencil clear value and write mask can be used even if stencil testing is disabled
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(nextState.stencilClearValue);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glClearStencil", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
        if (prevState.stencilWriteMask != nextState.stencilWriteMask) {
            mv.visitVarInsn(ALOAD, apiLvtIndex);
            mv.visitLdcInsn(nextState.stencilWriteMask);
            mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glStencilMask", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);
        }
        if (nextState.stencil) { //stencil is enabled, update the state
            if (prevState.stencilCompare != nextState.stencilCompare || prevState.stencilReference != nextState.stencilReference || prevState.stencilCompareMask != nextState.stencilCompareMask) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(nextState.stencilCompare));
                mv.visitLdcInsn(nextState.stencilReference);
                mv.visitLdcInsn(nextState.stencilCompareMask);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glStencilFunc", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
            }
            if (prevState.stencilOperationFail != nextState.stencilOperationFail || prevState.stencilOperationPass != nextState.stencilOperationPass || prevState.stencilOperationDepthFail != nextState.stencilOperationDepthFail) {
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GLEnumUtil.from(nextState.stencilOperationFail));
                mv.visitLdcInsn(GLEnumUtil.from(nextState.stencilOperationPass));
                mv.visitLdcInsn(GLEnumUtil.from(nextState.stencilOperationDepthFail));
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glStencilOp", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE, INT_TYPE), true);
            }
        }

        //simple bindings
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

        //buffer bindings
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

        //texture bindings
        nextState.textureBindings.forEach((unit, nextTuple) -> {
            Tuple<TextureTarget, Integer> prevTuple = prevState.textureBindings.get(unit);
            if (!Objects.equals(prevTuple, nextTuple)) {
                //switch texture units
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GL_TEXTURE0 + unit);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glActiveTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);

                if (prevTuple != null && prevTuple.getA() != nextTuple.getA()) { //different targets, unbind old texture
                    mv.visitVarInsn(ALOAD, apiLvtIndex);
                    mv.visitLdcInsn(prevTuple.getA().target());
                    mv.visitLdcInsn(0);
                    mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE), true);
                }

                //bind new texture
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(nextTuple.getA().target());
                mv.visitLdcInsn(nextTuple.getB());
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE), true);
            }
        });
        prevState.textureBindings.forEach((unit, prevTuple) -> {
            if (!nextState.textureBindings.containsKey(unit)) { //unit is no longer present in new state, unbind old texture
                //switch texture units
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(GL_TEXTURE0 + unit);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glActiveTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE), true);

                //unbind old texture
                mv.visitVarInsn(ALOAD, apiLvtIndex);
                mv.visitLdcInsn(prevTuple.getA().target());
                mv.visitLdcInsn(0);
                mv.visitMethodInsn(INVOKEINTERFACE, getInternalName(GLAPI.class), "glBindTexture", getMethodDescriptor(VOID_TYPE, INT_TYPE, INT_TYPE), true);
            }
        });
    }

    //blend
    private final boolean blend;
    private final BlendFactor blendFactorSrcRGB;
    private final BlendFactor blendFactorSrcA;
    private final BlendFactor blendFactorDstRGB;
    private final BlendFactor blendFactorDstA;
    private final BlendOp blendOpRGB;
    private final BlendOp blendOpA;
    private final float blendColorR;
    private final float blendColorG;
    private final float blendColorB;
    private final float blendColorA;

    //color
    private final float colorClearR;
    private final float colorClearG;
    private final float colorClearB;
    private final float colorClearA;
    private final int colorMask;

    //depth
    private final boolean depth;
    private final double depthClearValue;
    private final boolean depthWriteMask;
    private final Comparison depthCompare;

    //stencil
    private final boolean stencil;
    private final int stencilClearValue;
    private final int stencilWriteMask;
    private final int stencilCompareMask;
    private final int stencilReference;
    private final Comparison stencilCompare;
    private final StencilOperation stencilOperationFail;
    private final StencilOperation stencilOperationPass;
    private final StencilOperation stencilOperationDepthFail;

    //simple bindings
    private final int program;
    private final int vao;

    //buffer bindings
    @With(AccessLevel.PRIVATE)
    private final Map<Integer, Integer> shaderStorageBufferBindings;
    @With(AccessLevel.PRIVATE)
    private final Map<Integer, Integer> uniformBufferBindings;

    //texture bindings
    private final Map<Integer, Tuple<TextureTarget, Integer>> textureBindings;

    private State() {
        this.blend = false;
        this.blendFactorSrcRGB = BlendFactor.ONE;
        this.blendFactorSrcA = BlendFactor.ONE;
        this.blendFactorDstRGB = BlendFactor.ZERO;
        this.blendFactorDstA = BlendFactor.ZERO;
        this.blendOpRGB = BlendOp.ADD;
        this.blendOpA = BlendOp.ADD;
        this.blendColorR = 0.0f;
        this.blendColorG = 0.0f;
        this.blendColorB = 0.0f;
        this.blendColorA = 0.0f;

        this.colorClearR = 0.0f;
        this.colorClearG = 0.0f;
        this.colorClearB = 0.0f;
        this.colorClearA = 0.0f;
        this.colorMask = 0xF;

        this.depth = false;
        this.depthClearValue = 1.0d;
        this.depthWriteMask = true;
        this.depthCompare = Comparison.LESS;

        this.stencil = false;
        this.stencilClearValue = 0;
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

        this.textureBindings = ImmutableMap.of();
    }

    /**
     * Sets the current GL state to the state described by this instance.
     *
     * @param api the {@link GLAPI} instance
     */
    public void set(@NonNull GLAPI api) {
        if (this.blend) {
            api.glEnable(GL_BLEND);
        } else {
            api.glDisable(GL_BLEND);
        }
        api.glBlendFuncSeparate(GLEnumUtil.from(this.blendFactorSrcRGB), GLEnumUtil.from(this.blendFactorDstRGB), GLEnumUtil.from(this.blendFactorSrcA), GLEnumUtil.from(this.blendFactorDstA));
        api.glBlendEquationSeparate(GLEnumUtil.from(this.blendOpRGB), GLEnumUtil.from(this.blendOpA));
        api.glBlendColor(this.blendColorR, this.blendColorG, this.blendColorG, this.blendColorA);

        api.glClearColor(this.colorClearR, this.colorClearG, this.colorClearB, this.colorClearA);
        api.glColorMask((this.colorMask & 1) != 0, (this.colorMask & 2) != 0, (this.colorMask & 4) != 0, (this.colorMask & 8) != 0);

        if (this.depth) {
            api.glEnable(GL_DEPTH_TEST);
        } else {
            api.glDisable(GL_DEPTH_TEST);
        }
        api.glClearDepth(this.depthClearValue);
        api.glDepthMask(this.depthWriteMask);
        api.glDepthFunc(GLEnumUtil.from(this.depthCompare));

        if (this.stencil) {
            api.glEnable(GL_STENCIL_TEST);
        } else {
            api.glDisable(GL_STENCIL_TEST);
        }
        api.glClearStencil(this.stencilClearValue);
        api.glStencilFunc(GLEnumUtil.from(this.stencilCompare), this.stencilReference, this.stencilCompareMask);
        api.glStencilMask(this.stencilWriteMask);
        api.glStencilOp(GLEnumUtil.from(this.stencilOperationFail), GLEnumUtil.from(this.stencilOperationPass), GLEnumUtil.from(this.stencilOperationDepthFail));

        api.glUseProgram(this.program);
        api.glBindVertexArray(this.vao);

        this.shaderStorageBufferBindings.forEach((index, buffer) -> api.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, buffer));
        this.uniformBufferBindings.forEach((index, buffer) -> api.glBindBufferBase(GL_UNIFORM_BUFFER, index, buffer));
    }

    public State withShaderStorageBuffer(int index, int buffer) {
        return this.withShaderStorageBufferBindings(cowImmutableMapPut(this.shaderStorageBufferBindings, index, buffer));
    }

    public State withUniformBuffer(int index, int buffer) {
        return this.withUniformBufferBindings(cowImmutableMapPut(this.uniformBufferBindings, index, buffer));
    }

    public State withTexture(int unit, @NonNull TextureTarget target, int texture) {
        return this.withTextureBindings(cowImmutableMapPut(this.textureBindings, unit, new Tuple<>(target, texture)));
    }
}
