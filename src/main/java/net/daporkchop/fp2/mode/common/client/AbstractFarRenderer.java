/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.client.ClientConstants;
import net.daporkchop.fp2.client.ShaderFP2StateHelper;
import net.daporkchop.fp2.client.ShaderGlStateHelper;
import net.daporkchop.fp2.client.TexUVs;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.util.DirectLongStack;
import net.daporkchop.fp2.util.math.Sphere;
import net.daporkchop.fp2.util.math.Volume;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarRenderer<POS extends IFarPos, T extends IFarTile> implements IFarRenderer {
    protected final IFarRenderMode<POS, T> mode;

    protected final FarRenderCache<POS, T> cache;

    protected final int maxLevel = FP2Config.maxLevels - 1;

    protected final GLBuffer drawCommandBuffer = new GLBuffer(GL_STREAM_DRAW);

    protected final DirectLongStack index = new DirectLongStack();
    protected final IFarRenderStrategy<POS, T> strategy;

    public AbstractFarRenderer(@NonNull IFarRenderMode<POS, T> mode) {
        this.mode = mode;

        this.strategy = this.createStrategy();

        this.cache = new FarRenderCache<>(this);
    }

    /**
     * @return the {@link IFarRenderStrategy} used by this renderer
     */
    protected abstract IFarRenderStrategy<POS, T> createStrategy();

    @Override
    public void render(float partialTicks, @NonNull WorldClient world, @NonNull Minecraft mc, @NonNull IFrustum frustum) {
        ClientConstants.update();

        checkGLError("pre fp2 build index");
        Volume[] volumes = this.createVolumesForSelection(partialTicks, world, mc, frustum);

        this.index.clear();
        this.cache.tree.select(volumes, frustum, this.index);

        if (this.index.isEmpty()) {
            return; //nothing to render...
        }

        checkGLError("pre fp2 setup");
        this.updateAndBindSSBOs(partialTicks, world, mc, frustum);

        this.prepareGlState(partialTicks, world, mc, frustum);
        try {
            this.updateAndBindUBOs(partialTicks, world, mc, frustum);
            checkGLError("post fp2 setup");

            checkGLError("pre fp2 render");
            this.index.doWithValues(this.strategy::render);
            checkGLError("post fp2 render");
        } finally {
            checkGLError("pre fp2 reset");
            this.resetGlState(partialTicks, world, mc, frustum);
            checkGLError("post fp2 reset");
        }

        /*FarRenderIndex index = this.cache.rebuildIndex(volumes, frustum);
        checkGLError("post fp2 build index");
        if (index.isEmpty()) {
            return; //nothing to render...
        }

        checkGLError("pre fp2 setup");
        this.updateAndBindSSBOs(partialTicks, world, mc, frustum);

        this.prepareGlState(partialTicks, world, mc, frustum);
        try (VertexArrayObject vao = this.cache.vao().bind()) {
            this.updateAndBindUBOs(partialTicks, world, mc, frustum);
            checkGLError("post fp2 setup");

            checkGLError("pre fp2 render");
            this.render0(partialTicks, world, mc, frustum, index);
            checkGLError("post fp2 render");
        } finally {
            checkGLError("pre fp2 reset");
            this.resetGlState(partialTicks, world, mc, frustum);
            checkGLError("post fp2 reset");
        }*/
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
