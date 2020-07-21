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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
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
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiecePos;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.strategy.heightmap.render.HeightmapTerrainRenderer.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapTerrainCache {
    protected final HeightmapTerrainRenderer renderer;

    //protected final Map<HeightmapPiecePos, VertexArrayObject> pieces = new HashMap<>();
    protected final Map<HeightmapPiecePos, Tile> tiles = new HashMap<>();

    protected final ShaderStorageBuffer indexSSBO = new ShaderStorageBuffer();
    protected final ShaderStorageBuffer positionSSBO = new ShaderStorageBuffer();
    protected final ShaderStorageBuffer dataSSBO = new ShaderStorageBuffer();

    protected final BitSet activeDataSlots = new BitSet();
    protected int dataSize = 1;

    protected final VertexArrayObject vao = new VertexArrayObject();

    public HeightmapTerrainCache(@NonNull HeightmapTerrainRenderer renderer) {
        this.renderer = renderer;

        int size = glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
        FP2.LOGGER.info(PStrings.fastFormat("Max SSBO size: %d bytes (%.2f MiB)", size, size / (1024.0d * 1024.0d)));

        try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
            OpenGL.checkGLError("pre allocate initial data buffer");
            glBufferData(GL_SHADER_STORAGE_BUFFER, this.dataSize * (long) HeightmapPiece.TOTAL_SIZE, GL_STATIC_DRAW);
            OpenGL.checkGLError("post allocate initial data buffer");
        }

        try (VertexArrayObject vao = this.vao.bind()) {
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
        } finally {
            for (int i = 0; i <= 2; i++) {
                glDisableVertexAttribArray(i);
            }

            this.renderer.mesh.close();
        }
    }

    protected int allocateSlot() {
        int slot = this.activeDataSlots.nextClearBit(0);
        this.activeDataSlots.set(slot);
        if (slot >= this.dataSize) {
            this.dataSize <<= 1;
            try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                //grow data SSBO

                long size = this.dataSize * (long) HeightmapPiece.TOTAL_SIZE;
                FP2.LOGGER.info(PStrings.fastFormat("Growing data SSBO to %d bytes (%.2f MiB)", size, size / (1024.0d * 1024.0d)));

                ByteBuf buffer = PooledByteBufAllocator.DEFAULT.directBuffer((int) size >> 1).writerIndex((int) size >> 1);
                try {
                    glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0L, buffer.nioBuffer());

                    glBufferData(GL_SHADER_STORAGE_BUFFER, this.dataSize * (long) HeightmapPiece.TOTAL_SIZE, GL_STATIC_DRAW);

                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0L, buffer.nioBuffer());
                } finally {
                    buffer.release();
                }
            }
        }
        return slot;
    }

    @SuppressWarnings("deprecation")
    public void receivePiece(@NonNull HeightmapPiece piece) {
        CompletableFuture.supplyAsync(
                () -> {
                    ByteBuf buffer = PooledByteBufAllocator.DEFAULT.directBuffer(HEIGHTMAP_VOXELS * HEIGHTMAP_VOXELS * 4 * 4)
                            .order(ByteOrder.nativeOrder());

                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

                    for (int x = 0; x < HEIGHTMAP_VOXELS; x++) {
                        for (int z = 0; z < HEIGHTMAP_VOXELS; z++) {
                            int height = piece.height(x, z);
                            int block = piece.block(x, z);

                            pos.setPos(piece.x() * HEIGHTMAP_VOXELS + x, height, piece.z() * HEIGHTMAP_VOXELS + z);

                            buffer.writeInt(height)
                                    .writeInt((piece.light(x, z) << 24) | block)
                                    .writeInt(Minecraft.getMinecraft().getBlockColors().colorMultiplier(Block.getStateById(block), piece, pos, 0))
                                    .writeInt(Minecraft.getMinecraft().getBlockColors().colorMultiplier(Blocks.WATER.getDefaultState(), piece, pos, 0));
                        }
                    }
                    return buffer;
                }, RENDER_WORKERS)
                .thenAcceptAsync(buffer -> {
                    try {
                        this.tiles.compute(piece.pos(), (p, tile) -> {
                            if (tile == null) {
                                tile = new Tile(piece);
                            } else {
                                tile.piece(piece);
                            }
                            if (tile.slot < 0) {
                                tile.slot(this.allocateSlot());
                            }

                            try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                                glBufferSubData(GL_SHADER_STORAGE_BUFFER, tile.slot * (long) HeightmapPiece.TOTAL_SIZE, buffer.nioBuffer());
                            }

                            return tile;
                        });
                    } finally {
                        buffer.release();
                    }
                }, ClientThreadExecutor.INSTANCE);

        if (true) {
            return;
        }

        Minecraft.getMinecraft().addScheduledTask(() -> {
            this.tiles.compute(piece.pos(), (p, tile) -> {
                if (tile == null) {
                    tile = new Tile(piece);
                } else {
                    tile.piece(piece);
                }
                if (tile.slot < 0) {
                    tile.slot(this.allocateSlot());
                }

                try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                    OpenGL.checkGLError("pre upload tile data");
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, tile.slot * (long) HeightmapPiece.TOTAL_SIZE, tile.piece.data());
                    OpenGL.checkGLError("post upload tile data");
                }

                /*try (VertexArrayObject vao = new VertexArrayObject().bind()) {
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
                }*/
                return tile;
            });
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
        int positions = this.generateAndUploadPositions();

        this.positionSSBO.bindSSBO(2);
        this.indexSSBO.bindSSBO(3);
        this.dataSSBO.bindSSBO(4);

        try (VertexArrayObject vao = this.vao.bind()) {
            try (ShaderProgram shader = TERRAIN_SHADER.use()) {
                GlStateManager.disableAlpha();

                glDrawElementsInstanced(GL_TRIANGLES, this.renderer.meshVertexCount, GL_UNSIGNED_SHORT, 0L, positions);

                GlStateManager.enableAlpha();
            }
            try (ShaderProgram shader = WATER_SHADER.use()) {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glUniform1f(shader.uniformLocation("seaLevel"), 63.0f);
                glDrawElementsInstanced(GL_TRIANGLES, this.renderer.meshVertexCount, GL_UNSIGNED_SHORT, 0L, positions);

                GlStateManager.disableBlend();
            }
        }
    }

    @SuppressWarnings("deprecation")
    protected void generateAndUploadIndex() {
        final int HEADER_SIZE = 2 * 2;

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Tile tile : this.tiles.values()) {
            int i = tile.piece.x();
            if (i < minX) {
                minX = i;
            }
            if (i > maxX) {
                maxX = i;
            }
            i = tile.piece.z();
            if (i < minZ) {
                minZ = i;
            }
            if (i > maxZ) {
                maxZ = i;
            }
        }

        int dx = ++maxX - minX;
        int dz = ++maxZ - minZ;
        int area = dx * dz;

        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.directBuffer((HEADER_SIZE + area) * 4)
                .order(ByteOrder.nativeOrder())
                .writeInt(minX).writeInt(minZ)
                .writeInt(dx).writeInt(dz);

        try {
            for (int i = 0; i < area; i++) {
                buffer.writeInt(-1);
            }

            int _minX = minX;
            int _minZ = minZ;
            this.tiles.values().forEach(tile -> {
                if (tile.slot >= 0) {
                    int index = (tile.piece.x() - _minX) * dz + tile.piece.z() - _minZ;
                    buffer.setInt((HEADER_SIZE + index) * 4, tile.slot);
                }
            });

            try (ShaderStorageBuffer ssbo = this.indexSSBO.bind()) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, buffer.nioBuffer(), GL_STATIC_DRAW);
            }
        } finally {
            buffer.release();
        }
    }

    @SuppressWarnings("deprecation")
    protected int generateAndUploadPositions() {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.directBuffer((this.tiles.size() * 2) * 4)
                .order(ByteOrder.nativeOrder());

        try {
            int count = this.tiles.size();

            this.tiles.values().forEach(tile -> {
                buffer.writeInt(tile.piece.x() * HEIGHTMAP_VOXELS).writeInt(tile.piece.z() * HEIGHTMAP_VOXELS);
            });

            try (ShaderStorageBuffer ssbo = this.positionSSBO.bind()) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, buffer.nioBuffer(), GL_STATIC_DRAW);
            }

            return count;
        } finally {
            buffer.release();
        }
    }

    @RequiredArgsConstructor
    @Getter
    @Setter
    @Accessors(fluent = true, chain = true)
    protected static class Tile {
        @NonNull
        protected HeightmapPiece piece;
        protected int slot = -1;
    }
}
