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

package net.daporkchop.fp2.util.datastructure;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.util.math.Sphere;
import net.daporkchop.fp2.util.math.Volume;
import net.daporkchop.lib.common.util.PArrays;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Immutable, 3-dimensional implementation of an octree over a cloud of points with {@code int} coordinates.
 *
 * @author DaPorkchop_
 */
//TODO: this is a simple implementation, but certainly not particularly fast. let's optimize this to use flattened structures (probably off-heap)
public final class PointOctree3I {
    protected static final int TARGET_POINTS_PER_NODE = 256;

    protected final Node root;

    public PointOctree3I(@NonNull int[]... points) {
        for (int[] point : points) {
            checkArg(point.length == 3, "point has invalid number of dimensions: %d (expected 3)", point.length);
        }

        this.root = points.length > 0 ? new Node(points) : null;
    }

    /**
     * Finds the point in the octree which has the shortest distance to the given point.
     *
     * @param point the point
     * @return the closest point to the given point, or {@code null} if no points match
     */
    public int[] nearestNeighbor(@NonNull int[] point) {
        checkArg(point.length == 3, "point has invalid number of dimensions: %d (expected 3)", point.length);

        if (this.root == null) { //octree is empty! no points match
            return null;
        }

        NearestNeighborQuery query = new NearestNeighborQuery(point);
        this.root.nearestNeighbor(query);
        return query.bestNeighbor;
    }

    protected static class Node {
        protected final Node[] children;
        protected final int[][] points;

        protected final AxisAlignedBB bb;
        protected final int middleX;
        protected final int middleY;
        protected final int middleZ;

        public Node(@NonNull int[]... points) {
            checkArg(points.length > 0, "at least one point must be given!");

            //compute point cloud bounds and centers
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (int[] point : points) {
                minX = min(minX, point[0]);
                maxX = max(maxX, point[0]);
                minY = min(minY, point[1]);
                maxY = max(maxY, point[1]);
                minZ = min(minZ, point[2]);
                maxZ = max(maxZ, point[2]);
            }
            this.bb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
            this.middleX = (minX + maxX) >> 1;
            this.middleY = (minY + maxY) >> 1;
            this.middleZ = (minZ + maxZ) >> 1;

            //split points up across all 8 child nodes
            List<int[]>[] buffers = uncheckedCast(new List[8]);
            PArrays.fill(buffers, ArrayList::new);

            for (int[] point : points) {
                int bdx = (point[0] - this.middleX) >>> 31;
                int bdy = (point[1] - this.middleY) >>> 31;
                int bdz = (point[2] - this.middleZ) >>> 31;
                buffers[(bdx << 2) | (bdy << 1) | bdz].add(point);
            }

            if (points.length <= TARGET_POINTS_PER_NODE //there's a sufficiently small number of points for this node to be a leaf
                || Arrays.stream(buffers).filter(list -> !list.isEmpty()).count() == 1L) { //all points fit into a single sub-volume?!?
                this.children = null;
                this.points = points;
            } else {
                this.children = Stream.of(buffers).map(list -> list.isEmpty() ? null : new Node(list.toArray(new int[0][]))).toArray(Node[]::new);
                this.points = null;
            }
        }

        /**
         * @return whether or not this node is a leaf
         */
        protected boolean isLeaf() {
            return this.children == null;
        }

        protected void nearestNeighbor(@NonNull NearestNeighborQuery query) {
            if (!query.boundingVolume().intersects(this.bb)) { //query doesn't intersect this node, there's nothing to do!
                return;
            }

            if (this.isLeaf()) { //this is a leaf node: do a brute-force search against every point
                for (int[] point : this.points) {
                    query.update(point);
                }
                return;
            }

            //step 1: recurse down as far as possible into nodes which are as close as possible to the query point

            //find the child node with the minimum distance
            int bdx = (query.point[0] - this.middleX) >>> 31;
            int bdy = (query.point[1] - this.middleY) >>> 31;
            int bdz = (query.point[2] - this.middleZ) >>> 31;
            Node closestChild = this.children[(bdx << 2) | (bdy << 1) | bdz];

            //recurse into close child
            if (closestChild != null) {
                closestChild.nearestNeighbor(query);
            }

            //step 2: attempt query against all remaining children that still intersect the best query volume

            for (Node child : this.children) {
                if (child != null && child != closestChild && query.boundingVolume().intersects(child.bb)) { //don't query same child twice
                    child.nearestNeighbor(query);
                }
            }
        }
    }

    @RequiredArgsConstructor
    protected static class NearestNeighborQuery {
        @NonNull
        protected final int[] point;

        protected int[] bestNeighbor;
        protected int bestNeighborDistSq = Integer.MAX_VALUE;

        public void update(@NonNull int[] point) {
            int dx = this.point[0] - point[0];
            int dy = this.point[1] - point[1];
            int dz = this.point[2] - point[2];
            int distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq < this.bestNeighborDistSq) {
                this.bestNeighborDistSq = distanceSq;
                this.bestNeighbor = point;
            }
        }

        public Volume boundingVolume() {
            return new Sphere(this.point[0], this.point[1], this.point[2], sqrt(this.bestNeighborDistSq));
        }
    }
}
