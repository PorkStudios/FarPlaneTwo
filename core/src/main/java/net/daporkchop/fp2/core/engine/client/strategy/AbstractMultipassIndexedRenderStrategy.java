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

package net.daporkchop.fp2.core.engine.client.strategy;

import lombok.NonNull;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.client.AbstractFarRenderer;
import net.daporkchop.fp2.core.engine.client.bake.IBakeOutputStorage;
import net.daporkchop.fp2.core.engine.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.core.engine.client.bake.indexed.IndexedBakeOutputStorage;
import net.daporkchop.fp2.core.engine.client.index.CPUCulledRenderIndex;
import net.daporkchop.fp2.core.engine.client.index.GPUCulledRenderIndex;
import net.daporkchop.fp2.core.engine.client.index.IRenderIndex;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.command.CommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.fp2.gl.draw.list.DrawCommandIndexed;
import net.daporkchop.fp2.gl.draw.list.DrawListBuilder;
import net.daporkchop.lib.common.util.PArrays;

import static net.daporkchop.fp2.core.FP2Core.*;
import static net.daporkchop.fp2.core.engine.client.RenderConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractMultipassIndexedRenderStrategy<SG, SL> extends AbstractRenderStrategy<IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> implements IMultipassRenderStrategy<IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> {
    public AbstractMultipassIndexedRenderStrategy(@NonNull AbstractFarRenderer farRenderer) {
        super(farRenderer);
    }

    public abstract IndexFormat indexFormat();

    public abstract AttributeFormat<SG> globalFormat();

    public abstract AttributeFormat<SL> vertexFormat();

    @Override
    public IRenderIndex<IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> createIndex() {
        return fp2().globalConfig().performance().gpuFrustumCulling()
                ? new GPUCulledRenderIndex<>(this)
                : new CPUCulledRenderIndex<>(this);
    }

    @Override
    public IndexedBakeOutput<SG, SL> createBakeOutput() {
        return new IndexedBakeOutput<>(this.globalFormat().createWriter(), this.vertexFormat().createWriter(), PArrays.filledFrom(RENDER_PASS_COUNT, IndexWriter[]::new, this.indexFormat()::createWriter));
    }

    @Override
    public IBakeOutputStorage<IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> createBakeOutputStorage() {
        return new IndexedBakeOutputStorage<>(this.alloc, this.globalFormat(), this.vertexFormat(), this.indexFormat(), RENDER_PASS_COUNT);
    }

    @Override
    public DrawListBuilder<DrawCommandIndexed> createCommandBuffer(@NonNull DrawBindingIndexed binding) {
        return this.gl.createDrawListIndexed(binding);
    }

    @Override
    public DrawBindingBuilder<DrawBindingIndexed> configureDrawBinding(@NonNull DrawBindingBuilder<DrawBindingIndexed> builder) {
        return builder
                .withUniform(this.uniformBuffer)
                .withUniformArray(this.textureUVs.listsBuffer())
                .withUniformArray(this.textureUVs.quadsBuffer())
                .withTexture(this.textureTerrain)
                .withTexture(this.textureLightmap);
    }

    @Override
    public void render(@NonNull CommandBufferBuilder builder, @NonNull IRenderIndex<IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> index) {
        IMultipassRenderStrategy.super.render(builder, index);
    }

    @Override
    public boolean shouldConfigChangeCauseReload(FP2Config prev, FP2Config next) {
        return IMultipassRenderStrategy.super.shouldConfigChangeCauseReload(prev, next);
    }
}
