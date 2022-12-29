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
 */

package net.daporkchop.fp2.api.world.level;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.common.annotation.param.Positive;

import java.util.List;

import static java.lang.Math.*;

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
     * The number of block types that exist.
     *
     * @see FExtendedStateRegistryData#type(int)
     */
    public static final int BLOCK_TYPES = 3;

    //
    // STATE INFO FLAGS
    //

    /**
     * Indicates that the block type is a liquid.
     */
    public static final int STATE_FLAG_LIQUID = 1;

    //
    // DATA BAND ORDINALS
    //

    /**
     * Ordinal of the states data band.
     * <p>
     * States are represented as an {@code int}, as returned by the corresponding methods in {@link FGameRegistry}.
     */
    public static final int DATA_BAND_ORDINAL_STATES = 0;

    /**
     * Ordinal of the biomes data band.
     * <p>
     * Biomes are represented as an {@code int}, as returned by the corresponding methods in {@link FGameRegistry}.
     */
    public static final int DATA_BAND_ORDINAL_BIOMES = 1;

    /**
     * Ordinal of the block+sky light data band.
     * <p>
     * Block+sky light levels are represented as a {@code byte}, as returned by {@link BlockLevelConstants#packLight(int, int)}. Light levels are unsigned nibbles (4-bit integers),
     * where {@code 0} is the darkest and {@code 15} is the brightest possible value.
     */
    public static final int DATA_BAND_ORDINAL_LIGHT = 2;

    /**
     * The number of data bands that exist.
     */
    public static final int DATA_BANDS = 3;

    //
    // TYPE TRANSITION CONSTANTS
    //

    private static final byte TYPE_TRANSITIONS = (byte) BLOCK_TYPES * BLOCK_TYPES;

    //
    // TYPE TRANSITION OUTPUT BAND ORDINALS
    //

    /**
     * Ordinal of the "type transitions" type transition query output band.
     * <p>
     * Type transitions are represented as a {@code byte}, and information can be extracted from them using the corresponding methods in {@link BlockLevelConstants}:<br>
     * <ul>
     *     <li>{@link #isValidTypeTransition(byte)}</li>
     *     <li>{@link #getTypeTransitionFromType(byte)}</li>
     *     <li>{@link #getTypeTransitionToType(byte)}</li>
     * </ul>
     */
    public static final int TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_TRANSITIONS = 0;

    /**
     * Ordinal of the x coordinates type transition query output band.
     * <p>
     * X coordinates are represented as an {@code int}.
     */
    public static final int TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_X = 1;

    /**
     * Ordinal of the y coordinates type transition query output band.
     * <p>
     * Y coordinates are represented as an {@code int}.
     */
    public static final int TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Y = 2;

    /**
     * Ordinal of the Z coordinates type transition query output band.
     * <p>
     * Z coordinates are represented as an {@code int}.
     */
    public static final int TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_COORDS_Z = 3;

    /**
     * Ordinal of the "skipped NoData" type transition query output band.
     * <p>
     * "Skipped NoData" values are represented as a {@code boolean}.
     */
    public static final int TYPE_TRANSITION_OUTPUT_BAND_ORDINAL_SKIPPED_NODATA = 4;

    /**
     * The number of type transition query output bands that exist.
     */
    public static final int TYPE_TRANSITION_OUTPUT_BANDS = 5;

    //
    // BLOCK TYPE HELPERS
    //

    /**
     * Checks if the given ordinal number corresponds to a valid block type.
     *
     * @param blockTypeOrdinal the ordinal number
     * @return whether the given ordinal number corresponds to a valid block type
     */
    public static boolean isValidBlockType(int blockTypeOrdinal) {
        //noinspection ConstantConditions
        return ((BLOCK_TYPES - 1) & BLOCK_TYPES) == 0 //check if BLOCK_TYPES is a power of two, if so we can use an optimized implementation
                ? (blockTypeOrdinal & (BLOCK_TYPES - 1)) == blockTypeOrdinal
                : blockTypeOrdinal >= 0 && blockTypeOrdinal < BLOCK_TYPES;
    }

    /**
     * Checks if the given bitfield represents a valid set of block types.
     *
     * @param blockTypeBitfield the bitfield
     * @return whether the given bitfield represents a valid set of block types
     */
    public static boolean isValidBlockTypeSet(int blockTypeBitfield) {
        //noinspection ConstantConditions
        return BLOCK_TYPES == Integer.SIZE
               || (blockTypeBitfield & ((1 << BLOCK_TYPES) - 1)) == blockTypeBitfield;
    }

    /**
     * Gets a flag indicating that the block type with the given ordinal number is enabled.
     *
     * @param blockTypeOrdinal the ordinal number of the block type
     * @return a flag indicating that the block type with the given ordinal number is enabled
     */
    public static int blockTypeFlag(int blockTypeOrdinal) {
        assert isValidDataBand(blockTypeOrdinal) : "illegal block type ordinal " + blockTypeOrdinal;
        return 1 << blockTypeOrdinal;
    }

    /**
     * Gets an {@code int} with bits set indicating that the block types with the given ordinal numbers are enabled.
     *
     * @param blockTypeOrdinals the ordinal numbers of the block types
     * @return a set of bits indicating that the block types with the given ordinal numbers are enabled
     */
    public static int blockTypeFlags(int @NonNull ... blockTypeOrdinals) {
        int flags = 0;
        for (int blockTypeOrdinal : blockTypeOrdinals) {
            flags |= blockTypeFlag(blockTypeOrdinal);
        }
        return flags;
    }

    /**
     * @return a bitfield indicating every block type is enabled
     */
    public static int allBlockTypes() {
        return (1 << BLOCK_TYPES) - 1;
    }

    /**
     * Checks whether a block type is enabled.
     *
     * @param enabledBlockTypes a bitfield indicating which block types are enabled
     * @param blockTypeOrdinal  the ordinal number of the block type to check for
     * @return whether the block type is enabled
     */
    public static boolean isBlockTypeEnabled(int enabledBlockTypes, int blockTypeOrdinal) {
        assert isValidBlockType(blockTypeOrdinal) : "illegal block type ordinal " + blockTypeOrdinal;
        return (enabledBlockTypes & (1 << blockTypeOrdinal)) != 0;
    }

    /**
     * Checks whether every block type is enabled.
     *
     * @param enabledBlockTypes a bitfield indicating which block types are enabled
     * @return whether every block type is enabled
     */
    public static boolean isAllBlockTypesEnabled(int enabledBlockTypes) {
        return enabledBlockTypes == allBlockTypes();
    }

    //
    // STATE INFO FLAG HELPERS
    //

    /**
     * Checks whether the given {@link FExtendedStateRegistryData#stateInfo(int) block state info flags} value contains all of the expected flags.
     *
     * @param stateInfo     the {@link FExtendedStateRegistryData#stateInfo(int) block state info flags}
     * @param expectedFlags the flags to check for, combined using bitwise OR
     * @return whether the given {@link FExtendedStateRegistryData#stateInfo(int) block state info flags} value contains all of the expected flags
     */
    public static boolean hasAllStateFlags(int stateInfo, int expectedFlags) {
        return (stateInfo & expectedFlags) == expectedFlags;
    }

    /**
     * Checks whether the given {@link FExtendedStateRegistryData#stateInfo(int) block state info flags} value contains at least one of the expected flags.
     *
     * @param stateInfo     the {@link FExtendedStateRegistryData#stateInfo(int) block state info flags}
     * @param expectedFlags the flags to check for, combined using bitwise OR
     * @return whether the given {@link FExtendedStateRegistryData#stateInfo(int) block state info flags} value contains at least one of the expected flags
     */
    public static boolean hasAnyStateFlags(int stateInfo, int expectedFlags) {
        return (stateInfo & expectedFlags) != 0;
    }

    //
    // DATA BAND HELPERS
    //

    /**
     * Checks if the given ordinal number corresponds to a valid data band.
     *
     * @param dataBandOrdinal the ordinal number
     * @return whether the given ordinal number corresponds to a valid data band
     */
    public static boolean isValidDataBand(int dataBandOrdinal) {
        //noinspection ConstantConditions
        return ((DATA_BANDS - 1) & DATA_BANDS) == 0 //check if DATA_BANDS is a power of two, if so we can use an optimized implementation
                ? (dataBandOrdinal & (DATA_BANDS - 1)) == dataBandOrdinal
                : dataBandOrdinal >= 0 && dataBandOrdinal < DATA_BANDS;
    }

    /**
     * Gets a flag indicating that the data band with the given ordinal number is enabled.
     *
     * @param dataBandOrdinal the ordinal number of the data band
     * @return a flag indicating that the data band with the given ordinal number is enabled
     */
    public static int dataBandFlag(int dataBandOrdinal) {
        assert isValidDataBand(dataBandOrdinal) : "illegal data band ordinal " + dataBandOrdinal;
        return 1 << dataBandOrdinal;
    }

    /**
     * Gets an {@code int} with bits set indicating that the data bands with the given ordinal numbers are enabled.
     *
     * @param dataBandOrdinals the ordinal numbers of the data bands
     * @return a set of bits indicating that the data bands with the given ordinal numbers are enabled
     */
    public static int dataBandFlags(int @NonNull ... dataBandOrdinals) {
        int flags = 0;
        for (int dataBandOrdinal : dataBandOrdinals) {
            flags |= dataBandFlag(dataBandOrdinal);
        }
        return flags;
    }

    /**
     * Gets an {@code int} with bits set indicating that the all data bands are enabled.
     *
     * @return a set of bits indicating that all data bands are enabled
     */
    public static int allDataBands() {
        return (1 << DATA_BANDS) - 1;
    }

    /**
     * Checks whether a data band is enabled.
     *
     * @param enabledDataBands a bitfield indicating which data bands are enabled
     * @param dataBandOrdinal  the ordinal number of the data band to check for
     * @return whether the data band is enabled
     */
    public static boolean isDataBandEnabled(int enabledDataBands, int dataBandOrdinal) {
        assert isValidDataBand(dataBandOrdinal) : "illegal data band ordinal " + dataBandOrdinal;
        return (enabledDataBands & (1 << dataBandOrdinal)) != 0;
    }

    //
    // TYPE TRANSITION SEARCH HELPERS
    //

    /**
     * Checks whether the given {@code byte} is a valid type transition ordinal.
     *
     * @param typeTransition the {@code byte}
     * @return whether the given {@code byte} is a valid type transition ordinal
     */
    public static boolean isValidTypeTransition(byte typeTransition) {
        //noinspection ConstantConditions
        return ((TYPE_TRANSITIONS - 1) & TYPE_TRANSITIONS) == 0 //check if TYPE_TRANSITIONS is a power of two, if so we can use an optimized implementation
                ? (typeTransition & (TYPE_TRANSITIONS - 1)) == typeTransition
                : typeTransition >= 0 && typeTransition < TYPE_TRANSITIONS;
    }

    /**
     * Gets the block type being transitioned from in the given type transition ordinal.
     *
     * @param typeTransition the type transition ordinal
     * @return the block type being transitioned from
     */
    public static int getTypeTransitionFromType(byte typeTransition) {
        assert isValidTypeTransition(typeTransition) : "not a valid type transition " + typeTransition;
        return typeTransition % BLOCK_TYPES;
    }

    /**
     * Gets the block type being transitioned to in the given type transition ordinal.
     *
     * @param typeTransition the type transition ordinal
     * @return the block type being transitioned to
     */
    public static int getTypeTransitionToType(byte typeTransition) {
        assert isValidTypeTransition(typeTransition) : "not a valid type transition " + typeTransition;
        return typeTransition / BLOCK_TYPES;
    }

    /**
     * Gets the type transition ordinal representing a transition from the given block type to the given block type.
     *
     * @param fromBlockTypeOrdinal the ordinal of the block type being transitioned from
     * @param toBlockTypeOrdinal   the ordinal of the block type being transitioned to
     * @return the type transition ordinal
     */
    public static byte getTypeTransition(int fromBlockTypeOrdinal, int toBlockTypeOrdinal) {
        assert isValidBlockType(fromBlockTypeOrdinal) : "illegal block type ordinal " + fromBlockTypeOrdinal;
        assert isValidBlockType(toBlockTypeOrdinal) : "illegal block type ordinal " + toBlockTypeOrdinal;
        return (byte) (toBlockTypeOrdinal * BLOCK_TYPES + fromBlockTypeOrdinal);
    }

    //
    // TYPE TRANSITION QUERY OUTPUT BAND HELPERS
    //

    /**
     * Checks if the given ordinal number corresponds to a valid type transition query output band.
     *
     * @param typeTransitionQueryOutputBandOrdinal the ordinal number
     * @return whether the given ordinal number corresponds to a valid type transition query output band
     */
    public static boolean isValidTypeTransitionQueryOutputBand(int typeTransitionQueryOutputBandOrdinal) {
        //noinspection ConstantConditions
        return ((TYPE_TRANSITION_OUTPUT_BANDS - 1) & TYPE_TRANSITION_OUTPUT_BANDS)
               == 0 //check if DATA_BANDS is a power of two, if so we can use an optimized implementation
                ? (typeTransitionQueryOutputBandOrdinal & (TYPE_TRANSITION_OUTPUT_BANDS - 1)) == typeTransitionQueryOutputBandOrdinal
                : typeTransitionQueryOutputBandOrdinal >= 0 && typeTransitionQueryOutputBandOrdinal < TYPE_TRANSITION_OUTPUT_BANDS;
    }

    /**
     * Gets a flag indicating that the type transition query output band with the given ordinal number is enabled.
     *
     * @param typeTransitionQueryOutputBandOrdinal the ordinal number of the type transition query output band
     * @return a flag indicating that the type transition query output band with the given ordinal number is enabled
     */
    public static int typeTransitionQueryOutputBandFlag(int typeTransitionQueryOutputBandOrdinal) {
        assert isValidTypeTransitionQueryOutputBand(typeTransitionQueryOutputBandOrdinal) : "illegal type transition query output band ordinal "
                                                                                            + typeTransitionQueryOutputBandOrdinal;
        return 1 << typeTransitionQueryOutputBandOrdinal;
    }

    /**
     * Gets an {@code int} with bits set indicating that the type transition query output bands with the given ordinal numbers are enabled.
     *
     * @param typeTransitionQueryOutputBandOrdinals the ordinal numbers of the type transition query output bands
     * @return a set of bits indicating that the type transition query output bands with the given ordinal numbers are enabled
     */
    public static int typeTransitionQueryOutputBandFlags(int @NonNull ... typeTransitionQueryOutputBandOrdinals) {
        int flags = 0;
        for (int typeTransitionQueryOutputBandOrdinal : typeTransitionQueryOutputBandOrdinals) {
            flags |= typeTransitionQueryOutputBandFlag(typeTransitionQueryOutputBandOrdinal);
        }
        return flags;
    }

    /**
     * Gets an {@code int} with bits set indicating that the all type transition query output bands are enabled.
     *
     * @return a set of bits indicating that all type transition query output bands are enabled
     */
    public static int allTypeTransitionQueryOutputBands() {
        return (1 << TYPE_TRANSITION_OUTPUT_BANDS) - 1;
    }

    /**
     * Checks whether a type transition query output band is enabled.
     *
     * @param enabledTypeTransitionQueryOutputBands a bitfield indicating which type transition query output bands are enabled
     * @param typeTransitionQueryOutputBandOrdinal  the ordinal number of the type transition query output band to check for
     * @return whether the type transition query output band is enabled
     */
    public static boolean isTypeTransitionQueryOutputBandEnabled(int enabledTypeTransitionQueryOutputBands, int typeTransitionQueryOutputBandOrdinal) {
        assert isValidTypeTransitionQueryOutputBand(typeTransitionQueryOutputBandOrdinal) : "illegal type transition query output band ordinal "
                                                                                            + typeTransitionQueryOutputBandOrdinal;
        return (enabledTypeTransitionQueryOutputBands & (1 << typeTransitionQueryOutputBandOrdinal)) != 0;
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
        return (packedLight & 0xFF)
               >>> 4; //i'm pretty sure that doing it in this order will allow JIT to interpret it as an unsigned byte, thus reducing it to a simple shift
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
    // getNextTypeTransitions() IMPLEMENTATION HELPERS
    //

    /**
     * Checks if a vector starting at the given voxel position and pointing in the given {@link Direction direction} will ever intersect the given
     * {@link IntAxisAlignedBB AABB}.
     *
     * @param aabb        the {@link IntAxisAlignedBB AABB}
     * @param x           the vector's origin X coordinate
     * @param y           the vector's origin Y coordinate
     * @param z           the vector's origin Z coordinate
     * @param direction   the vector's direction
     * @param maxDistance the vector's maximum length. See {@link FBlockLevel#getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, net.daporkchop.fp2.api.world.level.query.QuerySamplingMode)}
     *                    for a more detailed description.
     * @return {@code true} if the vector will ever intersect the given {@link IntAxisAlignedBB AABB}, {@code false} otherwise
     */
    public static boolean willVectorIntersectAABB(@NonNull IntAxisAlignedBB aabb, int x, int y, int z, @NonNull Direction direction, @Positive long maxDistance) {
        return aabb.contains(x, y, z) //if the origin point is already inside the AABB, then it's pretty obvious that they intersect
               || (willVectorIntersectAABB_1d(x, direction.x(), aabb.minX(), aabb.maxX(), maxDistance)
                   && willVectorIntersectAABB_1d(y, direction.y(), aabb.minY(), aabb.maxY(), maxDistance)
                   && willVectorIntersectAABB_1d(z, direction.z(), aabb.minZ(), aabb.maxZ(), maxDistance));
    }

    private static boolean willVectorIntersectAABB_1d(int coord, int direction, int min, int max, @Positive long maxDistance) {
        //otherwise, check to see if the position will ever intersect it. there are two cases for each axis:
        // - if an axis is static, it must already intersect the AABB's bounds on that axis, as that axis' coordinate value will never change
        // - if an axis is changing, it must be:
        //   - increasing/decreasing towards the AABB and not away from it
        //   - at most maxDistance away from the position immediately outside the AABB
        return direction == 0
               ? coord >= min && coord < max
               : (direction < 0) == (min < coord) && max((long) coord - max, (long) min - 1L - coord) <= maxDistance;
    }

    public static long jumpToExclusiveDistance(@NonNull IntAxisAlignedBB aabb, int x, int y, int z, @NonNull Direction direction, @Positive long maxDistance) {
        assert willVectorIntersectAABB(aabb, x, y, z, direction, maxDistance) : "the given vector: (" + x + ',' + y + ',' + z + "),dir=" + direction + " will never intersect " + aabb;

        long dx = abs((long) x - jumpXCoordinateToExclusiveAABB(aabb, x, y, z, direction));
        long dy = abs((long) y - jumpYCoordinateToExclusiveAABB(aabb, x, y, z, direction));
        long dz = abs((long) z - jumpZCoordinateToExclusiveAABB(aabb, x, y, z, direction));

        long jumpDistance = max(max(dx, dy), dz);
        assert jumpDistance <= maxDistance : "cannot jump " + jumpDistance + " voxels when maxDistance is " + maxDistance;
        return jumpDistance;
    }

    /**
     * Given a vector starting at the given voxel position and pointing in the given {@link Direction direction}, advances along the length of the vector until a position
     * is found which {@link IntAxisAlignedBB#contains(int, int, int) intersects} the given {@link IntAxisAlignedBB AABB}, and returns the X coordinate value from
     * immediately before. In other words, if the X coordinate value is outside the given {@link IntAxisAlignedBB AABB}, skips to the X last coordinate immediately before
     * entering the given {@link IntAxisAlignedBB AABB}.
     * <p>
     * This method is implemented as if by
     * <blockquote><pre>{@code
     * int lastX = x, lastY = y, lastZ = z;
     * int nextX = x, nextY = y, nextZ = z;
     * while (!aabb.contains(nextX, nextY, nextZ)) {
     *     nextX = Math.addExact(lastX = nextX, direction.x());
     *     nextY = Math.addExact(lastY = nextY, direction.y());
     *     nextZ = Math.addExact(lastZ = nextZ, direction.z());
     * }
     * return lastX;
     * }</pre></blockquote>
     * <p>
     * This method assumes that the given vector will eventually intersect the given {@link IntAxisAlignedBB AABB}, and will result in undefined behavior if this is not
     * the case. If uncertain, use {@link #willVectorIntersectAABB(IntAxisAlignedBB, int, int, int, Direction, long)} to verify before returning anything.
     *
     * @param aabb      the {@link IntAxisAlignedBB AABB}
     * @param x         the vector's origin X coordinate
     * @param y         the vector's origin Y coordinate
     * @param z         the vector's origin Z coordinate
     * @param direction the vector's direction
     * @return returns the X coordinate value
     */
    public static int jumpXCoordinateToExclusiveAABB(@NonNull IntAxisAlignedBB aabb, int x, int y, int z, @NonNull Direction direction) {
        assert willVectorIntersectAABB(aabb, x, y, z, direction, Long.MAX_VALUE) : "the given vector: (" + x + ',' + y + ',' + z + "),dir=" + direction + " will never intersect " + aabb;

        if (!aabb.containsX(x) //the origin point isn't already inside the AABB
            && direction.x() != 0) { //the vector is parallel to the X axis
            return direction.x() < 0 ? aabb.maxX() : decrementExact(aabb.minX());
        } else {
            return x;
        }
    }

    /**
     * Given a vector starting at the given voxel position and pointing in the given {@link Direction direction}, advances along the length of the vector until a position
     * is found which {@link IntAxisAlignedBB#contains(int, int, int) intersects} the given {@link IntAxisAlignedBB AABB}, and returns the Y coordinate value from
     * immediately before. In other words, if the Y coordinate value is outside the given {@link IntAxisAlignedBB AABB}, skips to the Y last coordinate immediately before
     * entering the given {@link IntAxisAlignedBB AABB}.
     * <p>
     * This method is implemented as if by
     * <blockquote><pre>{@code
     * int lastX = x, lastY = y, lastZ = z;
     * int nextX = x, nextY = y, nextZ = z;
     * while (!aabb.contains(nextX, nextY, nextZ)) {
     *     nextX = Math.addExact(lastX = nextX, direction.x());
     *     nextY = Math.addExact(lastY = nextY, direction.y());
     *     nextZ = Math.addExact(lastZ = nextZ, direction.z());
     * }
     * return lastY;
     * }</pre></blockquote>
     * <p>
     * This method assumes that the given vector will eventually intersect the given {@link IntAxisAlignedBB AABB}, and will result in undefined behavior if this is not
     * the case. If uncertain, use {@link #willVectorIntersectAABB(IntAxisAlignedBB, int, int, int, Direction, long)} to verify before returning anything.
     *
     * @param aabb      the {@link IntAxisAlignedBB AABB}
     * @param x         the vector's origin X coordinate
     * @param y         the vector's origin Y coordinate
     * @param z         the vector's origin Z coordinate
     * @param direction the vector's direction
     * @return returns the Y coordinate value
     */
    public static int jumpYCoordinateToExclusiveAABB(@NonNull IntAxisAlignedBB aabb, int x, int y, int z, @NonNull Direction direction) {
        assert willVectorIntersectAABB(aabb, x, y, z, direction, Long.MAX_VALUE) : "the given vector: (" + x + ',' + y + ',' + z + "),dir=" + direction + " will never intersect " + aabb;

        if (!aabb.containsY(y) //the origin point isn't already inside the AABB
            && direction.y() != 0) { //the vector is parallel to the Y axis
            return direction.y() < 0 ? aabb.maxY() : decrementExact(aabb.minY());
        } else {
            return y;
        }
    }

    /**
     * Given a vector starting at the given voxel position and pointing in the given {@link Direction direction}, advances along the length of the vector until a position
     * is found which {@link IntAxisAlignedBB#contains(int, int, int) intersects} the given {@link IntAxisAlignedBB AABB}, and returns the Z coordinate value from
     * immediately before. In other words, if the Z coordinate value is outside the given {@link IntAxisAlignedBB AABB}, skips to the Z last coordinate immediately before
     * entering the given {@link IntAxisAlignedBB AABB}.
     * <p>
     * This method is implemented as if by
     * <blockquote><pre>{@code
     * int lastX = x, lastY = y, lastZ = z;
     * int nextX = x, nextY = y, nextZ = z;
     * while (!aabb.contains(nextX, nextY, nextZ)) {
     *     nextX = Math.addExact(lastX = nextX, direction.x());
     *     nextY = Math.addExact(lastY = nextY, direction.y());
     *     nextZ = Math.addExact(lastZ = nextZ, direction.z());
     * }
     * return lastZ;
     * }</pre></blockquote>
     * <p>
     * This method assumes that the given vector will eventually intersect the given {@link IntAxisAlignedBB AABB}, and will result in undefined behavior if this is not
     * the case. If uncertain, use {@link #willVectorIntersectAABB(IntAxisAlignedBB, int, int, int, Direction, long)} to verify before returning anything.
     *
     * @param aabb      the {@link IntAxisAlignedBB AABB}
     * @param x         the vector's origin X coordinate
     * @param y         the vector's origin Y coordinate
     * @param z         the vector's origin Z coordinate
     * @param direction the vector's direction
     * @return returns the Z coordinate value
     */
    public static int jumpZCoordinateToExclusiveAABB(@NonNull IntAxisAlignedBB aabb, int x, int y, int z, @NonNull Direction direction) {
        assert willVectorIntersectAABB(aabb, x, y, z, direction, Long.MAX_VALUE) : "the given vector: (" + x + ',' + y + ',' + z + "),dir=" + direction + " will never intersect " + aabb;

        if (!aabb.containsZ(z) //the origin point isn't already inside the AABB
            && direction.z() != 0) { //the vector is parallel to the Y axis
            return direction.z() < 0 ? aabb.maxZ() : decrementExact(aabb.minZ());
        } else {
            return z;
        }
    }
}
