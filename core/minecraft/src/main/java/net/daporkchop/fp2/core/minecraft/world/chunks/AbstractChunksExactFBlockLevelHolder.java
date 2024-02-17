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

package net.daporkchop.fp2.core.minecraft.world.chunks;

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
 * Base implementation of an {@link ExactFBlockLevelHolder} which serves a Minecraft-style world made up of chunk columns.
 * <p>
 * Serves as a shared base for implementations of {@link FBlockLevel} which are based on it.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractChunksExactFBlockLevelHolder<CHUNK> extends AbstractExactFBlockLevelHolder {
    private final NDimensionalIntSegtreeSet chunksExistIndex;
    private final AsyncCacheNBT<Vec2i, ?, CHUNK, ?> chunkCache;

    private final int chunkShift;

    private final int minHeight;
    private final int maxHeight;

    public AbstractChunksExactFBlockLevelHolder(@NonNull IFarLevelServer level, int chunkShift) {
        super(level);

        this.chunkShift = positive(chunkShift, "chunkShift");

        this.minHeight = this.bounds().minY();
        this.maxHeight = this.bounds().maxY();

        this.chunksExistIndex = this.createChunksExistIndex(level);
        this.chunkCache = this.createChunkCache(level);

        //register self to listen for events
        this.world().eventBus().registerWeak(this);
    }

    /**
     * Creates the {@link #chunksExistIndex}.
     * <p>
     * Note that this is called by {@link AbstractChunksExactFBlockLevelHolder}'s constructor. Accessing any internal state inside of this method will likely cause unexpected behavior.
     *
     * @param world the {@link IFarLevelServer} instanced passed to the constructor
     * @return the {@link #chunksExistIndex} to use
     */
    protected abstract NDimensionalIntSegtreeSet createChunksExistIndex(@NonNull IFarLevelServer world);

    /**
     * Creates the {@link #chunkCache}.
     * <p>
     * Note that this is called by {@link AbstractChunksExactFBlockLevelHolder}'s constructor. Accessing any internal state inside of this method will likely cause unexpected behavior.
     *
     * @param world the {@link IFarLevelServer} instanced passed to the constructor
     * @return the {@link #chunkCache} to use
     */
    protected abstract AsyncCacheNBT<Vec2i, ?, CHUNK, ?> createChunkCache(@NonNull IFarLevelServer world);

    public int chunkSize() {
        return 1 << this.chunkShift;
    }

    public int chunkMask() {
        return (1 << this.chunkShift) - 1;
    }

    @Override
    public void close() {
        this.world().eventBus().unregister(this);
    }

    @Override
    public FBlockLevel worldFor(@NonNull AllowGenerationRequirement requirement) {
        return this.regularWorld(requirement == AllowGenerationRequirement.ALLOWED);
    }

    protected AbstractChunksExactFBlockLevel<CHUNK> regularWorld(boolean generationAllowed) {
        return new AbstractChunksExactFBlockLevel<>(this, generationAllowed);
    }

    protected abstract AbstractPrefetchedChunksExactFBlockLevel<CHUNK> prefetchedWorld(boolean generationAllowed, @NonNull List<CHUNK> chunks);

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
     * Gets a {@link List} of the chunk positions which need to be prefetched in order to respond to a
     * {@link FBlockLevel#multiGetDense(int, int, int, int, int, int, int[], int, int[], int, byte[], int) dense multiGet} query.
     *
     * @return the positions of the chunks to prefetch
     */
    public final List<Vec2i> getChunkPositionsToPrefetchForMultiGetDense(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ) {
        BlockLevelConstants.validateDenseGridBounds(originX, originY, originZ, sizeX, sizeY, sizeZ);

        if (!this.isAnyPointValidDense(originX, originY, originZ, sizeX, sizeY, sizeZ)) {
            return Collections.emptyList();
        }

        //we assume that at least one Y coordinate is valid, meaning that the entire chunk needs to be prefetched as long as the horizontal coordinates are valid

        //find min and max chunk coordinates (upper bound is inclusive)
        int minX = max(originX, this.bounds().minX()) >> this.chunkShift();
        int minZ = max(originZ, this.bounds().minZ()) >> this.chunkShift();
        int maxX = min(originX + sizeX - 1, this.bounds().maxX() - 1) >> this.chunkShift();
        int maxZ = min(originZ + sizeZ - 1, this.bounds().maxZ() - 1) >> this.chunkShift();

        //collect all positions to a list
        List<Vec2i> positions = new ArrayList<>(multiplyExact(maxX - minX + 1, maxZ - minZ + 1));
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                positions.add(Vec2i.of(chunkX, chunkZ));
            }
        }
        return positions;
    }

    /**
     * Gets a {@link List} of the chunk positions which need to be prefetched in order to respond to a
     * {@link FBlockLevel#multiGetSparse(int, int, int, int, int, int, int, int[], int, int[], int, byte[], int) sparse multiGet} query.
     *
     * @return the positions of the chunks to prefetch
     */
    public final List<Vec2i> getChunkPositionsToPrefetchForMultiGetSparse(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ,
            @NotNegative int zoom) {
        BlockLevelConstants.validateSparseGridBounds(originX, originY, originZ, sizeX, sizeY, sizeZ, zoom);

        if (!this.isAnyPointValidSparse(originX, originY, originZ, sizeX, sizeY, sizeZ, zoom)) {
            return Collections.emptyList();
        }

        //we assume that at least one Y coordinate is valid, meaning that the entire chunk needs to be prefetched as long as the horizontal coordinates are valid

        //find chunk X,Z coordinates
        int[] chunkXs = this.getChunkCoords(originX, sizeX, zoom, this.bounds().minX(), this.bounds().maxX(), this.chunkShift(), this.chunkSize());
        int[] chunkZs = this.getChunkCoords(originZ, sizeZ, zoom, this.bounds().minZ(), this.bounds().maxZ(), this.chunkShift(), this.chunkSize());

        //collect all positions to a list
        List<Vec2i> positions = new ArrayList<>(multiplyExact(chunkXs.length, chunkZs.length));
        for (int chunkX : chunkXs) {
            for (int chunkZ : chunkZs) {
                positions.add(Vec2i.of(chunkX, chunkZ));
            }
        }
        return positions;
    }
}
