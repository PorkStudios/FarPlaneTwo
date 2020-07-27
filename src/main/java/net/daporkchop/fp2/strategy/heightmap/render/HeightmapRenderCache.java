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
import lombok.NonNull;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.client.gl.OpenGL;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.alloc.FixedSizeAllocator;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.open.LongObjOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;

import java.nio.ByteBuffer;

import static net.daporkchop.fp2.strategy.heightmap.render.HeightmapRenderHelper.*;
import static net.daporkchop.fp2.strategy.heightmap.render.HeightmapRenderer.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapRenderCache {
    protected final HeightmapRenderer renderer;

    protected final LongObjMap<Tile> roots = new LongObjOpenHashMap<>();

    protected final ShaderStorageBuffer dataSSBO = new ShaderStorageBuffer();
    protected final ByteBuffer zeroData = Constants.createByteBuffer(HEIGHTMAP_RENDER_SIZE);
    protected final Allocator dataAllocator = new FixedSizeAllocator(HEIGHTMAP_RENDER_SIZE, (oldSize, newSize) -> {
        try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
            //grow SSBO
            glBufferData(GL_SHADER_STORAGE_BUFFER, newSize, GL_STATIC_DRAW);

            //re-upload data
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0L, this.zeroData);
            this.roots.forEach((l, root) -> root.forEach(tile -> {
                if (tile.hasAddress()) {
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, tile.address, tile.renderData);
                }
            }));
        }
    });

    protected final ShaderStorageBuffer indexSSBO = new ShaderStorageBuffer();
    //protected final HeightmapRenderIndex index = new HeightmapRenderIndex();
    protected final HeightmapRenderIndex[] indices;

    public HeightmapRenderCache(@NonNull HeightmapRenderer renderer) {
        this.renderer = renderer;

        int size = glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
        FP2.LOGGER.info(PStrings.fastFormat("Max SSBO size: %d bytes (%.2f MiB)", size, size / (1024.0d * 1024.0d)));

        //allocate zero block
        checkState(this.dataAllocator.alloc(HEIGHTMAP_RENDER_SIZE) == 0L);

        this.indices = PArrays.filled(this.renderer.maxLevel + 1, HeightmapRenderIndex[]::new, HeightmapRenderIndex::new);
    }

    public void receivePiece(@NonNull HeightmapPiece piece) {
        ByteBuf bakedData = HeightmapRenderHelper.bakePiece(piece);
        HeightmapPos pos = piece.pos();

        Minecraft.getMinecraft().addScheduledTask(() -> {
            try {
                int maxLevel = this.renderer.maxLevel;
                long rootKey = BinMath.packXY(pos.x() >> (maxLevel - pos.level()), pos.z() >> (maxLevel - pos.level()));
                Tile rootTile = this.roots.get(rootKey);
                if (rootTile == null) {
                    //create root tile if absent
                    this.roots.put(rootKey, rootTile = new Tile(this, null, pos.x() >> (maxLevel - pos.level()), pos.z() >> (maxLevel - pos.level()), maxLevel));
                }
                Tile tile = rootTile.findOrCreateChild(pos.x(), pos.z(), pos.level());
                if (!tile.hasAddress()) {
                    //allocate address for tile
                    tile.assignAddress(this.dataAllocator.alloc(HEIGHTMAP_RENDER_SIZE));
                }

                //TODO: set tile min- and maxY

                //copy baked data into tile and upload to GPU
                bakedData.readBytes(tile.renderData);
                tile.renderData.clear();
                try (ShaderStorageBuffer ssbo = this.dataSSBO.bind()) {
                    glBufferSubData(GL_SHADER_STORAGE_BUFFER, tile.address, tile.renderData);
                }
            } finally {
                bakedData.release();
            }
        });
    }

    public void unloadPiece(@NonNull HeightmapPos pos) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            int maxLevel = this.renderer.maxLevel;
            long rootKey = BinMath.packXY(pos.x() >> (maxLevel - pos.level()), pos.z() >> (maxLevel - pos.level()));
            Tile rootTile = this.roots.get(rootKey);
            if (rootTile != null) {
                Tile unloadedTile = rootTile.findChild(pos.x(), pos.z(), pos.level());
                if (unloadedTile != null && unloadedTile.hasAddress()) {
                    //free address
                    this.dataAllocator.free(unloadedTile.address);

                    //inform tile that the address has been freed
                    if (unloadedTile.dropAddress()) {
                        this.roots.remove(rootKey);
                    }
                    return;
                }
            }
            FP2.LOGGER.warn("Attempted to unload already non-existent piece at {}!", pos);
        });
    }

    public Tile getTile(int x, int z, int level) {
        int maxLevel = this.renderer.maxLevel;
        Tile rootTile = this.roots.get(BinMath.packXY(x >> (maxLevel - level), z >> (maxLevel - level)));
        return rootTile != null ? rootTile.findChild(x, z, level) : null;
    }

    public void tileAdded(@NonNull Tile tile) {
        tile.neighbors[0] = tile;
        tile.neighbors[1] = this.getTile(tile.x, tile.z + 1, tile.level);
        tile.neighbors[2] = this.getTile(tile.x + 1, tile.z, tile.level);
        tile.neighbors[3] = this.getTile(tile.x + 1, tile.z + 1, tile.level);

        Tile t = this.getTile(tile.x, tile.z - 1, tile.level);
        if (t != null) {
            t.neighbors[1] = tile;
        }
        t = this.getTile(tile.x - 1, tile.z, tile.level);
        if (t != null) {
            t.neighbors[2] = tile;
        }
        t = this.getTile(tile.x - 1, tile.z - 1, tile.level);
        if (t != null) {
            t.neighbors[3] = tile;
        }
    }

    public void tileRemoved(@NonNull Tile tile) {
        Tile t = this.getTile(tile.x, tile.z - 1, tile.level);
        if (t != null) {
            t.neighbors[1] = null;
        }
        t = this.getTile(tile.x - 1, tile.z, tile.level);
        if (t != null) {
            t.neighbors[2] = null;
        }
        t = this.getTile(tile.x - 1, tile.z - 1, tile.level);
        if (t != null) {
            t.neighbors[3] = null;
        }
    }

    public void render(Volume[] ranges, ICamera frustum) {
        //rebuild and upload index
        /*this.index.reset();
        this.roots.forEach((l, tile) -> tile.select(ranges, frustum, this.index));
        if (this.index.size == 0) {
            return;
        }*/
        for (HeightmapRenderIndex index : this.indices) {
            index.reset();
        }
        this.roots.forEach((l, tile) -> tile.select(ranges, frustum, this.indices));

        /*try (ShaderStorageBuffer ssbo = this.indexSSBO.bind()) {
            OpenGL.checkGLError("pre upload index");
            //this.index.upload(GL_SHADER_STORAGE_BUFFER);
            OpenGL.checkGLError("post upload index");
        }*/

        //bind SSBOs
        this.indexSSBO.bindSSBO(2);
        this.dataSSBO.bindSSBO(3);

        //do the rendering stuff
        try (VertexArrayObject vao = this.renderer.vao.bind()) {
            try (ShaderProgram shader = TERRAIN_SHADER.use()) {
            try (ShaderStorageBuffer ssbo = this.indexSSBO.bind()) {
                    GlStateManager.disableAlpha();

                    for (int i = this.indices.length - 1; i >= 0; i--) {
                        HeightmapRenderIndex index = this.indices[i];
                        if (index.size > 0) {
                            index.upload(GL_SHADER_STORAGE_BUFFER);

                            glDrawElementsInstanced(GL_TRIANGLES, this.renderer.meshVertexCount, GL_UNSIGNED_SHORT, 0L, index.size);

                            GlStateManager.clear(GL_DEPTH_BUFFER_BIT);
                        }
                    }

                    GlStateManager.enableAlpha();
                }
            }

            /*GlStateManager.disableAlpha();
            GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            try (ShaderStorageBuffer ssbo = this.indexSSBO.bind()) {
                for (int i = this.indices.length - 1; i >= 0; i--) {
                    HeightmapRenderIndex index = this.indices[i];
                    if (index.size > 0) {
                        OpenGL.checkGLError("pre upload index");
                        index.upload(GL_SHADER_STORAGE_BUFFER);
                        OpenGL.checkGLError("post upload index");

                        try (ShaderProgram shader = TERRAIN_SHADER.use()) {
                            glDrawElementsInstanced(GL_TRIANGLES, this.renderer.meshVertexCount, GL_UNSIGNED_SHORT, 0L, index.size);
                        }

                        GlStateManager.enableBlend();
                        GlStateManager.enableAlpha();
                        try (ShaderProgram shader = WATER_SHADER.use()) {
                            glDrawElementsInstanced(GL_TRIANGLES, this.renderer.meshVertexCount, GL_UNSIGNED_SHORT, 0L, index.size);
                        }
                        GlStateManager.disableBlend();
                        GlStateManager.disableAlpha();

                        GlStateManager.clear(GL_DEPTH_BUFFER_BIT);
                    }
                }
            }

            GlStateManager.enableAlpha();*/

            /*try (ShaderProgram shader = TERRAIN_SHADER.use()) {
                GlStateManager.disableAlpha();

                glDrawElementsInstanced(GL_TRIANGLES, this.renderer.meshVertexCount, GL_UNSIGNED_SHORT, 0L, this.index.size);

                GlStateManager.enableAlpha();
            }
            try (ShaderProgram shader = WATER_SHADER.use()) {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glUniform1f(shader.uniformLocation("seaLevel"), 63.0f);

                glDrawElementsInstanced(GL_TRIANGLES, this.renderer.meshVertexCount, GL_UNSIGNED_SHORT, 0L, this.index.size);

                GlStateManager.disableBlend();
            }*/
        }
    }
}
