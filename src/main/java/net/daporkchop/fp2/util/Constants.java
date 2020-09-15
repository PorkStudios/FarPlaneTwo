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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomCubicWorldType;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.apache.logging.log4j.Logger;

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
    //definitions of tile sizes
    public static final int T_SHIFT = 4;
    public static final int T_MASK = (1 << T_SHIFT) - 1;
    public static final int T_VOXELS = 1 << T_SHIFT;
    public static final int T_VERTS = T_VOXELS + 1;

    public static Logger LOGGER;

    public static final SimpleNetworkWrapper NETWORK_WRAPPER = NetworkRegistry.INSTANCE.newSimpleChannel(FP2.MODID);

    public static final Ref<ZstdDeflater> ZSTD_DEF = ThreadRef.soft(() -> Zstd.PROVIDER.deflater(Zstd.PROVIDER.deflateOptions()));
    public static final Ref<ZstdInflater> ZSTD_INF = ThreadRef.soft(() -> Zstd.PROVIDER.inflater(Zstd.PROVIDER.inflateOptions()));

    public static final Gson GSON = new Gson();
    public static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    public static final boolean CC = Loader.isModLoaded("cubicchunks");
    public static final boolean CWG = Loader.isModLoaded("cubicgen");

    public static void bigWarning(String format, Object... data) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        LOGGER.warn("****************************************");
        LOGGER.warn("* " + format, data);
        for (int i = 2; i < 8 && i < trace.length; i++) {
            LOGGER.warn("*  at {}{}", trace[i].toString(), i == 7 ? "..." : "");
        }
        LOGGER.warn("****************************************");
    }

    /**
     * Converts an ARGB color to ABGR.
     *
     * @param in the ARGB color
     * @return the input color in ABGR
     */
    public static int convertARGB_ABGR(int in) {
        return (in & 0xFF00FF00) | ((in >>> 16) & 0xFF) | ((in & 0xFF) << 16);
    }

    /**
     * Checks whether or not the given {@link World} is a Cubic Chunks world.
     *
     * @param world the {@link World} to check
     * @return whether or not the given {@link World} is a Cubic Chunks world
     */
    public static boolean isCubicWorld(@NonNull World world) {
        return CC && world instanceof ICubicWorld && ((ICubicWorld) world).isCubicWorld();
    }

    /**
     * Checks whether or not the given {@link World} uses Cubic World Gen.
     *
     * @param world the {@link World} to check
     * @return whether or not the given {@link World} uses Cubic World Gen
     */
    public static boolean isCwgWorld(@NonNull World world) {
        return isCubicWorld(world) && Constants.CWG && world.getWorldType() instanceof CustomCubicWorldType;
    }

    /**
     * Converts a combined light value returned by {@link IBlockAccess#getCombinedLight(BlockPos, int)} to a more compact format that only uses
     * a single byte.
     *
     * @param combinedLight the combined light value to pack
     * @return the packed combined light value
     */
    public static int packCombinedLight(int combinedLight) {
        return (combinedLight >> 16) | ((combinedLight >> 4) & 0xF);
    }

    public static int packedLightTo8BitVec2(int packedLight) {
        int blockLight = packedLight & 0xF;
        int skyLight = packedLight >> 4;
        return (((skyLight << 4) | skyLight) << 8) | ((blockLight << 4) | blockLight);
    }

    //the following methods are copied from LWJGL's BufferUtils in order to ensure their availability on the dedicated server as well

    /**
     * @param size the size of the buffer
     * @return a {@link ByteBuffer} using the native byte order
     */
    public static ByteBuffer createByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    /**
     * @param size the size of the buffer
     * @return a {@link ByteBuffer} using the native byte order
     */
    public static ShortBuffer createShortBuffer(int size) {
        return createByteBuffer(size << 1).asShortBuffer();
    }

    /**
     * @param size the size of the buffer
     * @return a {@link CharBuffer} using the native byte order
     */
    public static CharBuffer createCharBuffer(int size) {
        return createByteBuffer(size << 1).asCharBuffer();
    }

    /**
     * @param size the size of the buffer
     * @return a {@link IntBuffer} using the native byte order
     */
    public static IntBuffer createIntBuffer(int size) {
        return createByteBuffer(size << 2).asIntBuffer();
    }

    /**
     * @param size the size of the buffer
     * @return a {@link LongBuffer} using the native byte order
     */
    public static LongBuffer createLongBuffer(int size) {
        return createByteBuffer(size << 3).asLongBuffer();
    }

    /**
     * @param size the size of the buffer
     * @return a {@link FloatBuffer} using the native byte order
     */
    public static FloatBuffer createFloatBuffer(int size) {
        return createByteBuffer(size << 2).asFloatBuffer();
    }

    /**
     * @param size the size of the buffer
     * @return a {@link DoubleBuffer} using the native byte order
     */
    public static DoubleBuffer createDoubleBuffer(int size) {
        return createByteBuffer(size << 3).asDoubleBuffer();
    }

    /**
     * @param capacity the capacity of the buffer
     * @return a {@link ByteBuf} using the native byte order
     */
    @SuppressWarnings("deprecation")
    public static ByteBuf allocateByteBuf(int capacity) {
        return PooledByteBufAllocator.DEFAULT.directBuffer(capacity, capacity)
                .order(ByteOrder.nativeOrder());
    }

    //reflection

    /**
     * Functions similarly to {@link PUnsafe#pork_getOffset(Class, String)}, but with support for multiple possible field names.
     * <p>
     * Used to get unsafe offsets for fields whose names may be obfuscated.
     *
     * @param clazz the {@link Class} containing the field to get
     * @param names the possible names of the field
     * @return the field's offset
     * @throws IllegalArgumentException if no field with any of the given names could be found
     */
    public static long fieldOffset(@NonNull Class<?> clazz, @NonNull String... names) {
        for (String name : names) {
            try {
                return PUnsafe.objectFieldOffset(clazz.getField(name));
            } catch (NoSuchFieldException e) {
            }
        }
        throw new IllegalArgumentException(PStrings.fastFormat("Unable to find field in class %s with names %s", clazz.getCanonicalName(), Arrays.toString(names)));
    }

    //math

    /**
     * Computes the X coordinate of the point where the line defined by the two given values (at x=0 and x=1, respectively) intersects the X axis.
     *
     * @param d0 the value at x=0
     * @param d1 the value at x=1
     */
    public static double minimize(double d0, double d1) {
        return d0 / (d0 - d1);
    }
}
