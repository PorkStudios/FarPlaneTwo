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

package net.daporkchop.fp2.core.engine.client.index;

import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.RenderConstants;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.ShaderProgram;

import java.util.Set;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public abstract class RenderIndex<VertexType extends AttributeStruct> implements AutoCloseable {
    /**
     * The OpenGL context.
     */
    public final OpenGL gl;

    /**
     * The {@link BakeStorage} which stores the baked tile data.
     * <p>
     * This is not owned by the render index. User code is responsible for closing the storage.
     */
    public final BakeStorage<VertexType> bakeStorage;

    /**
     * The {@link DirectMemoryAllocator} which is used for allocating direct memory needed by this index.
     */
    public final DirectMemoryAllocator alloc;

    @Override
    public abstract void close();

    /**
     * Notifies this index that the tiles at the given positions may have been changed in the bake storage.
     *
     * @param changedPositions the tile positions which changed
     */
    public abstract void notifyTilesChanged(Set<TilePos> changedPositions);

    /**
     * Updates the internal set of hidden tile positions.
     * <p>
     * Hidden tiles are excluded from tile selection, and will never be rendered until shown again. By default, all tiles are visible.
     *
     * @param hidden the tile positions to hide
     * @param shown  the tile positions to un-hide
     */
    public abstract void updateHidden(Set<TilePos> hidden, Set<TilePos> shown);

    /**
     * Determine which tiles need to be rendered for the current frame.
     *
     * @param frustum        the current view frustum
     * @param blockedTracker a {@link TerrainRenderingBlockedTracker} for tracking which level-0 tiles are blocked from rendering
     */
    public abstract void select(IFrustum frustum, TerrainRenderingBlockedTracker blockedTracker);

    /**
     * Draws the selected tiles at the given detail level using the currently bound shader.
     *
     * @param level         the detail level
     * @param pass          the render pass
     * @param shader        the shader which is currently bound and is going to be rendered with
     * @param uniformSetter a handle for setting uniform values in the draw shader
     */
    public abstract void draw(DrawMode mode, int level, int pass, DrawShaderProgram shader, ShaderProgram.UniformSetter uniformSetter);

    /**
     * @return the technique used by this render index to push tile positions to the shader
     */
    public abstract PosTechnique posTechnique();

    /**
     * A technique describing how shaders should access the tile position for a tile.
     *
     * @author DaPorkchop_
     */
    public enum PosTechnique {
        /**
         * The shader declares ordinary vertex attributes for {@link net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes}.
         * <p>
         * The render index is responsible for ensuring that the corresponding attribute arrays are bound to data representing the tile position, which
         * is likely done using a vertex attribute divisor and issuing instanced draws using a corresponding BaseInstance value.
         */
        VERTEX_ATTRIBUTE,
        /**
         * The shader declares a uniform buffer at binding location {@link RenderConstants#TILE_POS_ARRAY_UBO_BINDING}. The buffer must use the {@code std140}
         * layout, and contain an array of {@code ivec4} of length {@link RenderConstants#tilePosArrayElements(OpenGL)}.
         * <p>
         * The shader can access the tile position of the current tile by reading the uniform element at index {@code gl_DrawID}.
         */
        UNIFORM_ARRAY_DRAWID,
        /**
         * The shader declares a uniform {@code ivec4} named {@link RenderConstants#TILE_POS_UNIFORM_NAME}.
         */
        UNIFORM,
    }
}
