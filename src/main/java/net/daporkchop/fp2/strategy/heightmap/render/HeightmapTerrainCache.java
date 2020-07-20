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

package net.daporkchop.fp2.strategy.heightmap.render;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiecePos;
import net.daporkchop.fp2.util.Constants;
import net.minecraft.client.Minecraft;

import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.strategy.heightmap.render.HeightmapTerrainRenderer.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glVertexAttribIPointer;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapTerrainCache {
    //protected final Map<HeightmapPiecePos, VertexArrayObject> pieces = new HashMap<>();
    protected final Map<HeightmapPiecePos, Tile> tiles = new HashMap<>();

    protected final ShaderStorageBuffer indexSSBO = new ShaderStorageBuffer();
    protected final ShaderStorageBuffer dataSSBO = new ShaderStorageBuffer();

    protected final BitSet activeDataSlots = new BitSet();
    protected int dataSize = 1;

    public HeightmapTerrainCache() {
        try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, this.dataSize * (long) HeightmapPiece.TOTAL_SIZE, GL_STATIC_DRAW);
        }
    }

    protected int allocateSlot() {
        int slot = this.activeDataSlots.nextClearBit(0);
        this.activeDataSlots.set(slot);
        if (slot >= this.dataSize) {
            this.dataSize <<= 1;
            try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                //grow data SSBO
                glBufferData(GL_SHADER_STORAGE_BUFFER, this.dataSize * (long) HeightmapPiece.TOTAL_SIZE, GL_STATIC_DRAW);

                //re-upload tiles
                this.tiles.forEach((pos, tile) -> {
                    if (tile.slot >= 0) {
                        glBufferSubData(GL_SHADER_STORAGE_BUFFER, tile.slot * (long) HeightmapPiece.TOTAL_SIZE, tile.piece.data());
                    }
                });
            }
        }
        return slot;
    }

    public void receivePiece(@NonNull HeightmapPiece piece) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            this.tiles.merge(piece.pos(), new Tile(piece), (tOld, tIn) -> {
                if (tOld == null || tOld.slot < 0) {
                    tIn.slot(this.allocateSlot());
                } else {
                    tIn.slot(tOld.slot);
                }
                try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, tIn.slot * (long) HeightmapPiece.TOTAL_SIZE, tIn.piece.data());
                }

                try (VertexArrayObject vao = new VertexArrayObject().bind()) {
                    glEnableVertexAttribArray(0);
                    glEnableVertexAttribArray(1);
                    glEnableVertexAttribArray(2);
                    glEnableVertexAttribArray(3);
                    glEnableVertexAttribArray(4);

                    try (VertexBufferObject vbo = new VertexBufferObject().bind()) {
                        glBufferData(GL_ARRAY_BUFFER, piece.data(), GL_STATIC_DRAW);

                        glVertexAttribIPointer(0, 1, GL_INT, HeightmapPiece.ENTRY_SIZE, HeightmapPiece.HEIGHT_OFFSET);
                        vao.putDependency(0, vbo);

                        glVertexAttribIPointer(1, 1, GL_INT, HeightmapPiece.ENTRY_SIZE, HeightmapPiece.BLOCK_OFFSET);
                        vao.putDependency(1, vbo);

                        glVertexAttribIPointer(2, 1, GL_INT, HeightmapPiece.ENTRY_SIZE, HeightmapPiece.ATTRS_OFFSET);
                        vao.putDependency(2, vbo);
                    }

                    vao.putElementArray(MESH.bind());

                    tIn.vao(vao);
                } finally {
                    glDisableVertexAttribArray(0);
                    glDisableVertexAttribArray(1);
                    glDisableVertexAttribArray(2);
                    glDisableVertexAttribArray(3);
                    glDisableVertexAttribArray(4);

                    MESH.close();
                }
                return tIn;
            });
            /*try (VertexArrayObject vao = new VertexArrayObject().bind()) {
                glEnableVertexAttribArray(0);
                glEnableVertexAttribArray(1);
                glEnableVertexAttribArray(2);
                glEnableVertexAttribArray(3);
                glEnableVertexAttribArray(4);

                try (VertexBufferObject vbo = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, piece.data(), GL_STATIC_DRAW);

                    glVertexAttribIPointer(0, 1, GL_INT, HeightmapPiece.ENTRY_SIZE, HeightmapPiece.HEIGHT_OFFSET);
                    vao.putDependency(0, vbo);

                    glVertexAttribIPointer(1, 1, GL_INT, HeightmapPiece.ENTRY_SIZE, HeightmapPiece.BLOCK_OFFSET);
                    vao.putDependency(1, vbo);

                    glVertexAttribIPointer(2, 1, GL_INT, HeightmapPiece.ENTRY_SIZE, HeightmapPiece.ATTRS_OFFSET);
                    vao.putDependency(2, vbo);
                }

                vao.putElementArray(MESH.bind());

                this.pieces.put(piece.pos(), vao);
            } finally {
                glDisableVertexAttribArray(0);
                glDisableVertexAttribArray(1);
                glDisableVertexAttribArray(2);
                glDisableVertexAttribArray(3);
                glDisableVertexAttribArray(4);

                MESH.close();
            }*/
        });
    }

    public void unloadPiece(@NonNull HeightmapPiecePos pos) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Tile tile = this.tiles.remove(pos);
            if (tile != null && tile.slot >= 0) {
                this.activeDataSlots.clear(tile.slot);
            }
        });
    }

    public void render(float partialTicks, Minecraft mc) {
        if (this.tiles.isEmpty())   {
            return;
        }

        this.generateAndUploadIndex();

        this.indexSSBO.bindSSBO(2);
        this.dataSSBO.bindSSBO(3);

        this.tiles.forEach((pos, o) -> {
            glUniform2i(HEIGHT_SHADER.uniformLocation("camera_offset"), pos.x() * HEIGHTMAP_VOXELS, pos.z() * HEIGHTMAP_VOXELS);

            try (VertexArrayObject vao = o.vao().bind()) {
                glDrawElements(GL_TRIANGLES, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
            }
        });
    }

    protected void generateAndUploadIndex() {
        final int HEADER_SIZE = 2 * 2;

        int minX = this.tiles.values().stream().map(Tile::piece).mapToInt(HeightmapPiece::x).min().orElse(0);
        int maxX = this.tiles.values().stream().map(Tile::piece).mapToInt(HeightmapPiece::x).max().orElse(0) + 1;
        int minZ = this.tiles.values().stream().map(Tile::piece).mapToInt(HeightmapPiece::z).min().orElse(0);
        int maxZ = this.tiles.values().stream().map(Tile::piece).mapToInt(HeightmapPiece::z).max().orElse(0) + 1;
        int dx = maxX - minX;
        int dz = maxZ - minZ;
        int area = dx * dz;

        IntBuffer buffer = Constants.createIntBuffer(HEADER_SIZE + area)
                .put(minX).put(minZ)
                .put(dx).put(dz);

        while (buffer.hasRemaining()) {
            buffer.put(-1);
        }
        buffer.clear();

        this.tiles.values().forEach(tile -> {
            if (tile.slot >= 0) {
                int index = (tile.piece.x() - minX) * dz + tile.piece.z() - minZ;
                buffer.put(HEADER_SIZE + index, tile.slot);
            }
        });

        try (ShaderStorageBuffer ssbo = this.indexSSBO.bind()) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, buffer, GL_STATIC_DRAW);
        }
    }

    @RequiredArgsConstructor
    @Getter
    @Setter
    @Accessors(fluent = true, chain = true)
    protected static class Tile {
        @NonNull
        protected HeightmapPiece piece;
        protected VertexArrayObject vao;
        protected int slot = -1;
    }
}
