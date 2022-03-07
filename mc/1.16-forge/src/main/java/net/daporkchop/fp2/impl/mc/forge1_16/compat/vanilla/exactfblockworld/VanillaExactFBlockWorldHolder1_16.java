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

import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockWorldHolder;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.threading.futurecache.IAsyncCache;
import net.daporkchop.fp2.core.util.threading.lazy.LazyFutureTask;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.world.chunk.storage.ATChunkLoader1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.world.server.ATChunkManager1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.chunk.storage.IMixinIOWorker1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.server.IMixinServerWorld1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.util.threading.AsyncCacheNBT1_16;
import net.daporkchop.lib.common.function.throwing.ERunnable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.datafix.DefaultTypeReferences;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.chunk.storage.ChunkSerializer;
import net.minecraft.world.server.ServerWorld;

import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
public class VanillaExactFBlockWorldHolder1_16 implements ExactFBlockWorldHolder {
    protected final ServerWorld world;
    protected final IFarWorldServer farWorld;

    protected final FGameRegistry registry;

    protected final ChunkCache chunks = new ChunkCache();

    protected final NDimensionalIntSegtreeSet chunksExistCache;

    public VanillaExactFBlockWorldHolder1_16(@NonNull ServerWorld world) {
        this.world = world;
        this.farWorld = ((IMixinServerWorld1_16) world).fp2_farWorldServer();

        this.registry = this.farWorld.fp2_IFarWorld_registry();

        this.chunksExistCache = Datastructures.INSTANCE.nDimensionalIntSegtreeSet()
                .dimensions(2)
                .threadSafe(true)
                .initialPoints(() -> ((IMixinIOWorker1_16) ((ATChunkLoader1_16) world.getChunkSource().chunkMap).getWorker()).fp2_IOWorker_listChunksWithData()
                        .filter(entry -> ChunkSerializer.getChunkTypeFromTag(this.dfu(entry.getValue())) == ChunkStatus.Type.LEVELCHUNK)
                        .map(entry -> new int[]{ entry.getKey().x, entry.getKey().z })
                        .parallel())
                .build();

        this.farWorld.fp2_IFarWorldServer_eventBus().registerWeak(this);
    }

    @Override
    public FBlockWorld worldFor(@NonNull AllowGenerationRequirement requirement) {
        return new VanillaExactFBlockWorld1_16(this, this.registry, requirement == AllowGenerationRequirement.ALLOWED);
    }

    @Override
    public void close() {
        this.farWorld.fp2_IFarWorldServer_eventBus().unregister(this);
        this.chunksExistCache.release();
    }

    @FEventHandler
    private void onColumnSaved(@NonNull ColumnSavedEvent event) {
        if (event.column().isFullyPopulated()) {
            this.chunksExistCache.add(event.pos().x(), event.pos().y());
            this.chunks.notifyUpdate(new ChunkPos(event.pos().x(), event.pos().y()), (CompoundNBT) event.data());
        }
    }

    @FEventHandler
    private void onCubeSaved(@NonNull CubeSavedEvent event) {
        throw new UnsupportedOperationException("vanilla world shouldn't have cubes!");
    }

    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int minSectionX = minX >> 4;
        int minSectionZ = minZ >> 4;
        int maxSectionX = (maxX >> 4) + 1; //rounded up because maximum positions are inclusive
        int maxSectionZ = (maxZ >> 4) + 1;

        return maxY >= 0 && minY < this.world.getHeight() && this.chunksExistCache.containsAny(minSectionX, minSectionZ, maxSectionX, maxSectionZ);
    }

    protected OffThreadChunk1_16 getChunk(int chunkX, int chunkZ, boolean allowGeneration) throws GenerationNotAllowedException {
        return GenerationNotAllowedException.throwIfNull(this.chunks.get(new ChunkPos(chunkX, chunkZ), allowGeneration).join());
    }

    protected Stream<OffThreadChunk1_16> multiGetChunks(@NonNull Stream<ChunkPos> chunkPositions, boolean allowGeneration) throws GenerationNotAllowedException {
        //collect all futures into an array first in order to issue all tasks at once before blocking, thus ensuring maximum parallelism
        LazyFutureTask<OffThreadChunk1_16>[] chunkFutures = uncheckedCast(chunkPositions.map(pos -> this.chunks.get(pos, allowGeneration)).toArray(LazyFutureTask[]::new));

        return LazyFutureTask.scatterGather(chunkFutures).stream()
                .peek(GenerationNotAllowedException.uncheckedThrowIfNull());
    }

    protected CompoundNBT dfu(@NonNull CompoundNBT nbt) {
        int version = ChunkLoader.getVersion(nbt);
        if (version < SharedConstants.getCurrentVersion().getWorldVersion()) {
            nbt = NBTUtil.update(((ATChunkLoader1_16) this.world.getChunkSource().chunkMap).getFixerUpper(), DefaultTypeReferences.CHUNK, nbt, version);
            nbt.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
        }
        return nbt;
    }

    /**
     * {@link IAsyncCache} for chunks.
     *
     * @author DaPorkchop_
     */
    protected class ChunkCache extends AsyncCacheNBT1_16<ChunkPos, Object, OffThreadChunk1_16> {
        @Override
        protected OffThreadChunk1_16 parseNBT(@NonNull ChunkPos key, @NonNull Object param, @NonNull CompoundNBT nbt) {
            //upgrade chunk data if it's outdated
            nbt = VanillaExactFBlockWorldHolder1_16.this.dfu(nbt);

            return ChunkSerializer.getChunkTypeFromTag(nbt) == ChunkStatus.Type.LEVELCHUNK
                    ? new OffThreadChunk1_16(VanillaExactFBlockWorldHolder1_16.this.world, nbt)
                    : null; //chunk isn't fully populated, pretend it doesn't exist
        }

        @Override
        protected OffThreadChunk1_16 loadFromDisk(@NonNull ChunkPos key, @NonNull Object param) {
            //load the raw NBT data from the IOWorker
            //  ((damn) that's a (lot) of ((casts) !)))
            CompoundNBT nbt = ((IMixinIOWorker1_16) ((ATChunkLoader1_16) VanillaExactFBlockWorldHolder1_16.this.world.getChunkSource().chunkMap).getWorker()).fp2_IOWorker_loadFuture(key).join();
            return nbt != null
                    ? this.parseNBT(key, param, nbt) //we were able to read the data, delegate to parseNBT to do the actual parsing
                    : null; //chunk doesn't exist on disk
        }

        @Override
        protected void triggerGeneration(@NonNull ChunkPos key, @NonNull Object param) {
            VanillaExactFBlockWorldHolder1_16.this.farWorld.fp2_IFarWorld_workerManager().workExecutor().run((ERunnable) () -> {
                int x = key.x;
                int z = key.z;

                Chunk chunk = VanillaExactFBlockWorldHolder1_16.this.world.getChunk(x, z);
                VanillaExactFBlockWorldHolder1_16.this.world.getChunk(x + 1, z);
                VanillaExactFBlockWorldHolder1_16.this.world.getChunk(x + 1, z + 1);
                VanillaExactFBlockWorldHolder1_16.this.world.getChunk(x, z + 1);

                ((ATChunkManager1_16) VanillaExactFBlockWorldHolder1_16.this.world.getChunkSource().chunkMap).invokeSave(chunk);
            }).join();
        }
    }
}
