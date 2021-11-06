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
import net.daporkchop.fp2.client.gl.DrawMode;
import net.daporkchop.fp2.client.gl.ElementType;
import net.daporkchop.fp2.client.gl.command.IMultipassDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.command.elements.DrawElementsCommand;
import net.daporkchop.fp2.client.gl.command.elements.buffer.MultidrawIndirectMultipassDrawElementsCommandBuffer;
import net.daporkchop.fp2.client.gl.command.elements.buffer.SingleDrawIndirectMultipassDrawElementsCommandBuffer;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.binding.DrawBindingIndexed;
import net.daporkchop.fp2.gl.command.DrawCommandBuffer;
import net.daporkchop.fp2.gl.command.DrawCommandIndexed;
import net.daporkchop.fp2.gl.index.IndexFormat;
import net.daporkchop.fp2.gl.index.IndexWriter;
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

import static net.daporkchop.fp2.client.gl.GLCompatibilityHelper.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractMultipassIndexedRenderStrategy<POS extends IFarPos, T extends IFarTile> extends AbstractRenderStrategy<POS, T, IndexedBakeOutput, DrawBindingIndexed, DrawCommandIndexed> implements IMultipassRenderStrategy<POS, T, IndexedBakeOutput, DrawBindingIndexed, DrawCommandIndexed> {
    public AbstractMultipassIndexedRenderStrategy(@NonNull IFarRenderMode<POS, T> mode, @NonNull GL gl) {
        super(mode, gl);
    }

    public abstract IndexFormat indexFormat();

    @Override
    public IRenderIndex<POS, IndexedBakeOutput, DrawBindingIndexed, DrawCommandIndexed> createIndex() {
        return FP2Config.global().performance().gpuFrustumCulling()
                ? new GPUCulledRenderIndex<>(this)
                : new CPUCulledRenderIndex<>(this);
    }

    @Override
    public IndexedBakeOutput createBakeOutput() {
        return new IndexedBakeOutput(this.globalFormat().createGlobalWriter(), this.vertexFormat().createLocalWriter(), PArrays.filledFrom(RENDER_PASS_COUNT, IndexWriter[]::new, this.indexFormat()::createWriter));
    }

    @Override
    public IBakeOutputStorage<IndexedBakeOutput, DrawBindingIndexed, DrawCommandIndexed> createBakeOutputStorage() {
        return new IndexedBakeOutputStorage(this.alloc, this.vertexFormat(), this.indexFormat(), RENDER_PASS_COUNT);
    }

    @Override
    public DrawCommandBuffer<DrawCommandIndexed> createCommandBuffer(@NonNull DrawBindingIndexed binding) {
        return this.gl.createCommandBuffer()
                .forIndexed(binding)
                .build();
    }
}
