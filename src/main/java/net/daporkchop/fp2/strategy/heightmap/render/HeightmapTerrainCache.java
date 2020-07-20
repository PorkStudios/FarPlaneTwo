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
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.client.gl.OpenGL;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiecePos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.client.Minecraft;

import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.strategy.heightmap.render.HeightmapTerrainRenderer.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapTerrainCache {
    protected final HeightmapTerrainRenderer renderer;

    //protected final Map<HeightmapPiecePos, VertexArrayObject> pieces = new HashMap<>();
    protected final Map<HeightmapPiecePos, Tile> tiles = new HashMap<>();

    protected final ShaderStorageBuffer indexSSBO = new ShaderStorageBuffer();
    protected final ShaderStorageBuffer dataSSBO = new ShaderStorageBuffer();

    protected final BitSet activeDataSlots = new BitSet();
    protected int dataSize = 1;

    public HeightmapTerrainCache(@NonNull HeightmapTerrainRenderer renderer) {
        this.renderer = renderer;

        int size = glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
        FP2.LOGGER.info(PStrings.fastFormat("Max SSBO size: %d bytes (%.2f MiB)", size, size / (1024.0d * 1024.0d)));

        try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
            OpenGL.checkGLError("pre allocate initial data buffer");
            glBufferData(GL_SHADER_STORAGE_BUFFER, this.dataSize * (long) HeightmapPiece.TOTAL_SIZE, GL_STATIC_DRAW);
            OpenGL.checkGLError("post allocate initial data buffer");
        }
    }

    protected int allocateSlot() {
        int slot = this.activeDataSlots.nextClearBit(0);
        this.activeDataSlots.set(slot);
        if (slot >= this.dataSize) {
            this.dataSize <<= 1;
            try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                //grow data SSBO
                OpenGL.checkGLError("pre grow data buffer");

                long size = this.dataSize * (long) HeightmapPiece.TOTAL_SIZE;
                FP2.LOGGER.info(PStrings.fastFormat("Growing data SSBO to %d bytes (%.2f MiB)", size, size / (1024.0d * 1024.0d)));

                glBufferData(GL_SHADER_STORAGE_BUFFER, this.dataSize * (long) HeightmapPiece.TOTAL_SIZE, GL_STATIC_DRAW);
                OpenGL.checkGLError("post grow data buffer");

                //re-upload tiles
                this.tiles.forEach((pos, tile) -> {
                    if (tile.slot >= 0) {
                        OpenGL.checkGLError("pre re-upload tile data");
                        glBufferSubData(GL_SHADER_STORAGE_BUFFER, tile.slot * (long) HeightmapPiece.TOTAL_SIZE, tile.piece.data());
                        OpenGL.checkGLError("post re-upload tile data");
                    }
                });
            }
        }
        return slot;
    }

    public void receivePiece(@NonNull HeightmapPiece piece) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            this.tiles.compute(piece.pos(), (p, tile) -> {
                if (tile == null) {
                    tile = new Tile(piece);
                }
                if (tile.slot < 0) {
                    tile.slot(this.allocateSlot());
                }

                try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                    OpenGL.checkGLError("pre upload tile data");
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, tile.slot * (long) HeightmapPiece.TOTAL_SIZE, tile.piece.data());
                    OpenGL.checkGLError("post upload tile data");
                }

                try (VertexArrayObject vao = new VertexArrayObject().bind()) {
                    for (int i = 0; i <= 2; i++) {
                        glEnableVertexAttribArray(i);
                    }

                    try (VertexBufferObject vbo = this.renderer.coords.bind()) {
                        glVertexAttribIPointer(0, 2, GL_INT, 5 * 4, 0L);
                        glVertexAttribIPointer(1, 2, GL_INT, 5 * 4, 2 * 4L);
                        glVertexAttribIPointer(2, 1, GL_INT, 5 * 4, 4 * 4L);

                        for (int i = 0; i <= 2; i++) {
                            vao.putDependency(i, vbo);
                        }
                    }

                    vao.putElementArray(this.renderer.mesh.bind());

                    tile.vao(vao);
                } finally {
                    for (int i = 0; i <= 2; i++) {
                        glDisableVertexAttribArray(i);
                    }

                    this.renderer.mesh.close();
                }
                return tile;
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

                vao.putElementArray(mesh.bind());

                this.pieces.put(piece.pos(), vao);
            } finally {
                glDisableVertexAttribArray(0);
                glDisableVertexAttribArray(1);
                glDisableVertexAttribArray(2);
                glDisableVertexAttribArray(3);
                glDisableVertexAttribArray(4);

                mesh.close();
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
        if (this.tiles.isEmpty()) {
            return;
        }

        this.generateAndUploadIndex();

        OpenGL.checkGLError("pre bind SSBOs");

        this.indexSSBO.bindSSBO(2);
        this.dataSSBO.bindSSBO(3);

        OpenGL.checkGLError("post bind SSBOs");

        this.tiles.forEach((pos, o) -> {
            glUniform2i(HEIGHT_SHADER.uniformLocation("position_offset"), pos.x() * HEIGHTMAP_VOXELS, pos.z() * HEIGHTMAP_VOXELS);

            OpenGL.checkGLError("pre bind VAO");
            try (VertexArrayObject vao = o.vao().bind()) {
                OpenGL.checkGLError("pre render VAO");
                glDrawElements(GL_TRIANGLES, this.renderer.meshVertexCount, GL_UNSIGNED_SHORT, 0L);
                OpenGL.checkGLError("post render VAO");
            }
            OpenGL.checkGLError("post bind VAO");
        });
    }

    protected int lastCapacity = 0;

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

        if (buffer.capacity() != this.lastCapacity) {
            int o = this.lastCapacity * 4;
            int n = buffer.capacity() * 4;
            FP2.LOGGER.info(PStrings.fastFormat(
                    "tile index changed from %d bytes (%.2f MiB) to %d bytes (%.2f MiB)",
                    o, o / (1024.0d * 1024.0d),
                    n, n / (1024.0d * 1024.0d)
            ));
        }
        this.lastCapacity = buffer.capacity();

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
            OpenGL.checkGLError("pre upload tile index");
            glBufferData(GL_SHADER_STORAGE_BUFFER, buffer, GL_STATIC_DRAW);
            OpenGL.checkGLError("post upload tile index");
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
