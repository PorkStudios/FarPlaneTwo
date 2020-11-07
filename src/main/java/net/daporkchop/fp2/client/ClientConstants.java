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

package net.daporkchop.fp2.client;

import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.client.CubeProviderClient;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.util.threading.keyed.KeyedTaskScheduler;
import net.daporkchop.fp2.util.threading.keyed.PriorityKeyedTaskScheduler;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static java.lang.Math.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
@SideOnly(Side.CLIENT)
public class ClientConstants {
    public static final Minecraft mc = Minecraft.getMinecraft();

    public static KeyedTaskScheduler<IFarPos> RENDER_WORKERS;

    public static AxisAlignedBB CURRENT_RENDER_BB;

    public static void init() {
        checkState(RENDER_WORKERS == null, "render workers already running?!?");

        RENDER_WORKERS = new PriorityKeyedTaskScheduler<>(
                FP2Config.client.renderThreads,
                PThreadFactories.builder().daemon().minPriority().collapsingId().name("FP2 Rendering Thread #%d").build());
    }

    public static void shutdown() {
        checkState(RENDER_WORKERS != null, "render workers not running?!?");

        RENDER_WORKERS.shutdown();
        RENDER_WORKERS = null;
    }

    public void update() {
        double offset = -8.0d;
        double x = mc.player.posX + offset;
        double y = mc.player.posY + offset;
        double z = mc.player.posZ + offset;
        double sizeHorizontal = (mc.gameSettings.renderDistanceChunks - 2) << 4;
        double sizeVertical = CC ? max((CubicChunksConfig.verticalCubeLoadDistance - 2) << 4, sizeHorizontal) : sizeHorizontal;
        CURRENT_RENDER_BB = new AxisAlignedBB(
                x - sizeHorizontal, y - sizeVertical, z - sizeHorizontal,
                x + sizeHorizontal, y + sizeVertical, z + sizeHorizontal);
    }

    public boolean isChunkRenderable(int x, int y, int z) {
        if (false) {
            return false;
        }

        if (!CURRENT_RENDER_BB.intersects(x << 4, y << 4, z << 4, (x + 1) << 4, (y + 1) << 4, (z + 1) << 4)) {
            return false;
        }

        ChunkProviderClient provider = mc.world.getChunkProvider();
        if (CC && provider instanceof CubeProviderClient) {
            return ((CubeProviderClient) provider).getLoadedCube(x, y, z) != null;
        } else {
            return provider.isChunkGeneratedAt(x, z);
        }
    }
}
