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

package net.daporkchop.fp2.core.engine.client.index.attribdivisor;

import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.engine.DirectTilePosAccess;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.RenderIndex;
import net.daporkchop.fp2.core.engine.client.index.postable.RenderPosTable;
import net.daporkchop.fp2.core.engine.client.index.postable.SimpleRenderPosTable;
import net.daporkchop.fp2.core.engine.client.struct.VoxelGlobalAttributes;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.NewAttributeFormat;
import net.daporkchop.fp2.gl.attribute.vao.VertexArrayObject;
import net.daporkchop.lib.common.closeable.PResourceUtil;

import java.util.Set;

/**
 * Shared code for implementations of {@link RenderIndex} which feed attributes shared for all vertices in a tile to the vertex shader using a vertex attribute with a vertex divisor.
 * <p>
 * These will dispatch one instanced draw per tile with an instance count of 1 and a base instance which will correspond to the tile's tile position in a corresponding vertex array.
 *
 * @author DaPorkchop_
 */
public abstract class AbstractBaseInstanceRenderIndex<VertexType extends AttributeStruct> extends RenderIndex<VertexType> {
    protected final Set<TilePos> hiddenPositions = DirectTilePosAccess.newPositionHashSet();

    protected final RenderPosTable renderPosTable;
    protected final LevelPassArray<VertexArrayObject> vaos;

    public AbstractBaseInstanceRenderIndex(OpenGL gl, BakeStorage<VertexType> bakeStorage, DirectMemoryAllocator alloc, NewAttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
        super(gl, bakeStorage, alloc);

        try {
            this.renderPosTable = this.constructRenderPosTable(sharedVertexFormat);

            this.vaos = new LevelPassArray<>(
                    (level, pass) -> VertexArrayObject.builder(gl)
                            .buffer(this.renderPosTable.vertexBuffer(level), 1)
                            .buffer(bakeStorage.vertexBuffer(level, pass))
                            .elementBuffer(bakeStorage.indexBuffer(level, pass))
                            .build());
        } catch (Throwable t) {
            throw PResourceUtil.closeSuppressed(t, this);
        }
    }

    @Override
    public void close() {
        PResourceUtil.closeAll(this.vaos, this.renderPosTable);
    }

    protected RenderPosTable constructRenderPosTable(NewAttributeFormat<VoxelGlobalAttributes> sharedVertexFormat) {
        return new SimpleRenderPosTable(this.gl, sharedVertexFormat, this.alloc, Allocator.GrowFunction.def());
    }

    @Override
    public void updateHidden(Set<TilePos> hidden, Set<TilePos> shown) {
        this.hiddenPositions.removeAll(shown);
        this.hiddenPositions.addAll(hidden);
    }
}
