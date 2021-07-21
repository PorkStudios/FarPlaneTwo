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
import net.daporkchop.fp2.util.threading.ClientThreadExecutor;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.primitive.map.ObjLongMap;
import net.daporkchop.lib.primitive.map.open.ObjLongOpenHashMap;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import java.util.function.LongConsumer;
import java.util.stream.Stream;

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
    //this is enough levels for the tree to encompass the entire minecraft world
    public static final int DEPTH = 32 - Integer.numberOfLeadingZeros(60_000_000 >> T_SHIFT);

    /*
     * Constants:
     * - D: number of dimensions
     *
     * struct Node {
     *   byte flags;
     *   - bit 0x00: data //whether or not this node has been baked and contains data
     *   - bit 0x01: empty //whether or not this node has been baked, but contains no data
     *   - bit 0x02: selectable //whether or not this node may be selected for rendering
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
    public static final int FLAG_SELECTABLE = 1 << 2;

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

    protected final ObjLongMap<POS> positionsToNodes = new ObjLongOpenHashMap<POS>().defaultValue(0L);

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
        long node = this.positionsToNodes.get(pos);
        if (node == 0L) { //no node exists, create new one
            this.putRenderData0(DEPTH, this.root, pos, output);
        } else { //quickly re-use existing node without having to recurse through the tree
            this.setRenderData(node, pos, output);
        }
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
            //execute render output state changes as needed
            // (doing this here slightly reduces memory overhead, as the old data has already been released at this point)
            output.execute();

            //copy render data to node
            PUnsafe.copyMemory(output.renderData, node + this.tile_renderData, output.size);

            this.setFlags(node, FLAG_DATA);
        } else { //new render data is empty
            this.setFlags(node, FLAG_EMPTY);
        }
    }

    protected long createNode(int level, POS pos) {
        notNegative(level, "level");

        long node = PUnsafe.allocateMemory(this.nodeSize);
        PUnsafe.setMemory(node, this.nodeSize, (byte) 0);
        this.directPosAccess.storePos(pos, node + this.tile_pos);

        //store the node's address in the map
        this.positionsToNodes.put(pos, node);

        //check to see if this node is selectable
        this.recheckSelectable(node, pos);

        //this node has been newly created - we need to notify its selection parents
        this.updateParentSelectability(pos);

        return node;
    }

    /**
     * Removes the node at the given position.
     *
     * @param pos the position of the tile to remove
     */
    public void removeNode(@NonNull POS pos) {
        if (this.positionsToNodes.containsKey(pos)) { //there is a node for the requested position, so we should actually try to delete it
            this.removeNode0(DEPTH, this.root, pos);
        }
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

                POS childPos = uncheckedCast(pos.upTo(level - 1));
                //remove child from node map
                this.positionsToNodes.remove(childPos, child);

                //child node has been deleted - we need to notify its selection parents
                this.updateParentSelectability(childPos);

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

        this.clearFlags(node, FLAG_DATA | FLAG_EMPTY);
    }

    protected long findChildStep(int level, long node, POS pos) {
        notNegative(level, "level");

        if (pos.level() == level && pos.equals(this.directPosAccess.loadPos(node + this.tile_pos))) {
            return node;
        }

        int childIndex = this.childIndex(level, pos);
        return PUnsafe.getLong(node + this.children + childIndex * 8L);
    }

    protected void recheckSelectable(long node, POS pos) {
        boolean previouslySelectable = this.checkFlagsOR(node, FLAG_SELECTABLE);

        //a tile is considered selectable if at least one of the NODES (not tiles!) one layer down in a radius of 1 tile is absent.
        boolean nowSelectable = pos.level() == 0 //level-0 tiles are always selectable
                                || !PorkUtil.<Stream<POS>>uncheckedCast(pos.down().allPositionsInBB(1, 3)).allMatch(this.positionsToNodes::containsKey);

        if (previouslySelectable != nowSelectable) { //select-ability has changed
            if (nowSelectable) { //update flags
                this.setFlags(node, FLAG_SELECTABLE);
            } else {
                this.clearFlags(node, FLAG_SELECTABLE);
            }
        }
    }

    protected void updateParentSelectability(POS start) {
        PorkUtil.<Stream<POS>>uncheckedCast(start.up().allPositionsInBB(1, 1))
                .forEach(pos -> {
                    long node = this.positionsToNodes.get(pos);
                    if (node != 0L) {
                        this.recheckSelectable(node, pos);
                    }
                });
    }

    /**
     * Selects all tiles that are applicable for the given view ranges and frustum and adds them to the render index.
     *
     * @param frustum the view frustum
     * @param index   the render index to add tiles to
     */
    public void select(@NonNull IFrustum frustum, @NonNull DirectLongStack index) {
        this.select0(DEPTH, this.root, frustum, index);
    }

    protected void select0(int level, long node, IFrustum frustum, DirectLongStack index) {
        if (level == 0 && this.checkFlagsOR(node, FLAG_EMPTY)) { //TODO: remove "level == 0 && "
            //this tile is baked and empty, so we can be sure that none of its children will be non-empty and there's no reason to recurse any further
            return;
        } else if (level < DEPTH //don't do frustum culling on the root node, as it doesn't have a valid position because it's not really "there"
                   && !this.directPosAccess.inFrustum(node + this.tile_pos, frustum)) {
            //the frustum doesn't contain this tile's bounding box, so we can be certain that neither
            // this tile nor any of its children would be visible
            return;
        } else if (level == 0 && this.directPosAccess.isVanillaRenderable(node + this.tile_pos)) {
            //the node will be rendered by vanilla, so we don't want to draw over it
            return;
        }

        if (level != 0 //level-0 tiles will never have any children
            && this.hasAnyChildren(node)) { //no need to consider selecting children if the tile has no children

            //this tile has some children and intersects the selection volume for the lower zoom level, so we'll attempt to select
            // as many children as possible

            long firstChildAddr = node + this.children;
            long lastChildAddr = firstChildAddr + ((long) LONG_SIZE << this.d);

            for (long addr = firstChildAddr; addr != lastChildAddr; addr += LONG_SIZE) {
                long child = PUnsafe.getLong(addr);
                if (child != 0L) {
                    this.select0(level - 1, child, frustum, index);
                }
            }

            //don't return true because we want to allow the tiles to overlap
            //return true;
        }

        if (FP2_DEBUG && FP2Config.debug.skipLevel0 && level == 0) { //skip level-0 tiles when they're disabled by debug mode
            return;
        }

        if (this.checkFlagsAND(node, FLAG_DATA | FLAG_SELECTABLE)) {
            //actually append node to render list
            index.push(node + this.tile);
        }
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
