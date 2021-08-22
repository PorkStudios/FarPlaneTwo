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

package net.daporkchop.fp2.mode.common.client.index;

import lombok.NonNull;
import net.daporkchop.fp2.asm.interfaz.client.renderer.IMixinRenderGlobal;
import net.daporkchop.fp2.client.ShaderClippingStateHelper;
import net.daporkchop.fp2.client.gl.WorkGroupSize;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommand;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommandBuffer;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.shader.ComputeShaderBuilder;
import net.daporkchop.fp2.client.gl.shader.ComputeShaderProgram;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.util.alloc.Allocator;

import java.util.function.Consumer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * Implementation of {@link AbstractRenderIndex} which does frustum culling in a compute shader on the GPU.
 *
 * @author DaPorkchop_
 */
public class GPUCulledRenderIndex<POS extends IFarPos, C extends IDrawIndirectCommand> extends AbstractRenderIndex<POS, C, GPUCulledRenderIndex<POS, C>, GPUCulledRenderIndex.Level<POS, C>> {
    /**
     * The maximum permitted compute work group size.
     * <p>
     * Currently set to {@code 64}, which is nice because it's not too big, is a power of 2, and is also equal to 4³ and 8².
     */
    protected static final int MAX_COMPUTE_WORK_GROUP_SIZE = 64;

    /**
     * The minimum permitted total size.
     * <p>
     * This serves no purpose beyond the annoying fact that for some reason the Intel driver bugs out with multidraw when fewer than about 1024 commands are executed at
     * once, so we ensure it's always more than that.
     */
    protected static final long MIN_CAPACITY = 1024L;

    protected static final WorkGroupSize WORK_GROUP_SIZE = getOptimalComputeWorkSizePow2(null, MAX_COMPUTE_WORK_GROUP_SIZE);

    protected static final int POSITIONS_BUFFER_BINDING_INDEX = 3;
    protected static final int COMMANDS_BUFFER_BINDING_INDEX = 4;

    protected final ComputeShaderProgram cullShaderLevel0;
    protected final ComputeShaderProgram cullShader;

    public <T extends IFarTile> GPUCulledRenderIndex(@NonNull IFarRenderMode<POS, T> mode, @NonNull IFarRenderStrategy<POS, T, C> strategy, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray, @NonNull ComputeShaderBuilder cullShaderBuilder) {
        super(mode, strategy, vaoInitializer, elementArray);

        this.cullShaderLevel0 = cullShaderBuilder
                .withWorkGroupSize(WORK_GROUP_SIZE)
                .define("LEVEL_0")
                .link();
        this.cullShader = cullShaderBuilder
                .withWorkGroupSize(WORK_GROUP_SIZE)
                .link();
    }

    @Override
    protected Level<POS, C> createLevel(int level, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull IGLBuffer elementArray) {
        return new Level<>(this, vao -> {
            vaoInitializer.accept(vao);

            vao.putElementArray(elementArray);
        }, Allocator.GrowFunction.pow2(max(WORK_GROUP_SIZE.totalSize(), MIN_CAPACITY)), level);
    }

    @Override
    public void select(@NonNull IFrustum frustum, float partialTicks) {
        ShaderClippingStateHelper.update(frustum);
        ShaderClippingStateHelper.bind();

        for (Level level : this.levels) {
            level.select(frustum, partialTicks);
        }

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, POSITIONS_BUFFER_BINDING_INDEX, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, COMMANDS_BUFFER_BINDING_INDEX, 0);
    }

    /**
     * @author DaPorkchop_
     */
    protected static class Level<POS extends IFarPos, C extends IDrawIndirectCommand> extends AbstractRenderIndex.Level<POS, C, GPUCulledRenderIndex<POS, C>, Level<POS, C>> {
        protected final int level;

        public Level(@NonNull GPUCulledRenderIndex<POS, C> parent, @NonNull Consumer<VertexArrayObject> vaoInitializer, @NonNull Allocator.GrowFunction growFunction, int level) {
            super(parent, vaoInitializer, growFunction);

            this.level = level;
        }

        @Override
        protected IDrawIndirectCommandBuffer<C> createCommandBuffer(@NonNull GPUCulledRenderIndex<POS, C> parent) {
            //the command buffer must be on GPU memory in order for its contents to be accessible to the compute shader.
            //  we request a memory barrier to be placed in order to ensure that all compute shader invocations have completed before using the commands
            return parent.strategy.createCommandBufferFactory().commandBufferGPU(parent.directMemoryAlloc, true);
        }

        @Override
        protected void select0(@NonNull IFrustum frustum, float partialTicks) {
            //bind SSBOs
            this.positionsBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, POSITIONS_BUFFER_BINDING_INDEX);
            this.commandBuffer.openglBuffer().bindBase(GL_SHADER_STORAGE_BUFFER, COMMANDS_BUFFER_BINDING_INDEX);

            ComputeShaderProgram cullShader;
            if (this.level == 0) { //level-0: we should bind the vanilla renderability info (for use in shader) and use the level-0 shader
                ((IMixinRenderGlobal) mc.renderGlobal).fp2_vanillaRenderabilityTracker().bindForShaderUse();
                cullShader = this.parent.cullShaderLevel0;
            } else { //no need to bind any special resources, and we can use the standard shader
                cullShader = this.parent.cullShader;
            }

            try (ComputeShaderProgram boundCullShader = cullShader.use()) { //dispatch compute shader
                boundCullShader.dispatch(this.capacity);
            }
        }

        @Override
        protected void draw0(int pass) {
            //draw the buffered draw commands. we assume a memory barrier will be placed on GL_DRAW_INDIRECT_BUFFER, and that the buffer's contents will
            //  not have been changed since select0() ran.
            this.commandBuffer.draw(pass, RENDER_PASS_COUNT, this.capacity);
        }
    }
}
