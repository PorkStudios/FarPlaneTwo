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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla;

import net.minecraft.world.gen.layer.IntCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-static re-implementation of {@link IntCache}.
 *
 * @author DaPorkchop_
 */
public final class TLIntCache {
    private final List<int[]> freeSmallArrays = new ArrayList<>();
    private final List<int[]> inUseSmallArrays = new ArrayList<>();
    private final List<int[]> freeLargeArrays = new ArrayList<>();
    private final List<int[]> inUseLargeArrays = new ArrayList<>();
    private int intCacheSize = 256;

    public int[] getIntCache(int size) {
        if (size <= 256) {
            if (this.freeSmallArrays.isEmpty()) {
                int[] aint4 = new int[256];
                this.inUseSmallArrays.add(aint4);
                return aint4;
            } else {
                int[] aint3 = this.freeSmallArrays.remove(this.freeSmallArrays.size() - 1);
                this.inUseSmallArrays.add(aint3);
                return aint3;
            }
        } else if (size > this.intCacheSize) {
            this.intCacheSize = size;
            this.freeLargeArrays.clear();
            this.inUseLargeArrays.clear();
            int[] aint2 = new int[this.intCacheSize];
            this.inUseLargeArrays.add(aint2);
            return aint2;
        } else if (this.freeLargeArrays.isEmpty()) {
            int[] aint1 = new int[this.intCacheSize];
            this.inUseLargeArrays.add(aint1);
            return aint1;
        } else {
            int[] aint = this.freeLargeArrays.remove(this.freeLargeArrays.size() - 1);
            this.inUseLargeArrays.add(aint);
            return aint;
        }
    }

    public void resetIntCache() {
        if (!this.freeLargeArrays.isEmpty()) {
            this.freeLargeArrays.remove(this.freeLargeArrays.size() - 1);
        }

        if (!this.freeSmallArrays.isEmpty()) {
            this.freeSmallArrays.remove(this.freeSmallArrays.size() - 1);
        }

        this.freeLargeArrays.addAll(this.inUseLargeArrays);
        this.freeSmallArrays.addAll(this.inUseSmallArrays);
        this.inUseLargeArrays.clear();
        this.inUseSmallArrays.clear();
    }

    public String getCacheSizes() {
        return "cache: " + this.freeLargeArrays.size() + ", tcache: " + this.freeSmallArrays.size() + ", allocated: " + this.inUseLargeArrays.size() + ", tallocated: " + this.inUseSmallArrays.size();
    }
}
