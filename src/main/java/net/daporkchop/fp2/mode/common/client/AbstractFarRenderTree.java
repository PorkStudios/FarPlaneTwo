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

package net.daporkchop.fp2.mode.common.client;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.mode.api.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.util.DirectBufferReuse;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.util.function.LongConsumer;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * Outrageously complex implementation of an n-tree utilizing 100% off-heap memory.
 * <p>
 * I needed do this in order to avoid tons of indirection thanks to the fact that Java 8 doesn't allow me to make flat structures.
 * <p>
 * Dear OpenJDK team,
 * PLEASE FOR THE LOVE OF ALL THAT IS GOOD AND HOLY HURRY UP AND FINISH PROJECT VALHALLA AAAAAAAAAA
 * Sincerely,
 * Everyone
 *
 * @author DaPorkchop_
 */
public abstract class AbstractFarRenderTree<POS extends IFarPos, P extends IFarPiece<POS>> extends AbstractReleasable {
    //this is enough levels for the tree to encompass the entire minecraft world
    public static final int DEPTH = 32 - Integer.numberOfLeadingZeros(60_000_000 >> T_SHIFT);

    /*
     * Constants:
     * - D: number of dimensions
     *
     * Node structure:
     *   int flags;
     *   - 0x00: top
     *   - 0x01: bottom
     *   - 0x02: placeholder //whether or not this node is a placeholder and cannot actually store any data
     *   - 0x03: rendered
     *   - 0x04: selfOrAnyChildrenRendered
     *   - 0x05: opaque
     *   - 0x06: transparent
     *   int pos[D]; //only stores X(Y)Z coordinates, level is computed dynamically based on recursion depth
     *   RenderData opaque;
     *   RenderData transparent;
     *   Node* children[1 << D];
     *
     * RenderData structure:
     *   GpuBuffer vertices;
     *   GpuBuffer indices;
     *
     * GpuBuffer structure:
     *   void* addr; //address of data in main memory
     *   int off; //offset of data in gpu memory
     *   int count;
     *
     * Total size: 4+2*(2*(8+2*4))+D*4+2^D*8
     * - 2D: 108 bytes //~1.7 cache lines
     * - 3D: 144 bytes //~2.25 cache lines
     */

    public static final int FLAG_TOP = 1 << 0;
    public static final int FLAG_BOTTOM = 1 << 1;
    public static final int FLAG_PLACEHOLDER = 1 << 2;
    public static final int FLAG_RENDERED = 1 << 3;
    public static final int FLAG_SELFORANYCHILDRENRENDERED = 1 << 4;
    public static final int FLAG_OPAQUE = 1 << 5;
    public static final int FLAG_TRANSPARENT = 1 << 6;

    protected static final long GPUBUFFER_ADDR = 0L;
    protected static final long GPUBUFFER_OFF = GPUBUFFER_ADDR + 8L;
    protected static final long GPUBUFFER_SIZE = GPUBUFFER_OFF + 4L;
    protected static final long GPUBUFFER_STRUCT_SIZE = GPUBUFFER_SIZE + 4L;

    protected static final long RENDERDATA_VERTICES = 0L;
    protected static final long RENDERDATA_INDICES = RENDERDATA_VERTICES + GPUBUFFER_STRUCT_SIZE;
    protected static final long RENDERDATA_STRUCT_SIZE = RENDERDATA_INDICES + GPUBUFFER_STRUCT_SIZE;

    //the following 5 fields are the offsets of the actual node member variables from the node's pointer
    protected final long flags;
    protected final long pos;
    protected final long opaque;
    protected final long transparent;
    protected final long children;

    protected final long nodeSize;
    protected final int d;
    protected final int vertexSize;
    protected final int indexSize;
    protected final int maxLevel;

    protected final long root;
    protected final AbstractFarRenderCache<POS, P, ?> cache;

    protected final PCleaner cleaner;

