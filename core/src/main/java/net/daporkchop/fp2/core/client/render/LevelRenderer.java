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

package net.daporkchop.fp2.core.client.render;

import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.gl.OpenGL;

/**
 * Version-independent interface for rendering the level.
 *
 * @author DaPorkchop_
 */
public interface LevelRenderer {
    /**
     * @return the {@link IFarLevelClient} which is being rendered in
     */
    IFarLevelClient level();

    /**
     * Gets the render type which the given block state will be rendered as.
     *
     * @param state the block state
     * @return the render pass
     */
    int renderTypeForState(int state);

    /**
     * Gets the tint factor which will affect the texture color for the block state in the given biome at the given position.
     *
     * @param state the block state
     * @param biome the biome
     * @param x     the position's X coordinate
     * @param y     the position's Y coordinate
     * @param z     the position's Z coordinate
     * @return the tint factor, as an ARGB color
     */
    int tintFactorForStateInBiomeAtPos(int state, int biome, int x, int y, int z);

    /**
     * Gets the {@link TerrainRenderingBlockedTracker} used by this renderer for keeping track of which terrain cannot be rendered.
     *
     * @return the {@link TerrainRenderingBlockedTracker}
     */
    TerrainRenderingBlockedTracker blockedTracker();

    /**
     * @return the {@link TextureUVs} instance used in this level
     */
    TextureUVs textureUVs();

    /**
     * @return the OpenGL context used in this level
     */
    OpenGL gl();
}
