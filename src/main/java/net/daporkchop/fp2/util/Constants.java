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

package net.daporkchop.fp2.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.cubicgen.customcubic.CustomTerrainGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.FP2;
import net.daporkchop.fp2.asm.core.server.IMixinWorldBorder;
import net.daporkchop.fp2.util.math.IntAxisAlignedBB;
import net.daporkchop.lib.common.misc.string.PStrings;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.common.pool.handle.Handle;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.compression.zstd.Zstd;
import net.daporkchop.lib.compression.zstd.ZstdDeflater;
import net.daporkchop.lib.compression.zstd.ZstdInflater;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import javax.swing.JOptionPane;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

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

    public static final int GTH_SHIFT = 2; //generation tile shift (horizontal)
    public static final int GTH_MASK = (1 << GTH_SHIFT) - 1;
    public static final int GTH_COUNT = T_VOXELS >> GTH_SHIFT;
    public static final int GTH_SIZE = 1 << GTH_SHIFT; //generation tile size

    public static final int GTV_SHIFT = GTH_SHIFT + 1; //generation tile shift (vertical)
    public static final int GTV_MASK = (1 << GTV_SHIFT) - 1;
    public static final int GTV_COUNT = T_VOXELS >> GTV_SHIFT;
    public static final int GTV_SIZE = 1 << GTV_SHIFT;

    //this is enough levels for the tree to encompass the entire minecraft world
    public static final int MAX_LODS = 32 - Integer.numberOfLeadingZeros(60_000_000 >> T_SHIFT);

    public static final boolean FP2_TEST = Boolean.parseBoolean(System.getProperty("fp2.test", "false"));

    public static Logger FP2_LOG = new SimpleLogger("[fp2 bootstrap]", Level.INFO, true, false, true, false, "[yyyy/MM/dd HH:mm:ss:SSS]", null, new PropertiesUtil("log4j2.simplelog.properties"), System.out);

    public static final Cached<ZstdDeflater> ZSTD_DEF = Cached.threadLocal(() -> Zstd.PROVIDER.deflater(Zstd.PROVIDER.deflateOptions()), ReferenceStrength.WEAK);
    public static final Cached<ZstdInflater> ZSTD_INF = Cached.threadLocal(() -> Zstd.PROVIDER.inflater(Zstd.PROVIDER.inflateOptions()), ReferenceStrength.WEAK);

    public static final Gson GSON = new Gson();
    public static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    public static final IBlockState STATE_AIR = Blocks.AIR.getDefaultState();

    public static final boolean CC = !FP2_TEST && Loader.isModLoaded("cubicchunks");
    public static final boolean CWG = !FP2_TEST && Loader.isModLoaded("cubicgen");

    public static final Cached<ArrayAllocator<byte[]>> ALLOC_BYTE = Cached.threadLocal(() -> ArrayAllocator.pow2(byte[]::new, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);
    public static final Cached<ArrayAllocator<int[]>> ALLOC_INT = Cached.threadLocal(() -> ArrayAllocator.pow2(int[]::new, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);
    public static final Cached<ArrayAllocator<float[]>> ALLOC_FLOAT = Cached.threadLocal(() -> ArrayAllocator.pow2(float[]::new, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);
    public static final Cached<ArrayAllocator<double[]>> ALLOC_DOUBLE = Cached.threadLocal(() -> ArrayAllocator.pow2(double[]::new, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);
    public static final Cached<ArrayAllocator<Object[]>> ALLOC_OBJECT = Cached.threadLocal(() -> ArrayAllocator.pow2(Object[]::new, ReferenceStrength.STRONG, 32), ReferenceStrength.WEAK);

    public static final boolean IS_DEDICATED_SERVER = !FP2_TEST && FMLCommonHandler.instance().getSide() == Side.SERVER;
    public static final boolean IS_CLIENT = !FP2_TEST && FMLCommonHandler.instance().getSide() == Side.CLIENT;

    @SideOnly(Side.CLIENT)
    public static Minecraft MC;

    static {
        if (IS_CLIENT) { //initialize client-only fields here to prevent errors
            MC = Minecraft.getMinecraft();
        }
    }

    /**
     * Copypasta of {@link FMLLog#bigWarning(String, Object...)} with support for multi-line error messages.
     * <p>
     * Note that formatting is done using {@link PStrings#fastFormat(String, Object...)}, not Log4j's {@code {}}-style formatting.
     */
    public static void bigWarning(@NonNull String format, Object... params) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();

        FP2_LOG.warn("****************************************");
        for (String line : PStrings.fastFormat(format, params).split("\n")) {
            FP2_LOG.warn("* " + line);
        }
        for (int i = 2; i < 10 && i < trace.length; i++) {
            FP2_LOG.warn("*  at {}{}", trace[i].toString(), i == 9 ? "..." : "");
        }
        FP2_LOG.warn("****************************************");
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
     * Checks whether or not the given {@link WorldServer} uses Cubic World Gen.
     *
     * @param world the {@link WorldServer} to check
     * @return whether or not the given {@link WorldServer} uses Cubic World Gen
     */
    public static boolean isCwgWorld(@NonNull WorldServer world) {
        return CWG && getTerrainGenerator(world) instanceof CustomTerrainGenerator;
    }

    /**
     * Gets the terrain generator for the given {@link WorldServer}.
     *
     * @param world the {@link WorldServer} to get the terrain generator for
     * @return the terrain generator instance. May be an arbitrary type (e.g. {@link IChunkGenerator} or {@link ICubeGenerator})
     */
    public static Object getTerrainGenerator(@NonNull WorldServer world) {
        return isCubicWorld(world)
                ? ((ICubicWorldServer) world).getCubeGenerator() //this is a Cubic Chunks world, so we want to use the cube generator
                : world.getChunkProvider().chunkGenerator;
    }

    /**
     * Gets the coordinate limits for the given world.
     *
     * @param world the {@link WorldServer} to get the Y coordinate limits for
     * @return the coordinate limits
     */
    public static IntAxisAlignedBB getBounds(@NonNull World world) {
        int minY = 0;
        int maxY = world.getHeight() - 1;

        if (isCubicWorld(world)) { //this is a cubic chunks world, use cubic chunks API to get the real world height limits
            minY = ((ICubicWorld) world).getMinHeight();
            maxY = ((ICubicWorld) world).getMaxHeight() - 1;
        }
        final int HORIZONTAL_LIMIT = ((IMixinWorldBorder)world.getWorldBorder()).getWorldSize();
        return new IntAxisAlignedBB(-HORIZONTAL_LIMIT, minY, -HORIZONTAL_LIMIT, HORIZONTAL_LIMIT, maxY, HORIZONTAL_LIMIT);
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

    public static int packLight(int skyLight, int blockLight) {
        return (skyLight << 4) | blockLight;
    }

    public static int packedLightTo8BitVec2(int packedLight) {
        int blockLight = packedLight & 0xF;
        int skyLight = packedLight >> 4;
        return (((skyLight << 4) | skyLight) << 8) | ((blockLight << 4) | blockLight);
    }

    private static Class<?> toClass(@NonNull Object in) {
        if (in instanceof Class) {
            return uncheckedCast(in);
        } else if (in instanceof String) {
            return PorkUtil.classForName((String) in);
        } else {
            throw new IllegalArgumentException(PorkUtil.className(in));
        }
    }

    public static MethodHandle staticHandle(@NonNull Object clazz, @NonNull Object returnType, @NonNull String name, @NonNull Object... params) {
        try {
            MethodType type = MethodType.methodType(toClass(returnType), Arrays.stream(params).map(Constants::toClass).toArray(Class[]::new));
            return MethodHandles.lookup().findStatic(toClass(clazz), name, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
     * @param capacity the maximum capacity of the buffer
     * @return a {@link ByteBuf}
     */
    public static ByteBuf allocateByteBuf(int capacity) {
        return ByteBufAllocator.DEFAULT.directBuffer(min(capacity, 256), capacity);
    }

    /**
     * @param capacity the capacity of the buffer
     * @return a {@link ByteBuf}
     */
    public static ByteBuf allocateByteBufExactly(int capacity) {
        return ByteBufAllocator.DEFAULT.directBuffer(capacity, capacity);
    }

    /**
     * @param capacity the capacity of the buffer
     * @return a {@link ByteBuf} using the native byte order
     */
    @SuppressWarnings("deprecation")
    public static ByteBuf allocateByteBufNativeOrder(int capacity) {
        return allocateByteBuf(capacity).order(ByteOrder.nativeOrder());
    }

    /**
     * Ensures that the given buffer has the requested amount of space remaining. If not, the buffer will be grown.
     *
     * @param buffer the buffer
     * @param needed the required space
     * @return the buffer, or a new one if the buffer was grown
     */
    public static IntBuffer ensureWritable(IntBuffer buffer, int needed) {
        if (buffer.remaining() < needed) {
            IntBuffer bigger = createIntBuffer(buffer.capacity() << 1);
            bigger.put((IntBuffer) buffer.flip());
            PUnsafe.pork_releaseBuffer(buffer);
            buffer = bigger;
        }
        return buffer;
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

    //buffer I/O
    public static void writeVarInt(@NonNull ByteBuf dst, int value) {
        try (Handle<byte[]> handle = PorkUtil.TINY_BUFFER_POOL.get()) {
            byte[] arr = handle.get();
            int i = 0;
            do {
                byte temp = (byte) (value & 0b01111111);
                value >>>= 7;
                if (value != 0) {
                    temp |= 0b10000000;
                }
                arr[i++] = temp;
            } while (value != 0);
            dst.writeBytes(arr, 0, i);
        }
    }

    public static void writeVarIntZigZag(@NonNull ByteBuf dst, int value) {
        writeVarInt(dst, (value << 1) ^ (value >> 31));
    }

    public static void writeVarLong(@NonNull ByteBuf dst, long value) {
        try (Handle<byte[]> handle = PorkUtil.TINY_BUFFER_POOL.get()) {
            byte[] arr = handle.get();
            int i = 0;
            do {
                byte temp = (byte) (value & 0b01111111);
                value >>>= 7L;
                if (value != 0) {
                    temp |= 0b10000000;
                }
                arr[i++] = temp;
            } while (value != 0);
            dst.writeBytes(arr, 0, i);
        }
    }

    public static void writeVarLongZigZag(@NonNull ByteBuf dst, long value) {
        writeVarLong(dst, (value << 1L) ^ (value >> 63L));
    }

    public static int readVarInt(@NonNull ByteBuf src) {
        int bytesRead = 0;
        int value = 0;
        int b;
        do {
            b = src.readUnsignedByte();
            value |= ((b & 0b01111111) << (7 * bytesRead));

            if (++bytesRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((b & 0b10000000) != 0);
        return value;
    }

    public static int readVarIntZigZag(@NonNull ByteBuf src) {
        int i = readVarInt(src);
        return (i >> 1) ^ -(i & 1);
    }

    public static long readVarLong(@NonNull ByteBuf src) {
        int bytesRead = 0;
        long value = 0;
        int b;
        do {
            b = src.readUnsignedByte();
            value |= ((b & 0b01111111L) << (7 * bytesRead));

            if (++bytesRead > 10) {
                throw new RuntimeException("VarLong is too big");
            }
        } while ((b & 0b10000000) != 0);
        return value;
    }

    public static long readVarLongZigZag(@NonNull ByteBuf src) {
        long l = readVarLong(src);
        return (l >> 1L) ^ -(l & 1L);
    }

    public static void writeString(@NonNull ByteBuf dst, @NonNull String value) {
        int i = dst.writerIndex();
        int len = dst.writeInt(-1).writeCharSequence(value, StandardCharsets.UTF_8);
        dst.setInt(i, len);
    }

    public static String readString(@NonNull ByteBuf src) {
        return src.readCharSequence(src.readInt(), StandardCharsets.UTF_8).toString();
    }

    public static int or(@NonNull int[] arr) {
        int val = 0;
        for (int i : arr) {
            val |= i;
        }
        return val;
    }

    public static void unsupported(String msg) {
        bigWarning(msg);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            JOptionPane.showMessageDialog(null, msg, null, JOptionPane.ERROR_MESSAGE);
        }
        FMLCommonHandler.instance().exitJava(1, true);
    }

    public static <T> T preventInline(T value) {
        return value;
    }
}
