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

package net.daporkchop.fp2.mode.api;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * An identifier for a {@link IFarTile}.
 *
 * @author DaPorkchop_
 */
public interface IFarPos extends Comparable<IFarPos> {
    /**
     * @return the level of detail at this position
     */
    int level();

    /**
     * Gets the {@link IFarPos} containing this position at the given lower level of detail.
     *
     * @param targetLevel the level of detail to go up to
     * @return the {@link IFarPos} containing this position at the given lower level of detail
     */
    IFarPos upTo(int targetLevel);

    /**
     * @return the {@link IFarPos} containing this position at a lower level of detail
     */
    default IFarPos up() {
        return this.upTo(this.level() + 1);
    }

    /**
     * Writes this position to the given {@link ByteBuf}.
     * <p>
     * The written data must be deserializable by this position's render mode's {@link IFarRenderMode#readPos(ByteBuf)} method.
     *
     * @param dst the {@link ByteBuf} to write to
     */
    default void writePos(@NonNull ByteBuf dst) {
        dst.writeInt(this.level());
        this.writePosNoLevel(dst);
    }

    /**
     * @return the coordinates of this position (excluding the detail level)
     */
    int[] coordinates();

    /**
     * Writes this position to the given {@link ByteBuf}, without including the detail level
     *
     * @param dst the {@link ByteBuf} to write to
     */
    void writePosNoLevel(@NonNull ByteBuf dst);

    /**
     * Checks whether or not this position contains the given {@link IFarPos}.
     *
     * @param posIn the {@link IFarPos} to check
     * @return whether or not this position contains the given {@link IFarPos}
     */
    boolean contains(@NonNull IFarPos posIn);

    /**
     * @return the maximum volume that the tile at this position could possibly occupy
     */
    AxisAlignedBB bounds();

    /**
     * @return the maximum volume that the tile at this position could possibly occupy, with an additional padding of 1 unit in the positive direction
     */
    default AxisAlignedBB paddedBounds() {
        double padding = 1 << this.level();
        return this.bounds().expand(padding, padding, padding);
    }

    /**
     * Gets the Manhattan distance to the given {@link IFarPos}.
     * <p>
     * If the positions are at different detail levels, the distance is approximate.
     * <p>
     * Note that the distance may not be returned in blocks, but rather in arbitrary units. The only constraint is that it must remain consistent across
     * levels (such that the distance between any two positions at the same level is always twice the distance between the positions with the same axis
     * values one level lower).
     *
     * @param posIn the {@link IFarPos} to get the distance to
     * @return the Manhattan distance to the given {@link IFarPos}
     */
    int manhattanDistance(@NonNull IFarPos posIn);

    /**
     * Compares two positions in some arbitrary manner.
     * <p>
     * The function may be implemented in any way, but must be consistent and must only return {@code 0} for positions that are also considered
     * identical by {@link #equals(Object)}.
     * <p>
     * Failure to implement this correctly will result in a deadlock of the rendering threads!
     *
     * @see Comparable#compareTo(Object)
     */
    @Override
    int compareTo(IFarPos o);
}
