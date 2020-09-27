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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.mode.api.IFarPiece;
import net.daporkchop.fp2.mode.api.IFarPos;
import net.daporkchop.lib.unsafe.PCleaner;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.util.AbstractReleasable;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

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
     *   - 0x00: hasPiece //the piece object itself is stored in a hash table in the render cache
     *   - 0x01: doesSelfOrAnyChildrenHavePiece
     *   - 0x02: rendered
     *   - 0x03: hasOpaque
     *   - 0x04: hasTransparent
     *   int pos[D]; //only stores X(Y)Z coordinates, level is computed dynamically based on recursion depth
     *   RenderData opaque;
     *   RenderData transparent;
     *   Node* children[1 << D];
     *
     * RenderData structure:
     *   void* vertsAddr; //address of vertex data in main memory
     *   long vertsOff; //offset of vertex data in gpu memory
     *   void* indicesAddr;
     *   long indicesOff;
     *   int indicesCount;
     * TODO: this can be made smaller by storing index and vertex data in a single allocation, meaning i only have to store a single pointer
     *
     * Total size: 4+2*(4*8+4)+D*4+2^D*8
     * - 2D: 116 bytes //about 1.8 cache lines
     * - 3D: 152 bytes //about 2.4 cache lines
     */

    protected static final long RENDERDATA_VERTSADDR = 0L;
    protected static final long RENDERDATA_VERTSOFF = RENDERDATA_VERTSADDR + 8L;
    protected static final long RENDERDATA_INDICESADDR = RENDERDATA_VERTSOFF + 8L;
    protected static final long RENDERDATA_INDICESOFF = RENDERDATA_INDICESADDR + 8L;
    protected static final long RENDERDATA_INDICESCOUNT = RENDERDATA_INDICESOFF + 8L;
    protected static final long RENDERDATA_SIZE = RENDERDATA_INDICESCOUNT + 4L;

    protected final int d;

    //the following 5 fields are the offsets of the actual node member variables from the node's pointer
    protected final long flags;
    protected final long pos;
    protected final long opaque;
    protected final long transparent;
    protected final long children;

    protected final long nodeSize;

    protected final long root;

    protected final PCleaner cleaner;

    public AbstractFarRenderTree(int d) {
        this.d = positive(d, "d");

        this.flags = 0L;
        this.pos = this.flags + 4L;
        this.opaque = this.pos + d * 4L;
        this.transparent = this.opaque + RENDERDATA_SIZE;
        this.children = this.transparent + RENDERDATA_SIZE;

        this.nodeSize = this.children + this.childCount() * 8L;
        LOGGER.info("{}D tree node size: {} bytes", d, this.nodeSize);

        this.root = PUnsafe.allocateMemory(this.nodeSize);

        this.cleaner = PCleaner.cleaner(this, new ReleaseFunction(this.root, this.children, this.childCount()));
    }

    /**
     * @return the number of children that a single node can have
     */
    protected int childCount() {
        return 1 << this.d;
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
    protected abstract int childIndex(int level, @NonNull POS pos);

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
            PUnsafe.freeMemory(node);
        }
    }
}
