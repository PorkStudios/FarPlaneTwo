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

package net.daporkchop.fp2.common.util.alloc;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.lib.common.annotation.NotThreadSafe;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.math.PMath;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * A simple, efficient memory allocator for arbitrarily sized blocks of memory backed by a sequential heap.
 * <p>
 * Nodes are inserted and removed from the list whenever an allocation is added or removed, and merged whenever possible.
 * <p>
 * Unallocated memory blocks are stored in a TreeSet to allow efficient detection of the smallest possible block that would fit an allocation of the given size.
 *
 * @author DaPorkchop_
 */
@NotThreadSafe
public final class SequentialVariableSizedAllocator extends Allocator {
    /*
     * Performance characteristics (C=capacity, N=extents):
     *
     * alloc():
     *   - O(log2(N)) (average)
     *   - O(log2(N) + C) (worst-case)
     * free():
     *   - O(log2(N))
     *
     * The null address is -1.
     */

    private static final long MIN_ALLOC_SZ = 64L; //the maximum number of bytes we are willing to waste as padding at the end of a block

    private final long blockSize;
    private final GrowFunction growFunction;
    private final SequentialHeapManager manager;
    private long capacity;

    private final NavigableSet<Node> emptyNodes = new TreeSet<>((Comparator<Object>) (_a, _b) -> {
        Node b = (Node) _b;
        if (_a instanceof Long) {
            return Long.compare((Long) _a, b.size);
        } else {
            Node a = (Node) _a;
            int d = Long.compare(a.size, b.size);
            return d != 0 ? d : Long.compare(a.base, b.base);
        }
    });
    private final Long2ObjectMap<Node> usedNodes = new Long2ObjectRBTreeMap<>();
    private Node tail;

    public SequentialVariableSizedAllocator(long blockSize, @NonNull SequentialHeapManager manager) {
        this(blockSize, manager, GrowFunction.def());
    }

    public SequentialVariableSizedAllocator(long blockSize, @NonNull SequentialHeapManager manager, @NonNull GrowFunction growFunction) {
        super(-1L);
        this.blockSize = positive(blockSize, "blockSize");
        this.manager = manager;
        this.growFunction = growFunction;

        this.manager.brk(this.capacity = toInt(this.growFunction.grow(0L, blockSize << 4L)));

        //create wilderness node
        this.tail = new Node().base(0L).size(this.capacity);
        this.emptyNodes.add(this.tail);
    }

    @Override
    public long alloc(@NotNegative long rawSize) {
        if (rawSize == 0L) {
            return this.nullAddress;
        }

        //round up to block size
        rawSize = PMath.roundUp(positive(rawSize, "rawSize"), this.blockSize);

        Node found;
        do {
            found = this.emptyNodes.ceiling(uncheckedCast(rawSize));
        } while (found == null && this.expand());
        checkState(found != null, "unable to allocate memory!");

        this.emptyNodes.remove(found);

        if (found.size - rawSize > MIN_ALLOC_SZ) { //we don't want to waste too much space at the end, so we split the node in two and leave the remaining space unallocated
            Node split = new Node()
                    .base(found.base + rawSize).size(found.size - rawSize)
                    .next(found.next).prev(found);
            found.size(rawSize).next(split);
            if (split.next != null) {
                split.next.prev(split);
            } else {
                this.tail = split;
            }
            this.emptyNodes.add(split);
        }

        found.used(true);
        this.usedNodes.put(found.base, found);
        return found.base;
    }

    @Override
    public void free(long address) {
        if (address == this.nullAddress) {
            return;
        }

        Node node = this.usedNodes.remove(address);
        checkArg(node != null, "invalid address for free(): %d (allocator state: %s)", address, this);

        node.used(false);
        if (node.next != null && !node.next.used) { //next node isn't used either, we can merge forwards
            Node next = node.next;
            this.emptyNodes.remove(next);

            node.size(node.size + next.size).next(next.next);
            if (next.next != null) {
                next.next.prev(node);
            } else {
                this.tail = node;
            }
        }
        if (node.prev != null && !node.prev.used) { //previous node isn't used, we can merge backwards
            Node prev = node.prev;
            this.emptyNodes.remove(prev);

            prev.size(prev.size + node.size).next(node.next);
            if (node.next != null) {
                node.next.prev(prev);
            } else {
                this.tail = prev;
            }

            node = prev;
        }

        this.emptyNodes.add(node);
    }

    private boolean expand() {
        long oldCapacity = this.capacity;
        long newCapacity = this.growFunction.grow(oldCapacity, this.blockSize);
        checkState(newCapacity > oldCapacity, "newCapacity (%d) must be greater than oldCapacity (%d)", newCapacity, oldCapacity);
        this.manager.sbrk(newCapacity);
        this.capacity = newCapacity;
        long deltaCapacity = newCapacity - oldCapacity;

        if (this.tail.used) { //tail node is allocated, create new node to be used as tail
            long oldPrevTailSize = this.tail.size;
            long newPrevTailSize = PMath.roundUp(oldPrevTailSize, this.blockSize);
            long newTailOffset = newPrevTailSize - oldPrevTailSize;
            this.tail.size(newPrevTailSize);

            Node node = new Node().prev(this.tail).base(oldCapacity + newTailOffset).size(deltaCapacity - newTailOffset);
            this.tail.next(node);
            this.tail = node;
            this.emptyNodes.add(node);
        } else { //tail node is unused, expand it
            this.emptyNodes.remove(this.tail);
            this.tail.size(this.tail.size + deltaCapacity);
            this.emptyNodes.add(this.tail);
        }
        return true;
    }

    @Override
    public Stats stats() {
        long allocations = this.usedNodes.size();
        long allocatedSpace = this.usedNodes.values().stream().mapToLong(Node::size).sum();

        return Stats.builder()
                .heapRegions(1L)
                .allocations(allocations)
                .allocatedSpace(allocatedSpace)
                .totalSpace(this.capacity)
                .build();
    }

    /**
     * @author DaPorkchop_
     */
    @Getter
    @Setter
    private static final class Node {
        private long base;
        private long size;
        private Node next;
        private Node prev;
        private boolean used;
    }
}
