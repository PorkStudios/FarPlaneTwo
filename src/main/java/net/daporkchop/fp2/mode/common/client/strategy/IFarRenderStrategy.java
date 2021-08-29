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
import net.daporkchop.fp2.client.gl.command.IDrawCommand;
import net.daporkchop.fp2.client.gl.command.IMultipassDrawCommandBuffer;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.mode.common.client.bake.IMultipassBakeOutputStorage;
import net.daporkchop.fp2.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.mode.common.client.index.IRenderIndex;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;
import net.minecraft.util.BlockRenderLayer;

/**
 * @author DaPorkchop_
 */
public interface IFarRenderStrategy<POS extends IFarPos, T extends IFarTile, B extends IBakeOutput, C extends IDrawCommand> extends RefCounted {
    IFarRenderMode<POS, T> mode();

    ICullingStrategy<POS> cullingStrategy();

    IRenderIndex<POS, B, C> createIndex();

    IRenderBaker<POS, T, B> createBaker();

    B createBakeOutput();

    IMultipassBakeOutputStorage<B, C> createBakeOutputStorage();

    IMultipassDrawCommandBuffer<C> createCommandBuffer();

    void render(@NonNull IRenderIndex<POS, B, C> index, @NonNull BlockRenderLayer layer, boolean pre);

    @Override
    int refCnt();

    @Override
    IFarRenderStrategy<POS, T, B, C> retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;
}
