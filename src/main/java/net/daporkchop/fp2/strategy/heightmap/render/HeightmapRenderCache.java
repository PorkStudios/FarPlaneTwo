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

import lombok.NonNull;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.primitive.map.ObjLongMap;
import net.daporkchop.lib.primitive.map.open.ObjLongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

import java.util.Map;

import static net.daporkchop.fp2.strategy.heightmap.render.HeightmapRenderer.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
public class HeightmapRenderCache {
    protected final HeightmapRenderer renderer;

    protected Tile[] tiles = new Tile[0];

    protected final HeightmapRenderLevel[] levels = new HeightmapRenderLevel[FP2Config.maxLevels];

    protected final ObjLongMap<HeightmapPos> renderablePieceAddresses = new ObjLongOpenHashMap<>();

    public HeightmapRenderCache(@NonNull HeightmapRenderer renderer) {
        this.renderer = renderer;

        int size = glGetInteger(GL_MAX_SHADER_STORAGE_BLOCK_SIZE);
        FP2.LOGGER.info(PStrings.fastFormat("Max SSBO size: %d bytes (%.2f MiB)", size, size / (1024.0d * 1024.0d)));

        for (int l = this.levels.length - 1; l >= 0; l--) {
            this.levels[l] = new HeightmapRenderLevel(l, l == this.levels.length - 1 ? null : this.levels[l + 1]);
        }
    }

    public void receivePiece(@NonNull HeightmapPiece piece) {
        this.levels[piece.level()].receivePiece(piece);
    }

    public void unloadPiece(@NonNull HeightmapPos pos) {
        this.levels[pos.level()].unloadPiece(pos);
    }

    public void render(float partialTicks, Minecraft mc) {
        for (HeightmapRenderLevel lvl : this.levels) {
            this.tiles = lvl.prepare(this.tiles);
        }

        try (VertexArrayObject vao = this.renderer.vao.bind()) {
            try (ShaderProgram shader = TERRAIN_SHADER.use()) {
                GlStateManager.disableAlpha();

                for (HeightmapRenderLevel lvl : this.levels) {
                    lvl.render(shader, this.renderer.meshVertexCount);
                }

                GlStateManager.enableAlpha();
            }
            try (ShaderProgram shader = WATER_SHADER.use()) {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

                glUniform1f(shader.uniformLocation("seaLevel"), 63.0f);

                for (HeightmapRenderLevel lvl : this.levels) {
                    lvl.render(shader, this.renderer.meshVertexCount);
                }

                GlStateManager.disableBlend();
            }
        }
    }
}
