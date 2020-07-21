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

import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.Config;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.misc.threadfactory.ThreadFactoryBuilder;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
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
    public static EventExecutorGroup RENDER_WORKERS;

    public static void init() {
        checkState(RENDER_WORKERS == null);

        RENDER_WORKERS = new UnorderedThreadPoolEventExecutor(
                Config.client.renderThreads,
                new ThreadFactoryBuilder().daemon().collapsingId().formatId().name("FP2 Rendering Thread #%d").priority(Thread.MIN_PRIORITY).build());
    }

    public static void shutdown() {
        Future<?> mesh = RENDER_WORKERS.shutdownGracefully();

        FP2.LOGGER.info("Shutting down render worker pool...");
        mesh.syncUninterruptibly();
        FP2.LOGGER.info("Done.");

        RENDER_WORKERS = null;
    }

    public static final BlockRenderLayer[] BLOCK_RENDER_LAYERS = BlockRenderLayer.values();

    static {
        checkState(BLOCK_RENDER_LAYERS.length == 4, BLOCK_RENDER_LAYERS.length);
    }

    public static IntBuffer renderableChunksMask(Minecraft mc, IntBuffer buffer) {
        final int HEADER_SIZE = 2 * 4;

        CompiledChunkMask[] masks = mc.renderGlobal.renderInfos.stream()
                .map(i -> i.renderChunk)
                .filter(r -> !r.compiledChunk.isEmpty())
                .map(r -> {
                    CompiledChunk compiledChunk = r.compiledChunk;
                    int layers = 0;
                    for (int i = 0; i < 4; i++)  {
                        if (!compiledChunk.isLayerEmpty(BLOCK_RENDER_LAYERS[i]))    {
                            layers |= 1 << i;
                        }
                    }

                    BlockPos pos = r.position;
                    return new CompiledChunkMask(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4, layers);
                })
                .toArray(CompiledChunkMask[]::new);

        if (masks.length == 0)  {
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

        for (int i = 0, len = masks.length; i < len; i++)   {
            CompiledChunkMask mask = masks[i];
            if (mask.x < minX)  {
                minX = mask.x;
            }
            if (mask.x > maxX)  {
                maxX = mask.x;
            }
            if (mask.y < minY)  {
                minY = mask.y;
            }
            if (mask.y > maxY)  {
                maxY = mask.y;
            }
            if (mask.z < minZ)  {
                minZ = mask.z;
            }
            if (mask.z > maxZ)  {
                maxZ = mask.z;
            }
        }

        int dx = ++maxX - minX;
        int dy = ++maxY - minY;
        int dz = ++maxZ - minZ;
        int volume = dx * dy * dz;

        if (buffer == null || buffer.capacity() < HEADER_SIZE + volume * 3) {
            buffer = Constants.createIntBuffer(HEADER_SIZE + volume * 3);
        }
        buffer.clear();
        PUnsafe.setMemory(PUnsafe.pork_directBufferAddress(buffer), buffer.capacity() * 4, (byte) 0);

        buffer.put(minX).put(minY).put(minZ).put(0)
                .put(dx).put(dy).put(dz).put(0);

        for (int i = 0, len = masks.length; i < len; i++) {
            CompiledChunkMask mask = masks[i];
            int index = ((mask.x - minX) * dy + (mask.y - minY)) * dz + (mask.z - minZ);
            int arrayIndex = HEADER_SIZE + (index >> 3);
            buffer.put(arrayIndex, buffer.get(arrayIndex) | (mask.layers << ((index << 2) & 0xC)));
        }

        buffer.clear();
        return buffer;
    }

    @AllArgsConstructor
    private static final class CompiledChunkMask {
        private final int x;
        private final int y;
        private final int z;
        private final int layers;
    }
}
