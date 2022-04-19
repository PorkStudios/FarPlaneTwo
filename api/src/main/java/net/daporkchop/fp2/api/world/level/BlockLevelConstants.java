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

package net.daporkchop.fp2.api.world.level;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;

import java.util.function.Consumer;
import java.util.function.Function;

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

    //
    // DATA BAND ORDINALS
    //

    /**
     * Ordinal of the states band.
     * <p>
     * States are represented as an {@code int}, as returned by the corresponding methods in {@link FGameRegistry}.
     */
    public static final int BAND_ORDINAL_STATES = 0;

    /**
     * Ordinal of the biomes band.
     * <p>
     * Biomes are represented as an {@code int}, as returned by the corresponding methods in {@link FGameRegistry}.
     */
    public static final int BAND_ORDINAL_BIOMES = 1;

    /**
     * Ordinal of the block+sky light band.
     * <p>
     * Block+sky light levels are represented as a {@code byte}, as returned by {@link BlockLevelConstants#packLight(int, int)}. Light levels are unsigned nibbles (4-bit integers),
     * where {@code 0} is the darkest and {@code 15} is the brightest possible value.
     */
    public static final int BAND_ORDINAL_LIGHT = 2;

    //
    // DATA BAND HELPERS
    //

    /**
     * Gets a flag indicating that the data band with the given ordinal number is enabled.
     *
     * @param bandOrdinal the ordinal number of the band
     * @return a flag indicating that the data band with the given ordinal number is enabled
     */
    static int bandFlag(int bandOrdinal) {
        assert (bandOrdinal & 0x1F) == bandOrdinal : "illegal band ordinal " + bandOrdinal;
        return 1 << bandOrdinal;
    }

    /**
     * Checks whether or not a band is enabled.
     *
     * @param enabledBands a bitfield indicating which bands are enabled
     * @param bandOrdinal  the ordinal number of the band to check for
     * @return whether or not the band is enabled
     */
    static boolean isBandEnabled(int enabledBands, int bandOrdinal) {
        assert (bandOrdinal & 0x1F) == bandOrdinal : "illegal band ordinal " + bandOrdinal;
        return (enabledBands & (1 << bandOrdinal)) != 0;
    }

    //
    // LIGHT PACKING/UNPACKING HELPERS
    //

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

    //
    // QUERYSHAPE-IMPLEMENTATION-SPECIFIC OPTIMIZATION HELPERS
    //

    /**
     * Invokes a {@link TypedQueryShapeConsumer} with the given {@link FBlockLevel.QueryShape} as a parameter, automatically calling a type-specific method variant if possible.
     *
     * @param shape  the {@link FBlockLevel.QueryShape}
     * @param action the {@link TypedQueryShapeConsumer}
     */
    public static void withQueryShape(@NonNull FBlockLevel.QueryShape shape, @NonNull TypedQueryShapeConsumer action) {
        if (shape instanceof FBlockLevel.SinglePointQueryShape) {
            action.acceptPoint((FBlockLevel.SinglePointQueryShape) shape);
        } else if (shape instanceof FBlockLevel.MultiPointsQueryShape) {
            action.acceptPoints((FBlockLevel.MultiPointsQueryShape) shape);
        } else if (shape instanceof FBlockLevel.OriginSizeStrideQueryShape) {
            action.acceptOriginSizeStride((FBlockLevel.OriginSizeStrideQueryShape) shape);
        } else {
            action.acceptGeneric(shape);
        }
    }

    /**
     * Invokes a {@link TypedQueryShapeFunction} with the given {@link FBlockLevel.QueryShape} as a parameter, automatically calling a type-specific method variant if possible.
     *
     * @param shape  the {@link FBlockLevel.QueryShape}
     * @param action the {@link TypedQueryShapeFunction}
     * @return the {@link TypedQueryShapeFunction}'s return value
     */
    public static <R> R fromQueryShape(@NonNull FBlockLevel.QueryShape shape, @NonNull TypedQueryShapeFunction<R> action) {
        if (shape instanceof FBlockLevel.SinglePointQueryShape) {
            return action.applyPoint((FBlockLevel.SinglePointQueryShape) shape);
        } else if (shape instanceof FBlockLevel.MultiPointsQueryShape) {
            return action.applyPoints((FBlockLevel.MultiPointsQueryShape) shape);
        } else if (shape instanceof FBlockLevel.OriginSizeStrideQueryShape) {
            return action.applyOriginSizeStride((FBlockLevel.OriginSizeStrideQueryShape) shape);
        } else {
            return action.applyGeneric(shape);
        }
    }

    /**
     * A {@link Consumer}-syle callback function which accepts a {@link FBlockLevel.QueryShape}. Multiple methods are provided in order to allow optimized implementations for specific
     * {@link FBlockLevel.QueryShape} implementations.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface TypedQueryShapeConsumer {
        default void acceptPoint(@NonNull FBlockLevel.SinglePointQueryShape shape) {
            this.acceptGeneric(shape);
        }

        default void acceptPoints(@NonNull FBlockLevel.MultiPointsQueryShape shape) {
            this.acceptGeneric(shape);
        }

        default void acceptOriginSizeStride(@NonNull FBlockLevel.OriginSizeStrideQueryShape shape) {
            this.acceptGeneric(shape);
        }

        void acceptGeneric(@NonNull FBlockLevel.QueryShape shape);
    }

    /**
     * A {@link Function}-syle callback function which accepts a {@link FBlockLevel.QueryShape}. Multiple methods are provided in order to allow optimized implementations for specific
     * {@link FBlockLevel.QueryShape} implementations.
     *
     * @author DaPorkchop_
     */
    @FunctionalInterface
    public interface TypedQueryShapeFunction<R> {
        default R applyPoint(@NonNull FBlockLevel.SinglePointQueryShape shape) {
            return this.applyGeneric(shape);
        }

        default R applyPoints(@NonNull FBlockLevel.MultiPointsQueryShape shape) {
            return this.applyGeneric(shape);
        }

        default R applyOriginSizeStride(@NonNull FBlockLevel.OriginSizeStrideQueryShape shape) {
            return this.applyGeneric(shape);
        }

        R applyGeneric(@NonNull FBlockLevel.QueryShape shape);
    }
}
