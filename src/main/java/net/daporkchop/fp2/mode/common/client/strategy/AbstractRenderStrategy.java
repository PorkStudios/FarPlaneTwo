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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.command.IDrawCommand;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexLayout;
import net.daporkchop.fp2.client.gl.vertex.buffer.interleaved.InterleavedVertexLayout;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

/**
 * Base implementation of {@link IFarRenderStrategy}.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractRenderStrategy<POS extends IFarPos, T extends IFarTile, B extends IBakeOutput, C extends IDrawCommand> extends AbstractRefCounted implements IFarRenderStrategy<POS, T, B, C> {
    protected final Allocator alloc = new DirectMemoryAllocator();

    @Getter
    protected final IFarRenderMode<POS, T> mode;

    protected final VertexFormat vertexFormat;
    protected final IVertexLayout vertexLayout;

    public AbstractRenderStrategy(@NonNull IFarRenderMode<POS, T> mode, @NonNull VertexFormat vertexFormat) {
        this.mode = mode;

        this.vertexFormat = vertexFormat;
        this.vertexLayout = new InterleavedVertexLayout(this.alloc, vertexFormat);
    }

    @Override
    public IFarRenderStrategy<POS, T, B, C> retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        this.vertexLayout.release();
    }
}
