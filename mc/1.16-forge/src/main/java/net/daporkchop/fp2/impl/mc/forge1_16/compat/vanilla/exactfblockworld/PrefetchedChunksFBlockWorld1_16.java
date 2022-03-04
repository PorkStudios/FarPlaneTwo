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

package net.daporkchop.fp2.impl.mc.forge1_16.compat.vanilla.exactfblockworld;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.world.BlockWorldConstants;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSet;
import net.daporkchop.lib.primitive.list.LongList;
import net.daporkchop.lib.primitive.list.array.LongArrayList;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.open.LongObjOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.ChunkPos;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.daporkchop.fp2.impl.mc.forge1_16.compat.vanilla.exactfblockworld.VanillaExactFBlockWorld1_16.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link FBlockWorld} backed by an immutable hash table of prefetched chunks.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public final class PrefetchedChunksFBlockWorld1_16 implements FBlockWorld {
    public static PrefetchedChunksFBlockWorld1_16 prefetchChunks(@NonNull VanillaExactFBlockWorldHolder1_16 holder, @NonNull FGameRegistry registry, boolean generationAllowed, @NonNull NDimensionalIntSet chunkPositions) throws GenerationNotAllowedException {
        checkArg(chunkPositions.dimensions() == 3, "chunkPositions must be 3D, given: %dD", chunkPositions.dimensions());

        //convert chunkPositions to list
        LongList chunkPositionsList = new LongArrayList(toInt(chunkPositions.count()));
        chunkPositions.forEach2D((x, z) -> chunkPositionsList.add(ChunkPos.asLong(x, z)));

        return prefetchChunks(holder, registry, generationAllowed, chunkPositionsList);
    }

    public static PrefetchedChunksFBlockWorld1_16 prefetchChunks(@NonNull VanillaExactFBlockWorldHolder1_16 holder, @NonNull FGameRegistry registry, boolean generationAllowed, @NonNull LongList chunkPositions) throws GenerationNotAllowedException {
        return new PrefetchedChunksFBlockWorld1_16(holder, registry, holder.multiGetChunks(IntStream.range(0, chunkPositions.size()).mapToObj(i -> {
            long chunkPos = chunkPositions.get(i);
            return new ChunkPos(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
        }), generationAllowed));

        /*return ((IMixinServerWorld1_16) holder).fp2_farWorldServer().fp2_IFarWorld_workerManager().workExecutor().supply(() -> {
            //get all the futures
            List<CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> chunkFutures = new ArrayList<>(chunkPositions.size());
            chunkPositions.forEach((LongConsumer) chunkPos -> {
                //getChunkFuture doesn't exist on the dedicated server for some reason, so in order to get a chunk future without blocking we have to
                //  access the private getChunkFutureMainThread method while on the server thread.
                ATServerChunkProvider1_16 serverChunkProvider = (ATServerChunkProvider1_16) holder.getChunkSource();
                chunkFutures.add(serverChunkProvider.invokeGetChunkFutureMainThread(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos), ChunkStatus.FULL, true));
            });

            //join the futures and store their results in the chunks map
            LongObjMap<IChunk> chunks = new LongObjOpenHashMap<>(chunkPositions.size());
            for (CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> chunkFuture : chunkFutures) {
                ((IMixinServerChunkProvider) holder.getChunkSource()).fp2_IMixinServerChunkProvider_mainThreadProcessor().managedBlock(chunkFuture::isDone);

                IChunk chunk = chunkFuture.join().map(Function.identity(), error -> {
                    throw new IllegalStateException("Chunk not there when requested: " + error);
                });
                chunks.put(chunk.getPos().toLong(), chunk);
            }

            //extract lighting info from world and store it in lighting maps
            LongObjMap<NibbleArray> skyLight = new LongObjOpenHashMap<>(sectionPositions.size());
            LongObjMap<NibbleArray> blockLight = new LongObjOpenHashMap<>(sectionPositions.size());
            boolean hasSkyLight = holder.dimensionType().hasSkyLight();
            sectionPositions.forEach((LongConsumer) sectionPos -> {
                NibbleArray skyLightArray;
                NibbleArray blockLightArray;

                if ((SectionPos.y(sectionPos) & ~0xF) != 0) { //section position is outside y coordinate limits
                    skyLightArray = hasSkyLight && SectionPos.y(sectionPos) > 0 ? NIBBLE_ARRAY_15 : NIBBLE_ARRAY_0;
                    blockLightArray = NIBBLE_ARRAY_0;
                } else {
                    if (hasSkyLight) {
                        //TODO: figure out if falling back to 0 light is a safe assumption to make
                        skyLightArray = PorkUtil.fallbackIfNull(holder.getLightEngine().getLayerListener(LightType.SKY).getDataLayerData(SectionPos.of(sectionPos)), NIBBLE_ARRAY_0);
                    } else {
                        skyLightArray = NIBBLE_ARRAY_0;
                    }

                    blockLightArray = PorkUtil.fallbackIfNull(holder.getLightEngine().getLayerListener(LightType.BLOCK).getDataLayerData(SectionPos.of(sectionPos)), NIBBLE_ARRAY_0);
                }

                skyLight.put(sectionPos, skyLightArray);
                blockLight.put(sectionPos, blockLightArray);
            });

            return new PrefetchedChunksFBlockWorld1_16(registry, chunks, skyLight, blockLight);
        });*/
    }

    private final VanillaExactFBlockWorldHolder1_16 holder;
    private final FGameRegistry registry;

    private final LongObjMap<OffThreadChunk1_16> chunks = new LongObjOpenHashMap<>();

    public PrefetchedChunksFBlockWorld1_16(@NonNull VanillaExactFBlockWorldHolder1_16 holder, @NonNull FGameRegistry registry, @NonNull Stream<OffThreadChunk1_16> chunks) {
        this.holder = holder;
        this.registry = registry;

        chunks.forEach(chunk -> checkState(this.chunks.putIfAbsent(chunk.getPositionAsLong(), chunk) == null, "duplicate chunk at (%d, %d)", chunk.x(), chunk.z()));
    }

    @Override
    public void close() {
        //no-op
    }

    @Override
    public boolean generationAllowed() {
        return false;
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.holder.containsAnyData(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public int getState(int x, int y, int z) throws GenerationNotAllowedException {
        OffThreadChunk1_16 chunk = this.chunks.get(ChunkPos.asLong(x >> CHUNK_SHIFT, z >> CHUNK_SHIFT));
        assert chunk != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        BlockState blockState = chunk.getBlockState(x, y, z);
        FluidState fluidState = blockState.getFluidState();

        //gross hack for the fact that waterlogging isn't really supported yet
        //TODO: solve this for real
        if (fluidState.getType() != Fluids.EMPTY) {
            blockState = fluidState.createLegacyBlock();
        }

        return this.registry.state2id(blockState);
    }

    @Override
    public int getBiome(int x, int y, int z) throws GenerationNotAllowedException {
        OffThreadChunk1_16 chunk = this.chunks.get(ChunkPos.asLong(x >> CHUNK_SHIFT, z >> CHUNK_SHIFT));
        assert chunk != null : "position outside prefetched area: " + x + ',' + y + ',' + z;
        return this.registry.biome2id(chunk.biomes().getNoiseBiome(x >> 2, y >> 2, z >> 2)); //TODO: this doesn't do biome zooming (as would normally be done in BiomeManager)
    }

    @Override
    public byte getLight(int x, int y, int z) throws GenerationNotAllowedException {
        OffThreadChunk1_16 chunk = this.chunks.get(ChunkPos.asLong(x >> CHUNK_SHIFT, z >> CHUNK_SHIFT));
        assert chunk != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        return BlockWorldConstants.packLight(chunk.getSkyLight(x, y, z), chunk.getBlockLight(x, y, z));
    }
}
