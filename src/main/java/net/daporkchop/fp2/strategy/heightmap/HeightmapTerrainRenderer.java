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

package net.daporkchop.fp2.strategy.heightmap;

import lombok.NonNull;
import net.daporkchop.fp2.client.render.MatrixHelper;
import net.daporkchop.fp2.client.render.object.BufferTextureObject;
import net.daporkchop.fp2.client.render.object.ElementArrayObject;
import net.daporkchop.fp2.client.render.object.ShaderStorageBuffer;
import net.daporkchop.fp2.client.render.object.VertexArrayObject;
import net.daporkchop.fp2.client.render.object.VertexBufferObject;
import net.daporkchop.fp2.client.render.shader.ShaderManager;
import net.daporkchop.fp2.client.render.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.common.TerrainRenderer;
import net.daporkchop.fp2.util.Constants;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.minecraft.client.renderer.OpenGlHelper.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glUniform2;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL43.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class HeightmapTerrainRenderer extends TerrainRenderer {
    public static ShaderProgram HEIGHT_SHADER = ShaderManager.get("heightmap");
    public static final ElementArrayObject MESH = new ElementArrayObject();
    public static final int MESH_VERTEX_COUNT;

    public static final VertexBufferObject COORDS = new VertexBufferObject();

    static {
        ShortBuffer meshData = BufferUtils.createShortBuffer(HEIGHT_VERTS * HEIGHT_VERTS * 2 + 1);
        MESH_VERTEX_COUNT = genMesh(HEIGHT_VERTS, HEIGHT_VERTS, meshData);

        try (ElementArrayObject mesh = MESH.bind()) {
            GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, (ShortBuffer) meshData.flip(), GL_STATIC_DRAW);
        }
    }

    static {
        FloatBuffer coordsData = BufferUtils.createFloatBuffer(HEIGHT_VERTS * HEIGHT_VERTS * 2);
        for (int x = 0; x < HEIGHT_VERTS; x++) {
            for (int z = 0; z < HEIGHT_VERTS; z++) {
                coordsData.put(x).put(z);
            }
        }

        try (VertexBufferObject coords = COORDS.bind()) {
            GL15.glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) coordsData.flip(), GL_STATIC_DRAW);
        }
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

    public static final BufferTextureObject MAP_COLORS = new BufferTextureObject();
    public static final BufferTextureObject GRASS_COLORS = new BufferTextureObject();
    public static final IntBuffer GRASS_BUFFER = Constants.createIntBuffer(256 * 256 * 3);

    static {
        IntBuffer buffer = BufferUtils.createIntBuffer(MapColor.COLORS.length + 256);
        for (int i = 0, l = MapColor.COLORS.length; i < l; i++) {
            int color = MapColor.COLORS[i] != null ? MapColor.COLORS[i].colorValue : 0xFF00FF;
            buffer.put(i, Constants.convertARGB_ABGR(0xFF000000 | color));
        }

        try (VertexBufferObject vbo = new VertexBufferObject().bind()) {
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

            MAP_COLORS.useBuffer(vbo, GL_RGBA8);
        }

        for (int i = 0; i < 256; i++) {
            GRASS_BUFFER.put(256 * 256 * 2 + i, 0xFF000000 | Biome.getBiome(i, Biomes.PLAINS).getWaterColor());
        }
    }

    private final Map<ChunkPos, VertexArrayObject> chunks = new HashMap<>();

    private final Runnable uploadBiomeClimates = () -> {
        FloatBuffer biomeClimates = Constants.createFloatBuffer(256 * 2);

        for (int i = 0; i < 256; i++) {
            Biome biome = Biome.getBiome(i, Biomes.PLAINS);
            biomeClimates.put((i << 1), biome.getDefaultTemperature())
                    .put((i << 1) + 1, biome.getRainfall());
        }

        try (ShaderProgram shader = HEIGHT_SHADER.use()) {
            glUniform2(shader.uniformLocation("biome_climate"), biomeClimates);
        }
    };

    public HeightmapTerrainRenderer(@NonNull WorldClient world) {
        Minecraft.getMinecraft().addScheduledTask(this.uploadBiomeClimates);
    }

    public void receiveRemoteChunk(@NonNull HeightmapChunk chunk) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            try (VertexArrayObject vao = new VertexArrayObject().bind()) {
                glEnableVertexAttribArray(0);
                glEnableVertexAttribArray(1);
                glEnableVertexAttribArray(2);
                glEnableVertexAttribArray(3);

                try (VertexBufferObject coords = COORDS.bind()) {
                    glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
                    vao.putDependency(0, coords);
                }
                try (VertexBufferObject heights = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, chunk.height(), GL_STATIC_DRAW);
                    glVertexAttribIPointer(1, 1, GL_INT, 0, 0L);
                    vao.putDependency(1, heights);
                }
                try (VertexBufferObject colors = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, chunk.color(), GL_STATIC_DRAW);
                    glVertexAttribIPointer(2, 1, GL_INT, 0, 0L);
                    vao.putDependency(2, colors);
                }
                try (VertexBufferObject biomes = new VertexBufferObject().bind()) {
                    glBufferData(GL_ARRAY_BUFFER, chunk.biome(), GL_STATIC_DRAW);
                    glVertexAttribIPointer(3, 1, GL_UNSIGNED_BYTE, 0, 0L);
                    vao.putDependency(3, biomes);
                }

                vao.putElementArray(MESH.bind());

                this.chunks.put(new ChunkPos(chunk.x(), chunk.z()), vao);
            } finally {
                glDisableVertexAttribArray(0);
                glDisableVertexAttribArray(1);
                glDisableVertexAttribArray(2);
                glDisableVertexAttribArray(3);

                MESH.close();
            }
        });
    }

    protected boolean pressedLastFrame = false;

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        super.render(partialTicks, world, mc);

        if (Keyboard.isKeyDown(Keyboard.KEY_0)) {
            if (!this.pressedLastFrame) {
                HEIGHT_SHADER = ShaderManager.get("heightmap");
                this.uploadBiomeClimates.run();
                System.out.println("Reloaded shaders");
            }
            this.pressedLastFrame = true;
        } else {
            this.pressedLastFrame = false;
        }

        try (ShaderStorageBuffer loadedBuffer = new ShaderStorageBuffer().bind()) {
            List<Vec3i> positions = mc.renderGlobal.renderInfos.stream()
                    .map(i -> i.renderChunk)
                    .filter(r -> r.compiledChunk != CompiledChunk.DUMMY && !r.compiledChunk.isEmpty())
                    .map(RenderChunk::getPosition)
                    .map(p -> new Vec3i(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4))
                    .collect(Collectors.toList());
            IntBuffer buffer = Constants.createIntBuffer(positions.size() * 3 + 1);
            buffer.put(positions.size());
            positions.forEach(p -> buffer.put(p.getX()).put(p.getY()).put(p.getZ()));
            buffer.flip();

            glBufferData(GL_SHADER_STORAGE_BUFFER, buffer, GL_DYNAMIC_COPY);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, loadedBuffer.id());
        }

        GlStateManager.disableCull();

        glPushMatrix();
        glTranslated(-this.cameraX, -this.cameraY, -this.cameraZ);

        this.modelView = MatrixHelper.getMATRIX(GL_MODELVIEW_MATRIX, this.modelView);
        this.proj = MatrixHelper.getMATRIX(GL_PROJECTION_MATRIX, this.proj);

        try {
            try (BufferTextureObject tex = MAP_COLORS.bind()) {
                GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.enableTexture2D();

                try (BufferTextureObject tex2 = GRASS_COLORS.bind()) {
                    try (ShaderProgram shader = HEIGHT_SHADER.use()) {
                        ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_projection"), false, this.proj);
                        ARBShaderObjects.glUniformMatrix4ARB(shader.uniformLocation("camera_modelview"), false, this.modelView);

                        this.chunks.forEach((pos, o) -> {
                            glUniform2d(shader.uniformLocation("camera_offset"), pos.x * HEIGHT_VOXELS + .5d, pos.z * HEIGHT_VOXELS + .5d);

                            try (VertexArrayObject vao = o.bind()) {
                                glDrawElements(GL_TRIANGLE_STRIP, MESH_VERTEX_COUNT, GL_UNSIGNED_SHORT, 0L);
                            }
                        });
                    }
                }
                GlStateManager.disableTexture2D();
                GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            }
        } finally {
            glPopMatrix();

            GlStateManager.enableCull();
        }
    }
}
