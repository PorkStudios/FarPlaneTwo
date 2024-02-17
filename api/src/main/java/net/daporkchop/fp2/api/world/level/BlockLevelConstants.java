/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.api.world.level;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Constants and helper methods for users and implementors of {@link FBlockLevel}.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class BlockLevelConstants {
    //
    // BLOCK TYPES
    //

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
     * Checks whether the given {@code int} is a valid block type.
     *
     * @param type the block type to check
     * @return {@code true} if the given {@code int} is a valid block type
     */
    public static boolean isValidBlockType(int type) {
        return type >= BLOCK_TYPE_INVISIBLE && type <= BLOCK_TYPE_OPAQUE;
    }

    /**
     * The maximum allowed value for block or sky light.
     */
    public static final int MAX_LIGHT = 15;

    //
    // LIGHT PACKING/UNPACKING HELPERS
    //

    /**
     * Checks whether the given {@code int} is a valid light level.
     *
     * @param light the light level to check
     * @return {@code true} if the given {@code int} is a valid light level
     */
    public static boolean isValidLight(int light) {
        return (light & MAX_LIGHT) == light;
    }

    /**
     * Packs the given sky light and block light values together into a single {@code byte}.
     *
     * @param skyLight   the sky light value. The 4 lowest bits are kept, all other information is discarded.
     * @param blockLight the block light value. The 4 lowest bits are kept, all other information is discarded.
     * @return the packed light value
     */
    public static byte packLight(int skyLight, int blockLight) {
        assert isValidLight(skyLight) && isValidLight(blockLight) : "skyLight=" + skyLight + ", blockLight=" + blockLight;
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
     * Packs the given light opacity and light emission values together into a single {@code byte}.
     *
     * @param lightOpacity  the light opacity value. The 4 lowest bits are kept, all other information is discarded.
     * @param lightEmission the light emission value. The 4 lowest bits are kept, all other information is discarded.
     * @return the packed light value
     */
    public static byte packLightAttrs(int lightOpacity, int lightEmission) {
        assert isValidLight(lightOpacity) && isValidLight(lightEmission) : "lightOpacity=" + lightOpacity + ", lightEmission=" + lightEmission;
        return (byte) ((lightEmission << 4) | (lightOpacity & 0xF));
    }

    /**
     * Unpacks the light opacity value from the given {@code byte}.
     *
     * @param packedLightAttrs the packed light data, as returned by {@link #packLightAttrs(int, int)}
     * @return the light opacity value
     */
    public static int unpackLightOpacity(byte packedLightAttrs) {
        return packedLightAttrs & 0xF;
    }

    /**
     * Unpacks the light emission value from the given {@code byte}.
     *
     * @param packedLightAttrs the packed light data, as returned by {@link #packLightAttrs(int, int)}
     * @return the light emission value
     */
    public static int unpackLightEmission(byte packedLightAttrs) {
        return (packedLightAttrs & 0xFF) >>> 4;
    }

    //
    // ARGUMENT VALIDATION HELPERS
    //

    /**
     * Ensures that a dense sampling grid defined by an origin point and a size along each axis is valid.
     *
     * @param originX the origin X coordinate
     * @param originY the origin Y coordinate
     * @param originZ the origin Z coordinate
     * @param sizeX   the grid's size along the X axis
     * @param sizeY   the grid's size along the Y axis
     * @param sizeZ   the grid's size along the Z axis
     * @return the total number of sample points
     */
    public static int validateDenseGridBounds(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ) {
        checkArg(sizeX > 0 && sizeY > 0 && sizeZ > 0, "all grid dimensions must be positive!");
        checkArg(originX < originX + sizeX && originY < originY + sizeY && originZ < originZ + sizeZ, "integer overflow in grid coordinates!");
        return Math.multiplyExact(Math.multiplyExact(sizeX, sizeY), sizeZ);
    }

    /**
     * Ensures that a sparse sampling grid defined by an origin point, a spacing between samples and a sample count along each axis is valid.
     *
     * @param originX the origin X coordinate. Must be aligned to a multiple of {@code strideX}
     * @param originY the origin Y coordinate. Must be aligned to a multiple of {@code strideY}
     * @param originZ the origin Z coordinate. Must be aligned to a multiple of {@code strideZ}
     * @param sizeX   the number of samples to take along the X axis
     * @param sizeY   the number of samples to take along the Y axis
     * @param sizeZ   the number of samples to take along the Z axis
     * @param zoom    {@code log2} of the distance between samples along each axis
     * @return the total number of sample points
     */
    public static int validateSparseGridBounds(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ,
            @NotNegative int zoom) {
        checkArg(sizeX > 0 && sizeY > 0 && sizeZ > 0, "all grid dimensions must be positive!");

        //make sure there will be no overflow when computing the actual voxel positions
        int stride = 1 << notNegative(zoom, "zoom");
        addExact(originX, multiplyExact(stride, sizeX));
        addExact(originY, multiplyExact(stride, sizeY));
        addExact(originZ, multiplyExact(stride, sizeZ));

        checkArg(originX < originX + sizeX && originY < originY + sizeY && originZ < originZ + sizeZ, "integer overflow in grid coordinates!");
        return Math.multiplyExact(Math.multiplyExact(sizeX, sizeY), sizeZ);
    }
}
