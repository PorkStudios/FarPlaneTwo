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
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.piece.VoxelPiece;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Shared code for baking voxel geometry.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class VoxelBake {
    public final int VOXEL_VERTEX_ATTRIBUTE_COUNT = 5;

    public final int VOXEL_VERTEX_STATE_OFFSET = 0;
    public final int VOXEL_VERTEX_LIGHT_OFFSET = VOXEL_VERTEX_STATE_OFFSET + INT_SIZE;
    public final int VOXEL_VERTEX_COLOR_OFFSET = VOXEL_VERTEX_LIGHT_OFFSET + SHORT_SIZE;
    public final int VOXEL_VERTEX_POS_LOW_OFFSET = VOXEL_VERTEX_COLOR_OFFSET + MEDIUM_SIZE;
    public final int VOXEL_VERTEX_POS_HIGH_OFFSET = VOXEL_VERTEX_POS_LOW_OFFSET + MEDIUM_SIZE;

    public final int VOXEL_VERTEX_SIZE = VOXEL_VERTEX_POS_HIGH_OFFSET + MEDIUM_SIZE + 1; // +1 in order to pad to 16 bytes

    public void vertexAttributes(int attributeIndex) {
        glVertexAttribIPointer(attributeIndex++, 1, GL_UNSIGNED_INT, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_STATE_OFFSET); //state
        glVertexAttribPointer(attributeIndex++, 2, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_LIGHT_OFFSET); //light
        glVertexAttribPointer(attributeIndex++, 3, GL_UNSIGNED_BYTE, true, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_COLOR_OFFSET); //color
        glVertexAttribPointer(attributeIndex++, 3, GL_UNSIGNED_BYTE, false, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_POS_LOW_OFFSET); //pos_low
        glVertexAttribPointer(attributeIndex++, 3, GL_UNSIGNED_BYTE, false, VOXEL_VERTEX_SIZE, VOXEL_VERTEX_POS_HIGH_OFFSET); //pos_high
    }

    public void bakeForShaderDraw(@NonNull VoxelPos pos, @NonNull VoxelPiece[] srcs, @NonNull ByteBuf verts, @NonNull ByteBuf[] indices) {
        new VoxelRenderBaker().bake(pos, srcs, verts, indices);
    }
}
