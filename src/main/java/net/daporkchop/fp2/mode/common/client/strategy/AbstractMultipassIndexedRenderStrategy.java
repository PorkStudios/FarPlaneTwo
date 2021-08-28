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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.DrawMode;
import net.daporkchop.fp2.client.gl.ElementType;
import net.daporkchop.fp2.client.gl.command.IMultipassDrawCommandBuffer;
import net.daporkchop.fp2.client.gl.command.elements.DrawElementsCommand;
import net.daporkchop.fp2.client.gl.command.elements.IndirectMultipassDrawElementsCommandBuffer;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.bake.IMultipassBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.bake.indexed.MultipassIndexedBakeOutput;
import net.daporkchop.fp2.mode.common.client.bake.indexed.MultipassIndexedBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.index.CPUCulledRenderIndex;
import net.daporkchop.fp2.mode.common.client.index.GPUCulledRenderIndex;
import net.daporkchop.fp2.mode.common.client.index.IRenderIndex;
import net.daporkchop.lib.common.util.PArrays;

import java.util.function.Supplier;

import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractMultipassIndexedRenderStrategy<POS extends IFarPos, T extends IFarTile> extends AbstractRenderStrategy<POS, T, MultipassIndexedBakeOutput, DrawElementsCommand> {
    public AbstractMultipassIndexedRenderStrategy(@NonNull IFarRenderMode<POS, T> mode, @NonNull VertexFormat vertexFormat) {
        super(mode, vertexFormat);
    }

    @Override
    public IRenderIndex<POS, MultipassIndexedBakeOutput, DrawElementsCommand> createIndex() {
        return FP2Config.performance.gpuFrustumCulling ? new GPUCulledRenderIndex<>(this) : new CPUCulledRenderIndex<>(this);
    }

    @Override
    public MultipassIndexedBakeOutput createBakeOutput() {
        return new MultipassIndexedBakeOutput(this.vertexLayout.createBuilder(), PArrays.filled(RENDER_PASS_COUNT, ByteBuf[]::new, (Supplier<ByteBuf>) ByteBufAllocator.DEFAULT::directBuffer));
    }

    @Override
    public IMultipassBakeOutputStorage<MultipassIndexedBakeOutput, DrawElementsCommand> createBakeOutputStorage() {
        return new MultipassIndexedBakeOutputStorage(this.alloc, this.vertexLayout, ElementType.UNSIGNED_SHORT, RENDER_PASS_COUNT);
    }

    @Override
    public IMultipassDrawCommandBuffer<DrawElementsCommand> createCommandBuffer() {
        return new IndirectMultipassDrawElementsCommandBuffer(this.alloc, RENDER_PASS_COUNT, DrawMode.QUADS, ElementType.UNSIGNED_SHORT);
    }
}
