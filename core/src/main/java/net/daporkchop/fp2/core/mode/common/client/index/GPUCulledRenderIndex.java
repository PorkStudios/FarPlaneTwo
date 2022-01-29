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

package net.daporkchop.fp2.core.mode.common.client.index;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.list.DrawListBuilder;
import net.daporkchop.fp2.gl.draw.list.selected.ShaderSelectedDrawList;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.core.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.core.mode.common.client.strategy.IFarRenderStrategy;

/**
 * Implementation of {@link AbstractRenderIndex} which does frustum culling in a compute shader on the GPU.
 *
 * @author DaPorkchop_
 */
public class GPUCulledRenderIndex<POS extends IFarPos, BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends AbstractRenderIndex<POS, BO, DB, DC, ShaderSelectedDrawList<DC>> {
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

    //protected static final ComputeLocalSize WORK_GROUP_SIZE = getOptimalComputeWorkSizePow2(null, MAX_COMPUTE_WORK_GROUP_SIZE);
    //protected static final Allocator.GrowFunction GROW_FUNCTION = Allocator.GrowFunction.pow2(max(WORK_GROUP_SIZE.count(), MIN_CAPACITY));

    protected static final int POSITIONS_BUFFER_BINDING_INDEX = 3;
    protected static final int COMMANDS_BUFFER_BINDING_INDEX = 4;

    public <T extends IFarTile> GPUCulledRenderIndex(@NonNull IFarRenderStrategy<POS, T, BO, DB, DC> strategy) {
        super(strategy);
    }

    @Override
    protected AbstractRenderIndex<POS, BO, DB, DC, ShaderSelectedDrawList<DC>>.Level createLevel(int level) {
        return new Level(level);
    }

    @Override
    public void select(@NonNull IFrustum frustum) {
        /*ShaderClippingStateHelper.update(frustum);
        ShaderClippingStateHelper.bind();*/

        for (AbstractRenderIndex.Level level : this.levels) {
            level.select(frustum);
        }

        /*glBindBufferBase(GL_SHADER_STORAGE_BUFFER, POSITIONS_BUFFER_BINDING_INDEX, 0);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, COMMANDS_BUFFER_BINDING_INDEX, 0);*/
    }

    /**
     * @author DaPorkchop_
     */
    protected class Level extends AbstractRenderIndex<POS, BO, DB, DC, ShaderSelectedDrawList<DC>>.Level {
        //protected final ComputeShaderProgram cullShader;

        public Level(int level) {
            super(level, /*TODO: GROW_FUNCTION*/ null);

            //TODO: AAAAA
            /*ComputeShaderBuilder cullShaderBuilder = GPUCulledRenderIndex.this.cullingStrategy.cullShaderBuilder()
                    .withWorkGroupSize(WORK_GROUP_SIZE)
                    .define("LEVEL_0", this.level == 0);*/

            //TODO: this.cullShader = this.commandBuffer.configureShader(cullShaderBuilder).link();
        }

        @Override
        protected ShaderSelectedDrawList<DC> buildCommandBuffer(@NonNull DrawListBuilder<DC> builder) {
            return builder.buildShaderSelected();
        }

        @Override
        protected void select0(@NonNull IFrustum frustum) {
            //bind SSBOs
            //TODO: this.positionsBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, POSITIONS_BUFFER_BINDING_INDEX);

            if (this.level == 0) { //level-0: we should bind the vanilla renderability info (for use in shader) and use the level-0 shader
                //((IMixinRenderGlobal) MC.renderGlobal).fp2_vanillaRenderabilityTracker().bindForShaderUse();
            }

            //TODO: all of this
            /*try (ComputeShaderProgram cullShader = this.cullShader.use()) { //do frustum culling
                this.commandBuffer.select(cullShader);
            }*/
        }

        @Override
        protected void draw(@NonNull CommandBufferBuilder builder, @NonNull DrawShaderProgram shader, @NonNull DrawMode mode, @NonNull ShaderSelectedDrawList<DC> list, int pass) {
            throw new UnsupportedOperationException(); //TODO
        }
    }
}
