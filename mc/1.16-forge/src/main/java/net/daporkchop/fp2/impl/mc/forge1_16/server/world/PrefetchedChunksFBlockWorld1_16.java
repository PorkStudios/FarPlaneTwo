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

package net.daporkchop.fp2.impl.mc.forge1_16.server.world;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.open.LongObjOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.IChunk;

import static net.daporkchop.fp2.impl.mc.forge1_16.server.world.ExactFBlockWorld1_16.*;

/**
 * Implementation of {@link FBlockWorld} backed by an immutable hash table of prefetched chunks.
 *
 * @author DaPorkchop_
 */
@Getter
public final class PrefetchedChunksFBlockWorld1_16 implements FBlockWorld {
    private final FGameRegistry registry;
    private final LongObjMap<IChunk> chunks;

    public PrefetchedChunksFBlockWorld1_16(@NonNull FGameRegistry registry, @NonNull IChunk... chunks) {
        this.registry = registry;

        this.chunks = new LongObjOpenHashMap<>(chunks.length);
        for (IChunk chunk : chunks) {
            this.chunks.put(chunk.getPos().toLong(), chunk);
        }
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return true; //TODO: this is totally wrong
    }

    @Override
    public int getState(int x, int y, int z) {
        IChunk chunk = this.chunks.get(ChunkPos.asLong(x >> CHUNK_SHIFT, z >> CHUNK_SHIFT));
        assert chunk != null : "position outside prefetched area: " + x + ',' + y + ',' + z;
        return this.registry.state2id(chunk.getBlockState(new BlockPos(x, y, z)));
    }

    @Override
    public int getBiome(int x, int y, int z) {
        IChunk chunk = this.chunks.get(ChunkPos.asLong(x >> CHUNK_SHIFT, z >> CHUNK_SHIFT));
        assert chunk != null : "position outside prefetched area: " + x + ',' + y + ',' + z;
        return this.registry.biome2id(chunk.getBiomes().getNoiseBiome(x >> 2, y >> 2, z >> 2)); //TODO: this doesn't do biome zooming (as would normally be done in BiomeManager)
    }

    @Override
    public byte getLight(int x, int y, int z) {
        //TODO: this doesn't actually read light data from the world...
        return BlockWorldConstants.packLight(15, 0);
    }
}
