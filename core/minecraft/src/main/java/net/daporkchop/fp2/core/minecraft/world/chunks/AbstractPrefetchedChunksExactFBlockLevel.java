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

package net.daporkchop.fp2.core.minecraft.world.chunks;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.level.query.QuerySamplingMode;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.minecraft.world.AbstractPrefetchedExactFBlockLevel;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.open.LongObjOpenHashMap;

import java.util.List;

/**
 * Base implementation of an {@link FBlockLevel} which serves a Minecraft-style world made up of chunk columns.
 * <p>
 * This forms the internal API implementation used by {@link AbstractChunksExactFBlockLevelHolder}. It contains a group of chunks which have been prefetched and can be quickly accessed
 * without blocking.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractPrefetchedChunksExactFBlockLevel<CHUNK> extends AbstractPrefetchedExactFBlockLevel {
    private final AbstractChunksExactFBlockLevelHolder<CHUNK> holder;
    private final boolean generationAllowed;

    private final int chunkShift;

    private final LongObjMap<CHUNK> chunks;

    private final FGameRegistry registry;

    public AbstractPrefetchedChunksExactFBlockLevel(@NonNull AbstractChunksExactFBlockLevelHolder<CHUNK> holder, boolean generationAllowed, @NonNull List<CHUNK> chunks) {
        this.holder = holder;
        this.generationAllowed = generationAllowed;

        this.chunkShift = holder.chunkShift();

        this.registry = holder.registry();

        this.chunks = new LongObjOpenHashMap<>(chunks.size());
        chunks.forEach(chunk -> this.chunks.put(this.packedChunkPosition(chunk), chunk));
    }

    /**
     * Gets the X,Z coordinates of the given {@link CHUNK}, packed into a single {@code long} using {@link BinMath#packXY(int, int)}.
     *
     * @param chunk the {@link CHUNK}
     * @return the {@link CHUNK}'s packed coordinates
     */
    protected abstract long packedChunkPosition(@NonNull CHUNK chunk);

    @Override
    public void close() {
        //no-op, all resources are owned by AbstractChunksExactFBlockLevelHolder
    }

    @Override
    public IntAxisAlignedBB dataLimits() {
        return this.holder.bounds();
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.holder.containsAnyData(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public IntAxisAlignedBB guaranteedDataAvailableVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.holder.guaranteedDataAvailableVolume(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Gets the prefetched {@link CHUNK} which contains the given voxel position.
     *
     * @param x the voxel position's X coordinate
     * @param y the voxel position's Y coordinate
     * @param z the voxel position's Z coordinate
     * @return the prefetched {@link CHUNK}, or {@code null} if the chunk at the given position wasn't prefetched
     */
    protected CHUNK getPrefetchedChunk(int x, @SuppressWarnings("unused") int y, int z) {
        return this.chunks().get(BinMath.packXY(x >> this.chunkShift(), z >> this.chunkShift()));
    }

    /**
     * Gets the {@link CHUNK} which contains the given voxel position, and attempts to load it from the parent {@link AbstractChunksExactFBlockLevelHolder holder} if it isn't prefetched.
     *
     * @param x the voxel position's X coordinate
     * @param y the voxel position's Y coordinate
     * @param z the voxel position's Z coordinate
     * @return the {@link CHUNK}
     * @throws GenerationNotAllowedException if the chunk wasn't prefetched, generation is disallowed and the chunk is ungenerated
     */
    protected CHUNK getOrLoadChunk(int x, @SuppressWarnings("unused") int y, int z) throws GenerationNotAllowedException {
        CHUNK chunk = this.chunks().get(BinMath.packXY(x >> this.chunkShift(), z >> this.chunkShift()));

        if (chunk == null) {
            //this instance doesn't have the chunk prefetched, try to retrieve it from the holder...
            chunk = this.holder().getChunk(x >> this.chunkShift(), z >> this.chunkShift(), this.generationAllowed());

            //don't bother saving the loaded chunk into the cache:
            //- we don't want to modify this instance's state, in order to avoid causing future regular block accesses to be succeed when the would otherwise have failed
            //- this is very much an edge case which doesn't necessarily need to be fast
        }

        return chunk;
    }

    @Override
    public int getState(int x, int y, int z, @NotNegative int sampleResolution, QuerySamplingMode samplingMode) throws GenerationNotAllowedException {
        if (!this.holder.isValidPosition(x, y, z)) { //position is outside world, return 0
            return 0;
        }

        CHUNK chunk = this.getPrefetchedChunk(x, y, z);
        assert chunk != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        return this.getState(x, y, z, chunk);
    }

    protected abstract int getState(int x, int y, int z, CHUNK chunk) throws GenerationNotAllowedException;

    @Override
    public int getBiome(int x, int y, int z, @NotNegative int sampleResolution, QuerySamplingMode samplingMode) throws GenerationNotAllowedException {
        if (!this.holder.isValidPosition(x, y, z)) { //position is outside world, return 0
            return 0;
        }

        CHUNK chunk = this.getPrefetchedChunk(x, y, z);
        assert chunk != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        return this.getBiome(x, y, z, chunk);
    }

    protected abstract int getBiome(int x, int y, int z, CHUNK chunk) throws GenerationNotAllowedException;

    @Override
    public byte getLight(int x, int y, int z, @NotNegative int sampleResolution, QuerySamplingMode samplingMode) throws GenerationNotAllowedException {
        if (!this.holder.isValidPosition(x, y, z)) { //position is outside world, return default
            return y >= this.holder.bounds().maxY()
                    ? BlockLevelConstants.packLight(15, 0) //y coordinates are high, return full sky light
                    : BlockLevelConstants.packLight(0, 0);
        }

        CHUNK chunk = this.getPrefetchedChunk(x, y, z);
        assert chunk != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        return this.getLight(x, y, z, chunk);
    }

    protected abstract byte getLight(int x, int y, int z, CHUNK chunk) throws GenerationNotAllowedException;
}
