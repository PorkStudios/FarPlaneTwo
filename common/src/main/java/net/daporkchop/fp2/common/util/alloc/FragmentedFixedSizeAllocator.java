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

package net.daporkchop.fp2.common.util.alloc;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import lombok.NonNull;

import java.util.BitSet;
import java.util.NavigableMap;
import java.util.TreeMap;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A simple, efficient memory allocator for fixed-size units of memory backed by a fragmented heap.
 *
 * @author DaPorkchop_
 */
public final class FragmentedFixedSizeAllocator implements Allocator {
    /*
     * Performance characteristics (A=arenas, C=arena capacity):
     *
     * alloc():
     *   - O(1) (best-case)
     *   - O(C/2) (average)
     *   - O(log2(A) + C) (worst-case)
     * free():
     *   - O(log2(A))
     */

    protected final long blockSize;
    protected final int arenaCapacity;
    protected final Allocator allocator;

    protected final ReferenceLinkedOpenHashSet<Arena> nonFullArenas = new ReferenceLinkedOpenHashSet<>();
    protected final NavigableMap<Long, Arena> allArenas = new TreeMap<>(); //there's no fastutil equivalent to NavigableMap :(

    public FragmentedFixedSizeAllocator(long blockSize, @NonNull Allocator allocator) {
        this(blockSize, 4096, allocator);
    }

    public FragmentedFixedSizeAllocator(long blockSize, int arenaCapacity, @NonNull Allocator allocator) {
        this.blockSize = positive(blockSize, "blockSize");
        this.arenaCapacity = positive(arenaCapacity, "arenaCapacity");
        this.allocator = allocator;
    }

    @Override
    public long alloc(long size) {
        checkArg(size == this.blockSize, "size must be exactly block size (%d)", this.blockSize);

        Arena arena;
        if (this.nonFullArenas.isEmpty()) { //we should create a new arena
            arena = new Arena(this);
            this.allArenas.put(arena.startAddr, arena);
            this.nonFullArenas.add(arena);
        } else { //use the first available non-full arena
            arena = this.nonFullArenas.first();
        }

        int slot = arena.lowestClearBit;
        checkState(slot < this.arenaCapacity, "allocated too many slots (slot=%d, capacity=%d)", slot, this.arenaCapacity);

        arena.set(slot);
        arena.lowestClearBit = arena.nextClearBit(slot + 1);

        if (arena.lowestClearBit == this.arenaCapacity) { //the arena is now full, remove it from the non-full arenas set
            checkState(this.nonFullArenas.remove(arena), "arena was already un-marked as full?!?");
        }

        long addr = arena.startAddr + slot * this.blockSize;
        checkState(addr < arena.endAddr, "allocated address is too high?!? 0x%016x >= 0x%016x", addr, arena.endAddr);
        return addr;
    }

    @Override
    public void free(long address) {
        Arena arena = this.allArenas.floorEntry(address).getValue();
        checkArg(address < arena.endAddr, "address 0x%016x doesn't correspond to any arenas", address);

        int slot = toInt((address - arena.startAddr) / this.blockSize);
        checkState(slot < this.arenaCapacity, "slot is too high?!? (slot=%d, capacity=%d)", slot, this.arenaCapacity);
        checkArg(arena.get(slot), "address 0x%016x (in arena starting at 0x%016x) has already been freed", address, arena.startAddr);
        arena.clear(slot);

        if (arena.lowestClearBit == this.arenaCapacity) { //the arena was previously full, but now that a slot has been freed it isn't any more
            checkState(this.nonFullArenas.addAndMoveToFirst(arena), "arena starting at 0x%016x was already marked as non-full", arena.startAddr);
        }

        if (slot < arena.lowestClearBit) {
            arena.lowestClearBit = slot;
        }

        if (arena.isEmpty()) { //the arena is now empty, release it
            checkState(this.allArenas.remove(arena.startAddr, arena));
            checkState(this.nonFullArenas.remove(arena));
            this.allocator.free(arena.startAddr);
        }
    }

    /**
     * @author DaPorkchop_
     */
    protected static class Arena extends BitSet { //extend BitSet to eliminate an indirection
        protected final long startAddr;
        protected final long endAddr;

        protected int lowestClearBit = 0;

        public Arena(@NonNull FragmentedFixedSizeAllocator parent) {
            super(parent.arenaCapacity);

            this.startAddr = parent.allocator.alloc(parent.blockSize * parent.arenaCapacity);
            this.endAddr = this.startAddr + parent.blockSize * parent.arenaCapacity;
        }
    }
}
