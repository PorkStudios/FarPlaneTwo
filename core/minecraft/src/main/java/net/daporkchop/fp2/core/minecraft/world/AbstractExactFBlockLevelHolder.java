/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.minecraft.world;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractExactFBlockLevelHolder implements ExactFBlockLevelHolder {
    private final IFarLevelServer world;
    private final FGameRegistry registry;
    private final IntAxisAlignedBB bounds;

    public AbstractExactFBlockLevelHolder(@NonNull IFarLevelServer world) {
        this.world = world;
        this.registry = world.registry();
        this.bounds = world.coordLimits();
    }

    //
    // Generic coordinate utilities
    //

    /**
     * Checks whether or not the given Y coordinate is within the world's vertical limits.
     *
     * @param y the Y coordinate to check
     * @return whether or not the Y coordinate is valid
     */
    public boolean isValidY(int y) {
        return y >= this.bounds.minY() && y < this.bounds.maxY();
    }

    /**
     * Checks whether or not the given X,Z coordinates are within the world's horizontal limits.
     *
     * @param x the X coordinate of the position to check
     * @param z the Z coordinate of the position to check
     * @return whether or not the X,Z coordinates are valid
     */
    public boolean isValidXZ(int x, int z) {
        return x >= this.bounds.minX() && x < this.bounds.maxX() && z >= this.bounds.minZ() && z < this.bounds.maxZ();
    }

    /**
     * Checks whether or not the given point is within the world's limits.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @param z the point's Z coordinate
     * @return whether or not the given point is valid
     */
    public boolean isValidPosition(int x, int y, int z) {
        return this.bounds.contains(x, y, z);
    }

    //
    // Shared implementations of FBlockLevel's data availability accessors
    //

    /**
     * @see FBlockLevel#containsAnyData
     */
    public abstract boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    /**
     * @see FBlockLevel#guaranteedDataAvailableVolume
     */
    public abstract IntAxisAlignedBB guaranteedDataAvailableVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    //
    // Generic coordinate utilities, used when computing prefetching regions
    //

    protected boolean isAnyPointValid(@NonNull PointsQueryShape.OriginSizeStridePointsQueryShape shape) {
        return this.isAnyPointValid(shape.originX(), shape.sizeX(), shape.strideX(), this.bounds.minX(), this.bounds.maxX())
               && this.isAnyPointValid(shape.originY(), shape.sizeY(), shape.strideY(), this.bounds.minY(), this.bounds.maxY())
               && this.isAnyPointValid(shape.originZ(), shape.sizeZ(), shape.strideZ(), this.bounds.minZ(), this.bounds.maxZ());
    }

    protected boolean isAnyPointValid(int origin, int size, int stride, int min, int max) {
        //this could probably be implemented way faster, but i really don't care because this will never be called with a size larger than like 20

        for (int i = 0, pos = origin; i < size; i++, pos += stride) {
            if (pos >= min && pos < max) { //the point is valid
                return true;
            }
        }

        return false; //no points were valid
    }

    protected Consumer<IntConsumer> chunkCoordSupplier(int origin, int size, int stride, int min, int max, int chunkShift, int chunkSize) {
        if (stride >= chunkSize) {
            return callback -> {
                for (int i = 0, block = origin; i < size && block < max; i++, block += stride) {
                    if (block >= min) {
                        callback.accept(block >> chunkShift);
                    }
                }
            };
        } else {
            return callback -> {
                for (int chunk = max(origin, min) >> chunkShift, limit = min(origin + (size - 1) * stride, max) >> chunkShift; chunk <= limit; chunk++) {
                    callback.accept(chunk);
                }
            };
        }
    }
}
