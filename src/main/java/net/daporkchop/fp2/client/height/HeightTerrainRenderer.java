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

package net.daporkchop.fp2.client.height;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.daporkchop.fp2.client.common.TerrainRenderer;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.OpenSimplexNoiseEngine;
import net.daporkchop.lib.noise.engine.PerlinNoiseEngine;
import net.daporkchop.lib.noise.filter.OctaveFilter;
import net.daporkchop.lib.noise.filter.ScaleOctavesOffsetFilter;
import net.daporkchop.lib.random.impl.FastPRandom;
import net.daporkchop.fp2.client.render.render.MatrixHelper;
import net.daporkchop.fp2.client.render.render.shader.ShaderManager;
import net.daporkchop.fp2.client.render.render.shader.ShaderProgram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static net.daporkchop.fp2.util.Constants.*;
import static net.minecraft.client.renderer.OpenGlHelper.GL_STATIC_DRAW;
import static net.minecraft.client.renderer.OpenGlHelper.glBindBuffer;
import static net.minecraft.client.renderer.OpenGlHelper.glGenBuffers;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glVertexAttribIPointer;

/**
 * @author DaPorkchop_
 */
public class HeightTerrainRenderer extends TerrainRenderer {
    public static ShaderProgram HEIGHT_SHADER = ShaderManager.get("height");
    public static final int MESH_IBO = glGenBuffers();
    public static final int COORDS_VBO = glGenBuffers();
    public static final int MESH_VERTEX_COUNT;

    static {
        ShortBuffer mesh = BufferUtils.createShortBuffer(HEIGHT_TILE_VERTS * HEIGHT_TILE_VERTS * 2 + 1);
        MESH_VERTEX_COUNT = genMesh(HEIGHT_TILE_VERTS, HEIGHT_TILE_VERTS, mesh);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, MESH_IBO);
        GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, (ShortBuffer) mesh.flip(), GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    static {
        FloatBuffer coords = BufferUtils.createFloatBuffer(HEIGHT_TILE_VERTS * HEIGHT_TILE_VERTS * 2);
        for (int x = 0; x < HEIGHT_TILE_VERTS; x++) {
            for (int z = 0; z < HEIGHT_TILE_VERTS; z++) {
                coords.put(x).put(z);
            }
        }
        coords.flip();

        glBindBuffer(GL_ARRAY_BUFFER, COORDS_VBO);
        GL15.glBufferData(GL_ARRAY_BUFFER, coords, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private static int genMesh(int size, int edge, ShortBuffer out) {
        int verts = 0;
        for (int x = 0; x < size - 1; x++) {
            if ((x & 1) == 0) {
                for (int z = 0; z < size; z++, verts += 2) {
                    out.put((short) (x * edge + z)).put((short) ((x + 1) * edge + z));
                }
            } else {
                for (int z = size - 1; z > 0; z--, verts += 2) {
                    out.put((short) ((x + 1) * edge + z)).put((short) (x * edge + z - 1));
                }
            }
        }
        if ((size & 1) != 0 && size > 2) {
            out.put((short) ((size - 1) * edge));
            return verts + 1;
        } else {
            return verts;
        }
    }

    public static final NoiseSource NOISE = new OpenSimplexNoiseEngine(new FastPRandom()).scaled(0.03d).octaves(8).mul(256d);
    public static final Long2IntMap VBO_LOOKUP = new Long2IntOpenHashMap();

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        super.render(partialTicks, world, mc);

        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        //GlStateManager.enableBlend();
        GlStateManager.disableCull();

        glPushMatrix();
        glTranslated(-this.x, -this.y, -this.z);

        this.modelView = MatrixHelper.getMatrix(GL_MODELVIEW_MATRIX, this.modelView);
        this.proj = MatrixHelper.getMatrix(GL_PROJECTION_MATRIX, this.proj);

        try (ShaderProgram shader = HEIGHT_SHADER.use()) {
            ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_projection"), false, this.proj);
            ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_modelview"), false, this.modelView);

            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);

            glBindBuffer(GL_ARRAY_BUFFER, COORDS_VBO);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);

            for (int x = 0; x < 1; x++) {
                for (int z = 0; z < 1; z++) {
                    int i = VBO_LOOKUP.get(BinMath.packXY(x, z));
                    if (i == 0) {
                        i = glGenBuffers();

                        IntBuffer heights = BufferUtils.createIntBuffer(HEIGHT_TILE_VERTS * HEIGHT_TILE_VERTS);
                        for (int xx = 0; xx < HEIGHT_TILE_VERTS; xx++) {
                            for (int zz = 0; zz < HEIGHT_TILE_VERTS; zz++) {
                                heights.put((int) NOISE.get(x * HEIGHT_TILE_SQUARES + xx, z * HEIGHT_TILE_SQUARES + zz));
                            }
                        }
                        heights.flip();

                        glBindBuffer(GL_ARRAY_BUFFER, i);
                        glBufferData(GL_ARRAY_BUFFER, heights, GL_STATIC_DRAW);

                        VBO_LOOKUP.put(BinMath.packXY(x, z), i);
                    }

                    glBindBuffer(GL_ARRAY_BUFFER, i);
                    glVertexAttribIPointer(1, 1, GL_INT, 0, 0L);

                    ARBShaderObjects.glUniform2fARB(shader.uniformLocation("offset"), x * HEIGHT_TILE_SQUARES, z * HEIGHT_TILE_SQUARES);

                    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, MESH_IBO);
                    glDrawElements(GL_TRIANGLE_STRIP, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
                }
            }

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);
        }

        glPopMatrix();

        GlStateManager.enableCull();
        //GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableFog();
    }
}
