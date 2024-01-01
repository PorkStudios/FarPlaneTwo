/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.core.minecraft.util.threading.asynccache.AsyncCacheNBT;
import net.daporkchop.fp2.core.minecraft.world.AbstractExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.threading.lazy.LazyFutureTask;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.math.vector.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
public abstract class AbstractCubesExactFBlockLevelHolder<CUBE> extends AbstractExactFBlockLevelHolder {
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
     * Gets a {@link List} of the cube positions which need to be prefetched in order to respond to a
     * {@link FBlockLevel#multiGetDense(int, int, int, int, int, int, int[], int, int[], int, byte[], int) multiGet} query.
     *
     * @return the positions of the chunks to prefetch
     */
    public final List<Vec3i> getCubePositionsToPrefetchForMultiGetDense(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ) {
        BlockLevelConstants.validateDenseGridBounds(originX, originY, originZ, sizeX, sizeY, sizeZ);

        if (!this.isAnyPointValidDense(originX, originY, originZ, sizeX, sizeY, sizeZ)) {
            return Collections.emptyList();
        }

        //find min and max cube coordinates (upper bound is inclusive)
        int minX = max(originX, this.bounds().minX()) >> this.cubeShift();
        int minY = max(originY, this.bounds().minY()) >> this.cubeShift();
        int minZ = max(originZ, this.bounds().minZ()) >> this.cubeShift();
        int maxX = min(originX + sizeX - 1, this.bounds().maxX() - 1) >> this.cubeShift();
        int maxY = min(originY + sizeY - 1, this.bounds().maxY() - 1) >> this.cubeShift();
        int maxZ = min(originZ + sizeZ - 1, this.bounds().maxZ() - 1) >> this.cubeShift();

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

    /**
     * Gets a {@link List} of the cube positions which need to be prefetched in order to respond to a
     * {@link FBlockLevel#multiGetSparse(int, int, int, int, int, int, int, int[], int, int[], int, byte[], int) sparse multiGet} query.
     *
     * @return the positions of the cubes to prefetch
     */
    public final List<Vec3i> getCubePositionsToPrefetchForMultiGetSparse(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ,
            @NotNegative int zoom) {
        BlockLevelConstants.validateSparseGridBounds(originX, originY, originZ, sizeX, sizeY, sizeZ, zoom);

        if (!this.isAnyPointValidSparse(originX, originY, originZ, sizeX, sizeY, sizeZ, zoom)) {
            return Collections.emptyList();
        }

        //find cube X,Y,Z coordinates
        int[] cubeXs = this.getChunkCoords(originX, sizeX, zoom, this.bounds().minX(), this.bounds().maxX(), this.cubeShift(), this.cubeSize());
        int[] cubeYs = this.getChunkCoords(originY, sizeY, zoom, this.bounds().minY(), this.bounds().maxY(), this.cubeShift(), this.cubeSize());
        int[] cubeZs = this.getChunkCoords(originZ, sizeZ, zoom, this.bounds().minZ(), this.bounds().maxZ(), this.cubeShift(), this.cubeSize());

        //collect all positions to a list
        List<Vec3i> positions = new ArrayList<>(multiplyExact(multiplyExact(cubeXs.length, cubeYs.length), cubeZs.length));
        for (int cubeX : cubeXs) {
            for (int cubeY : cubeYs) {
                for (int cubeZ : cubeZs) {
                    positions.add(Vec3i.of(cubeX, cubeY, cubeZ));
                }
            }
        }
        return positions;
    }
}
