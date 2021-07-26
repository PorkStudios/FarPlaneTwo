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

package net.daporkchop.fp2.mode.common.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.client.gl.camera.IFrustum;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.api.IFarDirectPosAccess;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.api.IFarTile;
import net.daporkchop.fp2.util.datastructure.DirectLongStack;
import net.daporkchop.fp2.util.math.geometry.Volume;
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.util.function.LongConsumer;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Outrageously complex implementation of an n-tree utilizing 100% off-heap memory.
 * <p>
 * I needed do this in order to avoid tons of indirection thanks to the fact that Java 8 doesn't allow me to make flat structures.
 * <p>
 * Dear OpenJDK team,
 * PLEASE FOR THE LOVE OF ALL THAT IS GOOD AND HOLY HURRY UP AND FINISH PROJECT VALHALLA AAAAAAAAAA
 * Sincerely,
 * Everyone
 * <p>
 * (update: i've since learned that even with value types i STILL won't be able to implement this the way i want to, so... fuck.)
 *
 * @author DaPorkchop_
 */
public class FarRenderTree<POS extends IFarPos, T extends IFarTile> extends AbstractReleasable {
    /*
     * Constants:
     * - D: number of dimensions
     *
     * struct Node {
     *   byte flags;
     *   - 0x00: data //whether or not this node has been rendered and contains data
     *   - 0x01: empty //whether or not this node has been rendered, but contains no data
     *   - 0x02: render_parent //whether or not the parent tile may be rendered on top of this one
     *   Tile tile;
     *   Node* children[1 << D];
     * };
     *
     * struct Tile {
     *   Pos pos; //defined by IFarRenderStrategy implementation
     *   RenderData data; //defined by IFarRenderStrategy implementation
     * };
     */

    public static final int FLAG_DATA = 1 << 0;
    public static final int FLAG_EMPTY = 1 << 1;
    public static final int FLAG_RENDER_PARENT = 1 << 2;

    // begin field offsets

    protected final long flags = 0L;
    protected final long tile = 1L;
    protected final long children;

    protected final long tile_pos = 1L; // this.tile + 0L
    protected final long tile_renderData;

    // end field offsets

    protected final long posSize;
    protected final long renderDataSize;
    protected final long nodeSize;

    protected final int d;
    protected final int maxLevel;

    protected final long root;
    protected final IFarDirectPosAccess<POS> directPosAccess;
    protected final IFarRenderStrategy<POS, T> strategy;

    protected final PCleaner cleaner;

    public FarRenderTree(@NonNull IFarRenderMode<POS, T> mode, @NonNull IFarRenderStrategy<POS, T> strategy, int maxLevel) {
        this.strategy = strategy;
        this.directPosAccess = mode.directPosAccess();
        this.d = this.directPosAccess.axisCount();
        this.maxLevel = notNegative(maxLevel, "maxLevel");

        this.posSize = this.directPosAccess.posSize();
        this.renderDataSize = this.strategy.renderDataSize();

        this.tile_renderData = this.tile + this.posSize;
        this.children = this.tile_renderData + this.renderDataSize;

        this.nodeSize = this.children + ((long) LONG_SIZE << this.d);
        FP2_LOG.info("{}D tree node size: {} bytes", this.d, this.nodeSize);

        PUnsafe.setMemory(this.root = PUnsafe.allocateMemory(this.nodeSize), this.nodeSize, (byte) 0);

        this.cleaner = PCleaner.cleaner(this, new ReleaseFunction<>(this.root, this.flags, this.children, this.tile_renderData, this.d, this.strategy));
    }

    /**
     * Gets the index of the child tile which contains the tile at the given position.
     * <p>
     * If the level is less than or equal to the given position's level, the result is undefined.
     *
     * @param level the current level
     * @param pos   the target position
     * @return the index of the child tile which contains the tile at the given position
     */
    protected int childIndex(int level, @NonNull POS pos) {
        int shift = level - pos.level() - 1;
        int i = 0;
        for (int dim = 0; dim < this.d; dim++) {
            i |= ((this.directPosAccess.getAxisHeap(pos, dim) >>> shift) & 1) << dim;
        }
        return i;
    }

