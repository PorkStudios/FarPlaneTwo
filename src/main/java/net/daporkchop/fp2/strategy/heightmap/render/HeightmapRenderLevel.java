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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.NonNull;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;

import java.nio.ByteOrder;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.daporkchop.fp2.client.ClientConstants.RENDER_WORKERS;
import static net.daporkchop.fp2.strategy.heightmap.render.HeightmapRenderHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapRenderLevel {
    protected final ShaderStorageBuffer indexSSBO = new ShaderStorageBuffer();
    protected final ShaderStorageBuffer positionSSBO = new ShaderStorageBuffer();
    protected final ShaderStorageBuffer dataSSBO = new ShaderStorageBuffer();

    protected final BitSet activeDataSlots = new BitSet();
    protected long dataSize = 1L;

    protected final int level;
    protected final HeightmapRenderLevel next;

    protected final Map<HeightmapPos, Tile> tiles = new ObjObjOpenHashMap<>();

    public HeightmapRenderLevel(int level, HeightmapRenderLevel next) {
        this.level = level;
        this.next = next;

        try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, this.dataSize * HEIGHTMAP_RENDER_SIZE, GL_STATIC_DRAW);
        }
    }

    public void receivePiece(@NonNull HeightmapPiece piece) {
        CompletableFuture
                .supplyAsync(() -> HeightmapRenderHelper.bakePiece(piece), RENDER_WORKERS)
                .thenAcceptAsync(buffer -> this.storePiece(piece, buffer), ClientThreadExecutor.INSTANCE)
                .whenComplete((v, t) -> {
                    if (t != null) {
                        FP2.LOGGER.error("", t);
                    }
                });
    }

    protected void storePiece(@NonNull HeightmapPiece piece, @NonNull ByteBuf buf) {
        //FP2.LOGGER.info(PStrings.fastFormat("Storing piece %d,%d@%d", piece.x(), piece.z(), piece.level()));
        this.tiles.compute(piece.pos(), (pos, tile) -> {
            if (tile == null) {
                tile = new Tile(pos.x(), pos.z(), pos.level(), this.allocateSlot());
            }

            if (tile.renderData != null) {
                tile.renderData.release();
                tile.renderData = null;
            }

            try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                glBufferSubData(GL_SHADER_STORAGE_BUFFER, (long) tile.slot * HEIGHTMAP_RENDER_SIZE, buf.nioBuffer());
                tile.renderData = buf;
            }

            return tile;
        });
    }

    public void unloadPiece(@NonNull HeightmapPos pos) {
        //FP2.LOGGER.info(PStrings.fastFormat("Unloading piece %d,%d@%d", pos.x(), pos.z(), pos.level()));
        ClientThreadExecutor.INSTANCE.execute(() -> {
            Tile tile = this.tiles.remove(pos);
            if (tile != null) {
                if (tile.renderData != null) {
                    tile.renderData.release();
                    tile.renderData = null;
                }
                this.activeDataSlots.clear(tile.slot);
            }
        });
    }

    public Tile[] prepare(@NonNull Tile[] tiles) {
        if (!this.tiles.isEmpty()) {
            int len = this.tiles.size();
            tiles = this.tiles.values().toArray(tiles);

            this.generateAndUploadIndex(tiles, len);
            this.generateAndUploadPositions(tiles, len);
        }

        return tiles;
    }

    public void render(ShaderProgram shader, int meshVertexCount) {
        if (!this.tiles.isEmpty()) {
            this.positionSSBO.bindSSBO(2);
            this.indexSSBO.bindSSBO(4);
            this.dataSSBO.bindSSBO(6);

            if (this.next != null) {
                this.next.positionSSBO.bindSSBO(3);
                this.next.indexSSBO.bindSSBO(5);
                this.next.dataSSBO.bindSSBO(7);
            }

            glUniform1i(shader.uniformLocation("current_base_level"), this.level);

            glDrawElementsInstanced(GL_TRIANGLES, meshVertexCount, GL_UNSIGNED_SHORT, 0L, this.tiles.size());
        }
    }

    @SuppressWarnings("deprecation")
    protected void generateAndUploadIndex(@NonNull Tile[] tiles, int len) {
        final int HEADER_SIZE = 2 * 2;

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int i = 0; i < len; i++) {
            Tile tile = tiles[i];
            if (tile.x < minX) {
                minX = tile.x;
            }
            if (tile.x > maxX) {
                maxX = tile.x;
            }
            if (tile.z < minZ) {
                minZ = tile.z;
            }
            if (tile.z > maxZ) {
                maxZ = tile.z;
            }
        }

        int dx = ++maxX - minX;
        int dz = ++maxZ - minZ;
        int area = dx * dz;

        ByteBuf buffer = Constants.allocateByteBuf((HEADER_SIZE + area) * 4)
                .writeInt(minX).writeInt(minZ)
                .writeInt(dx).writeInt(dz);

        try {
            for (int i = 0; i < area; i++) {
                buffer.writeInt(-1);
            }

            for (int i = 0; i < len; i++) {
                Tile tile = tiles[i];
                int index = (tile.x - minX) * dz + tile.z - minZ;
                buffer.setInt((HEADER_SIZE + index) * 4, tile.slot);
            }

            try (ShaderStorageBuffer ssbo = this.indexSSBO.bind()) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, buffer.nioBuffer(), GL_STATIC_DRAW);
            }
        } finally {
            buffer.release();
        }
    }

    @SuppressWarnings("deprecation")
    protected void generateAndUploadPositions(@NonNull Tile[] tiles, int len) {
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.directBuffer((this.tiles.size() * 4) * 4)
                .order(ByteOrder.nativeOrder());

        try {
            for (int i = 0; i < len; i++) {
                Tile tile = tiles[i];
                buffer.writeInt(tile.x).writeInt(tile.z).writeInt(tile.level)
                        .writeInt(0); //padding
            }

            try (ShaderStorageBuffer ssbo = this.positionSSBO.bind()) {
                glBufferData(GL_SHADER_STORAGE_BUFFER, buffer.nioBuffer(), GL_STATIC_DRAW);
            }
        } finally {
            buffer.release();
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

                glBufferData(GL_SHADER_STORAGE_BUFFER, this.dataSize * HEIGHTMAP_RENDER_SIZE, GL_STATIC_DRAW);

                //re-upload old tiles
                this.tiles.values().forEach(tile -> {
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, (long) tile.slot * HEIGHTMAP_RENDER_SIZE, tile.renderData.nioBuffer());
                });
            }
        }
        return slot;
    }
}
