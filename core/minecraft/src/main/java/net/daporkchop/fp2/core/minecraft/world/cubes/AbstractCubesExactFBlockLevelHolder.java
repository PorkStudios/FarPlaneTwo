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

package net.daporkchop.fp2.core.minecraft.world.cubes;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import net.daporkchop.fp2.core.minecraft.util.threading.asynccache.AsyncCacheNBT;
import net.daporkchop.fp2.core.minecraft.world.AbstractExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.fp2.core.util.threading.lazy.LazyFutureTask;
import net.daporkchop.lib.math.vector.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.util.math.MathUtil.*;
import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Base implementation of an {@link ExactFBlockLevelHolder} which serves a Minecraft-style world made up of cubes.
 * <p>
 * Serves as a shared base for implementations of {@link FBlockLevel} which are based on it.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractCubesExactFBlockLevelHolder<CUBE> extends AbstractExactFBlockLevelHolder<AbstractPrefetchedCubesExactFBlockLevel<CUBE>> {
    private final NDimensionalIntSegtreeSet cubesExistIndex;
    private final AsyncCacheNBT<Vec3i, ?, CUBE, ?> cubeCache;

    private final int cubeShift;

    public AbstractCubesExactFBlockLevelHolder(@NonNull IFarLevelServer world, int cubeShift) {
        super(world);

        this.cubeShift = positive(cubeShift, "cubeShift");

        this.cubesExistIndex = this.createCubesExistIndex(world);
        this.cubeCache = this.createCubeCache(world);

        //register self to listen for events
        this.world().eventBus().registerWeak(this);
    }

    /**
     * Creates the {@link #cubesExistIndex}.
     * <p>
     * Note that this is called by {@link AbstractCubesExactFBlockLevelHolder}'s constructor. Accessing any internal state inside of this method will likely cause unexpected behavior.
     *
     * @param world the {@link IFarLevelServer} instanced passed to the constructor
     * @return the {@link #cubesExistIndex} to use
     */
    protected abstract NDimensionalIntSegtreeSet createCubesExistIndex(@NonNull IFarLevelServer world);

    /**
     * Creates the {@link #cubeCache}.
     * <p>
     * Note that this is called by {@link AbstractCubesExactFBlockLevelHolder}'s constructor. Accessing any internal state inside of this method will likely cause unexpected behavior.
     *
     * @param world the {@link IFarLevelServer} instanced passed to the constructor
     * @return the {@link #cubeCache} to use
     */
    protected abstract AsyncCacheNBT<Vec3i, ?, CUBE, ?> createCubeCache(@NonNull IFarLevelServer world);

    public int cubeSize() {
        return 1 << this.cubeShift;
    }

    public int cubeMask() {
        return (1 << this.cubeShift) - 1;
    }

    @Override
    public FBlockLevel worldFor(@NonNull AllowGenerationRequirement requirement) {
        return this.regularWorld(requirement == AllowGenerationRequirement.ALLOWED);
    }

    protected AbstractCubesExactFBlockLevel<CUBE> regularWorld(boolean generationAllowed) {
        return new AbstractCubesExactFBlockLevel<>(this, generationAllowed);
    }

    protected abstract AbstractPrefetchedCubesExactFBlockLevel<CUBE> prefetchedWorld(boolean generationAllowed, @NonNull List<CUBE> cubes);

    @Override
    public void close() {
        this.world().eventBus().unregister(this);
    }

    @FEventHandler
    protected void onColumnSaved(@NonNull ColumnSavedEvent event) {
        throw new UnsupportedOperationException("cubic world shouldn't have columns!");
    }

    @FEventHandler
    protected void onCubeSaved(@NonNull CubeSavedEvent event) {
        //cache the cube data for later use
        this.cubeCache.notifyUpdate(event.pos(), uncheckedCast(event.data()));

        //the cube at the given position was saved, therefore it must exist so we want it in the index
        this.cubesExistIndex.add(event.pos().x(), event.pos().y(), event.pos().z());
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int minCubeX = minX >> this.cubeShift();
        int minCubeY = minY >> this.cubeShift();
        int minCubeZ = minZ >> this.cubeShift();
        int maxCubeX = (maxX >> this.cubeShift()) + 1; //rounded up because maximum positions are inclusive
        int maxCubeY = (maxY >> this.cubeShift()) + 1;
        int maxCubeZ = (maxZ >> this.cubeShift()) + 1;

        return this.cubesExistIndex.containsAny(minCubeX, minCubeY, minCubeZ, maxCubeX, maxCubeY, maxCubeZ);
    }

    @Override
    public IntAxisAlignedBB guaranteedDataAvailableVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        //round X,Y,Z up/down to next cube boundary
        return new IntAxisAlignedBB(
                minX & ~this.cubeMask(), minY & ~this.cubeMask(), minZ & ~this.cubeMask(),
                asrCeil(maxX, this.cubeShift()) << this.cubeShift(), asrCeil(maxY, this.cubeShift()) << this.cubeShift(), asrCeil(maxZ, this.cubeShift()) << this.cubeShift());
    }

    public CUBE getCube(int cubeX, int cubeY, int cubeZ, boolean allowGeneration) throws GenerationNotAllowedException {
        return GenerationNotAllowedException.throwIfNull(this.cubeCache.get(Vec3i.of(cubeX, cubeY, cubeZ), allowGeneration).join());
    }

    public List<CUBE> multiGetCubes(@NonNull List<Vec3i> cubePositions, boolean allowGeneration) throws GenerationNotAllowedException {
        //collect all futures into an array first in order to issue all tasks at once before blocking, thus ensuring maximum parallelism
        LazyFutureTask<CUBE>[] cubeFutures = uncheckedCast(cubePositions.stream().map(pos -> this.cubeCache.get(pos, allowGeneration)).toArray(LazyFutureTask[]::new));

        //wait for all the futures to complete
        List<CUBE> cubes = LazyFutureTask.scatterGather(cubeFutures);
        cubes.forEach(GenerationNotAllowedException.uncheckedThrowIfNull());
        return cubes;
    }

    /**
     * Gets a {@link List} of the cube positions which need to be prefetched in order to respond to a query with the given shape.
     *
     * @param shape the {@link PointsQueryShape query's shape}
     * @return the positions of the cubes to prefetch
     */
    public List<Vec3i> getCubePositionsToPrefetch(@NonNull PointsQueryShape shape) {
        if (shape.count() == 0) { //the shape contains no points, we don't need to prefetch anything
            return Collections.emptyList();
        } else if (shape instanceof PointsQueryShape.SinglePointPointsQueryShape) {
            return this.getCubePositionsToPrefetch((PointsQueryShape.SinglePointPointsQueryShape) shape);
        } else if (shape instanceof PointsQueryShape.OriginSizeStridePointsQueryShape) {
            return this.getCubePositionsToPrefetch((PointsQueryShape.OriginSizeStridePointsQueryShape) shape);
        } else if (shape instanceof PointsQueryShape.MultiPointsPointsQueryShape) {
            return this.getCubePositionsToPrefetch((PointsQueryShape.MultiPointsPointsQueryShape) shape);
        } else {
            return this.getCubePositionsToPrefetchGeneric(shape);
        }
    }

    /**
     * Gets a {@link List} of the chunk positions which need to be prefetched in order to respond to a group of queries with the given shapes.
     *
     * @param shapes the {@link PointsQueryShape queries' shapes}
     * @return the positions of the chunks to prefetch
     */
    public List<Vec3i> getCubePositionsToPrefetch(@NonNull PointsQueryShape... shapes) {
        switch (shapes.length) {
            case 0: //there are no shapes, we don't need to prefetch anything
                return Collections.emptyList();
            case 1: //there is only a single shape, prefetch it individually
                return this.getCubePositionsToPrefetch(shapes[0]);
        }

        //collect all the positions into an NDimensionalIntSet
        {
            NDimensionalIntSet set = Datastructures.INSTANCE.nDimensionalIntSet().dimensions(3).threadSafe(false).build();

            //add the positions for each shape to the set
            for (PointsQueryShape shape : shapes) {
                this.getCubePositionsToPrefetch(set, shape);
            }

            //now that the shapes have been reduced to a set of unique positions, convert it to a list of Vec3i
            List<Vec3i> positions = new ArrayList<>(set.size());
            set.forEach3D((x, y, z) -> positions.add(Vec3i.of(x, y, z)));
            return positions;
        }
    }

    protected void getCubePositionsToPrefetch(@NonNull NDimensionalIntSet set, @NonNull PointsQueryShape shape) {
        if (shape.count() == 0) { //the shape contains no points, we don't need to prefetch anything
            //no-op
        } else if (shape instanceof PointsQueryShape.SinglePointPointsQueryShape) {
            this.getCubePositionsToPrefetch(set, (PointsQueryShape.SinglePointPointsQueryShape) shape);
        } else if (shape instanceof PointsQueryShape.OriginSizeStridePointsQueryShape) {
            this.getCubePositionsToPrefetch(set, (PointsQueryShape.OriginSizeStridePointsQueryShape) shape);
        } else if (shape instanceof PointsQueryShape.MultiPointsPointsQueryShape) {
            this.getCubePositionsToPrefetch(set, (PointsQueryShape.MultiPointsPointsQueryShape) shape);
        } else {
            this.getCubePositionsToPrefetchGeneric(set, shape);
        }
    }

    protected List<Vec3i> getCubePositionsToPrefetch(@NonNull PointsQueryShape.SinglePointPointsQueryShape shape) {
        return this.isValidPosition(shape.x(), shape.y(), shape.z())
                ? Collections.singletonList(Vec3i.of(shape.x() >> this.cubeShift(), shape.y() >> this.cubeShift(), shape.z() >> this.cubeShift()))
                : Collections.emptyList();
    }

    protected void getCubePositionsToPrefetch(@NonNull NDimensionalIntSet set, @NonNull PointsQueryShape.SinglePointPointsQueryShape shape) {
        if (this.isValidPosition(shape.x(), shape.y(), shape.z())) {
            set.add(shape.x() >> this.cubeShift(), shape.y() >> this.cubeShift(), shape.z() >> this.cubeShift());
        }
    }

    protected List<Vec3i> getCubePositionsToPrefetch(@NonNull PointsQueryShape.OriginSizeStridePointsQueryShape shape) {
        shape.validate();

        if (!this.isAnyPointValid(shape)) { //no points are valid, there's no reason to check anything
            return Collections.emptyList();
        } else if (shape.strideX() == 1 && shape.strideY() == 1 && shape.strideZ() == 1) { //shape is an ordinary AABB
            return this.getCubePositionsToPrefetchRegularAABB(shape);
        } else {
            return this.getCubePositionsToPrefetchSparseAABB(shape);
        }
    }

    protected void getCubePositionsToPrefetch(@NonNull NDimensionalIntSet set, @NonNull PointsQueryShape.OriginSizeStridePointsQueryShape shape) {
        shape.validate();

        if (!this.isAnyPointValid(shape)) { //no points are valid, there's no reason to check anything
            //no-op
        } else if (shape.strideX() == 1 && shape.strideY() == 1 && shape.strideZ() == 1) { //shape is an ordinary AABB
            this.getCubePositionsToPrefetchRegularAABB(set, shape);
        } else {
            this.getCubePositionsToPrefetchSparseAABB(set, shape);
        }
    }

    protected List<Vec3i> getCubePositionsToPrefetchRegularAABB(@NonNull PointsQueryShape.OriginSizeStridePointsQueryShape shape) {
        shape.validate();

        //find min and max cube coordinates (upper bound is inclusive)
        int minX = max(shape.originX(), this.bounds().minX()) >> this.cubeShift();
        int minY = max(shape.originY(), this.bounds().minY()) >> this.cubeShift();
        int minZ = max(shape.originZ(), this.bounds().minZ()) >> this.cubeShift();
        int maxX = min(shape.originX() + shape.sizeX() - 1, this.bounds().maxX() - 1) >> this.cubeShift();
        int maxY = min(shape.originY() + shape.sizeY() - 1, this.bounds().maxY() - 1) >> this.cubeShift();
        int maxZ = min(shape.originZ() + shape.sizeZ() - 1, this.bounds().maxZ() - 1) >> this.cubeShift();

        //collect all positions to a list
        List<Vec3i> positions = new ArrayList<>(multiplyExact(multiplyExact(maxX - minX + 1, maxY - minY + 1), maxZ - minZ + 1));
        for (int cubeX = minX; cubeX <= maxX; cubeX++) {
            for (int cubeY = minY; cubeY <= maxY; cubeY++) {
                for (int cubeZ = minZ; cubeZ <= maxZ; cubeZ++) {
                    positions.add(Vec3i.of(cubeX, cubeY, cubeZ));
                }
            }
        }
        return positions;
    }

    protected void getCubePositionsToPrefetchRegularAABB(@NonNull NDimensionalIntSet set, @NonNull PointsQueryShape.OriginSizeStridePointsQueryShape shape) {
        shape.validate();

        //find min and max cube coordinates (upper bound is inclusive)
        int minX = max(shape.originX(), this.bounds().minX()) >> this.cubeShift();
        int minY = max(shape.originY(), this.bounds().minY()) >> this.cubeShift();
        int minZ = max(shape.originZ(), this.bounds().minZ()) >> this.cubeShift();
        int maxX = min(shape.originX() + shape.sizeX() - 1, this.bounds().maxX() - 1) >> this.cubeShift();
        int maxY = min(shape.originY() + shape.sizeY() - 1, this.bounds().maxY() - 1) >> this.cubeShift();
        int maxZ = min(shape.originZ() + shape.sizeZ() - 1, this.bounds().maxZ() - 1) >> this.cubeShift();

        //add all positions to the set
        for (int cubeX = minX; cubeX <= maxX; cubeX++) {
            for (int cubeY = minY; cubeY <= maxY; cubeY++) {
                for (int cubeZ = minZ; cubeZ <= maxZ; cubeZ++) {
                    set.add(cubeX, cubeY, cubeZ);
                }
            }
        }
    }

    protected List<Vec3i> getCubePositionsToPrefetchSparseAABB(@NonNull PointsQueryShape.OriginSizeStridePointsQueryShape shape) {
        shape.validate();

        //find cube X,Y,Z coordinates
        Consumer<IntConsumer> cubeXSupplier = this.chunkCoordSupplier(shape.originX(), shape.sizeX(), shape.strideX(), this.bounds().minX(), this.bounds().maxX(), this.cubeShift(), this.cubeSize());
        Consumer<IntConsumer> cubeYSupplier = this.chunkCoordSupplier(shape.originY(), shape.sizeY(), shape.strideY(), this.bounds().minY(), this.bounds().maxY(), this.cubeShift(), this.cubeSize());
        Consumer<IntConsumer> cubeZSupplier = this.chunkCoordSupplier(shape.originZ(), shape.sizeZ(), shape.strideZ(), this.bounds().minZ(), this.bounds().maxZ(), this.cubeShift(), this.cubeSize());

        //collect all positions to a list
        List<Vec3i> positions = new ArrayList<>();
        cubeXSupplier.accept(cubeX -> cubeYSupplier.accept(cubeY -> cubeZSupplier.accept(cubeZ -> positions.add(Vec3i.of(cubeX, cubeY, cubeZ)))));
        return positions;
    }

    protected void getCubePositionsToPrefetchSparseAABB(@NonNull NDimensionalIntSet set, @NonNull PointsQueryShape.OriginSizeStridePointsQueryShape shape) {
        shape.validate();

        //find cube X,Y,Z coordinates
        Consumer<IntConsumer> cubeXSupplier = this.chunkCoordSupplier(shape.originX(), shape.sizeX(), shape.strideX(), this.bounds().minX(), this.bounds().maxX(), this.cubeShift(), this.cubeSize());
        Consumer<IntConsumer> cubeYSupplier = this.chunkCoordSupplier(shape.originY(), shape.sizeY(), shape.strideY(), this.bounds().minY(), this.bounds().maxY(), this.cubeShift(), this.cubeSize());
        Consumer<IntConsumer> cubeZSupplier = this.chunkCoordSupplier(shape.originZ(), shape.sizeZ(), shape.strideZ(), this.bounds().minZ(), this.bounds().maxZ(), this.cubeShift(), this.cubeSize());

        //add all positions to the set
        cubeXSupplier.accept(cubeX -> cubeYSupplier.accept(cubeY -> cubeZSupplier.accept(cubeZ -> set.add(cubeX, cubeY, cubeZ))));
    }

    protected List<Vec3i> getCubePositionsToPrefetch(@NonNull PointsQueryShape.MultiPointsPointsQueryShape shape) {
        //delegate to generic method, it's already the fastest possible approach for this shape implementation
        return this.getCubePositionsToPrefetchGeneric(shape);
    }

    protected void getCubePositionsToPrefetch(@NonNull NDimensionalIntSet set, @NonNull PointsQueryShape.MultiPointsPointsQueryShape shape) {
        //delegate to generic method, it's already the fastest possible approach for this shape implementation
        this.getCubePositionsToPrefetchGeneric(set, shape);
    }

    protected List<Vec3i> getCubePositionsToPrefetchGeneric(@NonNull PointsQueryShape shape) {
        shape.validate();

        {
            NDimensionalIntSet set = Datastructures.INSTANCE.nDimensionalIntSet().dimensions(3).threadSafe(false).build();

            //iterate over every position, recording all the ones which are valid
            shape.forEach((index, x, y, z) -> {
                if (this.isValidPosition(x, y, z)) {
                    set.add(x >> this.cubeShift(), y >> this.cubeShift(), z >> this.cubeShift());
                }
            });

            //now that the shape has been reduced to a set of unique positions, convert it to a list of Vec3i
            List<Vec3i> positions = new ArrayList<>(set.size());
            set.forEach3D((x, y, z) -> positions.add(Vec3i.of(x, y, z)));
            return positions;
        }
    }

    protected void getCubePositionsToPrefetchGeneric(@NonNull NDimensionalIntSet set, @NonNull PointsQueryShape shape) {
        shape.validate();

        //iterate over every position, recording all the ones which are valid
        shape.forEach((index, x, y, z) -> {
            if (this.isValidPosition(x, y, z)) {
                set.add(x >> this.cubeShift(), y >> this.cubeShift(), z >> this.cubeShift());
            }
        });
    }
}
