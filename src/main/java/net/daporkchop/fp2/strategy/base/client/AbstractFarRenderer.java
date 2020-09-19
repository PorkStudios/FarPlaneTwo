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

package net.daporkchop.fp2.strategy.base.client;

import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.client.ClientConstants;
import net.daporkchop.fp2.client.ShaderFP2StateHelper;
import net.daporkchop.fp2.client.ShaderGlStateHelper;
import net.daporkchop.fp2.client.gl.MatrixHelper;
import net.daporkchop.fp2.client.gl.OpenGL;
import net.daporkchop.fp2.client.gl.object.ShaderStorageBuffer;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.strategy.common.client.IFarRenderer;
import net.daporkchop.fp2.util.math.Sphere;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;

import java.nio.IntBuffer;

import static net.daporkchop.fp2.client.TexUVs.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractFarRenderer<POS extends IFarPos, P extends IFarPiece<POS>, T extends AbstractFarRenderTile<POS, P, T>> implements IFarRenderer<POS, P> {
    protected final AbstractFarRenderCache<POS, P, T> cache;

    protected final int maxLevel = FP2Config.maxLevels - 1;

    protected final ShaderStorageBuffer loadedSSBO = new ShaderStorageBuffer();

    protected IntBuffer loadedBuffer;

    public AbstractFarRenderer(@NonNull WorldClient world) {
        this.createRenderData();

        this.cache = this.createCache();
    }

    protected abstract void createRenderData();

    protected abstract AbstractFarRenderCache<POS, P, T> createCache();

    @Override
    public void receivePiece(@NonNull P piece) {
        ClientThreadExecutor.INSTANCE.execute(() -> this.cache.receivePiece(piece));
    }

    @Override
    public void unloadPiece(@NonNull POS pos) {
        ClientThreadExecutor.INSTANCE.execute(() -> this.cache.unloadPiece(pos));
    }

    @Override
    public void debug_renderPieces() {
        this.cache.debug_renderPieces();
    }

    /**
     * @return the {@link IFarRenderBaker} used by this renderer
     */
    public abstract IFarRenderBaker<POS, P> baker();

    /**
     * Actually renders the world.
     *
     * @param count the number of tiles that were added to the index
     */
    protected abstract void render0(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum, int count);

    @Override
    public void render(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum) {
        OpenGL.checkGLError("pre fp2 render");

        int count = this.cache.rebuildIndex(this.createVolumesForSelection(partialTicks, world, mc, frustum), frustum);
        if (count <= 0) {
            return; //nothing to render...
        }

        this.updateAndBindSSBOs(partialTicks, world, mc, frustum);

        this.prepareGlState(partialTicks, world, mc, frustum);
        try {
            this.updateAndBindUBOs(partialTicks, world, mc, frustum);

            this.render0(partialTicks, world, mc, frustum, count);
        } finally {
            this.resetGlState(partialTicks, world, mc, frustum);
        }

        OpenGL.checkGLError("post fp2 render");
    }

    //TODO: use cylinders for heightmap and spheres for voxel
    protected Volume[] createVolumesForSelection(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum) {
        Volume[] ranges = new Volume[this.maxLevel + 1];
        Entity entity = mc.getRenderViewEntity();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        for (int i = 0; i < ranges.length; i++) {
            ranges[i] = new Sphere(x, y, z, FP2Config.levelCutoffDistance << i);
        }
        return ranges;
    }

    protected void updateAndBindSSBOs(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum) {
        try (ShaderStorageBuffer loadedBuffer = this.loadedSSBO.bind()) {
            glBufferData(GL_SHADER_STORAGE_BUFFER, this.loadedBuffer = ClientConstants.renderableChunksMask(mc, this.loadedBuffer), GL_STATIC_DRAW);
            loadedBuffer.bindSSBO(0);
        }
        QUAD_INDICES.bindSSBO(1);
        QUAD_DATA.bindSSBO(2);
    }

    protected void updateAndBindUBOs(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum) {
        ShaderGlStateHelper.updateAndBind(partialTicks, mc);
        ShaderFP2StateHelper.updateAndBind(partialTicks, mc);
    }

    protected void prepareGlState(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum) {
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
    }

    protected void resetGlState(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull ICamera frustum) {
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
    }
}
