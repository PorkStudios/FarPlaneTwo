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
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.draw.DrawMode;

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
     * Hidden tiles are excluded from tile selection, and will never be rendered until shown again.
     *
     * @param hidden the tile positions to hide
     * @param shown  the tile positions to un-hide
     */
    public abstract void updateHidden(Set<TilePos> hidden, Set<TilePos> shown);

    /**
     * Determine which tiles need to be rendered for the current frame.
     *
     * @param frustum the current view frustum
     */
    public abstract void select(IFrustum frustum);

    /**
     * Draws the selected tiles at the given detail level using the currently bound shader.
     *
     * @param level the detail level
     * @param pass  the render pass
     */
    public abstract void draw(DrawMode mode, int level, int pass);
}
