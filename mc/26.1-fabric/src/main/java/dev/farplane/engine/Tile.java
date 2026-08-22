/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2026 DaPorkchop_
 * Portions Copyright (c) 2026 FarPlane contributors
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

package dev.farplane.engine;

import java.util.Arrays;

import static dev.farplane.engine.EngineConstants.*;

/**
 * On-heap sparse 16³ voxel tile storage.
 * Adapted from FarPlaneTwo {@code net.daporkchop.fp2.core.engine.Tile}.
 * <p>
 * Upstream uses off-heap memory ({@code PUnsafe.allocateMemory}). This rewrite uses
 * on-heap arrays for debuggability and to avoid PorkLib dependency.
 *
 * @author DaPorkchop_ (original)
 */
public class Tile {
    public static final int ENTRY_COUNT = T_VOXELS * T_VOXELS * T_VOXELS; // 4096

    // index[cellIndex] = data slot index, or -1 if unset
    private final short[] index = new short[ENTRY_COUNT];

    // packed data arrays (one entry per set voxel)
    // layout per entry: dx(8) | dy(8) | dz(8) | edges(8) — but we store them separately for clarity
    private int[] posX = new int[64];
    private int[] posY = new int[64];
    private int[] posZ = new int[64];
    private int[] edges = new int[64];
    private int[] state0 = new int[64];
    private int[] state1 = new int[64];
    private int[] state2 = new int[64];
    private int[] biomeAndLight = new int[64]; // biome << 8 | (light & 0xFF)

    private int count = 0;
    private long extra = 0L;

    public Tile() {
        reset();
    }

    public int count() {
        return count;
    }

    public long extra() {
        return extra;
    }

    public void extra(long extra) {
        this.extra = extra;
    }

    /**
     * Resets this tile, clearing all data.
     */
    public void reset() {
        count = 0;
        extra = 0L;
        Arrays.fill(index, (short) -1);
    }

    private static int cellIndex(int x, int y, int z) {
        return (x * T_VOXELS + y) * T_VOXELS + z;
    }

    private void ensureCapacity(int minCap) {
        if (posX.length >= minCap) return;
        int newCap = Math.max(posX.length * 2, minCap);
        posX = Arrays.copyOf(posX, newCap);
        posY = Arrays.copyOf(posY, newCap);
        posZ = Arrays.copyOf(posZ, newCap);
        edges = Arrays.copyOf(edges, newCap);
        state0 = Arrays.copyOf(state0, newCap);
        state1 = Arrays.copyOf(state1, newCap);
        state2 = Arrays.copyOf(state2, newCap);
        biomeAndLight = Arrays.copyOf(biomeAndLight, newCap);
    }

    /**
     * Sets the voxel at (x, y, z) with the given data.
     */
    public void set(int x, int y, int z, TileData data) {
        int ci = cellIndex(x, y, z);
        int slot = index[ci] & 0xFFFF;
        if (slot == 0xFFFF) {
            // new entry
            slot = count++;
            index[ci] = (short) slot;
            ensureCapacity(count);
        }
        posX[slot] = data.x;
        posY[slot] = data.y;
        posZ[slot] = data.z;
        edges[slot] = data.edges;
        state0[slot] = data.states[0];
        state1[slot] = data.states[1];
        state2[slot] = data.states[2];
        biomeAndLight[slot] = (data.biome << 8) | (data.light & 0xFF);
    }

    /**
     * Gets the voxel at (x, y, z). Returns true if the voxel is set.
     */
    public boolean get(int x, int y, int z, TileData data) {
        int ci = cellIndex(x, y, z);
        int slot = index[ci] & 0xFFFF;
        if (slot == 0xFFFF) return false;
        readSlot(slot, data);
        return true;
    }

    /**
     * Gets the voxel at the given sequential index. Returns the packed cell position.
     */
    public int get(int sequentialIndex, TileData data) {
        if (sequentialIndex < 0 || sequentialIndex >= count) throw new IndexOutOfBoundsException();
        readSlot(sequentialIndex, data);
        // find the cell index for this slot
        // This is O(n) but only used in baker which iterates all — acceptable for v1
        for (int ci = 0; ci < ENTRY_COUNT; ci++) {
            if ((index[ci] & 0xFFFF) == sequentialIndex) {
                return ci;
            }
        }
        return -1;
    }

    private void readSlot(int slot, TileData data) {
        data.x = posX[slot];
        data.y = posY[slot];
        data.z = posZ[slot];
        data.edges = edges[slot];
        data.states[0] = state0[slot];
        data.states[1] = state1[slot];
        data.states[2] = state2[slot];
        int bl = biomeAndLight[slot];
        data.biome = (bl >> 8) & 0xFF;
        data.light = (byte) bl;
    }

    /**
     * Returns whether this tile contains no data.
     */
    public boolean isEmpty() {
        return count == 0;
    }
}
