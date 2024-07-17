/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.core.engine.client;

import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.OpenGL;

/**
 * Constant values used throughout the render code.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class RenderConstants {
    public static final int LAYER_SOLID = 0;
    public static final int LAYER_CUTOUT = LAYER_SOLID + 1;
    public static final int LAYER_TRANSPARENT = LAYER_CUTOUT + 1;

    public static final int RENDER_PASS_COUNT = LAYER_TRANSPARENT + 1; //the total number of render passes

    public static final String TEXTURE_ATLAS_SAMPLER_NAME = "t_terrainAtlas";
    public static final int TEXTURE_ATLAS_SAMPLER_BINDING = 0;

    public static final String LIGHTMAP_SAMPLER_NAME = "t_lightmap";
    public static final int LIGHTMAP_SAMPLER_BINDING = 1;

    public static final String GLOBAL_UNIFORMS_UBO_NAME = "U_GlobalUniforms";
    public static final int GLOBAL_UNIFORMS_UBO_BINDING = 0;

    public static final String TEXTURE_UVS_LISTS_SSBO_NAME = "B_TexQuadLists";
    public static final int TEXTURE_UVS_LISTS_SSBO_BINDING = 0;
    public static final String TEXTURE_UVS_QUADS_SSBO_NAME = "B_TexQuads";
    public static final int TEXTURE_UVS_QUADS_SSBO_BINDING = 1;

    public static final String TILE_POS_ARRAY_UBO_NAME = "U_TilePosArray";
    public static final int TILE_POS_ARRAY_UBO_BINDING = 2;

    public static final String TILE_POSITIONS_SSBO_NAME = "B_TilePositions";
    public static final int TILE_POSITIONS_SSBO_BINDING = 2;

    public static final String INDIRECT_DRAWS_SSBO_NAME = "B_IndirectDraws";
    public static final int INDIRECT_DRAWS_SSBO_FIRST_BINDING = 3; // count=RENDER_PASS_COUNT

    public static final String VANILLA_RENDERABILITY_UBO_NAME = "U_VanillaRenderability"; //synced with resources/assets/fp2/shaders/util/vanilla_renderability.glsl
    public static final int VANILLA_RENDERABILITY_UBO_BINDING = 1;

    public static final String VANILLA_RENDERABILITY_SSBO_NAME = "B_VanillaRenderability"; //synced with resources/assets/fp2/shaders/util/vanilla_renderability.glsl
    public static final int VANILLA_RENDERABILITY_SSBO_BINDING = 7;

    /**
     * The length of the TilePos uniform array, in tile positions.
     *
     * @param gl the OpenGL context
     * @return the length of the TilePos uniform array, in tile positions
     */
    public static int tilePosArrayElements(OpenGL gl) {
        return gl.limits().maxUniformBlockSize() >> 4;
    }

    public static final String TILE_POS_UNIFORM_NAME = "u_TilePos";

    /**
     * Emits the indices for drawing a quad.
     *
     * @param indices        the {@link ByteBuf} to write the indices to
     * @param oppositeCorner the index of the vertex in the corner opposite the provoking vertex
     * @param c0             the index of one of the edge vertices
     * @param c1             the index of the other edge vertex
     * @param provoking      the index of the provoking vertex
     */
    public static void emitQuad(ByteBuf indices, int oppositeCorner, int c0, int c1, int provoking) {
        indices.writeShortLE(c1).writeShortLE(oppositeCorner).writeShortLE(c0).writeShortLE(provoking);
    }
}
