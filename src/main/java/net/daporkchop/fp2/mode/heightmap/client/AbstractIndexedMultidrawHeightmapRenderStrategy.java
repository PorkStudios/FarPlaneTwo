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

package net.daporkchop.fp2.mode.heightmap.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.indirect.elements.DrawElementsIndirectCommand;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuilder;
import net.daporkchop.fp2.mode.common.client.BakeOutput;
import net.daporkchop.fp2.mode.common.client.strategy.IndexedMultidrawRenderStrategy;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.util.alloc.DirectMemoryAllocator;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractIndexedMultidrawHeightmapRenderStrategy extends IndexedMultidrawRenderStrategy<HeightmapPos, HeightmapTile> implements IMultipassHeightmapRenderStrategy<DrawElementsIndirectCommand> {
    public AbstractIndexedMultidrawHeightmapRenderStrategy() {
        super(new DirectMemoryAllocator(), HeightmapBake.VERTEX_FORMAT);
    }

    @Override
    protected void bakeVertsAndIndices(@NonNull HeightmapPos pos, @NonNull HeightmapTile[] srcs, @NonNull BakeOutput output, @NonNull IVertexBuilder verts, @NonNull ByteBuf[] indices) {
        HeightmapBake.bakeForShaderDraw(pos, srcs, verts, indices);
    }
}
