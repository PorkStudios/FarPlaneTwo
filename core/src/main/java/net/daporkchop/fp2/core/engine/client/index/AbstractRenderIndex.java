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

import lombok.NonNull;
import lombok.val;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.postable.RenderPosTable;
import net.daporkchop.fp2.core.engine.client.index.postable.SimpleRenderPosTable;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeFormat;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.vao.VertexArrayObject;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Base implementation of {@link RenderIndex} which keeps track of which tiles are marked as "hidden" and provides a simpler mechanism for
 * implementations to handle tile updates.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractRenderIndex<VertexType extends AttributeStruct> extends RenderIndex<VertexType> {
    protected final Set<TilePos> hiddenPositions = DirectTilePosAccess.newPositionHashSet();

    public AbstractRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc) {
        super(gl, bakeStorage, alloc);
    }

    /**
     * Notifies that the tile at the given position may have changed in some way (it was hidden/shown or its data in the bake storage was
     * modified).
     *
     * @param pos the position of the modified tile
     */
    protected abstract void recomputeTile(@NonNull TilePos pos);

    //separate method to limit the number of lambda metaclasses generated at runtime
    private Consumer<TilePos> recomputeTile() {
        return this::recomputeTile;
    }

    @Override
    public void notifyTilesChanged(Set<TilePos> changedPositions) {
        changedPositions.forEach(this.recomputeTile());
    }

    @Override
    public void updateHidden(Set<TilePos> hidden, Set<TilePos> shown) {
        this.hiddenPositions.removeAll(shown);
        this.hiddenPositions.addAll(hidden);

        //recompute the draw commands for all affected tiles, so that all tiles which got hidden get excluded from the index
        val recomputeTile = this.recomputeTile();
        hidden.forEach(recomputeTile);
        shown.forEach(recomputeTile);
    }

    /**
     * Base implementation of {@link RenderIndex} which inherits functionality from {@link AbstractRenderIndex}. It also has a {@link RenderPosTable} for storing
     * tile positions on the GPU, and a {@link VertexArrayObject} per render pass and detail level, each configured with the appropriate vertex buffers (including an
     * instanced vertex buffer containing the tile position).
     *
     * @author DaPorkchop_
     */
    public static abstract class WithTilePosAttrib<VertexType extends AttributeStruct> extends AbstractRenderIndex<VertexType> {
        protected final RenderPosTable renderPosTable;
        protected final LevelPassArray<VertexArrayObject> vaos;

        public WithTilePosAttrib(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer) {
            super(gl, bakeStorage, alloc);

            try {
                this.renderPosTable = this.constructRenderPosTable(globalRenderer.voxelInstancedAttributesFormat);

                this.vaos = new LevelPassArray<>((level, pass) -> VertexArrayObject.builder(gl)
                        .buffer(bakeStorage.vertexBuffer(level, pass))
                        .buffer(this.renderPosTable.vertexBuffer(level), 1)
                        .elementBuffer(bakeStorage.indexBuffer(level, pass))
                        .build());
            } catch (Throwable t) {
                throw PResourceUtil.closeSuppressed(t, this);
            }
        }

        protected RenderPosTable constructRenderPosTable(AttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
            return new SimpleRenderPosTable(this.gl, sharedVertexFormat, this.alloc, Allocator.GrowFunction.def());
        }

        @Override
        public void close() {
            try (val ignored0 = this.renderPosTable;
                 val ignored1 = this.vaos) {
                super.close();
            }
        }
    }

    /**
     * Base implementation of {@link RenderIndex} which inherits functionality from {@link AbstractRenderIndex}. It also has a {@link VertexArrayObject} per render pass
     * and detail level, each configured with the appropriate vertex buffers.
     *
     * @author DaPorkchop_
     */
    public static abstract class OnlyVertexTypeAttribs<VertexType extends AttributeStruct> extends AbstractRenderIndex<VertexType> {
        protected final LevelPassArray<VertexArrayObject> vaos;

        public OnlyVertexTypeAttribs(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc) {
            super(gl, bakeStorage, alloc);

            try {
                this.vaos = new LevelPassArray<>((level, pass) -> VertexArrayObject.builder(gl)
                        .buffer(bakeStorage.vertexBuffer(level, pass))
                        .elementBuffer(bakeStorage.indexBuffer(level, pass))
                        .build());
            } catch (Throwable t) {
                throw PResourceUtil.closeSuppressed(t, this);
            }
        }

        @Override
        public void close() {
            try (val ignored0 = this.vaos) {
                super.close();
            }
        }
    }
}
