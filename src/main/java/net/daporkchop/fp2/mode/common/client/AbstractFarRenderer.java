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

package net.daporkchop.fp2.mode.common.client;

import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.client.ClientConstants;
import net.daporkchop.fp2.client.ShaderFP2StateHelper;
import net.daporkchop.fp2.client.ShaderGlStateHelper;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.mode.api.Compressed;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.mode.api.piece.IFarPiece;
import net.daporkchop.fp2.util.math.Sphere;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL40.*;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractFarRenderer<POS extends IFarPos, P extends IFarPiece> implements IFarRenderer<POS, P> {
    protected final AbstractFarRenderCache<POS, P> cache;

    protected final int maxLevel = FP2Config.maxLevels - 1;

    protected final GLBuffer drawCommandBuffer = new GLBuffer(GL_STREAM_DRAW);

    public AbstractFarRenderer(@NonNull WorldClient world) {
        this.cache = this.createCache();
    }

    protected abstract AbstractFarRenderCache<POS, P> createCache();

    @Override
    public void receivePiece(@NonNull Compressed<POS, P> piece) {
        ClientThreadExecutor.INSTANCE.execute(() -> this.cache.receivePiece(piece));
    }

    @Override
    public void unloadPiece(@NonNull POS pos) {
        ClientThreadExecutor.INSTANCE.execute(() -> this.cache.unloadPiece(pos));
    }

    /**
     * @return the {@link IFarRenderBaker} used by this renderer
     */
    public abstract IFarRenderBaker<POS, P> baker();

    /**
     * Actually renders the world.
     *
     * @param index the render index
     */
    protected abstract void render0(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull IFrustum frustum, @NonNull FarRenderIndex index);

    @Override
    public void render(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull IFrustum frustum) {
        ClientConstants.update();

        checkGLError("pre fp2 build index");
        FarRenderIndex index = this.cache.rebuildIndex(this.createVolumesForSelection(partialTicks, world, mc, frustum), frustum);
        checkGLError("post fp2 build index");
        if (index.isEmpty()) {
            return; //nothing to render...
        }

        checkGLError("pre fp2 setup");
        this.updateAndBindSSBOs(partialTicks, world, mc, frustum);

        this.prepareGlState(partialTicks, world, mc, frustum);
        try (VertexArrayObject vao = this.cache.vao().bind();
             GLBuffer drawCommandBuffer = this.drawCommandBuffer.bind(GL_DRAW_INDIRECT_BUFFER)) {
            this.updateAndBindUBOs(partialTicks, world, mc, frustum);
            checkGLError("post fp2 setup");

            checkGLError("pre fp2 render");
            this.render0(partialTicks, world, mc, frustum, index);
            checkGLError("post fp2 render");
        } finally {
            checkGLError("pre fp2 reset");
            this.resetGlState(partialTicks, world, mc, frustum);
            checkGLError("post fp2 reset");
        }
    }

    //TODO: use cylinders for heightmap and spheres for voxel
    protected Volume[] createVolumesForSelection(float partialTicks, WorldClient world, Minecraft mc, IFrustum frustum) {
        Volume[] ranges = new Volume[this.maxLevel + 1];
        Entity entity = mc.getRenderViewEntity();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        for (int i = 0; i < ranges.length; i++) {
            ranges[i] = new Sphere(x, y, z, FP2Config.levelCutoffDistance + T_VOXELS << i);
        }
        return ranges;
    }

    protected void updateAndBindSSBOs(float partialTicks, WorldClient world, Minecraft mc, IFrustum frustum) {
        TexUVs.bind();
    }

    protected void updateAndBindUBOs(float partialTicks, WorldClient world, Minecraft mc, IFrustum frustum) {
        ShaderGlStateHelper.updateAndBind(partialTicks, mc);
        ShaderFP2StateHelper.updateAndBind(partialTicks, mc);
    }

    protected void prepareGlState(float partialTicks, WorldClient world, Minecraft mc, IFrustum frustum) {
        GlStateManager.depthFunc(GL_LESS);

        mc.entityRenderer.enableLightmap();
    }

    protected void resetGlState(float partialTicks, WorldClient world, Minecraft mc, IFrustum frustum) {
        mc.entityRenderer.disableLightmap();

        GlStateManager.depthFunc(GL_LEQUAL);
    }
}