    /**
     * Checks whether or not the given node has all of the given flags set.
     *
     * @param node  the node to check
     * @param flags the flags to check for
     * @return whether or not the given node has all of the given flags set
     */
    public boolean checkFlagsAND(long node, int flags) {
        return (PUnsafe.getByte(node + this.flags) & flags) == flags;
    }

    /**
     * Checks whether or not the given node has any of the given flags set.
     *
     * @param node  the node to check
     * @param flags the flags to check for
     * @return whether or not the given node has any of the given flags set
     */
    public boolean checkFlagsOR(long node, int flags) {
        return (PUnsafe.getByte(node + this.flags) & flags) != 0;
    }

    /**
     * Sets the given flag for the given node.
     *
     * @param node  the node to update the flags for
     * @param flags the flags to set
     */
    public void setFlags(long node, int flags) {
        long addr = node + this.flags;
        PUnsafe.putByte(addr, (byte) (PUnsafe.getByte(addr) | flags));
    }

    /**
     * Clears the given flag for the given node.
     *
     * @param node  the node to update the flags for
     * @param flags the flags to clear
     */
    public void clearFlags(long node, int flags) {
        long addr = node + this.flags;
        PUnsafe.putByte(addr, (byte) (PUnsafe.getByte(addr) & ~flags));
    }

    /**
     * Checks whether or not the given node has any children.
     *
     * @param node the node
     * @return whether or not the given node has any children
     */
    protected boolean hasAnyChildren(long node) {
        for (long addr = node + this.children, lastChildAddr = addr + ((long) LONG_SIZE << this.d); addr != lastChildAddr; addr += LONG_SIZE) {
            long child = PUnsafe.getLong(addr);
            if (child != 0L) { //child exists
                return true;
            }
        }
        return false;
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
        if (this.checkFlagsAND(node, requiredFlags)) {
            action.accept(node);
        }

        for (long addr = node + this.children, lastChildAddr = addr + ((long) LONG_SIZE << this.d); addr != lastChildAddr; addr += LONG_SIZE) {
            long child = PUnsafe.getLong(addr);
            if (child != 0L) { //child exists
                this.forEach0(child, action, requiredFlags);
            }
        }
    }

    /**
     * Sets the render data for the tile at the given position.
     *
     * @param pos the position of the tile to set the render data for
     */
    public void putRenderData(@NonNull POS pos, BakeOutput output) {
        this.putRenderData0(MAX_LODS, this.root, pos, output);
    }

    protected void putRenderData0(int level, long node, POS pos, BakeOutput output) {
        long child = this.findChildStep(level, node, pos);
        if (child != node) {
            if (child == 0L) { //next node doesn't exist
                //allocate new node
                child = this.createNode(level - 1, uncheckedCast(pos.upTo(level - 1)));

                //store pointer into children array
                PUnsafe.putLong(node + this.children + this.childIndex(level, pos) * 8L, child);
            }

            this.putRenderData0(level - 1, child, pos, output);
        } else { //current node is the target node
            this.setRenderData(node, pos, output);
        }
    }

    protected void setRenderData(long node, POS pos, BakeOutput output) {
        //release exiting data if necessary
        this.deleteRenderData(node);

        if (output != null) { //new render data is non-empty
            PUnsafe.copyMemory(output.renderData, node + this.tile_renderData, output.size);

            this.setFlags(node, FLAG_DATA);

            if (output.forceRenderParent) {
                this.setFlags(node, FLAG_RENDER_PARENT);
            }
        } else { //new render data is empty
            this.setFlags(node, FLAG_EMPTY);
        }
    }

