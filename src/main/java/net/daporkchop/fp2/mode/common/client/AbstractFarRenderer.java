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
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.client.gl.object.GLBuffer;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.mode.api.client.IFarRenderer;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.util.datastructure.DirectLongStack;
import net.daporkchop.fp2.util.math.Sphere;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.util.Constants.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarRenderer<POS extends IFarPos, T extends IFarTile> extends AbstractReleasable implements IFarRenderer {
    protected final IFarClientContext<POS, T> context;
    protected final IFarRenderMode<POS, T> mode;

    protected final BakeManager<POS, T> bakeManager;

    protected final int maxLevel = FP2Config.maxLevels - 1;

    protected final GLBuffer drawCommandBuffer = new GLBuffer(GL_STREAM_DRAW);

    protected final DirectLongStack index = new DirectLongStack();
    protected final IFarRenderStrategy<POS, T> strategy;

    public AbstractFarRenderer(@NonNull IFarClientContext<POS, T> context) {
        this.context = context;
        this.mode = context.mode();

        this.strategy = this.strategy0();
        this.bakeManager = this.bakeManager0();
    }

    /**
     * @return the {@link IFarRenderStrategy} used by this renderer
     */
    protected abstract IFarRenderStrategy<POS, T> strategy0();

    /**
     * @return a new {@link BakeManager}
     */
    protected BakeManager<POS, T> bakeManager0() {
        return new BakeManager<>(this, this.context.tileCache());
    }

    @Override
    public void prepare(float partialTicks, @NonNull Minecraft mc, @NonNull IFrustum frustum) {
        checkGLError("pre fp2 build index");

        Volume[] volumes = this.createVolumesForSelection(partialTicks, mc);

        this.index.clear();
        this.bakeManager.tree.select(volumes, frustum, this.index);

        if (this.index.isEmpty()) {
            return; //nothing to render...
        }

        this.index.doWithValues(this.strategy::prepareRender);
        checkGLError("post fp2 prepare");
    }

    @Override
    public void render(@NonNull Minecraft mc, @NonNull BlockRenderLayer layer, boolean pre) {
        if (this.index.isEmpty()) {
            return; //nothing to render...
        }

        this.strategy.render(layer, pre);
        checkGLError("post fp2 render");
    }

    //TODO: use cylinders for heightmap and spheres for voxel
    protected Volume[] createVolumesForSelection(float partialTicks, Minecraft mc) {
        Volume[] ranges = new Volume[this.maxLevel + 1];
        Entity entity = mc.getRenderViewEntity();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        for (int i = 0; i < ranges.length; i++) {
            //TODO: remove * 3 and fix transition seams in FarRenderTree
            ranges[i] = new Sphere(x, y, z, (FP2Config.levelCutoffDistance + (T_VOXELS * 3)) << i);
            //ranges[i] = new Cube(x, y, z, FP2Config.levelCutoffDistance + T_VOXELS << i);
            //ranges[i] = new Cylinder(x, z, (FP2Config.levelCutoffDistance + T_VOXELS << i) + T_VOXELS * 4);
        }
        return ranges;
    }

    @Override
    protected void doRelease() {
        this.strategy.release();
        this.bakeManager.release();
    }
}
