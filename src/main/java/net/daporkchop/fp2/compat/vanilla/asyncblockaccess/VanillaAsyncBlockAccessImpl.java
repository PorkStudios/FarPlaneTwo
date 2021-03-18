/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.compat.vanilla.asyncblockaccess;

import io.netty.util.concurrent.ImmediateEventExecutor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.daporkchop.fp2.server.worldlistener.IWorldChangeListener;
import net.daporkchop.fp2.server.worldlistener.WorldChangeListenerManager;
import net.daporkchop.fp2.util.reference.WeakLongKeySelfRemovingReference;
import net.daporkchop.fp2.util.threading.LazyRunnablePFuture;
import net.daporkchop.fp2.util.threading.ServerThreadExecutor;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.concurrent.PFuture;
import net.daporkchop.lib.concurrent.PFutures;
import net.daporkchop.lib.primitive.lambda.LongObjObjFunction;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.concurrent.LongObjConcurrentHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import java.io.IOException;
import java.lang.ref.Reference;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Default implementation of {@link AsyncBlockAccess} for vanilla worlds.
 *
 * @author DaPorkchop_
 */
//this makes the assumption that asynchronous read-only access to Chunk is safe, which seems to be the case although i'm not entirely sure. testing needed
public class VanillaAsyncBlockAccessImpl implements AsyncBlockAccess, IWorldChangeListener {
    protected final WorldServer world;
    protected final AnvilChunkLoader io;

    protected final LongObjMap<Reference<NBTTagCompound>> nbtCache = new LongObjConcurrentHashMap<>();
    protected final LongObjMap<Object> cache = new LongObjConcurrentHashMap<>();

    public VanillaAsyncBlockAccessImpl(@NonNull WorldServer world) {
        this.world = world;
        this.io = (AnvilChunkLoader) this.world.getChunkProvider().chunkLoader;

        WorldChangeListenerManager.add(this.world, this);
    }

    protected Chunk getChunk(int chunkX, int chunkZ) {
        class State implements LongObjObjFunction<Object, Object> {
            Object chunk;

            @Override
            public Object apply(long pos, Object chunk) {
                if (chunk instanceof Reference) {
                    this.chunk = PorkUtil.<Reference<Chunk>>uncheckedCast(chunk).get();
                } else if (chunk instanceof LazyRunnablePFuture) {
                    this.chunk = chunk;
                }

                if (this.chunk != null) { //chunk object or future was obtained successfully, keep existing value in cache
                    return chunk;
                } else { //chunk isn't loaded or was garbage collected, we need to load it anew
                    return this.chunk = new ChunkLoadingLazyFuture(pos);
                }
            }

            public Chunk resolve() {
                if (this.chunk instanceof Chunk) {
                    return (Chunk) this.chunk;
                } else if (this.chunk instanceof LazyRunnablePFuture) {
                    LazyRunnablePFuture<Chunk> future = uncheckedCast(this.chunk);
                    future.run();
                    return future.join();
                }

                throw new IllegalStateException(Objects.toString(this.chunk));
            }
        }

        State state = new State();
        this.cache.compute(BinMath.packXY(chunkX, chunkZ), state);
        return state.resolve();
    }

    protected PFuture<Chunk> getChunkFuture(int chunkX, int chunkZ) {
        class State implements LongObjObjFunction<Object, Object> {
            Object chunk;

            @Override
            public Object apply(long pos, Object chunk) {
                if (chunk instanceof Reference) {
                    this.chunk = PorkUtil.<Reference<Chunk>>uncheckedCast(chunk).get();
                } else if (chunk instanceof LazyRunnablePFuture) {
                    this.chunk = chunk;
                }

                if (this.chunk != null) { //chunk object or future was obtained successfully, keep existing value in cache
                    return chunk;
                } else { //chunk isn't loaded or was garbage collected, we need to load it anew
                    return this.chunk = new ChunkLoadingLazyFuture(pos);
                }
            }

            public PFuture<Chunk> resolve() {
                if (this.chunk instanceof Chunk) {
                    return PFutures.successful((Chunk) this.chunk, ImmediateEventExecutor.INSTANCE);
                } else if (this.chunk instanceof LazyRunnablePFuture) {
                    LazyRunnablePFuture<Chunk> future = uncheckedCast(this.chunk);
                    future.run();
                    return future;
                }

                throw new IllegalStateException(Objects.toString(this.chunk));
            }
        }

        State state = new State();
        this.cache.compute(BinMath.packXY(chunkX, chunkZ), state);
        return state.resolve();
    }

    @Override
    public IBlockHeightAccess prefetch(@NonNull Stream<ChunkPos> columns) {
        List<PFuture<Chunk>> chunkFutures = columns.map(pos -> this.getChunkFuture(pos.x, pos.z)).collect(Collectors.toList());
        return new PrefetchedColumnsVanillaAsyncBlockAccess(this, this.world, chunkFutures.stream().map(PFuture::join));
    }

