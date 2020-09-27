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

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.FP2Config;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.threading.keyed.KeyedTaskScheduler;
import net.daporkchop.fp2.util.threading.keyed.PriorityKeyedTaskScheduler;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.IntBuffer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
@SideOnly(Side.CLIENT)
public class ClientConstants {
    public static final Minecraft mc = Minecraft.getMinecraft();

    public static KeyedTaskScheduler<IFarPos> RENDER_WORKERS;

    public synchronized static void init() {
        checkState(RENDER_WORKERS == null, "render workers already running?!?");

        RENDER_WORKERS = new PriorityKeyedTaskScheduler<>(
                FP2Config.client.renderThreads,
                PThreadFactories.builder().daemon().minPriority().collapsingId().name("FP2 Rendering Thread #%d").build());
    }

    public synchronized static void shutdown() {
        checkState(RENDER_WORKERS != null, "render workers not running?!?");

        RENDER_WORKERS.shutdown();
        RENDER_WORKERS = null;
    }

    public static IntBuffer renderableChunksMask(Minecraft mc, IntBuffer buffer) {
        final int HEADER_SIZE = 2 * 4;

        Vec3i[] positions = mc.renderGlobal.renderInfos.stream()
                .map(i -> i.renderChunk)
                .filter(r -> r.compiledChunk != CompiledChunk.DUMMY && !r.compiledChunk.isEmpty())
                .map(RenderChunk::getPosition)
                .map(p -> new Vec3i(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4))
                .toArray(Vec3i[]::new);

        if (positions.length == 0) {
            if (buffer == null) {
                buffer = Constants.createIntBuffer(HEADER_SIZE);
            }
            return buffer;
        }

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int i = 0, len = positions.length; i < len; i++) {
            Vec3i pos = positions[i];
            if (pos.getX() < minX) {
                minX = pos.getX();
            }
            if (pos.getX() > maxX) {
                maxX = pos.getX();
            }
            if (pos.getY() < minY) {
                minY = pos.getY();
            }
            if (pos.getY() > maxY) {
                maxY = pos.getY();
            }
            if (pos.getZ() < minZ) {
                minZ = pos.getZ();
            }
            if (pos.getZ() > maxZ) {
                maxZ = pos.getZ();
            }
        }

        int dx = ++maxX - minX;
        int dy = ++maxY - minY;
        int dz = ++maxZ - minZ;
        int volume = dx * dy * dz;

        if (buffer == null || buffer.capacity() < HEADER_SIZE + volume) {
            buffer = Constants.createIntBuffer(HEADER_SIZE + volume);
        }
        buffer.clear();
        PUnsafe.setMemory(PUnsafe.pork_directBufferAddress(buffer), buffer.capacity() * 4, (byte) 0);

        buffer.put(minX).put(minY).put(minZ).put(0)
                .put(dx).put(dy).put(dz).put(0);

        for (int i = 0, len = positions.length; i < len; i++) {
            Vec3i pos = positions[i];
            int index = ((pos.getX() - minX) * dy + (pos.getY() - minY)) * dz + (pos.getZ() - minZ);
            buffer.put(HEADER_SIZE + (index >> 5), buffer.get(HEADER_SIZE + (index >> 5)) | (1 << (index & 0x1F)));
        }

        buffer.clear();
        return buffer;
    }
}
