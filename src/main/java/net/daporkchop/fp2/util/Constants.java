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

package net.daporkchop.fp2.util;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.UnorderedThreadPoolEventExecutor;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.FP2;
import net.daporkchop.lib.common.misc.threadfactory.ThreadFactoryBuilder;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Various constants and helper methods used throughout the mod.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class Constants {
    public static final SimpleNetworkWrapper NETWORK_WRAPPER = NetworkRegistry.INSTANCE.newSimpleChannel(FP2.MODID);

    public static final EventExecutorGroup THREAD_POOL = new UnorderedThreadPoolEventExecutor(
            PorkUtil.CPU_COUNT,
            new ThreadFactoryBuilder().daemon().collapsingId().formatId().name("FP2 Worker Thread #%d").build());

    public static final boolean CC = Loader.isModLoaded("cubicchunks");
    public static final boolean CWG = Loader.isModLoaded("cubicgen");

    public static int convertARGB_ABGR(int in) {
        return (in & 0xFF00FF00) | ((in >>> 16) & 0xFF) | ((in & 0xFF) << 16);
    }

    @SideOnly(Side.CLIENT)
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
            buffer = createIntBuffer(HEADER_SIZE + volume * 3);
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

    public static boolean isCubicWorld(@NonNull World world) {
        return CC && world instanceof ICubicWorld && ((ICubicWorld) world).isCubicWorld();
    }

    //the following methods are copied from LWJGL's BufferUtils in order to ensure their availability on the dedicated server as well
    public static ByteBuffer createByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static ShortBuffer createShortBuffer(int size) {
        return createByteBuffer(size << 1).asShortBuffer();
    }

    public static CharBuffer createCharBuffer(int size) {
        return createByteBuffer(size << 1).asCharBuffer();
    }

    public static IntBuffer createIntBuffer(int size) {
        return createByteBuffer(size << 2).asIntBuffer();
    }

    public static LongBuffer createLongBuffer(int size) {
        return createByteBuffer(size << 3).asLongBuffer();
    }

    public static FloatBuffer createFloatBuffer(int size) {
        return createByteBuffer(size << 2).asFloatBuffer();
    }

    public static DoubleBuffer createDoubleBuffer(int size) {
        return createByteBuffer(size << 3).asDoubleBuffer();
    }
}
