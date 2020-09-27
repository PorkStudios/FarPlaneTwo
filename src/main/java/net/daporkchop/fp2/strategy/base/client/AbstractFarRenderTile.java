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

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.ElementArrayObject;
import net.daporkchop.fp2.client.gl.object.VertexBufferObject;
import net.daporkchop.fp2.strategy.common.IFarPiece;
import net.daporkchop.fp2.strategy.common.IFarPos;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.DirectBufferReuse;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.daporkchop.lib.unsafe.capability.Releasable;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.util.math.AxisAlignedBB;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.lwjgl.opengl.GL15.*;

/**
 * An entry in the client-side quad-/octree which stores the data for tiles to be rendered.
 *
 * @author DaPorkchop_
 */
//TODO: urgent: i need to separate the logic of having data from having an address, because the voxel renderer can have tiles with a piece but no data
@Getter
public abstract class AbstractFarRenderTile<POS extends IFarPos, P extends IFarPiece<POS>, T extends AbstractFarRenderTile<POS, P, T>> extends AxisAlignedBB {
    protected final POS pos; //position of the piece wrapped by this tile
    protected final int level; //detail level that this tile is at

    protected final T[] children; //non-null for levels above 0
    protected final T parent; //non-null for levels below max

    protected final AbstractFarRenderCache<POS, P, T> cache;

    protected P piece;
    protected boolean rendered = false; //whether or not this tile has been rendered
    protected FarRenderData renderOpaque;
    protected FarRenderData renderTransparent;

    public boolean doesSelfOrAnyChildrenHavePiece = false;

    public AbstractFarRenderTile(@NonNull AbstractFarRenderCache<POS, P, T> cache, T parent, @NonNull POS pos, @NonNull AxisAlignedBB bb, int childCount) {
        super(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);

        this.level = (this.pos = pos).level();
        this.cache = cache;
        this.parent = parent;

        this.children = pos.level() > 0 ? cache.tileArray().apply(childCount) : null;
    }

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

    public boolean hasPiece()  {
        return this.piece != null;
    }

    public void uploadVertices() {
        if (this.renderOpaque != null)  {
            this.renderOpaque.uploadVertices();
        }
        if (this.renderTransparent != null)  {
            this.renderTransparent.uploadVertices();
        }
    }
    
    public void uploadIndices() {
        if (this.renderOpaque != null)  {
            this.renderOpaque.uploadIndices();
        }
        if (this.renderTransparent != null)  {
            this.renderTransparent.uploadIndices();
        }
    }
    
    public void setOpaque(@NonNull ByteBuf vertices, @NonNull ByteBuf indices)  {
        //allocate render data
        if ((this.renderOpaque = this.preallocateRenderData(this.renderOpaque, vertices.readableBytes(), indices.readableBytes())) != null) {
            //copy data from buffers
            vertices.readBytes(DirectBufferReuse.wrapByte(this.renderOpaque.addressVertices, this.renderOpaque.sizeVertices));
            indices.readBytes(DirectBufferReuse.wrapByte(this.renderOpaque.addressIndices, this.renderOpaque.sizeIndices));
            
            //upload to gpu
            try (VertexBufferObject verticesBuffer = this.cache.vertices.bind()) {
                this.renderOpaque.uploadVertices();
            }
            try (ElementArrayObject indicesBuffer = this.cache.indices.bind()) {
                this.renderOpaque.uploadIndices();
            }
        }
        this.rendered = true;
    }

    public FarRenderData preallocateRenderData(FarRenderData data, int sizeVertices, int sizeIndices)  {
        if (sizeVertices == 0 || sizeIndices == 0)  { //there is nothing to render
            this.releaseRenderData(data);
            return null;
        } else if (data != null && data.sizeVertices == sizeVertices && data.sizeIndices == sizeIndices)    { //data already exists and is the same size as the new data
            return data;
        } else { //new data is a different size or old data doesn't exist, allocate new data
            this.releaseRenderData(data);

            return new FarRenderData(
                    this.cache.verticesAllocator.alloc(sizeVertices), PUnsafe.allocateMemory(sizeVertices), sizeVertices,
                    this.cache.indicesAllocator.alloc(sizeIndices), PUnsafe.allocateMemory(sizeIndices), sizeIndices);
        }
    }

    protected void releaseRenderData(FarRenderData data)    {
        if (data != null)   {
            data.release(this.cache.verticesAllocator, this.cache.indicesAllocator);
        }
    }

    public void releaseOpaque() {
        if (this.renderOpaque != null)  {
            this.renderOpaque.release(this.cache.verticesAllocator, this.cache.indicesAllocator);
            this.renderOpaque = null;
        }
    }

    public void releaseTransparent() {
        if (this.renderTransparent != null)  {
            this.renderTransparent.release(this.cache.verticesAllocator, this.cache.indicesAllocator);
            this.renderTransparent = null;
        }
    }

    public void setPiece(@NonNull P piece)  {
        this.piece = piece;
        this.markHasPiece(true);
    }

    public boolean dropPiece() {
        checkState(this.rendered, "hasn't been rendered?!?");
        this.rendered = false;
        this.piece = null;

        this.releaseOpaque();
        this.releaseTransparent();

        return this.markHasPiece(false);
    }

    protected boolean markHasPiece(boolean hasPiece) {
        if (!hasPiece && this.children != null) {
            //check if any children have an address
            for (int i = 0, len = this.children.length; !hasPiece && i < len; i++) {
                if (this.children[i] != null) {
                    hasPiece = this.children[i].doesSelfOrAnyChildrenHavePiece;
                }
            }
        }
        this.doesSelfOrAnyChildrenHavePiece = hasPiece;
        if (hasPiece) {
            if (this.parent != null)    {
                //inform parent that this tile has a piece now
                this.parent.markHasPiece(true);
            }
        } else {
            //neither this tile nor any of its children has an address
            if (this.parent != null) {
                //this tile has a parent, remove it from the parent
                this.parent.children[this.parent.childIndex(this.pos)] = null;

                //notify the parent that this tile has been removed
                return this.parent.markHasPiece(this.parent.hasPiece());
            } else {
                //this tile is the root, return true to inform render cache to delete the root
                return true;
            }
        }
        return false;
    }

    public boolean select(@NonNull Volume[] ranges, @NonNull ICamera frustum, @NonNull FarRenderIndex index) {
        if (this.parent != null //don't do range checking for top level, as it will cause a bunch of pieces to be loaded but never rendered
            && !ranges[this.level].intersects(this)) {
            //the view range for this level doesn't intersect this tile's bounding box,
            // so we can be certain that neither this tile nor any of its children would be contained
            return false;
        } else if (this.parent == null //don't do frustum culling on child nodes, as we currently cannot render only part of the parent
                   && !frustum.isBoundingBoxInFrustum(this)) {
            //the frustum doesn't contain this tile's bounding box, so we can be certain that neither
            // this tile nor any of its children would be visible
            return false;
        }

        CHILDREN:
        if (this.children != null && ranges[this.level - 1].intersects(this)) {
            //this tile intersects with the view distance for tiles below it, consider selecting them as well
            //if all children are selectable, select all of them
            if (this.rendered) {
                //this tile contains renderable data, so only add below pieces if all of them are present
                //this is necessary because there is no mechanism for rendering part of a tile
                int mark = index.mark();
                for (int i = 0, len = this.children.length; i < len; i++) {
                    if (this.children[i] == null || !this.children[i].select(ranges, frustum, index)) {
                        //if any one of the children cannot be added, abort and add this tile over the whole area instead
                        index.restore(mark);
                        break CHILDREN;
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
