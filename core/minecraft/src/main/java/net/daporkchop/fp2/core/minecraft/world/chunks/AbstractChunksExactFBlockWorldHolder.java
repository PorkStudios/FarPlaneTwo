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
 *
 */

package net.daporkchop.fp2.core.minecraft.world.chunks;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.core.minecraft.util.threading.asynccache.AsyncCacheNBT;
import net.daporkchop.fp2.core.minecraft.world.AbstractExactFBlockWorldHolder;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockWorldHolder;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.fp2.core.util.threading.lazy.LazyFutureTask;
import net.daporkchop.lib.math.vector.Vec2i;

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
 * Base implementation of an {@link ExactFBlockWorldHolder} which serves a Minecraft-style world made up of chunk columns.
 * <p>
 * Serves as a shared base for implementations of {@link FBlockWorld} which are based on it.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractChunksExactFBlockWorldHolder<CHUNK> extends AbstractExactFBlockWorldHolder {
    private final NDimensionalIntSegtreeSet chunksExistIndex;
    private final AsyncCacheNBT<Vec2i, ?, CHUNK, ?> chunkCache;

    private final int chunkShift;

    private final int minHeight;
    private final int maxHeight;

    public AbstractChunksExactFBlockWorldHolder(@NonNull IFarWorldServer world, int chunkShift) {
        super(world);

        this.chunkShift = positive(chunkShift, "chunkShift");

        this.minHeight = this.bounds().minY();
        this.maxHeight = this.bounds().maxY();

        this.chunksExistIndex = this.createChunksExistIndex(world);
        this.chunkCache = this.createChunkCache(world);

        //register self to listen for events
        this.world().fp2_IFarWorldServer_eventBus().registerWeak(this);
    }

    /**
     * Creates the {@link #chunksExistIndex}.
     * <p>
     * Note that this is called by {@link AbstractChunksExactFBlockWorldHolder}'s constructor. Accessing any internal state inside of this method will likely cause unexpected behavior.
     *
     * @param world the {@link IFarWorldServer} instanced passed to the constructor
     * @return the {@link #chunksExistIndex} to use
     */
    protected abstract NDimensionalIntSegtreeSet createChunksExistIndex(@NonNull IFarWorldServer world);

    /**
     * Creates the {@link #chunkCache}.
     * <p>
     * Note that this is called by {@link AbstractChunksExactFBlockWorldHolder}'s constructor. Accessing any internal state inside of this method will likely cause unexpected behavior.
     *
     * @param world the {@link IFarWorldServer} instanced passed to the constructor
     * @return the {@link #chunkCache} to use
     */
    protected abstract AsyncCacheNBT<Vec2i, ?, CHUNK, ?> createChunkCache(@NonNull IFarWorldServer world);

    public int chunkSize() {
        return 1 << this.chunkShift;
    }

    public int chunkMask() {
        return (1 << this.chunkShift) - 1;
    }

    @Override
    public void close() {
        this.world().fp2_IFarWorldServer_eventBus().unregister(this);
        this.chunksExistIndex().release();
    }

    @Override
    public FBlockWorld worldFor(@NonNull AllowGenerationRequirement requirement) {
        return this.regularWorld(requirement == AllowGenerationRequirement.ALLOWED);
    }

    protected AbstractChunksExactFBlockWorld<CHUNK> regularWorld(boolean generationAllowed) {
        return new AbstractChunksExactFBlockWorld<>(this, generationAllowed);
    }

    protected abstract AbstractPrefetchedChunksExactFBlockWorld<CHUNK> prefetchedWorld(boolean generationAllowed, @NonNull List<CHUNK> chunks);

    @FEventHandler
    protected void onColumnSaved(@NonNull ColumnSavedEvent event) {
        //cache the chunk data for later use
        this.chunkCache.notifyUpdate(event.pos(), uncheckedCast(event.data()));

        //the chunk at the given position was saved, therefore it must exist so we want it in the index
        this.chunksExistIndex.add(event.pos().x(), event.pos().y());
    }

    @FEventHandler
    protected void onCubeSaved(@NonNull CubeSavedEvent event) {
        throw new UnsupportedOperationException("vanilla world shouldn't have cubes!");
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int minChunkX = minX >> this.chunkShift();
        int minChunkZ = minZ >> this.chunkShift();
        int maxChunkX = (maxX >> this.chunkShift()) + 1; //rounded up because maximum positions are inclusive
        int maxChunkZ = (maxZ >> this.chunkShift()) + 1;

        return maxY >= this.minHeight() && minY < this.maxHeight() && this.chunksExistIndex.containsAny(minChunkX, minChunkZ, maxChunkX, maxChunkZ);
    }

    @Override
    public IntAxisAlignedBB guaranteedDataAvailableVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        //round X,Z up/down to next chunk boundary, Y is infinite
        return new IntAxisAlignedBB(
                minX & ~this.chunkMask(), Integer.MIN_VALUE, minZ & ~this.chunkMask(),
                asrCeil(maxX, this.chunkShift()) << this.chunkShift(), Integer.MAX_VALUE, asrCeil(maxZ, this.chunkShift()) << this.chunkShift());
    }

    public CHUNK getChunk(int chunkX, int chunkZ, boolean allowGeneration) throws GenerationNotAllowedException {
        return GenerationNotAllowedException.throwIfNull(this.chunkCache.get(Vec2i.of(chunkX, chunkZ), allowGeneration).join());
    }

    public List<CHUNK> multiGetChunks(@NonNull List<Vec2i> chunkPositions, boolean allowGeneration) throws GenerationNotAllowedException {
        //collect all futures into an array first in order to issue all tasks at once before blocking, thus ensuring maximum parallelism
        LazyFutureTask<CHUNK>[] chunkFutures = uncheckedCast(chunkPositions.stream().map(pos -> this.chunkCache.get(pos, allowGeneration)).toArray(LazyFutureTask[]::new));

        //wait for all the futures to complete
        List<CHUNK> chunks = LazyFutureTask.scatterGather(chunkFutures);
        chunks.forEach(GenerationNotAllowedException.uncheckedThrowIfNull());
        return chunks;
    }

    /**
     * Gets a {@link List} of the chunk positions which need to be prefetched in order to respond to a query with the given shape.
     *
     * @param shape the {@link FBlockWorld.QueryShape query's shape}
     * @return the positions of the chunks to prefetch
     */
    public List<Vec2i> getChunkPositionsToPrefetch(@NonNull FBlockWorld.QueryShape shape) {
        if (shape.count() == 0) { //the shape contains no points, we don't need to prefetch anything
            return Collections.emptyList();
        } else if (shape instanceof FBlockWorld.SinglePointQueryShape) {
            return this.getChunkPositionsToPrefetch((FBlockWorld.SinglePointQueryShape) shape);
        } else if (shape instanceof FBlockWorld.OriginSizeStrideQueryShape) {
            return this.getChunkPositionsToPrefetch((FBlockWorld.OriginSizeStrideQueryShape) shape);
        } else if (shape instanceof FBlockWorld.MultiPointsQueryShape) {
            return this.getChunkPositionsToPrefetch((FBlockWorld.MultiPointsQueryShape) shape);
        } else {
            return this.getChunkPositionsToPrefetchGeneric(shape);
        }
    }

    /**
     * Gets a {@link List} of the chunk positions which need to be prefetched in order to respond to a group of queries with the given shapes.
     *
     * @param shapes the {@link FBlockWorld.QueryShape queries' shapes}
     * @return the positions of the chunks to prefetch
     */
    public List<Vec2i> getChunkPositionsToPrefetch(@NonNull FBlockWorld.QueryShape... shapes) {
        switch (shapes.length) {
            case 0: //there are no shapes, we don't need to prefetch anything
                return Collections.emptyList();
            case 1: //there is only a single shape, prefetch it individually
                return this.getChunkPositionsToPrefetch(shapes[0]);
        }

        //collect all the positions into an NDimensionalIntSet
        try (NDimensionalIntSet set = Datastructures.INSTANCE.nDimensionalIntSet().dimensions(2).threadSafe(false).build()) {
            //add the positions for each shape to the set
            for (FBlockWorld.QueryShape shape : shapes) {
                this.getChunkPositionsToPrefetch(set, shape);
            }

            //now that the shapes have been reduced to a set of unique positions, convert it to a list of Vec2i
            List<Vec2i> positions = new ArrayList<>(toIntExact(set.count()));
            set.forEach2D((x, z) -> positions.add(Vec2i.of(x, z)));
            return positions;
        }
    }

    protected void getChunkPositionsToPrefetch(@NonNull NDimensionalIntSet set, @NonNull FBlockWorld.QueryShape shape) {
        if (shape.count() == 0) { //the shape contains no points, we don't need to prefetch anything
            //no-op
        } else if (shape instanceof FBlockWorld.SinglePointQueryShape) {
            this.getChunkPositionsToPrefetch(set, (FBlockWorld.SinglePointQueryShape) shape);
        } else if (shape instanceof FBlockWorld.OriginSizeStrideQueryShape) {
            this.getChunkPositionsToPrefetch(set, (FBlockWorld.OriginSizeStrideQueryShape) shape);
        } else if (shape instanceof FBlockWorld.MultiPointsQueryShape) {
            this.getChunkPositionsToPrefetch(set, (FBlockWorld.MultiPointsQueryShape) shape);
        } else {
            this.getChunkPositionsToPrefetchGeneric(set, shape);
        }
    }

    protected List<Vec2i> getChunkPositionsToPrefetch(@NonNull FBlockWorld.SinglePointQueryShape shape) {
        return this.isValidPosition(shape.x(), shape.y(), shape.z())
                ? Collections.singletonList(Vec2i.of(shape.x() >> this.chunkShift(), shape.z() >> this.chunkShift()))
                : Collections.emptyList();
    }

    protected void getChunkPositionsToPrefetch(@NonNull NDimensionalIntSet set, @NonNull FBlockWorld.SinglePointQueryShape shape) {
        if (this.isValidPosition(shape.x(), shape.y(), shape.z())) {
            set.add(shape.x() >> this.chunkShift(), shape.z() >> this.chunkShift());
        }
    }

    protected List<Vec2i> getChunkPositionsToPrefetch(@NonNull FBlockWorld.OriginSizeStrideQueryShape shape) {
        shape.validate();

        if (!this.isAnyPointValid(shape)) { //no points are valid, there's no reason to check anything
            return Collections.emptyList();
        } else if (shape.strideX() == 1 && shape.strideY() == 1 && shape.strideZ() == 1) { //shape is an ordinary AABB
            return this.getChunkPositionsToPrefetchRegularAABB(shape);
        } else {
            return this.getChunkPositionsToPrefetchSparseAABB(shape);
        }
    }

    protected void getChunkPositionsToPrefetch(@NonNull NDimensionalIntSet set, @NonNull FBlockWorld.OriginSizeStrideQueryShape shape) {
        shape.validate();

        if (!this.isAnyPointValid(shape)) { //no points are valid, there's no reason to check anything
            //no-op
        } else if (shape.strideX() == 1 && shape.strideY() == 1 && shape.strideZ() == 1) { //shape is an ordinary AABB
            this.getChunkPositionsToPrefetchRegularAABB(set, shape);
        } else {
            this.getChunkPositionsToPrefetchSparseAABB(set, shape);
        }
    }

    protected List<Vec2i> getChunkPositionsToPrefetchRegularAABB(@NonNull FBlockWorld.OriginSizeStrideQueryShape shape) {
        shape.validate();

        //we assume that at least one Y coordinate is valid, meaning that the entire chunk needs to be prefetched as long as the horizontal coordinates are valid

        //find min and max chunk coordinates (upper bound is inclusive)
        int minX = max(shape.originX(), this.bounds().minX()) >> this.chunkShift();
        int minZ = max(shape.originZ(), this.bounds().minZ()) >> this.chunkShift();
        int maxX = min(shape.originX() + shape.sizeX() - 1, this.bounds().maxX() - 1) >> this.chunkShift();
        int maxZ = min(shape.originZ() + shape.sizeZ() - 1, this.bounds().maxZ() - 1) >> this.chunkShift();

        //collect all positions to a list
        List<Vec2i> positions = new ArrayList<>(multiplyExact(maxX - minX + 1, maxZ - minZ + 1));
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                positions.add(Vec2i.of(chunkX, chunkZ));
            }
        }
        return positions;
    }

    protected void getChunkPositionsToPrefetchRegularAABB(@NonNull NDimensionalIntSet set, @NonNull FBlockWorld.OriginSizeStrideQueryShape shape) {
        shape.validate();

        //we assume that at least one Y coordinate is valid, meaning that the entire chunk needs to be prefetched as long as the horizontal coordinates are valid

        //find min and max chunk coordinates (upper bound is inclusive)
        int minX = max(shape.originX(), this.bounds().minX()) >> this.chunkShift();
        int minZ = max(shape.originZ(), this.bounds().minZ()) >> this.chunkShift();
        int maxX = min(shape.originX() + shape.sizeX() - 1, this.bounds().maxX() - 1) >> this.chunkShift();
        int maxZ = min(shape.originZ() + shape.sizeZ() - 1, this.bounds().maxZ() - 1) >> this.chunkShift();

        //add all positions to the set
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                set.add(chunkX, chunkZ);
            }
        }
    }

    protected List<Vec2i> getChunkPositionsToPrefetchSparseAABB(@NonNull FBlockWorld.OriginSizeStrideQueryShape shape) {
        shape.validate();

        //we assume that at least one Y coordinate is valid, meaning that the entire chunk needs to be prefetched as long as the horizontal coordinates are valid

        //find chunk X,Z coordinates
        Consumer<IntConsumer> chunkXSupplier = this.chunkCoordSupplier(shape.originX(), shape.sizeX(), shape.strideX(), this.bounds().minX(), this.bounds().maxX(), this.chunkShift(), this.chunkSize());
        Consumer<IntConsumer> chunkZSupplier = this.chunkCoordSupplier(shape.originZ(), shape.sizeZ(), shape.strideZ(), this.bounds().minZ(), this.bounds().maxZ(), this.chunkShift(), this.chunkSize());

        //collect all positions to a list
        List<Vec2i> positions = new ArrayList<>();
        chunkXSupplier.accept(chunkX -> chunkZSupplier.accept(chunkZ -> positions.add(Vec2i.of(chunkX, chunkZ))));
        return positions;
    }

    protected void getChunkPositionsToPrefetchSparseAABB(@NonNull NDimensionalIntSet set, @NonNull FBlockWorld.OriginSizeStrideQueryShape shape) {
        shape.validate();

        //we assume that at least one Y coordinate is valid, meaning that the entire chunk needs to be prefetched as long as the horizontal coordinates are valid

        //find chunk X,Z coordinates
        Consumer<IntConsumer> chunkXSupplier = this.chunkCoordSupplier(shape.originX(), shape.sizeX(), shape.strideX(), this.bounds().minX(), this.bounds().maxX(), this.chunkShift(), this.chunkSize());
        Consumer<IntConsumer> chunkZSupplier = this.chunkCoordSupplier(shape.originZ(), shape.sizeZ(), shape.strideZ(), this.bounds().minZ(), this.bounds().maxZ(), this.chunkShift(), this.chunkSize());

        //add all positions to the set
        chunkXSupplier.accept(chunkX -> chunkZSupplier.accept(chunkZ -> set.add(chunkX, chunkZ)));
    }

    protected List<Vec2i> getChunkPositionsToPrefetch(@NonNull FBlockWorld.MultiPointsQueryShape shape) {
        //delegate to generic method, it's already the fastest possible approach for this shape implementation
        return this.getChunkPositionsToPrefetchGeneric(shape);
    }

    protected void getChunkPositionsToPrefetch(@NonNull NDimensionalIntSet set, @NonNull FBlockWorld.MultiPointsQueryShape shape) {
        //delegate to generic method, it's already the fastest possible approach for this shape implementation
        this.getChunkPositionsToPrefetchGeneric(set, shape);
    }

    protected List<Vec2i> getChunkPositionsToPrefetchGeneric(@NonNull FBlockWorld.QueryShape shape) {
        shape.validate();

        try (NDimensionalIntSet set = Datastructures.INSTANCE.nDimensionalIntSet().dimensions(2).threadSafe(false).build()) {
            //iterate over every position, recording all the ones which are valid
            shape.forEach((index, x, y, z) -> {
                if (this.isValidPosition(x, y, z)) {
                    set.add(x >> this.chunkShift(), z >> this.chunkShift());
                }
            });

            //now that the shape has been reduced to a set of unique positions, convert it to a list of Vec2i
            List<Vec2i> positions = new ArrayList<>(toIntExact(set.count()));
            set.forEach2D((x, z) -> positions.add(Vec2i.of(x, z)));
            return positions;
        }
    }

    protected void getChunkPositionsToPrefetchGeneric(@NonNull NDimensionalIntSet set, @NonNull FBlockWorld.QueryShape shape) {
        shape.validate();

        //iterate over every position, recording all the ones which are valid
        shape.forEach((index, x, y, z) -> {
            if (this.isValidPosition(x, y, z)) {
                set.add(x >> this.chunkShift(), z >> this.chunkShift());
            }
        });
    }
}
