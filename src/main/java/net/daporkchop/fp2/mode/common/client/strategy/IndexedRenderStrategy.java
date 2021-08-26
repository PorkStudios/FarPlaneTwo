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
import io.netty.buffer.CompositeByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.AllocatedGLBuffer;
import net.daporkchop.fp2.client.gl.indirect.IDrawIndirectCommandBufferFactory;
import net.daporkchop.fp2.client.gl.indirect.elements.DrawElementsIndirectCommand;
import net.daporkchop.fp2.client.gl.indirect.elements.DrawElementsIndirectCommandBufferFactory;
import net.daporkchop.fp2.client.gl.vertex.attribute.VertexFormat;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuilder;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.BakeOutput;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class IndexedRenderStrategy<POS extends IFarPos, T extends IFarTile> extends BaseRenderStrategy<POS, T, DrawElementsIndirectCommand> {
    /*
     * struct RenderData {
     *   [...] //inherited from BaseRenderStrategy
     *   u64 indexOffset; //offset of index data from the beginning of gpu memory, in multiples of index size
     *   u32 indexCount[RENDER_PASS_COUNT]; //number of indices used by each render pass
     * };
     */

    protected static final long _RENDERDATA_INDEXOFFSET_OFFSET = BaseRenderStrategy._RENDERDATA_SIZE;
    protected static final long _RENDERDATA_INDEXCOUNT_OFFSET = _RENDERDATA_INDEXOFFSET_OFFSET + LONG_SIZE;

    protected static final long _RENDERDATA_SIZE = _RENDERDATA_INDEXCOUNT_OFFSET + INT_SIZE * RENDER_PASS_COUNT;

    protected static long _renderdata_indexOffset(long renderData) {
        return PUnsafe.getLong(renderData + _RENDERDATA_INDEXOFFSET_OFFSET);
    }

    protected static void _renderdata_indexOffset(long renderData, long indexOffset) {
        PUnsafe.putLong(renderData + _RENDERDATA_INDEXOFFSET_OFFSET, indexOffset);
    }

    protected static int _renderdata_indexCount(long renderData, int renderPass) {
        return PUnsafe.getInt(renderData + _RENDERDATA_INDEXCOUNT_OFFSET + INT_SIZE * renderPass);
    }

    protected static void _renderdata_indexCount(long renderData, int renderPass, int indexCount) {
        PUnsafe.putInt(renderData + _RENDERDATA_INDEXCOUNT_OFFSET + INT_SIZE * renderPass, indexCount);
    }

    protected final AllocatedGLBuffer indices = AllocatedGLBuffer.create("indices", GL_DYNAMIC_DRAW, INDEX_SIZE, true);

    public IndexedRenderStrategy(@NonNull Allocator alloc, @NonNull VertexFormat vertexFormat) {
        super(alloc, vertexFormat);
    }

    @Override
    public IDrawIndirectCommandBufferFactory<DrawElementsIndirectCommand> createCommandBufferFactory() {
        return new DrawElementsIndirectCommandBufferFactory(GL_QUADS, INDEX_TYPE);
    }

    @Override
    public long renderDataSize() {
        return _RENDERDATA_SIZE;
    }

    @Override
    public void deleteRenderData(long renderData) {
        super.deleteRenderData(renderData);

        this.indices.free(_renderdata_indexOffset(renderData) * INDEX_SIZE);
    }

    @Override
    protected void bakeVerts(@NonNull POS pos, @NonNull T[] srcs, @NonNull BakeOutput output, @NonNull IVertexBuilder verts) {
        ByteBuf[] indices = new ByteBuf[RENDER_PASS_COUNT];
        for (int i = 0; i < RENDER_PASS_COUNT; i++) {
            indices[i] = ByteBufAllocator.DEFAULT.directBuffer();
        }
        try {
            this.bakeVertsAndIndices(pos, srcs, output, verts, indices);

            if (verts.size() == 0) { //there are no vertices, meaning nothing to draw
                return;
            }

            int totalIndexCount = 0;
            for (int i = 0; i < RENDER_PASS_COUNT; i++) {
                int indexCount = indices[i].readableBytes() >> INDEX_SHIFT;
                _renderdata_indexCount(output.renderData, i, indexCount);
                totalIndexCount += indexCount;
            }

            if (totalIndexCount == 0) { //there are no indices, meaning nothing to draw
                verts.clear(); //clear vertex output buffer to ensure BaseRenderStrategy#bake reports that the tile is empty
                return;
            }

            CompositeByteBuf merged = ByteBufAllocator.DEFAULT.compositeDirectBuffer(RENDER_PASS_COUNT);
            for (ByteBuf buf : indices) {
                if (buf.isReadable()) {
                    merged.addComponent(true, buf.retain());
                }
            }

            output.uploadAndStoreAddress(merged, this.indices, IndexedRenderStrategy::_renderdata_indexOffset, INDEX_SIZE);
        } finally {
            for (ByteBuf buf : indices) {
                buf.release();
            }
        }
    }

    protected abstract void bakeVertsAndIndices(@NonNull POS pos, @NonNull T[] srcs, @NonNull BakeOutput output, @NonNull IVertexBuilder verts, @NonNull ByteBuf[] indices);

    @Override
    protected void doRelease() {
        super.doRelease();

        this.indices.delete();
    }
}
