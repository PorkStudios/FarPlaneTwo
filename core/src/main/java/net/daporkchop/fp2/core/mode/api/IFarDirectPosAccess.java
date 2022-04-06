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

package net.daporkchop.fp2.core.mode.api;

import lombok.NonNull;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.core.util.math.geometry.Volume;

import java.util.List;
import java.util.Set;

/**
 * Provides access to an off-heap representation of a {@link IFarPos}.
 *
 * @author DaPorkchop_
 */
public interface IFarDirectPosAccess<POS extends IFarPos> {
    /**
     * @return the number of spatial dimensions required to represent this position
     */
    int axisCount();

    /**
     * @return the off-heap size of a position, in bytes
     */
    long posSize();

    /**
     * Stores a position off-heap at the given memory address.
     *
     * @param pos  the position
     * @param addr the memory address
     */
    void storePos(@NonNull POS pos, long addr);

    /**
     * Loads the position at the give memory address onto the Java heap.
     *
     * @param addr the memory address
     * @return the position
     */
    POS loadPos(long addr);

    /**
     * Gets the position's offset along the given axis.
     *
     * @param pos  the position
     * @param axis the axis number
     * @return the position's offset along the given axis
     */
    int getAxisHeap(@NonNull POS pos, int axis);

    /**
     * Gets the position's offset along the given axis.
     *
     * @param addr the memory address of the off-heap position
     * @param axis the axis number
     * @return the position's offset along the given axis
     */
    int getAxisDirect(long addr, int axis);

    /**
     * Checks the positions at the given memory addresses for equality.
     * <p>
     * Functionally identical to {@code loadPos(addr1).equals(loadPos(addr2))}.
     *
     * @return whether or not the two positions are equal
     */
    boolean equalsPos(long addr1, long addr2);

    /**
     * Hashes the position at the given memory address.
     * <p>
     * Functionally identical to {@code loadPos(addr).localHash()}.
     *
     * @param addr the memory address
     * @return the position's hash
     */
    int hashPos(long addr);

    /**
     * Hashes the position at the given memory address.
     * <p>
     * Functionally identical to {@code loadPos(addr).hashCode()}.
     *
     * @param addr the memory address
     * @return the position's locality-sensitive hash
     */
    long localHashPos(long addr);

    /**
     * Checks whether or not the tile at the given position intersects the given volume.
     *
     * @param addr   the memory address of the off-heap position
     * @param volume the volume
     * @return whether or not the tile at the given position intersects the given volume
     */
    boolean intersects(long addr, @NonNull Volume volume);

    /**
     * Checks whether or not the tile at the given position is contained by the given volume.
     *
     * @param addr   the memory address of the off-heap position
     * @param volume the volume
     * @return whether or not the tile at the given position is contained by the given volume
     */
    boolean containedBy(long addr, @NonNull Volume volume);

    /**
     * Checks whether or not the tile at the given position is in the given frustum.
     *
     * @param addr    the memory address of the off-heap position
     * @param frustum the frustum
     * @return whether or not the tile at the given position is in the given frustum
     */
    boolean inFrustum(long addr, @NonNull IFrustum frustum);

    /**
     * @return a new {@link Set} which can store positions of type {@link POS}
     */
    Set<POS> newPositionSet();

    /**
     * @return a new {@link List} which can store positions of type {@link POS}
     */
    List<POS> newPositionList();
}
