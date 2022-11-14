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
 */

package net.daporkchop.fp2.core.mode.common.client.index;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.core.mode.common.client.strategy.IFarRenderStrategy;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.AttributeBuffer;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeUsage;
import net.daporkchop.fp2.gl.attribute.BufferUsage;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.list.DrawListBuilder;
import net.daporkchop.fp2.gl.draw.list.selected.ShaderSelectedDrawList;
import net.daporkchop.fp2.gl.draw.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.transform.TransformLayout;
import net.daporkchop.fp2.gl.transform.binding.TransformBinding;
import net.daporkchop.fp2.gl.transform.shader.TransformShaderProgram;

import java.util.stream.Stream;

/**
 * Implementation of {@link AbstractRenderIndex} which does frustum culling in a compute shader on the GPU.
 *
 * @author DaPorkchop_
 */
public class GPUCulledRenderIndex<POS extends IFarPos, BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends AbstractRenderIndex<POS, BO, DB, DC, ShaderSelectedDrawList<DC>> {
    protected static final Allocator.GrowFunction GROW_FUNCTION = Allocator.GrowFunction.pow2(64L);

    private AttributeFormat<IFrustum.ClippingPlanes> clippingPlanesUniformFormat;
    private AttributeBuffer<IFrustum.ClippingPlanes> clippingPlanesUniformBuffer;

    public <T extends IFarTile> GPUCulledRenderIndex(@NonNull IFarRenderStrategy<POS, T, BO, DB, DC> strategy) {
        super(strategy);
    }

    @Override
    protected void doRelease() {
        //close uniform buffer
        try (AttributeBuffer<IFrustum.ClippingPlanes> clippingPlanesUniformBuffer = this.clippingPlanesUniformBuffer) {
            super.doRelease();
        }
    }

    @Override
    protected AbstractRenderIndex<POS, BO, DB, DC, ShaderSelectedDrawList<DC>>.Level createLevel(int level) {
        return new Level(level);
    }

    @Override
    public void select(@NonNull IFrustum frustum) {
        //set clipping planes uniform
        try (IFrustum.ClippingPlanes clippingPlanes = this.clippingPlanesUniformBuffer().setToSingle()) {
            frustum.configureClippingPlanes(clippingPlanes);
        }
        assert this.clippingPlanesUniformBuffer.capacity() == 1;

        for (AbstractRenderIndex.Level level : this.levels) {
            level.select(frustum);
        }
    }

    private AttributeFormat<IFrustum.ClippingPlanes> clippingPlanesUniformFormat() {
        if (this.clippingPlanesUniformFormat != null) {
            return this.clippingPlanesUniformFormat;
        } else {
            return this.clippingPlanesUniformFormat = this.strategy.gl().createAttributeFormat(IFrustum.ClippingPlanes.class)
                    .useFor(AttributeUsage.UNIFORM)
                    .build();
        }
    }

    private AttributeBuffer<IFrustum.ClippingPlanes> clippingPlanesUniformBuffer() {
        if (this.clippingPlanesUniformBuffer != null) {
            return this.clippingPlanesUniformBuffer;
        } else {
            return this.clippingPlanesUniformBuffer = this.clippingPlanesUniformFormat().createBuffer(BufferUsage.STATIC_DRAW);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected class Level extends AbstractRenderIndex<POS, BO, DB, DC, ShaderSelectedDrawList<DC>>.Level {
        protected final TransformLayout selectionLayout;
        protected final TransformShaderProgram selectionShader;

        protected final TransformBinding[] selectionBindings;

        @SneakyThrows({ ShaderCompilationException.class, ShaderLinkageException.class })
        public Level(int level) {
            super(level, GROW_FUNCTION);

            GL gl = GPUCulledRenderIndex.this.strategy.gl();

            //we silently assume that all the command buffer's selection layouts are compatible with each other
            this.selectionLayout = GPUCulledRenderIndex.this.strategy.configureSelectionLayout(this.commandBuffers[0].configureTransformLayoutForSelection(gl.createTransformLayout()), level)
                    .withUniform(GPUCulledRenderIndex.this.clippingPlanesUniformFormat())
                    .build();
            this.selectionShader = this.commandBuffers[0].configureTransformShaderProgramForSelection(gl.createTransformShaderProgram(this.selectionLayout))
                    .addShader(GPUCulledRenderIndex.this.strategy.configureSelectionShader(this.commandBuffers[0].configureTransformShaderForSelection(gl.createTransformShader(this.selectionLayout)), level)
                            .build())
                    .build();

            this.selectionBindings = Stream.of(this.commandBuffers)
                    .map(list -> this.storage.createSelectionBinding(GPUCulledRenderIndex.this.strategy.configureSelectionBinding(list.configureTransformBindingForSelection(this.selectionLayout.createBinding()), level))
                            .withUniform(GPUCulledRenderIndex.this.clippingPlanesUniformBuffer())
                            .build())
                    .toArray(TransformBinding[]::new);
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
            builder.drawSelectedList(shader, mode, list, this.selectionShader, this.selectionBindings[pass]);
        }

        @Override
        public void close() {
            super.close();

            for (TransformBinding binding : this.selectionBindings) {
                binding.close();
            }
            this.selectionShader.close();
            this.selectionLayout.close();
        }
    }
}
