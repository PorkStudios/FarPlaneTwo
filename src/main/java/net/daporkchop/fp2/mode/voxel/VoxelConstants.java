/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.mode.voxel;

import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Objects;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * Constants and helpers to be used by voxel generators.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class VoxelConstants {
    /**
     * The size of the fractional part of a voxel position, in bits.
     */
    public static final int POS_FRACT_SHIFT = 3;

    /**
     * The number 1 in voxel position scale.
     */
    public static final int POS_ONE = 1 << POS_FRACT_SHIFT;

    /**
     * The number of voxel edges considered by the voxel renderer.
     */
    public static final int EDGE_COUNT = 3;

    /**
     * Defines all of the edges of a voxel relevant to the voxel renderer based on their point indices.
     */
    public static final int[] EDGE_VERTEX_MAP = {
            3, 7, // x-axis
            5, 7, // y-axis
            6, 7  // z-axis
    };

    public static final int EDGE_DIR_NONE = 0;
    public static final int EDGE_DIR_POSITIVE = 1;
    public static final int EDGE_DIR_NEGATIVE = 2;
    public static final int EDGE_DIR_BOTH = 3;

    public static final int EDGE_DIR_MASK = 3;

    /**
     * The number of voxel edges considered by a QEF.
     */
    public static final int QEF_EDGE_COUNT = 12;

    /**
     * Defines all of the edges of a voxel considered by a QEF based on their point indices.
     */
    public static final int[] QEF_EDGE_VERTEX_MAP = {
            0b000, 0b100, 0b001, 0b101, 0b001, 0b110, 0b011, 0b111, // x-axis
            0b000, 0b010, 0b001, 0b011, 0b100, 0b110, 0b101, 0b111, // y-axis
            0b000, 0b001, 0b010, 0b011, 0b100, 0b101, 0b110, 0b111  // z-axis
    };

    /**
     * The number of vertex indices emitted on a connected edge.
     */
    public static final int CONNECTION_INDEX_COUNT = 4;

    /**
     * Defines the offsets of all connections for all vertices on the three flagged edges on a voxel.
     */
    public static final int[] CONNECTION_INDICES = {
            0, 1, 2, 3,
            0, 1, 4, 5,
            0, 2, 4, 6
    };

    /**
     * The number of high-resolution neighbor voxels for a low-resolution connected edge.
     */
    public static final int CONNECTION_SUB_NEIGHBOR_COUNT = 6;

    /**
     * Defines the offsets of all high-resolution neighbor voxels for all connectable edges on a low-resolution connected voxel.
     */
    public static final int[] CONNECTION_SUB_NEIGHBORS = {
            1, 2, 3, 5, 6, 7,
            2, 3, 4, 5, 6, 7,
            1, 3, 4, 5, 6, 7
    };

    public static final int[] CONNECTION_INTERSECTION_VOLUMES = {
            T_VOXELS, T_VOXELS, T_VOXELS,
            T_VOXELS, T_VOXELS, 1,
            T_VOXELS, 1, T_VOXELS,
            T_VOXELS, 1, 1,
            1, T_VOXELS, T_VOXELS,
            1, T_VOXELS, 1,
            1, 1, T_VOXELS,
            1, 1, 1
    };

    public static final int TYPE_AIR = 0;
    public static final int TYPE_TRANSPARENT = 1;
    public static final int TYPE_OPAQUE = 2;

    @SideOnly(Side.CLIENT)
    public static final int RENDER_TYPE_OPAQUE = 0;
    @SideOnly(Side.CLIENT)
    public static final int RENDER_TYPE_CUTOUT = 1;
    @SideOnly(Side.CLIENT)
    public static final int RENDER_TYPE_TRANSLUCENT = 2;
    @SideOnly(Side.CLIENT)
    public static final int RENDER_TYPES = 3;

    public static int voxelType(IBlockState state) {
        if (state.isOpaqueCube()) {
            return TYPE_OPAQUE;
        } else if (state.getMaterial().isSolid() || state.getMaterial().isLiquid()) {
            return TYPE_TRANSPARENT;
        } else {
            return TYPE_AIR;
        }
    }

    @SideOnly(Side.CLIENT)
    public static int renderType(IBlockState state) {
        switch (state.getBlock().getRenderLayer()) {
            case SOLID:
                return RENDER_TYPE_OPAQUE;
            case TRANSLUCENT:
                return RENDER_TYPE_TRANSLUCENT;
            case CUTOUT:
            case CUTOUT_MIPPED:
                return RENDER_TYPE_CUTOUT;
        }
        throw new IllegalArgumentException(Objects.toString(state));
    }

    /**
     * Emits the indices for drawing a quad.
     *
     * @param indices        the {@link ByteBuf} to write the indices to
     * @param oppositeCorner the index of the vertex in the corner opposite the provoking vertex
     * @param c0             the index of one of the edge vertices
     * @param c1             the index of the other edge vertex
     * @param provoking      the index of the provoking vertex
     */
    @SideOnly(Side.CLIENT)
    public static void emitQuad(ByteBuf indices, int oppositeCorner, int c0, int c1, int provoking) {
        indices.writeShort(oppositeCorner).writeShort(c0).writeShort(provoking); //first triangle
        indices.writeShort(c1).writeShort(oppositeCorner).writeShort(provoking); //second triangle
    }
}
