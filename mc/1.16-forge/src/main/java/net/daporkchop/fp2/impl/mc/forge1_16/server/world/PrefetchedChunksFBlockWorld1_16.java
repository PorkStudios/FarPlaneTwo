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

import com.mojang.datafixers.util.Either;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.world.server.ATServerChunkProvider1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.server.IMixinServerChunkProvider;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.server.IMixinServerWorld1_16;
import net.daporkchop.lib.common.util.PArrays;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.open.LongObjOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.daporkchop.fp2.impl.mc.forge1_16.server.world.ExactFBlockWorld1_16.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Implementation of {@link FBlockWorld} backed by an immutable hash table of prefetched chunks.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public final class PrefetchedChunksFBlockWorld1_16 implements FBlockWorld {
    private static final NibbleArray NIBBLE_ARRAY_0 = new NibbleArray();
    private static final NibbleArray NIBBLE_ARRAY_15 = new NibbleArray(PArrays.filled(2048, (byte) 0xFF));

    public static CompletableFuture<PrefetchedChunksFBlockWorld1_16> prefetchSections(@NonNull ServerWorld world, @NonNull FGameRegistry registry, @NonNull List<SectionPos> sectionPositions) {
        //find set of unique chunk positions
        Set<ChunkPos> chunkPositions = sectionPositions.stream().map(SectionPos::chunk).collect(Collectors.toSet());

        return ((IMixinServerWorld1_16) world).fp2_farWorldServer().fp2_IFarWorld_workerManager().workExecutor().supply(() -> {
            //get all the futures
            CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>[] chunkFutures = uncheckedCast(chunkPositions.stream()
                    .map(pos -> {
                        //getChunkFuture doesn't exist on the dedicated server for some reason, so in order to get a chunk future without blocking we have to
                        //  access the private getChunkFutureMainThread method while on the server thread.
                        ATServerChunkProvider1_16 serverChunkProvider = (ATServerChunkProvider1_16) world.getChunkSource();
                        return serverChunkProvider.invokeGetChunkFutureMainThread(pos.x, pos.z, ChunkStatus.FULL, true);
                    })
                    .toArray(CompletableFuture[]::new));

            //wait for all the futures to be completed
            ((IMixinServerChunkProvider) world.getChunkSource()).fp2_IMixinServerChunkProvider_mainThreadProcessor()
                    .managedBlock(CompletableFuture.allOf(chunkFutures)::isDone);

            //join the futures and store their results in the chunks map
            LongObjMap<IChunk> chunks = new LongObjOpenHashMap<>(chunkPositions.size());
            for (CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> chunkFuture : chunkFutures) {
                IChunk chunk = chunkFuture.join().map(Function.identity(), error -> {
                    throw new IllegalStateException("Chunk not there when requested: " + error);
                });
                chunks.put(chunk.getPos().toLong(), chunk);
            }

            //extract lighting info from world and store it in lighting maps
            LongObjMap<NibbleArray> skyLight = new LongObjOpenHashMap<>(sectionPositions.size());
            LongObjMap<NibbleArray> blockLight = new LongObjOpenHashMap<>(sectionPositions.size());
            boolean hasSkyLight = world.dimensionType().hasSkyLight();
            sectionPositions.forEach(sectionPos -> {
                NibbleArray skyLightArray;
                NibbleArray blockLightArray;

                if ((sectionPos.y() & ~0xF) != 0) { //section position is outside y coordinate limits
                    skyLightArray = hasSkyLight && sectionPos.y() > 0 ? NIBBLE_ARRAY_15 : NIBBLE_ARRAY_0;
                    blockLightArray = NIBBLE_ARRAY_0;
                } else {
                    if (hasSkyLight) {
                        //TODO: figure out if falling back to 0 light is a safe assumption to make
                        skyLightArray = PorkUtil.fallbackIfNull(world.getLightEngine().getLayerListener(LightType.SKY).getDataLayerData(sectionPos), NIBBLE_ARRAY_0);
                    } else {
                        skyLightArray = NIBBLE_ARRAY_0;
                    }

                    blockLightArray = PorkUtil.fallbackIfNull(world.getLightEngine().getLayerListener(LightType.BLOCK).getDataLayerData(sectionPos), NIBBLE_ARRAY_0);
                }

                skyLight.put(sectionPos.asLong(), skyLightArray);
                blockLight.put(sectionPos.asLong(), blockLightArray);
            });

            return new PrefetchedChunksFBlockWorld1_16(registry, chunks, skyLight, blockLight);
        });
    }

    @NonNull
    private final FGameRegistry registry;
    @NonNull
    private final LongObjMap<IChunk> chunks;
    @NonNull
    private final LongObjMap<NibbleArray> skyLight;
    @NonNull
    private final LongObjMap<NibbleArray> blockLight;

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
        NibbleArray skyLight = this.skyLight.get(SectionPos.asLong(x >> CHUNK_SHIFT, y >> CHUNK_SHIFT, z >> CHUNK_SHIFT));
        NibbleArray blockLight = this.blockLight.get(SectionPos.asLong(x >> CHUNK_SHIFT, y >> CHUNK_SHIFT, z >> CHUNK_SHIFT));
        assert skyLight != null && blockLight != null : "position outside prefetched area: " + x + ',' + y + ',' + z;
        return BlockWorldConstants.packLight(skyLight.get(x & CHUNK_MASK, y & CHUNK_MASK, z & CHUNK_MASK), blockLight.get(x & CHUNK_MASK, y & CHUNK_MASK, z & CHUNK_MASK));
    }
}
