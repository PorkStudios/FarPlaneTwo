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

package net.daporkchop.fp2.mode.common.client.strategy;

import lombok.NonNull;
import net.daporkchop.fp2.client.GlStateUniformAttributes;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.attribute.global.DrawGlobalFormat;
import net.daporkchop.fp2.gl.attribute.local.DrawLocalFormat;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.draw.command.DrawCommandBuffer;
import net.daporkchop.fp2.gl.draw.command.DrawCommandIndexed;
import net.daporkchop.fp2.gl.draw.index.IndexFormat;
import net.daporkchop.fp2.gl.draw.index.IndexWriter;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.mode.common.client.bake.indexed.IndexedBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.index.CPUCulledRenderIndex;
import net.daporkchop.fp2.mode.common.client.index.GPUCulledRenderIndex;
import net.daporkchop.fp2.mode.common.client.index.IRenderIndex;
import net.daporkchop.lib.common.util.PArrays;

import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractMultipassIndexedRenderStrategy<POS extends IFarPos, T extends IFarTile, SG, SL> extends AbstractRenderStrategy<POS, T, IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> implements IMultipassRenderStrategy<POS, T, IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> {
    public AbstractMultipassIndexedRenderStrategy(@NonNull IFarRenderMode<POS, T> mode, @NonNull GL gl) {
        super(mode, gl);
    }

    public abstract IndexFormat indexFormat();

    public abstract DrawGlobalFormat<SG> globalFormat();

    public abstract DrawLocalFormat<SL> vertexFormat();

    @Override
    public IRenderIndex<POS, IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> createIndex() {
        return false && FP2Config.global().performance().gpuFrustumCulling()
                ? new GPUCulledRenderIndex<>(this)
                : new CPUCulledRenderIndex<>(this);
    }

    @Override
    public IndexedBakeOutput<SG, SL> createBakeOutput() {
        return new IndexedBakeOutput<>(this.globalFormat().createWriter(), this.vertexFormat().createWriter(), PArrays.filledFrom(RENDER_PASS_COUNT, IndexWriter[]::new, this.indexFormat()::createWriter));
    }

    @Override
    public IBakeOutputStorage<IndexedBakeOutput<SG, SL>, DrawBindingIndexed, DrawCommandIndexed> createBakeOutputStorage() {
        return new IndexedBakeOutputStorage<>(this.alloc, this.uniformBuffer, this.globalFormat(), this.vertexFormat(), this.indexFormat(), RENDER_PASS_COUNT);
    }

    @Override
    public DrawCommandBuffer<DrawCommandIndexed> createCommandBuffer(@NonNull DrawBindingIndexed binding) {
        return this.gl.createCommandBuffer()
                .forIndexed(binding)
                .build();
    }

    @Override
    public DrawBindingBuilder<DrawBindingIndexed> configureDrawBinding(@NonNull DrawBindingBuilder<DrawBindingIndexed> builder) {
        return builder
                .withUniformArrays(this.textureUVs.listsBuffer())
                .withUniformArrays(this.textureUVs.quadsBuffer());
    }

    @Override
    public void preRender() {
        super.preRender();
        IMultipassRenderStrategy.super.preRender();

        this.uniformBuffer.set(new GlStateUniformAttributes().initFromGlState(MC.getRenderPartialTicks(), MC));
    }
}
