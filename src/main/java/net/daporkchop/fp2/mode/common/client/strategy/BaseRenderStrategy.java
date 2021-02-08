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
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.client.AllocatedGLBuffer;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.lib.unsafe.PUnsafe;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class BaseRenderStrategy<POS extends IFarPos, P extends IFarPiece> implements IFarRenderStrategy<POS, P> {
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
    protected final long vertexSize;

    public BaseRenderStrategy(long vertexSize) {
        this.vertices = AllocatedGLBuffer.create(GL_DYNAMIC_DRAW, this.vertexSize = vertexSize, true);
    }

    @Override
    public long renderDataSize() {
        return _RENDERDATA_SIZE;
    }

    @Override
    public void bake(long renderData, int tileX, int tileY, int tileZ, int zoom, @NonNull P[] srcs) {
    }

    protected abstract void bakeVerts(int tileX, int tileY, int tileZ, int zoom, @NonNull P[] srcs, @NonNull ByteBuf verts);
}