    public AbstractFarRenderTree(@NonNull AbstractFarRenderCache<POS, P, ?> cache, int d) {
        this.d = positive(d, "d");

        this.flags = 0L;
        this.pos = this.flags + 4L;
        this.opaque = this.pos + d * 4L;
        this.transparent = this.opaque + RENDERDATA_STRUCT_SIZE;
        this.children = this.transparent + RENDERDATA_STRUCT_SIZE;

        this.nodeSize = this.children + this.childCount() * 8L;
        LOGGER.info("{}D tree node size: {} bytes", d, this.nodeSize);

        this.root = PUnsafe.allocateMemory(this.nodeSize);

        this.cleaner = PCleaner.cleaner(this, new ReleaseFunction(this.root, this.flags, this.opaque, this.transparent, this.children, this.childCount()));

        this.cache = cache;
        this.vertexSize = this.cache.vertexSize();
        this.indexSize = this.cache.indexSize();
        this.maxLevel = this.cache.renderer.maxLevel;
    }

    /**
     * @return the number of children that a single node can have
     */
    protected int childCount() {
        return 1 << this.d;
    }

    /**
     * Stores the given position off-heap.
     *
     * @param pos     the address to the store the position at
     * @param toStore the position to store
     */
    protected abstract void storePos(long pos, @NonNull POS toStore);

    /**
     * Checks whether or not the given positions are equal.
     *
     * @param aLevel the level of the first position
     * @param aPos   the coordinates of the first position
     * @param b      the second position
     * @return whether or not the given positions are equal
     */
    protected abstract boolean isPosEqual(int aLevel, long aPos, @NonNull POS b);

    /**
     * Gets the index of the child tile which contains the tile at the given position.
     * <p>
     * If the level is less than or equal to the given position's level, the result is undefined.
     *
     * @param level the current level
     * @param pos   the target position
     * @return the index of the child tile which contains the tile at the given position
     */
    protected abstract int childIndex(int level, @NonNull POS pos);

    /**
     * Checks whether or not the given node has all of the given flags set.
     *
     * @param node  the node to check
     * @param flags the flags to check for
     * @return whether or not the given node has all of the given flags set
     */
    public boolean checkFlags(long node, int flags) {
        return (PUnsafe.getInt(node + this.flags) & flags) == flags;
    }

    /**
     * Sets the given flag for the given node.
     *
     * @param node  the node to update the flags for
     * @param flags the flags to set
     */
    public void setFlags(long node, int flags) {
        long addr = node + this.flags;
        PUnsafe.putInt(addr, PUnsafe.getInt(addr) | flags);
    }

    /**
     * Clears the given flag for the given node.
     *
     * @param node  the node to update the flags for
     * @param flags the flags to clear
     */
    public void clearFlags(long node, int flags) {
        long addr = node + this.flags;
        PUnsafe.putInt(addr, PUnsafe.getInt(addr) & ~flags);
    }

    /**
     * Runs the given function for every node in the tree.
     *
     * @param action        the function to run
     * @param requiredFlags the flags that must be set in order for the node to be considered
     */
    public void forEach(@NonNull LongConsumer action, int requiredFlags) {
        this.forEach0(this.root, action, requiredFlags);
    }

    protected void forEach0(long node, LongConsumer action, int requiredFlags) {
        if (this.checkFlags(node, requiredFlags)) {
            action.accept(node);
        }

        for (int i = 0, childCount = this.childCount(); i < childCount; i++) {
            long child = PUnsafe.getLong(node + this.children + i * 8L);
            if (child != 0L) { //child exists
                this.forEach0(child, action, requiredFlags);
            }
        }
    }

    /**
     * Sets the render data for the piece at the given position.
     *
     * @param pos            the position of the piece to set the render data for
     * @param opaqueVertices a {@link ByteBuf} containing the opaque vertex data
     * @param opaqueIndices  a {@link ByteBuf} containing the opaque index data
     */
    public void putRenderData(@NonNull POS pos, @NonNull ByteBuf opaqueVertices, @NonNull ByteBuf opaqueIndices) {
        this.putRenderData0(DEPTH, this.root, pos, opaqueVertices, opaqueIndices);
    }

