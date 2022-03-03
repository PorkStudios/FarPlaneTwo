/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.api.world;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class BlockWorldConstants {
    /**
     * The block type used to indicate that a block is invisible. Block states using this type will be treated as air.
     *
     * @see FExtendedStateRegistryData#type(int)
     */
    public static final int BLOCK_TYPE_INVISIBLE = 0;

    /**
     * The block type used to indicate that a block is partially transparent or translucent.
     *
     * @see FExtendedStateRegistryData#type(int)
     */
    public static final int BLOCK_TYPE_TRANSPARENT = 1;

    /**
     * The block type used to indicate that a block is fully opaque.
     *
     * @see FExtendedStateRegistryData#type(int)
     */
    public static final int BLOCK_TYPE_OPAQUE = 2;

    /**
     * Packs the given sky light and block light values together into a single {@code byte}.
     *
     * @param skyLight   the sky light value. The 4 lowest bits are kept, all other information is discarded.
     * @param blockLight the block light value. The 4 lowest bits are kept, all other information is discarded.
     * @return the packed light value
     */
    public static byte packLight(int skyLight, int blockLight) {
        return (byte) ((skyLight << 4) | (blockLight & 0xF));
    }

    /**
     * Unpacks the sky light value from the given {@code byte}.
     *
     * @param packedLight the packed light data, as returned by {@link #packLight(int, int)}
     * @return the sky light value
     */
    public static int unpackSkyLight(byte packedLight) {
        return (packedLight & 0xFF) >>> 4; //i'm pretty sure that doing it in this order will allow JIT to interpret it as an unsigned byte, thus reducing it to a simple shift
    }

    /**
     * Unpacks the block light value from the given {@code byte}.
     *
     * @param packedLight the packed light data, as returned by {@link #packLight(int, int)}
     * @return the block light value
     */
    public static int unpackBlockLight(byte packedLight) {
        return packedLight & 0xF;
    }

    /**
     * Validates the arguments for a call to {@link FBlockWorld#getData(int[], int, int, int[], int, int, byte[], int, int, int, int, int, int, int, int, int, int, int)},
     * throwing an exception if the parameters are invalid.
     *
     * @see FBlockWorld#getData(int[], int, int, int[], int, int, byte[], int, int, int, int, int, int, int, int, int, int, int)
     */
    public static void validateArgsForGetData(
            int[] states, int statesOff, int statesStride,
            int[] biomes, int biomesOff, int biomesStride,
            byte[] light, int lightOff, int lightStride,
            int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) {
        int count = positive(sizeX, "sizeX") * positive(sizeY, "sizeY") * positive(sizeZ, "sizeZ");
        if (states != null) {
            checkRangeLen(states.length, statesOff, positive(statesStride, "statesStride") * count);
        }
        if (biomes != null) {
            checkRangeLen(biomes.length, biomesOff, positive(biomesStride, "biomesStride") * count);
        }
        if (light != null) {
            checkRangeLen(light.length, lightOff, positive(lightStride, "lightStride") * count);
        }
        positive(strideX, "strideX");
        positive(strideY, "strideY");
        positive(strideZ, "strideZ");
    }

    /**
     * Validates the arguments for a call to {@link FBlockWorld#getData(int[], int, int, int[], int, int, byte[], int, int, int[], int, int, int[], int, int, int[], int, int, int)},
     * throwing an exception if the parameters are invalid.
     *
     * @see FBlockWorld#getData(int[], int, int, int[], int, int, byte[], int, int, int[], int, int, int[], int, int, int[], int, int, int)
     */
    public static void validateArgsForGetData(
            int[] states, int statesOff, int statesStride,
            int[] biomes, int biomesOff, int biomesStride,
            byte[] light, int lightOff, int lightStride,
            @NonNull int[] xs, int xOff, int xStride,
            @NonNull int[] ys, int yOff, int yStride,
            @NonNull int[] zs, int zOff, int zStride,
            int count) {
        notNegative(count, "count");
        if (states != null) {
            checkRangeLen(states.length, statesOff, positive(statesStride, "statesStride") * count);
        }
        if (biomes != null) {
            checkRangeLen(biomes.length, biomesOff, positive(biomesStride, "biomesStride") * count);
        }
        if (light != null) {
            checkRangeLen(light.length, lightOff, positive(lightStride, "lightStride") * count);
        }
        checkRangeLen(xs.length, xOff, positive(xStride, "xStride") * count);
        checkRangeLen(ys.length, yOff, positive(yStride, "yStride") * count);
        checkRangeLen(zs.length, zOff, positive(zStride, "zStride") * count);
    }
}
