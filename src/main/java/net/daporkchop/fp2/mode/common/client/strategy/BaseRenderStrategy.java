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
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.AllocatedGLBuffer;
import net.daporkchop.fp2.client.gl.commandbuffer.IDrawCommandBuffer;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.common.client.BakeOutput;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseRenderStrategy<POS extends IFarPos, T extends IFarTile> extends AbstractReleasable implements IFarRenderStrategy<POS, T> {
    /*
     * struct RenderData {
     *   u64 vertexOffset; //offset of vertex data from the beginning of gpu memory, in multiples of vertex size
     *   u32 vertexCount; //the number of vertex elements
     * };
     */

    protected static final long _RENDERDATA_VERTEXOFFSET_OFFSET = 0L;
    protected static final long _RENDERDATA_VERTEXCOUNT_OFFSET = _RENDERDATA_VERTEXOFFSET_OFFSET + LONG_SIZE;

    protected static final long _RENDERDATA_SIZE = _RENDERDATA_VERTEXCOUNT_OFFSET + INT_SIZE;

    protected static long _renderdata_vertexOffset(long renderData) {
        return PUnsafe.getLong(renderData + _RENDERDATA_VERTEXOFFSET_OFFSET);
    }

    protected static void _renderdata_vertexOffset(long renderData, long vertexOffset) {
        PUnsafe.putLong(renderData + _RENDERDATA_VERTEXOFFSET_OFFSET, vertexOffset);
    }

    protected static int _renderdata_vertexCount(long renderData) {
        return PUnsafe.getInt(renderData + _RENDERDATA_VERTEXCOUNT_OFFSET);
    }

    protected static void _renderdata_vertexCount(long renderData, int vertexCount) {
        PUnsafe.putInt(renderData + _RENDERDATA_VERTEXCOUNT_OFFSET, vertexCount);
    }

    protected final AllocatedGLBuffer vertices;
    protected final int vertexSize;

    public BaseRenderStrategy(int vertexSize) {
        this.vertices = AllocatedGLBuffer.create("vertices", GL_DYNAMIC_DRAW, this.vertexSize = vertexSize, true);
    }

    protected abstract IDrawCommandBuffer createCommandBuffer();

    @Override
    public long renderDataSize() {
        return _RENDERDATA_SIZE;
    }

    @Override
    public void deleteRenderData(long renderData) {
        this.vertices.free(_renderdata_vertexOffset(renderData) * this.vertexSize);
    }

    @Override
    public boolean bake(@NonNull POS pos, @NonNull T[] srcs, @NonNull BakeOutput output) {
        ByteBuf verts = ByteBufAllocator.DEFAULT.directBuffer();
        try {
            this.bakeVerts(pos, srcs, output, verts);

            int vertexCount = verts.readableBytes() / this.vertexSize;

            if (vertexCount == 0) {
                return false;
            }

            _renderdata_vertexCount(output.renderData, vertexCount);
            output.uploadAndStoreAddress(verts.retain(), this.vertices, BaseRenderStrategy::_renderdata_vertexOffset, this.vertexSize);
            return true;
        } finally {
            verts.release();
        }
    }

    protected abstract void bakeVerts(@NonNull POS pos, @NonNull T[] srcs, @NonNull BakeOutput output, @NonNull ByteBuf verts);

    @Override
    public void executeBakeOutput(@NonNull POS pos, @NonNull BakeOutput output) {
        try (AllocatedGLBuffer vertices = this.vertices.bind(GL_ARRAY_BUFFER)) {
            output.execute();
        }
    }

    @Override
    protected void doRelease() {
        this.vertices.delete();
    }
}