    protected long putRenderData0(int level, long node, POS pos, ByteBuf opaqueVertices, ByteBuf opaqueIndices) {
        long child = this.findChildStep(level, node, pos);
        if (child != node) {
            if (child == 0L) { //next node doesn't exist
                //allocate new node
                child = this.createNode(level - 1, uncheckedCast(pos.upTo(level - 1)));

                //store pointer into children array
                PUnsafe.putLong(node + this.children + this.childIndex(level, pos) * 8L, child);
            }

            return this.putRenderData0(level - 1, child, pos, opaqueVertices, opaqueIndices);
        }

        this.setRenderData(node, node + this.opaque, FLAG_OPAQUE, opaqueVertices, opaqueIndices);
        this.setFlags(node, FLAG_RENDERED);

        return node;
    }

    protected void setRenderData(long node, long data, int layerFlag, ByteBuf vertices, ByteBuf indices)   {
        if (this.checkFlags(node, FLAG_RENDERED | layerFlag)) { //render layer already has data which needs to be released first
            //free vertex data
            PUnsafe.freeMemory(PUnsafe.getLong(data + RENDERDATA_VERTICES + GPUBUFFER_ADDR));
            this.cache.verticesAllocator.free((long) PUnsafe.getInt(data + RENDERDATA_VERTICES + GPUBUFFER_OFF) * this.vertexSize);

            //free index data
            PUnsafe.freeMemory(PUnsafe.getLong(data + RENDERDATA_INDICES + GPUBUFFER_ADDR));
            this.cache.indicesAllocator.free((long) PUnsafe.getInt(data + RENDERDATA_INDICES + GPUBUFFER_OFF) * this.indexSize);
        }

        if (vertices.isReadable() && indices.isReadable())  {
            //mark layer as rendered
            this.setFlags(node, layerFlag);

            //copy vertex data
            int sizeBytes = vertices.readableBytes();
            long addr = PUnsafe.allocateMemory(sizeBytes);
            vertices.readBytes(DirectBufferReuse.wrapByte(addr, sizeBytes));
            PUnsafe.putLong(data + RENDERDATA_VERTICES + GPUBUFFER_ADDR, addr);
            PUnsafe.putInt(data + RENDERDATA_VERTICES + GPUBUFFER_OFF, toInt(this.cache.verticesAllocator.alloc(sizeBytes) / this.vertexSize));
            PUnsafe.putInt(data + RENDERDATA_VERTICES + GPUBUFFER_SIZE, sizeBytes / this.vertexSize);

            //copy index data
            sizeBytes = indices.readableBytes();
            addr = PUnsafe.allocateMemory(sizeBytes);
            indices.readBytes(DirectBufferReuse.wrapByte(addr, sizeBytes));
            PUnsafe.putLong(data + RENDERDATA_INDICES + GPUBUFFER_ADDR, addr);
            PUnsafe.putInt(data + RENDERDATA_INDICES + GPUBUFFER_OFF, toInt(this.cache.indicesAllocator.alloc(sizeBytes) / this.indexSize));
            PUnsafe.putInt(data + RENDERDATA_INDICES + GPUBUFFER_SIZE, sizeBytes / this.indexSize);

            //upload data to gpu
            this.uploadRenderData(data);
        } else {
            this.clearFlags(node, layerFlag);
        }
    }

    /**
     * Gets the node at the given position, creating it if it doesn't exist.
     *
     * @param pos the position of the node to get
     * @return the node at the given position
     */
    public long getOrCreateNode(@NonNull POS pos) {
        return this.getOrCreateNode0(DEPTH, this.root, pos);
    }

    protected long getOrCreateNode0(int level, long node, POS pos) {
        long child = this.findChildStep(level, node, pos);
        if (child == 0L) { //next node doesn't exist
            //allocate new node
            child = this.createNode(level - 1, uncheckedCast(pos.upTo(level - 1)));

            //store pointer into children array
            PUnsafe.putLong(node + this.children + this.childIndex(level, pos) * 8L, child);
        } else if (child == node) { //we are currently at the target node
            return node;
        }

        return this.getOrCreateNode0(level - 1, child, pos);
    }

    protected long createNode(int level, POS pos) {
        long node = PUnsafe.allocateMemory(this.nodeSize);

        int flags = 0;
        if (level > this.maxLevel) { //the node is above the maximum level, it can't be anything other than a placeholder node
            flags |= FLAG_PLACEHOLDER;
        } else {
            if (level == this.maxLevel) { //the node is at the highest level
                flags |= FLAG_TOP;
            }
            if (level == 0) { //the node is at the lowest level
                flags |= FLAG_BOTTOM;
            }
        }
        PUnsafe.putInt(node + this.flags, flags); //store flags in node

        return node;
    }

