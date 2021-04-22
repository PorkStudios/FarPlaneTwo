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

package net.daporkchop.fp2.mode.heightmap.client;

import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.ctx.IFarClientContext;
import net.daporkchop.fp2.mode.common.client.AbstractFarRenderer;
import net.daporkchop.fp2.mode.common.client.IFarRenderStrategy;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.heightmap.HeightmapTile;
import net.daporkchop.fp2.util.math.Cylinder;
import net.daporkchop.fp2.util.math.Sphere;
import net.daporkchop.fp2.util.math.Volume;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
@SideOnly(Side.CLIENT)
public abstract class HeightmapRenderer extends AbstractFarRenderer<HeightmapPos, HeightmapTile> {
    public HeightmapRenderer(@NonNull IFarClientContext<HeightmapPos, HeightmapTile> context) {
        super(context);
    }

    @Override
    protected Volume[] createVolumesForSelection(float partialTicks, Minecraft mc) {
        Volume[] ranges = new Volume[this.maxLevel + 1];
        Entity entity = mc.getRenderViewEntity();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        for (int i = 0; i < ranges.length; i++) {
            ranges[i] = new Cylinder(x, z, (FP2Config.levelCutoffDistance + (T_VOXELS * 3)) << i);
        }
        return ranges;
    }

    public static class ShaderMultidraw extends HeightmapRenderer {
        public ShaderMultidraw(@NonNull IFarClientContext<HeightmapPos, HeightmapTile> context) {
            super(context);
        }

        @Override
        protected IFarRenderStrategy<HeightmapPos, HeightmapTile> strategy0() {
            return new ShaderBasedIndexedMultidrawHeightmapRenderStrategy();
        }
    }
}
