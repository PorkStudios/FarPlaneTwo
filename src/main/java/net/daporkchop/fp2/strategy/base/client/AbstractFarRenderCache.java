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

package net.daporkchop.fp2.strategy.base.client;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.DrawIndirectBuffer;
import net.daporkchop.fp2.client.gl.object.ElementArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.VariableSizedAllocator;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.primitive.lambda.LongLongConsumer;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;
import net.minecraft.client.renderer.culling.ICamera;

import java.util.Map;
import java.util.function.IntFunction;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL40.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarRenderCache<POS extends IFarPos, P extends IFarPiece<POS>, T extends AbstractFarRenderTile<POS, P, T>> {
    protected final AbstractFarRenderer<POS, P, T> renderer;

    protected final Map<POS, T> roots = new ObjObjOpenHashMap<>();

    protected final IntFunction<T[]> tileArray;

    protected final IntFunction<P[]> pieceArray;

    protected final DrawIndirectBuffer drawCommandBuffer = new DrawIndirectBuffer();

    protected final FarRenderIndex index;

    protected final VertexArrayObject vao = new VertexArrayObject();

    protected final VertexBufferObject vertices = new VertexBufferObject();

    protected final Allocator verticesAllocator;

    protected final int vertexSize;

    protected final ElementArrayObject indices = new ElementArrayObject();

    protected final Allocator indicesAllocator;

    protected final int indexType;

    protected final int indexSize;

    protected final IFarRenderBaker<POS, P> baker;

    public AbstractFarRenderCache(@NonNull AbstractFarRenderer<POS, P, T> renderer, int vertexSize, @NonNull IntFunction<T[]> tileArray, @NonNull IntFunction<P[]> pieceArray) {
        this.renderer = renderer;

        this.tileArray = tileArray;
        this.pieceArray = pieceArray;

        switch (this.indexType = (this.baker = renderer.baker()).indexType()) {
            case GL_UNSIGNED_BYTE:
                this.indexSize = 1;
                break;
            case GL_UNSIGNED_SHORT:
                this.indexSize = 2;
                break;
            case GL_UNSIGNED_INT:
                this.indexSize = 4;
                break;
            default:
                throw new IllegalArgumentException(PStrings.fastFormat("Invalid index type: %d", this.indexType));
        }

        this.indicesAllocator = new VariableSizedAllocator(this.indexSize, (oldSize, newSize) -> {
            LOGGER.info("Growing indices buffer from {} to {} bytes", oldSize, newSize);

            try (ElementArrayObject indices = this.indices.bind()) {
                //grow SSBO
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, newSize, GL_STATIC_DRAW);

                //re-upload data
                this.roots.forEach((l, root) -> root.forEach(tile -> {
                    if (tile.hasAddress()) {
                        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, tile.addressIndices, tile.renderDataIndices);
                    }
                }));
            }

            this.rebuildVAO();
        });

        this.verticesAllocator = new VariableSizedAllocator(this.vertexSize = vertexSize, (oldSize, newSize) -> {
            LOGGER.info("Growing vertices buffer from {} to {} bytes", oldSize, newSize);

            try (VertexBufferObject vertices = this.vertices.bind()) {
                //grow SSBO
                glBufferData(GL_ARRAY_BUFFER, newSize, GL_STATIC_DRAW);

                //re-upload data
                this.roots.forEach((l, root) -> root.forEach(tile -> {
                    if (tile.hasAddress()) {
                        glBufferSubData(GL_ARRAY_BUFFER, tile.addressVertices, tile.renderDataVertices);
                    }
                }));
            }

            this.rebuildVAO();
        });

        this.index = new FarRenderIndex(this.indexSize, this.vertexSize);
    }

    protected void rebuildVAO() {
        int attribs = this.baker.vertexAttributes();
        try (VertexArrayObject vao = this.vao.bind()) {
            for (int i = 0; i <= attribs; i++) {
                glEnableVertexAttribArray(i);
            }

            try (VertexBufferObject vbo = this.vertices.bind()) {
                this.baker.assignVertexAttributes();

                for (int i = 0; i <= attribs; i++) {
                    vao.putDependency(i, vbo);
                }
            }

            vao.putElementArray(this.indices.bind());
        } finally {
            for (int i = 0; i <= attribs; i++) {
                glDisableVertexAttribArray(i);
            }

            this.indices.close();
        }
    }

    public abstract Allocator createAllocator(@NonNull LongLongConsumer resizeCallback);

    public abstract T createTile(T parent, @NonNull POS pos);

    public abstract void tileAdded(@NonNull T tile);

    public abstract void tileRemoved(@NonNull T tile);

    public void receivePiece(@NonNull P piece) {
        { //insert new tile with piece
            POS rootPos = uncheckedCast(piece.pos().upTo(this.renderer.maxLevel));
            T rootTile = this.roots.get(rootPos);
            if (rootTile == null) {
                //create root tile if absent
                this.roots.put(rootPos, rootTile = this.createTile(null, rootPos));
            }
            rootTile.findOrCreateChild(piece.pos()).piece = piece;
        }

        this.baker.bakeOutputs(piece.pos())
                .forEach(pos -> {
                    P[] inputs = this.baker.bakeInputs(pos)
                            .map(inputPos -> {
                                POS rootPos = uncheckedCast(inputPos.upTo(this.renderer.maxLevel));
                                T rootTile = this.roots.get(rootPos);
                                T tile = rootTile != null ? rootTile.findChild(inputPos) : null;
                                return tile != null ? tile.piece : null;
                            })
                            .toArray(this.pieceArray);

                    RENDER_WORKERS.submit(pos, () -> {
                        ByteBuf vertices = Constants.allocateByteBuf(this.baker.estimatedVerticesBufferCapacity());
                        ByteBuf indices = Constants.allocateByteBuf(this.baker.estimatedIndicesBufferCapacity());

                        this.baker.bake(pos, inputs, vertices, indices);

                        if (vertices.isReadable() && indices.isReadable()) {
                            //upload to GPU on client thread
                            ClientThreadExecutor.INSTANCE.execute(() -> {
                                try {
                                    this.addPiece(piece, vertices, indices);
                                } finally {
                                    vertices.release();
                                    indices.release();
                                }
                            });
                        } else {
                            vertices.release();
                            indices.release();
                        }
                    });
                });
    }

    public void addPiece(@NonNull P piece, @NonNull ByteBuf vertices, @NonNull ByteBuf indices) {
        POS pos = piece.pos();
        POS rootPos = uncheckedCast(pos.upTo(this.renderer.maxLevel));

        //the tile should already have been created by the head of receivePiece, if it's not there it's been removed while the tile was baking
        T rootTile = this.roots.get(rootPos);
        if (rootTile == null) {
            //create root tile if absent
            this.roots.put(rootPos, rootTile = this.createTile(null, rootPos));
        }
        T tile = rootTile.findOrCreateChild(pos);
        if (tile.piece != null && tile.piece != piece) {
            return;
        }

        //allocate address for tile
        tile.assignAddress(vertices.readableBytes(), indices.readableBytes());

        //copy baked data into tile and upload to GPU
        vertices.readBytes(tile.renderDataVertices);
        tile.renderDataVertices.clear();
        try (VertexBufferObject verticesBuffer = this.vertices.bind()) {
            glBufferSubData(GL_ARRAY_BUFFER, tile.addressVertices, tile.renderDataVertices);
        }

        indices.readBytes(tile.renderDataIndices);
        tile.renderDataIndices.clear();
        try (ElementArrayObject indicesBuffer = this.indices.bind()) {
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, tile.addressIndices, tile.renderDataIndices);
        }
    }

    public void unloadPiece(@NonNull POS pos) {
        RENDER_WORKERS.submit(pos, () -> { //make sure that any in-progress bake tasks are finished before the piece is removed
            ClientThreadExecutor.INSTANCE.execute(() -> this.removePiece(pos));
        });
    }

    public void removePiece(@NonNull POS pos) {
        POS rootPos = uncheckedCast(pos.upTo(this.renderer.maxLevel));
        T rootTile = this.roots.get(rootPos);
        if (rootTile != null) {
            T unloadedTile = rootTile.findChild(pos);
            if (unloadedTile != null && unloadedTile.hasAddress()) {
                //inform tile that the address has been freed
                if (unloadedTile.dropAddress()) {
                    this.roots.remove(rootPos);
                }
                return;
            }
        }
        Constants.LOGGER.warn("Attempted to unload already non-existent piece at {}!", pos);
    }

    public T getTile(@NonNull POS pos) {
        T rootTile = this.roots.get(PorkUtil.<POS>uncheckedCast(pos.upTo(this.renderer.maxLevel)));
        return rootTile != null ? rootTile.findChild(pos) : null;
    }

    public int rebuildIndex(@NonNull Volume[] ranges, @NonNull ICamera frustum) {
        this.index.reset();
        this.roots.forEach((l, tile) -> tile.select(ranges, frustum, this.index));
        if (this.index.size == 0) {
            return 0;
        }

        try (DrawIndirectBuffer drawCommandBuffer = this.drawCommandBuffer.bind()) {
            this.index.upload(GL_DRAW_INDIRECT_BUFFER);
        }
        return this.index.size;
    }
}
