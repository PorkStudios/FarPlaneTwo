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

import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.client.ClientConstants;
import net.daporkchop.fp2.client.GlobalInfo;
import net.daporkchop.fp2.client.ShaderFP2StateHelper;
import net.daporkchop.fp2.client.ShaderGlStateHelper;
import net.daporkchop.fp2.client.gl.MatrixHelper;
import net.daporkchop.fp2.client.gl.OpenGL;
import net.daporkchop.fp2.client.gl.object.ElementArrayObject;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.client.gl.object.UniformBufferObject;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.client.gl.shader.ShaderProgram;
import net.daporkchop.fp2.strategy.common.client.IFarRenderer;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPos;
import net.daporkchop.fp2.util.math.Cylinder;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.fp2.util.threading.KeyedTaskScheduler;
import net.daporkchop.fp2.util.threading.PriorityThreadFactory;
import net.daporkchop.lib.common.misc.threadfactory.ThreadFactoryBuilder;
import net.daporkchop.lib.unsafe.PCleaner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static net.daporkchop.fp2.client.ClientConstants.*;
import static net.daporkchop.fp2.client.GlobalInfo.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public class HeightmapRenderer implements IFarRenderer<HeightmapPos, HeightmapPiece> {
    public static ShaderProgram TERRAIN_SHADER = ShaderManager.get("heightmap/terrain");
    public static ShaderProgram WATER_SHADER = ShaderManager.get("heightmap/water");

    public static void reloadHeightShader(boolean notify) {
        ShaderProgram terrain = TERRAIN_SHADER;
        boolean skipWater = false;
        ShaderProgram water = WATER_SHADER;
        try {
            TERRAIN_SHADER = ShaderManager.get("heightmap/terrain");
            if (!skipWater) {
                WATER_SHADER = ShaderManager.get("heightmap/water");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (notify) {
                if (TERRAIN_SHADER == terrain || (!skipWater && WATER_SHADER == water)) {
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentString("§cheightmap shader reload failed (check console)."));
                } else {
                    Minecraft.getMinecraft().player.sendMessage(new TextComponentString("§aheightmap shader successfully reloaded."));
                }
            }
        }
        GlobalInfo.init();
        GlobalInfo.reloadUVs();
    }

    private static int genMesh(int size, int edge, ShortBuffer out) {
        int verts = 0;
        for (int x = 0; x < size - 1; x++) {
            for (int z = 0; z < size - 1; z++) {
                out.put((short) ((x + 1) * edge + z))
                        .put((short) ((x + 1) * edge + (z + 1)))
                        .put((short) (x * edge + z));
                out.put((short) (x * edge + (z + 1)))
                        .put((short) ((x + 1) * edge + (z + 1)))
                        .put((short) (x * edge + z));
                verts += 6;
            }
        }
        return verts;
    }

    protected final int maxLevel = FP2Config.maxLevels - 1;

    protected final HeightmapRenderCache cache;
    protected IntBuffer renderableChunksMask;

    public final ElementArrayObject mesh = new ElementArrayObject();
    public final int meshVertexCount;

    public final VertexBufferObject coords = new VertexBufferObject();

    public final UniformBufferObject uniforms = new UniformBufferObject();

    protected final VertexArrayObject vao = new VertexArrayObject();

    protected final KeyedTaskScheduler<HeightmapPos> scheduler = new KeyedTaskScheduler<>(
            FP2Config.client.renderThreads,
            new PriorityThreadFactory(
                    new ThreadFactoryBuilder().daemon().collapsingId().formatId().name("FP2 Rendering Thread #%d").priority(Thread.MIN_PRIORITY).build(),
                    Thread.MIN_PRIORITY));

    public HeightmapRenderer(@NonNull WorldClient world) {
        {
            @RequiredArgsConstructor
            class SchedulerReleaser implements Runnable {
                @NonNull
                protected final KeyedTaskScheduler<HeightmapPos> scheduler;

                @Override
                public void run() {
                    //avoid calling shutdown on the reference handler thread
                    GlobalEventExecutor.INSTANCE.execute(this.scheduler::shutdown);
                }
            }
            PCleaner.cleaner(this, new SchedulerReleaser(this.scheduler));
        }

        {
            ShortBuffer meshData = BufferUtils.createShortBuffer(T_VERTS * T_VERTS * 6 + 1);
            this.meshVertexCount = genMesh(T_VERTS, T_VERTS, meshData);
            meshData.flip();

            try (ElementArrayObject mesh = this.mesh.bind()) {
                OpenGL.checkGLError("pre upload mesh");
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, meshData, GL_STATIC_DRAW);
                OpenGL.checkGLError("post upload mesh");
            }
        }

        {
            ByteBuffer coordsData = BufferUtils.createByteBuffer(T_VERTS * T_VERTS * 5);
            for (int x = 0; x < T_VERTS; x++) {
                for (int z = 0; z < T_VERTS; z++) {
                    coordsData.put((byte) x).put((byte) z)
                            .put((byte) (x & T_MASK)).put((byte) (z & T_MASK))
                            .put((byte) ((x & T_MASK) * T_VOXELS + (z & T_MASK)));
                }
            }
            coordsData.flip();

            try (VertexBufferObject coords = this.coords.bind()) {
                OpenGL.checkGLError("pre upload coords");
                glBufferData(GL_ARRAY_BUFFER, coordsData, GL_STATIC_DRAW);
                OpenGL.checkGLError("post upload coords");
            }
        }

        try (UniformBufferObject uniforms = this.uniforms.bind()) {
            glBufferData(GL_UNIFORM_BUFFER, 16L * 4L * 2L + 8L * 4L, GL_STATIC_DRAW);
        }

        try (VertexArrayObject vao = this.vao.bind()) {
            for (int i = 0; i <= 2; i++) {
                glEnableVertexAttribArray(i);
            }

            try (VertexBufferObject vbo = this.coords.bind()) {
                glVertexAttribIPointer(0, 2, GL_UNSIGNED_BYTE, 5, 0L);
                glVertexAttribIPointer(1, 2, GL_UNSIGNED_BYTE, 5, 2L);
                glVertexAttribIPointer(2, 1, GL_UNSIGNED_BYTE, 5, 4L);

                for (int i = 0; i <= 2; i++) {
                    vao.putDependency(i, vbo);
                }
            }

            vao.putElementArray(this.mesh.bind());
        } finally {
            for (int i = 0; i <= 2; i++) {
                glDisableVertexAttribArray(i);
            }

            this.mesh.close();
        }

        this.cache = new HeightmapRenderCache(this);
    }

    @Override
    public void receivePiece(@NonNull HeightmapPiece piece) {
        this.scheduler.submit(piece.pos(), () -> this.cache.receivePiece(piece));
    }

    @Override
    public void unloadPiece(@NonNull HeightmapPos pos) {
        this.scheduler.submit(pos, () -> this.cache.unloadPiece(pos));
    }

    @Override
    public void render(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum) {
        OpenGL.checkGLError("pre fp2 render");

        try (ShaderStorageBuffer loadedBuffer = new ShaderStorageBuffer().bind()) {
            OpenGL.checkGLError("pre upload renderable chunks mask");
            glBufferData(GL_SHADER_STORAGE_BUFFER, this.renderableChunksMask = ClientConstants.renderableChunksMask(mc, this.renderableChunksMask), GL_STATIC_DRAW);
            OpenGL.checkGLError("post upload renderable chunks mask");
            loadedBuffer.bindSSBO(0);
        }
        GLOBAL_INFO.bindSSBO(1);

        GlStateManager.disableCull();

        GlStateManager.matrixMode(GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        MatrixHelper.reversedZ(mc.entityRenderer.getFOVModifier(partialTicks, true), (float) mc.displayWidth / (float) mc.displayHeight, 0.05f);

        GlStateManager.depthFunc(GL_GREATER);

        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);

        GlStateManager.clearDepth(0.0d);
        GlStateManager.clear(GL_DEPTH_BUFFER_BIT); //TODO: it might be better to use a separate depth buffer (maybe)

        GlStateManager.matrixMode(GL_MODELVIEW);
        GlStateManager.pushMatrix();

        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, mc.gameSettings.mipmapLevels > 0);

        mc.entityRenderer.enableLightmap();

        try {
            ShaderGlStateHelper.updateAndBind(partialTicks, mc);
            ShaderFP2StateHelper.updateAndBind(partialTicks, mc);

            Volume[] ranges = new Volume[this.maxLevel + 1];
            Entity entity = mc.getRenderViewEntity();
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
            for (int i = 0; i < ranges.length; i++) {
                ranges[i] = new Cylinder(x, z, FP2Config.levelCutoffDistance << i);
            }

            this.cache.render(ranges, frustum);
        } finally {
            mc.entityRenderer.disableLightmap();

            mc.getTextureManager().getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();

            glClipControl(GL_LOWER_LEFT, GL_NEGATIVE_ONE_TO_ONE);
            GlStateManager.depthFunc(GL_LEQUAL);

            GlStateManager.clearDepth(1.0d);
            GlStateManager.clear(GL_DEPTH_BUFFER_BIT);

            GlStateManager.matrixMode(GL_PROJECTION);
            GlStateManager.popMatrix();

            GlStateManager.matrixMode(GL_MODELVIEW);
            GlStateManager.popMatrix();

            GlStateManager.enableCull();

            OpenGL.checkGLError("post fp2 render");
        }
    }
}
