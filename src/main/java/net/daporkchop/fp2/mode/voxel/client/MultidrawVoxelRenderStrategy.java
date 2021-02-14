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
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.client.render.IDrawMode;
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
public class MultidrawVoxelRenderStrategy extends MultidrawRenderStrategy<VoxelPos, VoxelPiece> implements IVoxelRenderStrategy {
    public MultidrawVoxelRenderStrategy() {
        super(VoxelBake.VOXEL_VERTEX_SIZE, VoxelBake.VOXEL_VERTEX_ATTRIBUTE_COUNT);
    }

    @Override
    protected void configureVertexAttributes(int attributeIndex) {
        VoxelBake.vertexAttributes(attributeIndex);
    }

    @Override
    protected void bakeVertsAndIndices(@NonNull VoxelPos pos, @NonNull VoxelPiece[] srcs, @NonNull BakeOutput output, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices) {
        VoxelBake.bakeForShaderDraw(pos, srcs, verts, indices);
    }

    @Override
    protected void draw() {
        try (ShaderProgram program = VoxelShaders.SOLID_SHADER.use()) {
            this.drawSolid.draw();
        }
    }

    @Override
    public void drawTile(@NonNull IDrawMode dst, long tile) {
        long pos = _tile_pos(tile);
        long renderData = _tile_renderData(tile);

        dst.drawElements(_pos_tileX(pos), _pos_tileY(pos), _pos_tileZ(pos), _pos_level(pos),
                toInt(_renderdata_vertexOffset(renderData) / this.vertexSize), //baseVertex
                toInt(_renderdata_indexOffset(renderData) >> INDEX_SHIFT), //firstIndex
                _renderdata_indexCount(renderData, 0)); //count
    }
}