    protected long createNode(int level, POS pos) {
        notNegative(level, "level");

        long node = PUnsafe.allocateMemory(this.nodeSize);
        PUnsafe.setMemory(node, this.nodeSize, (byte) 0);
        this.directPosAccess.storePos(pos, node + this.tile_pos);

        return node;
    }

    /**
     * Removes the node at the given position.
     *
     * @param pos the position of the tile to remove
     */
    public void removeNode(@NonNull POS pos) {
        this.removeNode0(MAX_LODS, this.root, pos);
    }

    protected boolean removeNode0(int level, long node, POS pos) {
        long child = this.findChildStep(level, node, pos);
        if (child != node) {
            if (child == 0L) { //next node doesn't exist
                return false; //exit without changing anything
            }

            if (this.removeNode0(level - 1, child, pos)) { //the child node should be removed
                //release child's memory
                PUnsafe.freeMemory(child);

                //remove reference to child from children array
                PUnsafe.putLong(node + this.children + this.childIndex(level, pos) * 8L, 0L);

                if (!this.checkFlagsOR(node, FLAG_DATA | FLAG_EMPTY)) { //node itself has no data
                    //if the node has no children, we return true, indicating that this node is now empty and should be deleted
                    return !this.hasAnyChildren(node);
                }
            }

            return false; //no further changes are necessary
        } else { //current node is the target node
            this.deleteRenderData(node);

            //if the node has no children, we return true, indicating that this node is now empty and should be deleted
            return !this.hasAnyChildren(node);
        }
    }

    protected void deleteRenderData(long node) {
        if (this.checkFlagsOR(node, FLAG_DATA)) { //current node has data
            //free render data allocations
            this.strategy.deleteRenderData(node + this.tile_renderData);

            //zero out render data
            PUnsafe.setMemory(node + this.tile_renderData, this.renderDataSize, (byte) 0);
        }

        //clear all flags
        this.clearFlags(node, 0xFFFFFFFF);
    }

    protected long findChildStep(int level, long node, POS pos) {
        notNegative(level, "level");

        if (pos.level() == level && pos.equals(this.directPosAccess.loadPos(node + this.tile_pos))) {
            return node;
        }

        int childIndex = this.childIndex(level, pos);
        return PUnsafe.getLong(node + this.children + childIndex * 8L);
    }

    /**
     * Selects all tiles that are applicable for the given view ranges and frustum and adds them to the render index.
     *
     * @param ranges  an array of volumes defining the LoD level cutoff ranges
     * @param frustum the view frustum
     * @param index   the render index to add tiles to
     */
    public void select(@NonNull Volume[] ranges, @NonNull IFrustum frustum, @NonNull DirectLongStack index) {
        this.select0(MAX_LODS, this.root, ranges, frustum, index);
    }