    @Override
    public IBlockHeightAccess prefetch(@NonNull Stream<ChunkPos> columns, @NonNull Function<IBlockHeightAccess, Stream<Vec3i>> cubesMappingFunction) {
        return this.prefetch(columns); //silently ignore cubes
    }

    @Override
    public void onColumnSaved(@NonNull World world, int columnX, int columnZ, @NonNull NBTTagCompound nbt) {
        long pos = BinMath.packXY(columnX, columnZ);
        this.nbtCache.put(pos, new WeakLongKeySelfRemovingReference<>(nbt, pos, this.nbtCache));
        this.cache.remove(pos);
    }

    @Override
    public void onCubeSaved(@NonNull World world, int cubeX, int cubeY, int cubeZ, @NonNull NBTTagCompound nbt) {
        throw new UnsupportedOperationException("vanilla world shouldn't have cubes!");
    }

    @Override
    public int getTopBlockY(int blockX, int blockZ) {
        return this.getChunk(blockX >> 4, blockZ >> 4).getHeightValue(blockX & 0xF, blockZ & 0xF) - 1;
    }

    @Override
    public int getTopBlockYBelow(int blockX, int blockY, int blockZ) {
        throw new UnsupportedOperationException("Not implemented"); //TODO: i could actually write an implementation for this
    }

    @Override
    public int getBlockLight(BlockPos pos) {
        if (!this.world.isValid(pos)) {
            return 0;
        } else {
            return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getLightFor(EnumSkyBlock.BLOCK, pos);
        }
    }

    @Override
    public int getSkyLight(BlockPos pos) {
        if (!this.world.provider.hasSkyLight()) {
            return 0;
        } else if (!this.world.isValid(pos)) {
            return 15;
        } else {
            return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getLightFor(EnumSkyBlock.SKY, pos);
        }
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getBlockState(pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getBiome(pos, null); //provider is not used on client
    }

    @Override
    public WorldType getWorldType() {
        return this.world.getWorldType();
    }

    /**
     * Lazily loads chunks.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class ChunkLoadingLazyFuture extends LazyRunnablePFuture<Chunk> {
        protected final long pos;

        @Override
        protected Chunk run0() throws Exception {
            while (true) {
                Reference<NBTTagCompound> reference = VanillaAsyncBlockAccessImpl.this.nbtCache.get(this.pos);
                NBTTagCompound nbt;

                Chunk chunk;
                if (reference == null || (nbt = reference.get()) == null) { //not in write cache, load it from disk
                    Object[] data = VanillaAsyncBlockAccessImpl.this.io.loadChunk__Async(VanillaAsyncBlockAccessImpl.this.world, BinMath.unpackX(this.pos), BinMath.unpackY(this.pos));
                    chunk = data != null ? (Chunk) data[0] : null;
                } else { //re-use cached nbt
                    chunk = VanillaAsyncBlockAccessImpl.this.io.checkedReadChunkFromNBT(VanillaAsyncBlockAccessImpl.this.world, BinMath.unpackX(this.pos), BinMath.unpackY(this.pos), nbt);
                }

                if (chunk != null && !chunk.isEmpty() && chunk.isTerrainPopulated()) {
                    return chunk;
                }

                //load and immediately save column on server thread
                CompletableFuture.runAsync(() -> {
                    int x = BinMath.unpackX(this.pos);
                    int z = BinMath.unpackY(this.pos);

                    //we load the chunk, as well as all of its neighbors, all in a specific order in order to ensure that the chunk is FULLY populated.
                    // see net.minecraftforge.server.command.ChunkGenWorker#doWork() for more info

                    for (int dx = -1; dx <= 0; dx++) {
                        for (int dz = -1; dz <= 0; dz++) {
                            VanillaAsyncBlockAccessImpl.this.world.getChunk(x + dx, z + dz);
                            VanillaAsyncBlockAccessImpl.this.world.getChunk(x + dx + 1, z + dz);
                            VanillaAsyncBlockAccessImpl.this.world.getChunk(x + dx + 1, z + dz + 1);
                            VanillaAsyncBlockAccessImpl.this.world.getChunk(x + dx, z + dz + 1);
                        }
                    }

                    Chunk c = VanillaAsyncBlockAccessImpl.this.world.getChunk(BinMath.unpackX(this.pos), BinMath.unpackY(this.pos));
                    try {
                        VanillaAsyncBlockAccessImpl.this.io.saveChunk(VanillaAsyncBlockAccessImpl.this.world, c);
                    } catch (IOException | MinecraftException e) {
                        LOGGER.error("Unable to save chunk!", e);
                    }
                }, ServerThreadExecutor.INSTANCE).join();
            }
        }

        @Override
        protected void postRun() {
            //swap out self in cache with a weak reference to self
            VanillaAsyncBlockAccessImpl.this.cache.replace(this.pos, this, new WeakLongKeySelfRemovingReference<>(this.getNow(), this.pos, VanillaAsyncBlockAccessImpl.this.cache));
        }
    }
}
