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

import net.daporkchop.fp2.client.common.TerrainRenderer;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.noise.NoiseSource;
import net.daporkchop.lib.noise.engine.PerlinNoiseEngine;
import net.daporkchop.lib.noise.filter.ScaleOctavesOffsetFilter;
import net.daporkchop.lib.random.impl.FastPRandom;
import net.daporkchop.pepsimod.util.render.MatrixHelper;
import net.daporkchop.pepsimod.util.render.shader.ShaderManager;
import net.daporkchop.pepsimod.util.render.shader.ShaderProgram;
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
import static net.daporkchop.lib.common.math.PMath.*;
import static net.minecraft.client.renderer.OpenGlHelper.GL_STATIC_DRAW;
import static net.minecraft.client.renderer.OpenGlHelper.glBindBuffer;
import static net.minecraft.client.renderer.OpenGlHelper.glGenBuffers;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * @author DaPorkchop_
 */
public class HeightTerrainRenderer extends TerrainRenderer {
    public static ShaderProgram HEIGHT_SHADER = ShaderManager.get("height");
    public static final int MESH_IBO = glGenBuffers();
    public static final int COORDS_VBO = glGenBuffers();
    public static final int MESH_VERTEX_COUNT;

    public static final Map<Long, Integer> VBO_LOOKUP = new HashMap<>();
    public static final AtomicInteger DATA_GEN_STARTED = new AtomicInteger();

    static {
        ShortBuffer mesh = BufferUtils.createShortBuffer(HEIGHT_TILE_VERTS * HEIGHT_TILE_VERTS * 2 + 1);
        genMesh(HEIGHT_TILE_SQUARES, HEIGHT_TILE_VERTS, mesh);
        MESH_VERTEX_COUNT = mesh.flip().remaining();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, MESH_IBO);
        GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, mesh, GL_STATIC_DRAW);
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

    private static void genMesh(int size, int edge, ShortBuffer out) {
        for (int x = 0; x < size - 1; x++) {
            if ((x & 1) == 0) {
                for (int z = 0; z < size; z++) {
                    out.put((short) (x * edge + z)).put((short) ((x + 1) * edge + z));
                }
            } else {
                for (int z = size - 1; z > 0; z--) {
                    out.put((short) ((x + 1) * edge + z)).put((short) (x * edge + z - 1));
                }
            }
        }
        if ((size & 1) != 0 && size > 2) {
            out.put((short) ((size - 1) * edge));
        }
    }

    public static final NoiseSource NOISE = new ScaleOctavesOffsetFilter(new PerlinNoiseEngine(new FastPRandom()), 0.05d, 0.05d, 0.05d, 32, 128.0d, 128.0d);

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        super.render(partialTicks, world, mc);

        if (false && DATA_GEN_STARTED.compareAndSet(0, 1)) {
            new Thread(() -> {
                for (int x = -2; x < 2; x++) {
                    for (int z = -2; z < 2; z++) {
                        FloatBuffer heights = BufferUtils.createFloatBuffer(HEIGHT_TILE_VERTS * HEIGHT_TILE_VERTS);
                        for (int xx = 0; xx < HEIGHT_TILE_VERTS; xx++) {
                            for (int zz = 0; zz < HEIGHT_TILE_VERTS; zz++) {
                                heights.put((float) NOISE.get(x * HEIGHT_TILE_SQUARES + xx, z * HEIGHT_TILE_SQUARES + zz));
                            }
                        }
                        heights.flip();
                        long l = BinMath.packXY(x, z);
                        mc.addScheduledTask(() -> {
                            int i = glGenBuffers();
                            glBindBuffer(GL_ARRAY_BUFFER, i);
                            glBufferData(GL_ARRAY_BUFFER, heights, GL_STATIC_DRAW);
                            VBO_LOOKUP.put(l, i);
                            glBindBuffer(GL_ARRAY_BUFFER, 0);
                        });
                    }
                }
            }).start();
        }

        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        //GlStateManager.enableBlend();
        GlStateManager.disableCull();

        glPushMatrix();
        glTranslated(-this.x, -this.y, -this.z);

        this.modelView = MatrixHelper.getModelViewMatrix(this.modelView);
        this.proj = MatrixHelper.getProjectionMatrix(this.proj);

        try (ShaderProgram shader = HEIGHT_SHADER.use()) {
            ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_projection"), false, this.proj);
            ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_modelview"), false, this.modelView);

            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);

            glBindBuffer(GL_ARRAY_BUFFER, COORDS_VBO);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);

            /*VBO_LOOKUP.clear();
            VBO_LOOKUP.forEach((l, i) -> {
                glBindBuffer(GL_ARRAY_BUFFER, i);
                glVertexAttribPointer(1, 1, GL_FLOAT, false, 0, 0L);

                ARBShaderObjects.glUniform2fARB(shader.uniformLocation("offset"), BinMath.unpackX(l) * HEIGHT_TILE_SQUARES, BinMath.unpackY(l) * HEIGHT_TILE_SQUARES);

                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, MESH_IBO);
                glDrawElements(GL_TRIANGLE_STRIP, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
            });*/

            int i = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, i);
            FloatBuffer heights = BufferUtils.createFloatBuffer(HEIGHT_TILE_VERTS * HEIGHT_TILE_VERTS);
            for (int x = 0; x < 1; x++) {
                for (int z = 0; z < 1; z++) {
                    for (int xx = 0; xx < HEIGHT_TILE_VERTS; xx++) {
                        for (int zz = 0; zz < HEIGHT_TILE_VERTS; zz++) {
                            heights.put((float) NOISE.get(x * HEIGHT_TILE_SQUARES + xx, z * HEIGHT_TILE_SQUARES + zz) * 2f);
                        }
                    }
                    heights.flip();
                    glBufferData(GL_ARRAY_BUFFER, heights, GL_STATIC_DRAW);
                    glVertexAttribPointer(1, 1, GL_FLOAT, false, 0, 0L);

                    ARBShaderObjects.glUniform2fARB(shader.uniformLocation("offset"), x * HEIGHT_TILE_SQUARES, z * HEIGHT_TILE_SQUARES);

                    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, MESH_IBO);
                    glDrawElements(GL_TRIANGLE_STRIP, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
                }
            }
            glDeleteBuffers(i);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);

            glDisableVertexAttribArray(0);
            glDisableVertexAttribArray(1);

            /*builder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            builder.pos(0, 0, 0).endVertex();
            builder.pos(0, 70, 0).endVertex();
            builder.pos(1, 70, 0).endVertex();
            builder.pos(1, 0, 0).endVertex();
            tessellator.draw();*/
        }

        glPopMatrix();

        GlStateManager.enableCull();
        //GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableFog();
    }
}