    protected boolean select0(int level, long node, Volume[] ranges, IFrustum frustum, DirectLongStack index) {
        if (level == 0 && this.checkFlagsOR(node, FLAG_EMPTY)) { //TODO: remove "level == 0 && "
            //this tile is baked and empty, so we can be sure that none of its children will be non-empty and there's no reason to recurse any further
            return true;
        } else if (level < this.maxLevel //don't do range checking for the top level, as it will cause a bunch of tiles to be loaded but never rendered
                   && !this.directPosAccess.intersects(node + this.tile_pos, ranges[level])) {
            //the view range for this level doesn't intersect this tile's bounding box,
            // so we can be certain that neither this tile nor any of its children would be contained
            return false;
        } else if (level < MAX_LODS //don't do frustum culling on the root node, as it doesn't have a valid position because it's not really "there"
                   && !this.directPosAccess.inFrustum(node + this.tile_pos, frustum)) {
            //the frustum doesn't contain this tile's bounding box, so we can be certain that neither
            // this tile nor any of its children would be visible
            return true; //return true to prevent parent node from skipping all high-res tiles if some of them were outside of the frustum
        } else if (level == 0 && this.directPosAccess.isVanillaRenderable(node + this.tile_pos)) {
            //the node will be rendered by vanilla, so we don't want to draw over it
            return true;
        } else if (!this.checkFlagsOR(node, FLAG_DATA | FLAG_EMPTY)) { //this node has no data
            //try to select as many children as possible, but only return true if all of them were successful
            boolean allSuccess = true;
            for (long addr = node + this.children, lastChildAddr = addr + ((long) LONG_SIZE << this.d); addr != lastChildAddr; addr += LONG_SIZE) {
                long child = PUnsafe.getLong(addr);
                allSuccess &= child != 0L //check if child is set
                              && this.select0(level - 1, child, ranges, frustum, index);
            }
            return allSuccess;
        }

        //at this point we know that the tile has some data and is selectable

        CHILDREN:
        if (level != 0 && this.hasAnyChildren(node) && this.directPosAccess.containedBy(node + this.tile_pos, ranges[level - 1])) {
            //this tile is entirely within the view distance of the lower zoom level for tiles below it, so let's consider selecting them as well
            //if all children are selectable, select all of them
            //otherwise (if only some/none of the children are selectable) we ignore all of the children and only output the current tile. the better
            // solution to this problem would be to have some mechanism to allow rendering a certain part of a tile, but the memory overhead required
            // to do so would be atrocious.

            long firstChildAddr = node + this.children;
            long lastChildAddr = firstChildAddr + ((long) LONG_SIZE << this.d);

            //first pass to simply check for any null children before recursing into any of them
            for (long addr = firstChildAddr; addr != lastChildAddr; addr += LONG_SIZE) {
                if (PUnsafe.getLong(addr) == 0L) {
                    break CHILDREN;
                }
            }

            boolean shouldRenderSelf = false;

            int mark = index.mark();
            for (long addr = firstChildAddr; addr != lastChildAddr; addr += LONG_SIZE) {
                long child = PUnsafe.getLong(addr);
                if (!this.select0(level - 1, child, ranges, frustum, index)) {
                    //if any one of the children cannot be added, abort and add this tile over the whole area instead
                    index.restore(mark);
                    break CHILDREN;
                }

                shouldRenderSelf |= this.checkFlagsOR(child, FLAG_RENDER_PARENT);
            }

            //all of the children were able to be added

            if (!shouldRenderSelf //unless explicitly requested, we shouldn't be rendering ourself over a high-res tile
                || this.directPosAccess.containedBy(node + this.tile_pos, ranges[level - 1].shrink(T_VOXELS << level))) {
                return true;
            }
        }

        if (FP2_DEBUG && FP2Config.debug.skipLevel0 && level == 0) {
            return false;
        }

        if (this.checkFlagsOR(node, FLAG_DATA)) {
            //actually append node to render list
            index.push(node + this.tile);
        }
        return true;
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
    private static final class ReleaseFunction<POS extends IFarPos, T extends IFarTile> implements Runnable {
        protected final long root;
        protected final long flags;
        protected final long children;
        protected final long tile_renderData;
        protected final int d;

        @NonNull
        protected final IFarRenderStrategy<POS, T> strategy;

        @Override
        public void run() {
            ClientThreadExecutor.INSTANCE.execute(() -> this.deleteNode(this.root));
        }

        protected void deleteNode(long node) {
            for (long addr = node + this.children, lastChildAddr = addr + ((long) LONG_SIZE << this.d); addr != lastChildAddr; addr += LONG_SIZE) {
                long child = PUnsafe.getLong(addr);
                if (child != 0L) { //child exists
                    this.deleteNode(child);
                }
            }

            //release render data
            if ((PUnsafe.getByte(node + this.flags) & FLAG_DATA) != 0) {
                this.strategy.deleteRenderData(node + this.tile_renderData);
            }

            PUnsafe.freeMemory(node);
        }
    }
}
