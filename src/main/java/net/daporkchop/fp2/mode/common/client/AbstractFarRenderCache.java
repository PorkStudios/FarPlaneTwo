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

package net.daporkchop.fp2.mode.common.client;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.camera.Frustum;
import net.daporkchop.fp2.client.gl.object.DrawIndirectBuffer;
import net.daporkchop.fp2.client.gl.object.ElementArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.mode.api.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.VariableSizedAllocator;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.util.GenericMatcher;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.client.renderer.culling.ICamera;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.mode.common.client.AbstractFarRenderTree.*;
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
public abstract class AbstractFarRenderCache<POS extends IFarPos, P extends IFarPiece<POS>> {
    protected final AbstractFarRenderer<POS, P> renderer;

    protected final AbstractFarRenderTree<POS, P> tree;
    protected final Map<POS, P> pieces = new ConcurrentHashMap<>();

    protected final IntFunction<POS[]> posArray;
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

    public AbstractFarRenderCache(@NonNull AbstractFarRenderer<POS, P> renderer, int vertexSize) {
        this.renderer = renderer;

        Class<POS> posClass = GenericMatcher.uncheckedFind(this.getClass(), AbstractFarRenderCache.class, "POS");
        this.posArray = size -> uncheckedCast(Array.newInstance(posClass, size));
        Class<P> pieceClass = GenericMatcher.uncheckedFind(this.getClass(), AbstractFarRenderCache.class, "P");
        this.pieceArray = size -> uncheckedCast(Array.newInstance(pieceClass, size));

        this.vertexSize = vertexSize;
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

        this.tree = this.createTree();

        this.verticesAllocator = new VariableSizedAllocator(this.vertexSize, (oldSize, newSize) -> {
            LOGGER.info("Growing vertices buffer from {} to {} bytes", oldSize, newSize);

            try (VertexBufferObject vertices = this.vertices.bind()) {
                //grow SSBO
                glBufferData(GL_ARRAY_BUFFER, newSize, GL_STATIC_DRAW);

                //re-upload data
                this.tree.forEach(this.tree::uploadVertices, FLAG_RENDERED);
            }

            this.rebuildVAO();
        });

        this.indicesAllocator = new VariableSizedAllocator(this.indexSize, (oldSize, newSize) -> {
            LOGGER.info("Growing indices buffer from {} to {} bytes", oldSize, newSize);

            try (ElementArrayObject indices = this.indices.bind()) {
                //grow SSBO
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, newSize, GL_STATIC_DRAW);

                //re-upload data
                this.tree.forEach(this.tree::uploadIndices, FLAG_RENDERED);
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

    protected abstract AbstractFarRenderTree<POS, P> createTree();

    public void receivePiece(@NonNull P pieceIn) {
        final int maxLevel = this.renderer.maxLevel;

        this.pieces.put(pieceIn.pos(), pieceIn);

        this.baker.bakeOutputs(pieceIn.pos())
                .forEach(pos -> {
                    if (pos.level() < 0 || pos.level() > maxLevel) {
                        return;
                    }

                    P[] inputPieces = this.baker.bakeInputs(pos)
                            .map(this.pieces::get)
                            .toArray(this.pieceArray);

                    P piece = Arrays.stream(inputPieces).filter(p -> p != null && pos.equals(p.pos())).findAny().orElse(null);
                    if (piece == null) {
                        return;
                    }

                    RENDER_WORKERS.submit(pos, () -> {
                        if (this.pieces.get(pos) != piece) { //piece was modified before this task could be executed
                            return;
                        }

                        ByteBuf vertices = Constants.allocateByteBuf(this.baker.estimatedVerticesBufferCapacity());
                        ByteBuf indices = Constants.allocateByteBuf(this.baker.estimatedIndicesBufferCapacity());
                        this.baker.bake(pos, inputPieces, vertices, indices);

                        //upload to GPU on client thread
                        ClientThreadExecutor.INSTANCE.execute(() -> this.addPiece(piece, vertices, indices));
                    });
                });
    }

    public void addPiece(@NonNull P piece, @NonNull ByteBuf vertices, @NonNull ByteBuf indices) {
        try {
            POS pos = piece.pos();
            if (this.pieces.get(pos) != piece) {
                return;
            }

            try (VertexBufferObject verticesBuffer = this.vertices.bind();
                 ElementArrayObject indicesBuffer = this.indices.bind()) {
                this.tree.putRenderData(pos, vertices, indices);
            }
        } finally {
            vertices.release();
            indices.release();
        }
    }

    public void unloadPiece(@NonNull POS pos) {
        RENDER_WORKERS.submit(pos, () -> { //make sure that any in-progress bake tasks are finished before the piece is removed
            ClientThreadExecutor.INSTANCE.execute(() -> this.removePiece(pos));
        });
    }

    public void removePiece(@NonNull POS pos) {
        if (this.pieces.remove(pos) == null) {
            Constants.LOGGER.warn("Attempted to unload already non-existent piece at {}!", pos);
        }
        this.tree.removeNode(pos);
    }

    public int rebuildIndex(@NonNull Volume[] ranges, @NonNull ICamera frustum) {
        this.index.reset();
        this.tree.select(ranges, (Frustum) frustum, this.index);
        if (this.index.size == 0) {
            return 0;
        }

        try (DrawIndirectBuffer drawCommandBuffer = this.drawCommandBuffer.bind()) {
            this.index.upload(GL_DRAW_INDIRECT_BUFFER);
        }
        return this.index.size;
    }
}
