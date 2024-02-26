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
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.annotation.param.Positive;
import net.daporkchop.lib.math.vector.Vec2i;

import java.util.List;

/**
 * Base implementation of an {@link FBlockLevel} which serves a Minecraft-style world made up of chunk columns.
 * <p>
 * This forms the user-facing API implementation used by {@link AbstractChunksExactFBlockLevelHolder}. It does not do anything by itself, but rather prefetches the chunks which need to be
 * accessed before delegating to {@link AbstractPrefetchedChunksExactFBlockLevel}.
 * <p>
 * You have been epicly trolled by this class' name: because there's no actual implementation-specific logic going on here, I didn't actually make it abstract! Ha! I bet you feel
 * really stupid right now.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class AbstractChunksExactFBlockLevel<CHUNK> implements FBlockLevel {
    @NonNull
    private final AbstractChunksExactFBlockLevelHolder<CHUNK> holder;
    private final boolean generationAllowed;

    @Override
    public void close() {
        //no-op, all resources are owned by AbstractChunksExactFBlockLevelHolder
    }

    @Override
    public FGameRegistry registry() {
        return this.holder.registry();
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

    @Override
    public int getState(int x, int y, int z) throws GenerationNotAllowedException {
        //delegate to multiGet because it'll delegate to AbstractPrefetchedChunksExactFBlockLevel, which can access neighboring chunks if Block#getActualState accesses a
        //  state which goes over a cube/column border. this is slow, but i don't care because the single getter methods are dumb and bad anyway.
        int[] buf = new int[1];
        this.multiGetDense(x, y, z, 1, 1, 1, buf, 0, null, 0, null, 0);
        return buf[0];
    }

    @Override
    public int getBiome(int x, int y, int z) throws GenerationNotAllowedException {
        //delegate to multiGet because it'll delegate to AbstractPrefetchedChunksExactFBlockLevel, which can access neighboring chunks if Block#getActualState accesses a
        //  state which goes over a cube/column border. this is slow, but i don't care because the single getter methods are dumb and bad anyway.
        int[] buf = new int[1];
        this.multiGetDense(x, y, z, 1, 1, 1, null, 0, buf, 0, null, 0);
        return buf[0];
    }

    @Override
    public byte getLight(int x, int y, int z) throws GenerationNotAllowedException {
        //delegate to multiGet because it'll delegate to AbstractPrefetchedChunksExactFBlockLevel, which can access neighboring chunks if Block#getActualState accesses a
        //  state which goes over a cube/column border. this is slow, but i don't care because the single getter methods are dumb and bad anyway.
        byte[] buf = new byte[1];
        this.multiGetDense(x, y, z, 1, 1, 1, null, 0, null, 0, buf, 0);
        return buf[0];
    }

    @Override
    public void multiGetDense(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ,
            int[] states, @NotNegative int statesOff,
            int[] biomes, @NotNegative int biomesOff,
            byte[] lights, @NotNegative int lightsOff) throws GenerationNotAllowedException {
        //figure out which chunks need to be prefetched
        List<Vec2i> prefetchPositions = this.holder.getChunkPositionsToPrefetchForMultiGetDense(originX, originY, originZ, sizeX, sizeY, sizeZ);

        //prefetch all the chunks, then delegate the actual query execution to AbstractPrefetchedChunksExactFBlockLevel
        this.holder.prefetchedWorld(this.generationAllowed, this.holder.multiGetChunks(prefetchPositions, this.generationAllowed))
                .multiGetDense(originX, originY, originZ, sizeX, sizeY, sizeZ, states, statesOff, biomes, biomesOff, lights, lightsOff);
    }

    @Override
    public void multiGetSparse(
            int originX, int originY, int originZ,
            @Positive int sizeX, @Positive int sizeY, @Positive int sizeZ,
            @NotNegative int zoom,
            int[] states, @NotNegative int statesOff,
            int[] biomes, @NotNegative int biomesOff,
            byte[] lights, @NotNegative int lightsOff) throws GenerationNotAllowedException {
        //figure out which chunks need to be prefetched
        List<Vec2i> prefetchPositions = this.holder.getChunkPositionsToPrefetchForMultiGetSparse(originX, originY, originZ, sizeX, sizeY, sizeZ, zoom);

        //prefetch all the chunks, then delegate the actual query execution to AbstractPrefetchedChunksExactFBlockLevel
        this.holder.prefetchedWorld(this.generationAllowed, this.holder.multiGetChunks(prefetchPositions, this.generationAllowed))
                .multiGetSparse(originX, originY, originZ, sizeX, sizeY, sizeZ, zoom, states, statesOff, biomes, biomesOff, lights, lightsOff);
    }
}