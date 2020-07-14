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
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.Config;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.lib.common.misc.threadfactory.ThreadFactoryBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.IntBuffer;
import java.util.List;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
@SideOnly(Side.CLIENT)
public class ClientConstants {
    public static EventExecutorGroup RENDER_WORKERS;

    public static boolean reversedZ = false;

    public void init() {
        checkState(RENDER_WORKERS == null);

        RENDER_WORKERS = new UnorderedThreadPoolEventExecutor(
                Config.ClientConfig.renderThreads,
                new ThreadFactoryBuilder().daemon().collapsingId().formatId().name("FP2 Rendering Thread #%d").priority(Thread.MIN_PRIORITY).build());
    }

    public void shutdown() {
        Future<?> mesh = RENDER_WORKERS.shutdownGracefully();

        FP2.LOGGER.info("Shutting down render worker pool...");
        mesh.syncUninterruptibly();
        FP2.LOGGER.info("Done.");

        RENDER_WORKERS = null;
    }

    public static IntBuffer renderableChunksMask(Minecraft mc, IntBuffer buffer) {
        final int HEADER_SIZE = 2 * 4;

        List<Vec3i> positions = mc.renderGlobal.renderInfos.stream()
                .map(i -> i.renderChunk)
                .filter(r -> r.compiledChunk != CompiledChunk.DUMMY && !r.compiledChunk.isEmpty())
                .map(RenderChunk::getPosition)
                .map(p -> new Vec3i(p.getX() >> 4, p.getY() >> 4, p.getZ() >> 4))
                .collect(Collectors.toList());

        int minX = positions.stream().mapToInt(Vec3i::getX).min().orElse(0);
        int maxX = positions.stream().mapToInt(Vec3i::getX).max().orElse(0) + 1;
        int minY = positions.stream().mapToInt(Vec3i::getY).min().orElse(0);
        int maxY = positions.stream().mapToInt(Vec3i::getY).max().orElse(0) + 1;
        int minZ = positions.stream().mapToInt(Vec3i::getZ).min().orElse(0);
        int maxZ = positions.stream().mapToInt(Vec3i::getZ).max().orElse(0) + 1;
        int dx = maxX - minX;
        int dy = maxY - minY;
        int dz = maxZ - minZ;
        int volume = dx * dy * dz;

        if (buffer == null || buffer.capacity() < HEADER_SIZE + volume * 3) {
            buffer = Constants.createIntBuffer(HEADER_SIZE + volume * 3);
        }
        buffer.clear();

        if (positions.isEmpty()) {
            return buffer;
        }

        buffer.put(minX).put(minY).put(minZ).put(0)
                .put(dx).put(dy).put(dz).put(0);
        for (int i = 0, len = positions.size(); i < len; i++) {
            Vec3i pos = positions.get(i);
            int index = ((pos.getX() - minX) * dy + (pos.getY() - minY)) * dz + (pos.getZ() - minZ);
            buffer.put(HEADER_SIZE + (index >> 5), buffer.get(HEADER_SIZE + (index >> 5)) | (1 << (index & 0x1F)));
        }

        buffer.clear();
        return buffer;
    }

    public void beginRenderWorld() {
        if (!reversedZ) {
            reversedZ = true;
            //glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
            GlStateManager.clearDepth(0.0d);
            GlStateManager.clear(GL_DEPTH_BUFFER_BIT);
            glDepthRange(1, 0);

            int depthFunc = invertGlDepthFunction(GlStateManager.depthState.depthFunc);
            if (depthFunc != GlStateManager.depthState.depthFunc) {
                glDepthFunc(GlStateManager.depthState.depthFunc = depthFunc);
            }
        }
    }

    public void endRenderWorld() {
        if (reversedZ) {
            reversedZ = false;
            //glClipControl(GL_LOWER_LEFT, GL_NEGATIVE_ONE_TO_ONE);
            GlStateManager.clearDepth(1.0d);
            GlStateManager.clear(GL_DEPTH_BUFFER_BIT);
            glDepthRange(0, 1);

            int depthFunc = invertGlDepthFunction(GlStateManager.depthState.depthFunc);
            if (depthFunc != GlStateManager.depthState.depthFunc) {
                glDepthFunc(GlStateManager.depthState.depthFunc = depthFunc);
            }
        }
    }

    public int invertGlDepthFunction(int in) {
        switch (in) {
            case GL_LESS:
                return GL_GREATER;
            case GL_GREATER:
                return GL_LESS;
            case GL_LEQUAL:
                return GL_GEQUAL;
            case GL_GEQUAL:
                return GL_LEQUAL;
        }
        return in;
    }
}
