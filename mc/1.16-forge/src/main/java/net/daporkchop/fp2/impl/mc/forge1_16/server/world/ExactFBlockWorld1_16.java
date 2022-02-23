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
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.server.IMixinServerWorld1_16;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public class ExactFBlockWorld1_16 implements FBlockWorld {
    public static final int CHUNK_SHIFT = 4;
    public static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;
    public static final int CHUNK_MASK = CHUNK_SIZE - 1;

    @NonNull
    protected final ServerWorld world;
    @NonNull
    protected final FGameRegistry registry;

    @Override
    public void close() {
        //no-op
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return true; //TODO: this means exact generation will always be used!!!
    }

    @Override
    public int getState(int x, int y, int z) {
        //TODO: this will internally block on a CompletableFuture not managed by the world's worker manager, which could cause a deadlock!
        return this.registry.state2id(this.world.getBlockState(new BlockPos(x, y, z)));
    }

    @Override
    public int getBiome(int x, int y, int z) {
        //TODO: i'm fairly certain this will not use the biome data from disk if the chunk isn't loaded
        return this.registry.biome2id(this.world.getBiome(new BlockPos(x, y, z)));
    }

    @Override
    public byte getLight(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return BlockWorldConstants.packLight(this.world.getBrightness(LightType.SKY, pos), this.world.getBrightness(LightType.BLOCK, pos));
    }

    @Override
    public void getData(int[] states, int statesOff, int statesStride,
                        int[] biomes, int biomesOff, int biomesStride,
                        byte[] light, int lightOff, int lightStride,
                        int x, int y, int z, int sizeX, int sizeY, int sizeZ, int strideX, int strideY, int strideZ) {
        int count = positive(sizeX, "sizeX") * positive(sizeY, "sizeY") * positive(sizeZ, "sizeZ");
        if (states != null) {
            checkRangeLen(states.length, statesOff, positive(statesStride, "statesStride") * count);
        }
        if (biomes != null) {
            checkRangeLen(biomes.length, biomesOff, positive(biomesStride, "biomesStride") * count);
        }
        if (light != null) {
            checkRangeLen(light.length, lightOff, positive(lightStride, "lightStride") * count);
        }
        positive(strideX, "strideX");
        positive(strideY, "strideY");
        positive(strideZ, "strideZ");

        //prefetch all affected chunks
        //int chunkCountX = strideX >= CHUNK_SIZE ? sizeX : (x >> CHUNK_SHIFT) - ((x + sizeX * strideX) >> CHUNK_SHIFT);
        //int chunkCountZ = strideZ >= CHUNK_SIZE ? sizeZ : (z >> CHUNK_SHIFT) - ((z + sizeZ * strideZ) >> CHUNK_SHIFT);
        Consumer<IntConsumer> chunkXSupplier = this.chunkCoordSupplier(x, sizeX, strideX);
        Consumer<IntConsumer> chunkZSupplier = this.chunkCoordSupplier(z, sizeZ, strideZ);

        //prefetch all chunks on server thread
        IChunk[] chunks = ((IMixinServerWorld1_16) this.world).fp2_farWorldServer().fp2_IFarWorld_workerManager().workExecutor().supply(() -> {
            List<CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> tmp = new ArrayList<>();

            //get all the futures
            chunkXSupplier.accept(chunkX -> chunkZSupplier.accept(chunkZ -> {
                ATServerChunkProvider1_16 serverChunkProvider = (ATServerChunkProvider1_16) this.world.getChunkSource();
                tmp.add(serverChunkProvider.invokeGetChunkFutureMainThread(chunkX, chunkZ, ChunkStatus.FULL, true));
            }));

            //block on all the futures
            return tmp.stream()
                    .map(future -> {
                        ((ATServerChunkProvider1_16) this.world.getChunkSource()).getFp2_MainThreadProcessor().managedBlock(future::isDone);
                        return future.join().map(Function.identity(), error -> {
                            throw new IllegalStateException("Chunk not there when requested: " + error);
                        });
                    })
                    .toArray(IChunk[]::new);
        }).join();

        //delegate to PrefetchedChunksFBlockWorld1_16
        new PrefetchedChunksFBlockWorld1_16(this.registry, chunks)
                .getData(states, statesOff, statesStride, biomes, biomesOff, biomesStride, light, lightOff, lightStride, x, y, z, sizeX, sizeY, sizeZ, strideX, strideY, strideZ);
    }

    protected Consumer<IntConsumer> chunkCoordSupplier(int base, int size, int stride) {
        if (stride >= CHUNK_SIZE) {
            return callback -> {
                for (int i = 0, block = base; i < size; i++, block += stride) {
                    callback.accept(block >> CHUNK_SHIFT);
                }
            };
        } else {
            return callback -> {
                for (int chunk = base >> CHUNK_SHIFT, limit = (base + size * stride - 1) >> CHUNK_SHIFT; chunk <= limit; chunk++) {
                    callback.accept(chunk);
                }
            };
        }
    }
}
