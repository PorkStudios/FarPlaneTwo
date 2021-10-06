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

package net.daporkchop.fp2.debug.util;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.util.annotation.DebugOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static java.lang.Math.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Container class for various structs containing statistics useful while debugging.
 *
 * @author DaPorkchop_
 */
@UtilityClass
@DebugOnly
public class DebugStats {
    /**
     * @author DaPorkchop_
     */
    @Builder
    @Data
    public static final class TileSnapshot {
        public static final TileSnapshot ZERO = builder().build();

        protected final long allocatedSpace;
        protected final long totalSpace;

        protected final long uncompressedSize;

        /**
         * Combines two {@link TileSnapshot} instances.
         *
         * @param other the other instance
         * @return an instance with the combined values
         */
        public TileSnapshot add(@NonNull TileSnapshot other) {
            return new TileSnapshot(
                    this.allocatedSpace + other.allocatedSpace,
                    this.totalSpace + other.totalSpace,
                    this.uncompressedSize + other.uncompressedSize);
        }

        /**
         * Subtracts the given {@link TileSnapshot} instance from this instance.
         *
         * @param other the other instance
         * @return an instance with the subtracted values
         */
        public TileSnapshot sub(@NonNull TileSnapshot other) {
            return new TileSnapshot(
                    this.allocatedSpace - other.allocatedSpace,
                    this.totalSpace - other.totalSpace,
                    this.uncompressedSize - other.uncompressedSize);
        }
    }
    
    /**
     * @author DaPorkchop_
     */
    @Builder
    @Data
    public static final class TileCache {
        public static final TileCache ZERO = builder().build();

        protected final long tileCount;
        protected final long tileCountWithData;

        protected final long allocatedSpace;
        protected final long totalSpace;
        
        protected final long uncompressedSize;

        /**
         * Combines two {@link TileCache} instances.
         *
         * @param other the other instance
         * @return an instance with the combined values
         */
        public TileCache add(@NonNull TileCache other) {
            return new TileCache(
                    this.tileCount + other.tileCount,
                    this.tileCountWithData + other.tileCountWithData,
                    this.allocatedSpace + other.allocatedSpace,
                    this.totalSpace + other.totalSpace,
                    this.uncompressedSize + other.uncompressedSize);
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Builder
    @Data
    @SideOnly(Side.CLIENT)
    public static final class Renderer {
        public static final Renderer ZERO = builder().build();

        protected final long bakedTiles;
        protected final long bakedTilesWithData;

        protected final long allocatedVRAM;
        protected final long totalVRAM;

        protected final long allocatedIndices;
        protected final long totalIndices;
        protected final long indexSize;

        protected final long allocatedVertices;
        protected final long totalVertices;
        protected final long vertexSize;

        /**
         * Combines two {@link Renderer} instances.
         *
         * @param other the other instance
         * @return an instance with the combined values
         */
        public Renderer add(@NonNull Renderer other) {
            checkArg(((this.indexSize == 0L) ^ (other.indexSize == 0L)) || this.indexSize == other.indexSize, "cannot add renderer debug stats with different index sizes (%d != %d)", this.indexSize, other.indexSize);
            checkArg(((this.vertexSize == 0L) ^ (other.vertexSize == 0L)) || this.vertexSize == other.vertexSize, "cannot add renderer debug stats with different index sizes (%d != %d)", this.indexSize, other.indexSize);

            long thisIndexMask = this.indexSize != 0L ? -1L : 0L;
            long otherIndexMask = other.indexSize != 0L ? -1L : 0L;
            long thisVertexMask = this.vertexSize != 0L ? -1L : 0L;
            long otherVertexMask = other.vertexSize != 0L ? -1L : 0L;

            return new Renderer(this.bakedTiles + other.bakedTiles,
                    this.bakedTilesWithData + other.bakedTilesWithData,
                    this.allocatedVRAM + other.allocatedVRAM,
                    this.totalVRAM + other.totalVRAM,
                    (this.allocatedIndices & thisIndexMask) + (other.allocatedIndices & otherIndexMask),
                    (this.totalIndices & thisIndexMask) + (other.totalIndices & otherIndexMask),
                    max(this.indexSize, other.indexSize),
                    (this.allocatedVertices & thisVertexMask) + (other.allocatedVertices & otherVertexMask),
                    (this.totalVertices & thisVertexMask) + (other.totalVertices & otherVertexMask),
                    max(this.vertexSize, other.vertexSize));
        }
    }

    /**
     * @author DaPorkchop_
     */
    @Builder
    @Data
    public static final class Allocator {
        public static final Allocator ZERO = builder().build();

        protected final long heapRegions;
        protected final long allocations;

        protected final long allocatedSpace;
        protected final long totalSpace;

        /**
         * Combines two {@link Allocator} instances.
         *
         * @param other the other instance
         * @return an instance with the combined values
         */
        public Allocator add(@NonNull Allocator other) {
            return new Allocator(
                    this.heapRegions + other.heapRegions,
                    this.allocations + other.allocations,
                    this.allocatedSpace + other.allocatedSpace,
                    this.totalSpace + other.totalSpace);
        }
    }
}