    protected long findChildStep(int level, long node, POS pos) {
        if (this.isPosEqual(level, node + this.pos, pos)) {
            return node;
        }

        int childIndex = this.childIndex(level, pos);
        return PUnsafe.getLong(node + this.children + childIndex * 8L);
    }

    public void uploadRenderData(long data) {
        this.uploadGpuBuffer(GL_ARRAY_BUFFER, data + RENDERDATA_VERTICES, this.vertexSize);
        this.uploadGpuBuffer(GL_ELEMENT_ARRAY_BUFFER, data + RENDERDATA_INDICES, this.indexSize);
    }

    public void uploadVertices(long node) {
        if (this.checkFlags(node, FLAG_RENDERED | FLAG_OPAQUE)) {
            this.uploadGpuBuffer(GL_ARRAY_BUFFER, node + this.opaque + RENDERDATA_VERTICES, this.vertexSize);
        }
        if (this.checkFlags(node, FLAG_RENDERED | FLAG_TRANSPARENT)) {
            this.uploadGpuBuffer(GL_ARRAY_BUFFER, node + this.transparent + RENDERDATA_VERTICES, this.vertexSize);
        }
    }

    public void uploadIndices(long node) {
        if (this.checkFlags(node, FLAG_RENDERED | FLAG_OPAQUE)) {
            this.uploadGpuBuffer(GL_ELEMENT_ARRAY_BUFFER, node + this.opaque + RENDERDATA_INDICES, this.indexSize);
        }
        if (this.checkFlags(node, FLAG_RENDERED | FLAG_TRANSPARENT)) {
            this.uploadGpuBuffer(GL_ELEMENT_ARRAY_BUFFER, node + this.transparent + RENDERDATA_INDICES, this.indexSize);
        }
    }

    public void uploadGpuBuffer(int slot, long gpuBuffer, int sizeFactor) {
        glBufferSubData(slot,
                (long) PUnsafe.getInt(gpuBuffer + GPUBUFFER_OFF) * sizeFactor,
                DirectBufferReuse.wrapByte(PUnsafe.getLong(gpuBuffer + GPUBUFFER_ADDR), PUnsafe.getInt(gpuBuffer + GPUBUFFER_SIZE) * sizeFactor));
    }

    @Override
    protected void doRelease() {
        this.cleaner.clean();
    }

    /**
     * Recursively deletes all nodes in the tree.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    private static final class ReleaseFunction implements Runnable {
        protected final long root;
        protected final long flags;
        protected final long opaque;
        protected final long transparent;
        protected final long children;
        protected final int childCount;

        @Override
        public void run() {
            this.deleteNode(this.root);
        }

        protected void deleteNode(long node) {
            for (int i = 0; i < this.childCount; i++) {
                long child = PUnsafe.getLong(node + this.children + i * 8L);
                if (child != 0L) { //child exists
                    this.deleteNode(child);
                }
            }

            //free render data allocations
            if ((PUnsafe.getInt(node + this.flags) & FLAG_OPAQUE) == FLAG_OPAQUE)   {
                PUnsafe.freeMemory(PUnsafe.getLong(node + this.opaque + RENDERDATA_VERTICES + GPUBUFFER_ADDR));
                PUnsafe.freeMemory(PUnsafe.getLong(node + this.opaque + RENDERDATA_INDICES + GPUBUFFER_ADDR));
            }
            if ((PUnsafe.getInt(node + this.flags) & FLAG_TRANSPARENT) == FLAG_TRANSPARENT)   {
                PUnsafe.freeMemory(PUnsafe.getLong(node + this.transparent + RENDERDATA_VERTICES + GPUBUFFER_ADDR));
                PUnsafe.freeMemory(PUnsafe.getLong(node + this.transparent + RENDERDATA_INDICES + GPUBUFFER_ADDR));
            }

            PUnsafe.freeMemory(node);
        }
    }
}
