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

package net.daporkchop.fp2.core.mode.common.client.strategy;

import lombok.NonNull;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.draw.list.DrawList;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.core.mode.common.client.bake.IBakeOutput;
import net.daporkchop.fp2.core.mode.common.client.bake.IBakeOutputStorage;
import net.daporkchop.fp2.core.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.core.mode.common.client.index.IRenderIndex;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

/**
 * @author DaPorkchop_
 */
public interface IFarRenderStrategy<POS extends IFarPos, T extends IFarTile, BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends RefCounted {
    IFarRenderMode<POS, T> mode();

    ICullingStrategy cullingStrategy();

    GL gl();

    DrawLayout drawLayout();

    IRenderIndex<POS, BO, DB, DC> createIndex();

    IRenderBaker<POS, T, BO> createBaker();

    BO createBakeOutput();

    IBakeOutputStorage<BO, DB, DC> createBakeOutputStorage();

    DrawList<DC> createCommandBuffer(@NonNull DB binding);

    DrawBindingBuilder<DB> configureDrawBinding(@NonNull DrawBindingBuilder<DB> builder);

    void render(@NonNull IRenderIndex<POS, BO, DB, DC> index, int layer, boolean pre);

    @Override
    int refCnt();

    @Override
    IFarRenderStrategy<POS, T, BO, DB, DC> retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;
}
