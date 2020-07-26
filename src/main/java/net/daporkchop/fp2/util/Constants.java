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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.FP2;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.ref.Ref;
import net.daporkchop.lib.common.ref.ThreadRef;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.compression.zstd.ZstdDeflater;
import net.daporkchop.lib.compression.zstd.ZstdInflater;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Various constants and helper methods used throughout the mod.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class Constants {
    public static final SimpleNetworkWrapper NETWORK_WRAPPER = NetworkRegistry.INSTANCE.newSimpleChannel(FP2.MODID);

    public static final Ref<ZstdDeflater> DEFLATER_CACHE = ThreadRef.soft(() -> Zstd.PROVIDER.deflater(Zstd.PROVIDER.deflateOptions()));
    public static final Ref<ZstdInflater> INFLATER_CACHE = ThreadRef.soft(() -> Zstd.PROVIDER.inflater(Zstd.PROVIDER.inflateOptions()));

    public static final boolean CC = Loader.isModLoaded("cubicchunks");
    public static final boolean CWG = Loader.isModLoaded("cubicgen");

    public static int convertARGB_ABGR(int in) {
        return (in & 0xFF00FF00) | ((in >>> 16) & 0xFF) | ((in & 0xFF) << 16);
    }

    public static boolean isCubicWorld(@NonNull World world) {
        return CC && world instanceof ICubicWorld && ((ICubicWorld) world).isCubicWorld();
    }

    public static int packCombinedLight(int combinedLight) {
        return (combinedLight >> 16) | ((combinedLight >> 4) & 0xF);
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

    @SuppressWarnings("deprecation")
    public static ByteBuf allocateByteBuf(int capacity) {
        return PooledByteBufAllocator.DEFAULT.directBuffer(capacity, capacity)
                .order(ByteOrder.nativeOrder());
    }

    public static long fieldOffset(@NonNull Class<?> clazz, @NonNull String... names) {
        for (String name : names) {
            try {
                return PUnsafe.objectFieldOffset(clazz.getField(name));
            } catch (NoSuchFieldException e) {
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("Unable to find field in class %s with names %s", clazz.getCanonicalName(), Arrays.toString(names)));
    }
}
