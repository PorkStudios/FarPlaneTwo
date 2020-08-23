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

package net.daporkchop.fp2.strategy.base.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.alloc.Allocator;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.math.AxisAlignedBB;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractFarRenderTile<POS extends IFarPos, I extends AbstractFarRenderIndex<POS, T, I>, T extends AbstractFarRenderTile<POS, I, T>> extends AxisAlignedBB {
    protected static final long MINY_OFFSET = Constants.fieldOffset(AxisAlignedBB.class, "minY", "field_72338_b");
    protected static final long MAXY_OFFSET = Constants.fieldOffset(AxisAlignedBB.class, "maxY", "field_72337_e");

    protected final POS pos;
    protected final int level;

    protected final T[] children; //non-null for levels above 0
    protected final T[] neighbors;

    protected final AbstractFarRenderCache<POS, ?, T, I> cache;
    protected final T parent;

    protected long address = -1L;
    protected ByteBuffer renderData;

    public boolean doesSelfOrAnyChildrenHaveAddress = false;

    public AbstractFarRenderTile(@NonNull AbstractFarRenderCache<POS, ?, T, I> cache, T parent, @NonNull POS pos, @NonNull AxisAlignedBB bb, int childCount) {
        super(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);

        this.level = (this.pos = pos).level();
        this.cache = cache;
        this.parent = parent;

        this.children = pos.level() > 0 ? this.tileArray(childCount) : null;
        this.neighbors = this.tileArray(childCount);

        cache.tileAdded(uncheckedCast(this));
    }

    protected abstract T[] tileArray(int size);

    protected abstract int childIndex(@NonNull POS pos);

    public T findChild(@NonNull POS pos) {
        T next = this.findChildStep(pos);
        return next == this || next == null ? next : next.findChild(pos);
    }

    public T findOrCreateChild(@NonNull POS pos) {
        T next = this.findChildStep(pos);
        if (next == null) {
            next = this.cache.createTile(uncheckedCast(this), uncheckedCast(pos.upTo(this.level - 1)));
            this.children[this.childIndex(pos)] = next;
        }
        return next == this ? next : next.findOrCreateChild(pos);
    }

    public T findChildStep(@NonNull POS pos) {
        if (this.pos.equals(pos)) {
            return uncheckedCast(this);
        }
        checkState(this.level > pos.level());
        //checkState((pos.x() >> (this.level - pos.level())) == this.x && (pos.z() >> (this.level - pos.level())) == this.z);
        return this.children[this.childIndex(pos)];
    }

    public void forEach(@NonNull Consumer<T> callback) {
        callback.accept(uncheckedCast(this));
        if (this.children != null) {
            for (T child : this.children) {
                if (child != null) {
                    child.forEach(callback);
                }
            }
        }
    }

    public boolean hasAddress() {
        return this.address >= 0L;
    }

    public void assignAddress(int size, @NonNull Allocator allocator) {
        if (this.hasAddress())    {
            if (this.renderData.capacity() == size) {
                return; //already has correctly sized allocation, do nothing
            }

            //release all old allocations
            allocator.free(this.address);
            this.address = -1L;
            PUnsafe.pork_releaseBuffer(this.renderData);
            this.renderData = null;
        }

        //allocate new data
        this.address = allocator.alloc(size);
        this.renderData = Constants.createByteBuffer(size);
        this.markHasAddress(true);
    }

    public boolean dropAddress() {
        checkState(this.hasAddress(), "doesn't have an address?!?");
        this.address = -1L;
        PUnsafe.pork_releaseBuffer(this.renderData);
        this.renderData = null;
        return this.markHasAddress(false);
    }

    protected boolean markHasAddress(boolean hasAddress) {
        if (!hasAddress && this.children != null) {
            //check if any children have an address
            for (int i = 0, len = this.children.length; !hasAddress && i < len; i++) {
                if (this.children[i] != null) {
                    hasAddress = this.children[i].doesSelfOrAnyChildrenHaveAddress;
                }
            }
        }
        this.doesSelfOrAnyChildrenHaveAddress = hasAddress;
        if (!hasAddress) {
            this.cache.tileRemoved(uncheckedCast(this));

            //neither this tile nor any of its children has an address
            if (this.parent != null) {
                //this tile has a parent, remove it from the parent
                this.parent.children[this.parent.childIndex(this.pos)] = null;

                //notify the parent that this tile has been removed
                return this.parent.markHasAddress(this.parent.hasAddress());
            } else {
                //this tile is the root, return true to inform render cache to delete the root
                return true;
            }
        }
        return false;
    }

    public boolean select(@NonNull Volume[] ranges, @NonNull ICamera frustum, @NonNull I index) {
        if (!ranges[this.level].intersects(this)) {
            //the view range for this level doesn't intersect this tile's bounding box,
            // so we can be certain that neither this tile nor any of its children would be contained
            return false;
        } else if (!frustum.isBoundingBoxInFrustum(this)) {
            //the frustum doesn't contain this tile's bounding box, so we can be certain that neither
            // this tile nor any of its children would be visible
            return false;
        }

        if (this.children != null && ranges[this.level - 1].intersects(this)) {
            //this tile intersects with the view distance for tiles below it, consider selecting them as well
            //if all children are selectable, select all of them
            if (this.hasAddress()) {
                //this tile contains renderable data, so only add below pieces if all of them are present
                //this is necessary because there is no mechanism for rendering part of a tile
                int mark = index.mark();
                for (int i = 0, len = this.children.length; i < len; i++) {
                    if (this.children[i] == null || !this.children[i].select(ranges, frustum, index)) {
                        //if any one of the children cannot be added, abort and add this tile over the whole area instead
                        index.restore(mark);
                        return index.add(uncheckedCast(this));
                    }
                }
                return true;
            } else {
                //this tile has no data, add as many children as possible and return true if any of them could be added
                boolean flag = false;
                for (int i = 0, len = this.children.length; i < len; i++) {
                    if (this.children[i] != null) {
                        flag |= this.children[i].select(ranges, frustum, index);
                    }
                }
                return flag;
            }
        }

        //add self to render output
        return index.add(uncheckedCast(this));
    }
}
