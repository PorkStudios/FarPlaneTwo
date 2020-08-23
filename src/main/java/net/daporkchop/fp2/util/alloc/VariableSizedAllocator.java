/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.util.alloc;

import lombok.NonNull;
import net.daporkchop.lib.common.math.PMath;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.primitive.lambda.IntIntFunction;
import net.daporkchop.lib.primitive.lambda.LongLongConsumer;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.minecraft.util.math.MathHelper.*;

/**
 * @author DaPorkchop_
 */
public final class VariableSizedAllocator implements Allocator {
    private static final int MIN_ALLOC_SZ = 4;

    private static final int BIN_COUNT = 9;
    private static final int BIN_MAX_IDX = (BIN_COUNT - 1);

    private static int getBinIndex(int size) {
        return clamp(31 - Integer.numberOfLeadingZeros(max(size, MIN_ALLOC_SZ)) - 2, 0, BIN_MAX_IDX);
    }

    protected final long block;
    protected final IntIntFunction growFunction;
    protected final LongLongConsumer resizeCallback;
    protected int size;

    protected final Bin[] bins = PArrays.filled(BIN_COUNT, Bin[]::new, Bin::new);
    protected final NavigableMap<Integer, Node> nodes = new TreeMap<>();

    public VariableSizedAllocator(long blockSize, @NonNull LongLongConsumer resizeCallback) {
        this(blockSize, resizeCallback, 0);
    }

    public VariableSizedAllocator(long blockSize, @NonNull LongLongConsumer resizeCallback, int initialBlockCount) {
        this(blockSize, resizeCallback, FixedSizeAllocator.DEFAULT_GROW_FUNCTION, initialBlockCount);
    }

    public VariableSizedAllocator(long blockSize, @NonNull LongLongConsumer resizeCallback, @NonNull IntIntFunction growFunction, int initialBlockCount) {
        this.block = positive(blockSize, "blockSize");
        this.size = notNegative(initialBlockCount, "initialBlockCount");
        this.resizeCallback = resizeCallback;
        this.growFunction = growFunction;
    }

    @Override
    public long alloc(long rawSize) {
        //round up to block size
        rawSize = PMath.roundUp(positive(rawSize, "rawSize"), this.block);
        int size = toInt(rawSize / this.block);

        int index;
        Node found;

        do {
            found = this.bins[index = getBinIndex(size)].getBestFit(size);
            while (found == null && ++index < BIN_COUNT) {
                found = this.bins[index].getBestFit(size);
            }
        } while (found == null && this.expand());
        checkState(found != null);

        if (found.size - size > MIN_ALLOC_SZ) {
            Node split = new Node();
            split.base = found.base + size;
            split.size = found.size - size;
            split.hole = true;

            this.nodes.put(split.base, split);
            this.bins[getBinIndex(split.size)].addNode(split);

            found.size = size;
        }

        found.hole = false;
        this.bins[index].removeNode(found);

        found.prev = null;
        found.next = null;

        return found.base * this.block;
    }

    @Override
    public void free(long address) {
        Node head = this.nodes.get(toInt(address / this.block));
        checkArg(head != null, "invalid address: %d", address);

        Integer base = head.base;

        Integer nextBase = this.nodes.higherKey(base);
        if (nextBase != null) {
            Node next = this.nodes.get(nextBase);
            if (next.hole) {
                checkState(this.nodes.remove(nextBase, next));
                this.bins[getBinIndex(next.size)].removeNode(next);

                head.size += next.size;
            }
        }

        Integer prevBase = this.nodes.lowerKey(base);
        if (prevBase != null) {
            Node prev = this.nodes.get(prevBase);
            if (prev.hole) {
                this.bins[getBinIndex(prev.size)].removeNode(prev);

                head.base = prev.base;
                head.size += prev.size;

                checkState(this.nodes.remove(base, head));
                checkState(this.nodes.replace(prevBase, prev, head));
            }
        }

        head.hole = true;
        this.bins[getBinIndex(head.size)].addNode(head);
    }

    @Override
    public void close() {
        //no-op
    }

    private boolean expand() {
        int oldSize = this.size;
        this.size = this.growFunction.applyAsInt(oldSize);
        int deltaSize = this.size - oldSize;
        checkState(deltaSize > 0, "size (%d) must be greater than previous size (%d)", this.size, oldSize);
        this.resizeCallback.accept(this.block * oldSize, this.block * this.size);

        Map.Entry<Integer, Node> wildernessEntry = this.nodes.lastEntry();
        if (wildernessEntry != null && wildernessEntry.getValue().hole) {
            Node wilderness = wildernessEntry.getValue();
            this.bins[getBinIndex(wilderness.size)].removeNode(wilderness);
            wilderness.size += deltaSize;
            this.bins[getBinIndex(wilderness.size)].addNode(wilderness);
        } else {
            Node wilderness = new Node();
            wilderness.base = oldSize;
            wilderness.size = deltaSize;
            wilderness.hole = true;
            this.nodes.put(wilderness.base, wilderness);
            this.bins[getBinIndex(wilderness.size)].addNode(wilderness);
        }
        return true;
    }

    private static final class Node {
        private Node next;
        private Node prev;
        private int base;
        private int size;
        private boolean hole;
    }

    private static final class Bin {
        private Node head;

        public void addNode(Node node) {
            node.next = node.prev = null;

            if (this.head == null) {
                this.head = node;
                return;
            }

            Node current = this.head;
            Node previous = null;
            while (current != null && current.size <= node.size) {
                previous = current;
                current = current.next;
            }

            if (current == null) {
                previous.next = node;
                node.prev = previous;
            } else if (previous != null) {
                node.next = current;
                previous.next = node;

                node.prev = previous;
                current.prev = node;
            } else {
                node.next = this.head;
                this.head.prev = node;
                this.head = node;
            }
        }

        public void removeNode(Node node) {
            if (this.head == null) {
                return;
            } else if (this.head == node) {
                this.head = this.head.next;
                return;
            }

            Node temp = this.head.next;
            while (temp != null) {
                if (temp == node) {
                    if (temp.next == null) {
                        temp.prev.next = null;
                    } else {
                        temp.prev.next = temp.next;
                        temp.next.prev = temp.prev;
                    }
                    return;
                }
                temp = temp.next;
            }
        }

        public Node getBestFit(int size) {
            Node temp = this.head;

            while (temp != null) {
                if (temp.size >= size) {
                    return temp;
                }
                temp = temp.next;
            }
            return null;
        }

        public Node getLastNode() {
            Node temp = this.head;

            while (temp.next != null) {
                temp = temp.next;
            }
            return temp;
        }
    }
}
