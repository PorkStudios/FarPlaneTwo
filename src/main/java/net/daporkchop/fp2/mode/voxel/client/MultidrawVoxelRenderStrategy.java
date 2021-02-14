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

package net.daporkchop.fp2.mode.voxel.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.mode.common.client.BakeOutput;
import net.daporkchop.fp2.mode.common.client.strategy.MultidrawRenderStrategy;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;

import static net.daporkchop.fp2.mode.common.client.RenderConstants.*;
import static net.daporkchop.fp2.mode.voxel.client.VoxelRenderConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class MultidrawVoxelRenderStrategy extends MultidrawRenderStrategy<VoxelPos, VoxelPiece> implements IVoxelRenderStrategy, IShaderBasedVoxelRenderStrategy {
    public MultidrawVoxelRenderStrategy() {
        super(VoxelBake.VOXEL_VERTEX_SIZE);
    }

    @Override
    protected void configureVertexAttributes(@NonNull IGLBuffer buffer, @NonNull VertexArrayObject vao) {
        VoxelBake.vertexAttributes(buffer, vao);
    }

    @Override
    protected void bakeVertsAndIndices(@NonNull VoxelPos pos, @NonNull VoxelPiece[] srcs, @NonNull BakeOutput output, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices) {
        VoxelBake.bakeForShaderDraw(pos, srcs, verts, indices);
    }

    @Override
    protected void draw() {
        this.drawSolid(this.layers[0]);
        this.drawCutout(this.layers[1]);
        this.drawTransparent(this.layers[2]);
    }

    @Override
    protected void drawTile(long tile) {
        long pos = _tile_pos(tile);
        long renderData = _tile_renderData(tile);

        int tileX = _pos_tileX(pos);
        int tileY = _pos_tileY(pos);
        int tileZ = _pos_tileZ(pos);
        int level = _pos_level(pos);

        int baseVertex = toInt(_renderdata_vertexOffset(renderData) / this.vertexSize);
        int firstIndex = toInt(_renderdata_indexOffset(renderData) >> INDEX_SHIFT);

        for (int i = 0; i < RENDER_PASS_COUNT; i++) {
            int count = _renderdata_indexCount(renderData, i);
            if (count != 0) {
                this.layers[i].drawElements(tileX, tileY, tileZ, level, baseVertex, firstIndex, count);
                firstIndex += count;
            }
        }
    }
}
