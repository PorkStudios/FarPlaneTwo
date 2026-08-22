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

import static dev.farplane.engine.EngineConstants.*;

/**
 * Represents a position in the tile octree.
 * Adapted from FarPlaneTwo {@code net.daporkchop.fp2.core.engine.TilePos}.
 *
 * @author DaPorkchop_ (original)
 */
public record TilePos(int level, int x, int y, int z) implements Comparable<TilePos> {

    // --- Block coordinate helpers ---

    public int minBlockX() { return x << (T_SHIFT + level); }
    public int minBlockY() { return y << (T_SHIFT + level); }
    public int minBlockZ() { return z << (T_SHIFT + level); }

    public int maxBlockX() { return (x + 1) << (T_SHIFT + level); }
    public int maxBlockY() { return (y + 1) << (T_SHIFT + level); }
    public int maxBlockZ() { return (z + 1) << (T_SHIFT + level); }

    public int sideLength() { return T_VOXELS << level; }

    // --- Octree navigation ---

    public TilePos up() {
        return new TilePos(level + 1, x >> 1, y >> 1, z >> 1);
    }

    public TilePos down() {
        return new TilePos(level - 1, x << 1, y << 1, z << 1);
    }

    public TilePos upTo(int targetLevel) {
        if (targetLevel == level) return this;
        int shift = targetLevel - level;
        return new TilePos(targetLevel, x >> shift, y >> shift, z >> shift);
    }

    public TilePos downTo(int targetLevel) {
        if (targetLevel == level) return this;
        int shift = level - targetLevel;
        return new TilePos(targetLevel, x << shift, y << shift, z << shift);
    }

    // --- Distance ---

    public int manhattanDistance(TilePos other) {
        if (level == other.level) {
            return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
        }
        int s0 = Math.max(other.level - level, 0);
        int s1 = Math.max(level - other.level, 0);
        int s2 = Math.max(s0, s1);
        return (Math.abs((x >> s0) - (other.x >> s1)) << s2)
             + (Math.abs((y >> s0) - (other.y >> s1)) << s2)
             + (Math.abs((z >> s0) - (other.z >> s1)) << s2);
    }

    // --- Comparable (sort by level, then x, then z, then y — matches FP2) ---

    @Override
    public int compareTo(TilePos other) {
        int d = Integer.compare(level, other.level);
        if (d == 0) d = Integer.compare(x, other.x);
        if (d == 0) d = Integer.compare(z, other.z);
        if (d == 0) d = Integer.compare(y, other.y);
        return d;
    }

    @Override
    public int hashCode() {
        return x * 1317194159 + y * 1964379643 + z * 1656858407 + level;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof TilePos tp
                && level == tp.level && x == tp.x && y == tp.y && z == tp.z;
    }
}
