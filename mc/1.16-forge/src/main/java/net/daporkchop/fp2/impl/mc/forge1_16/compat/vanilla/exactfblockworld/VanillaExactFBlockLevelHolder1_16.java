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

package net.daporkchop.fp2.impl.mc.forge1_16.compat.vanilla.exactfblockworld;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.minecraft.util.threading.asynccache.AsyncCacheNBT;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractChunksExactFBlockLevelHolder;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractPrefetchedChunksExactFBlockLevel;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.threading.futurecache.IAsyncCache;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.world.chunk.storage.ATChunkLoader1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.at.world.server.ATChunkManager1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.chunk.storage.IMixinIOWorker1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.asm.interfaz.world.server.IMixinServerWorld1_16;
import net.daporkchop.fp2.impl.mc.forge1_16.server.world.level.FLevelServer1_16;
import net.daporkchop.lib.common.function.throwing.ERunnable;
import net.daporkchop.lib.math.vector.Vec2i;
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

import java.util.List;

/**
 * @author DaPorkchop_
 */
public class VanillaExactFBlockLevelHolder1_16 extends AbstractChunksExactFBlockLevelHolder<OffThreadChunk1_16> {
    public VanillaExactFBlockLevelHolder1_16(@NonNull FLevelServer1_16 level) {
        super(level, 4);
    }

    @Override
    protected NDimensionalIntSegtreeSet createChunksExistIndex(@NonNull IFarLevelServer level) {
        return Datastructures.INSTANCE.nDimensionalIntSegtreeSet()
                .dimensions(2)
                .threadSafe(true)
                .initialPoints(() -> ((IMixinIOWorker1_16) ((ATChunkLoader1_16) ((ServerWorld) level.implLevel()).getChunkSource().chunkMap).getWorker()).fp2_IOWorker_listChunks()
                        //this doesn't filter out chunks that aren't fully generated, but including those would make iteration massively slower. i'll just take the performance
                        //  hit of having to try to load a bunch of not-quite-fully-generated chunks into the cache during runtime, at least until i make the segment tree lazily
                        //  initialized.
                        .map(pos -> new int[]{ pos.x, pos.z })
                        .parallel())
                .build();
    }

    @Override
    protected AsyncCacheNBT<Vec2i, ?, OffThreadChunk1_16, ?> createChunkCache(@NonNull IFarLevelServer level) {
        return new ChunkCache((ServerWorld) level.implLevel());
    }

    @Override
    protected AbstractPrefetchedChunksExactFBlockLevel<OffThreadChunk1_16> prefetchedWorld(boolean generationAllowed, @NonNull List<OffThreadChunk1_16> chunks) {
        return new PrefetchedChunksFBlockLevel1_16(this, generationAllowed, chunks);
    }

    @FEventHandler
    protected void onColumnSaved(@NonNull ColumnSavedEvent event) {
        if (event.column().isFullyPopulated()) { //we don't care unless the column is fully populated
            super.onColumnSaved(event);
        }
    }

    /**
     * {@link IAsyncCache} for chunks.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @NonNull
    protected static class ChunkCache extends AsyncCacheNBT<Vec2i, Object, OffThreadChunk1_16, CompoundNBT> {
        @NonNull
        private final ServerWorld world;

        protected CompoundNBT dfu(@NonNull CompoundNBT nbt) {
            int version = ChunkLoader.getVersion(nbt);
            if (version < SharedConstants.getCurrentVersion().getWorldVersion()) {
                nbt = NBTUtil.update(((ATChunkLoader1_16) this.world.getChunkSource().chunkMap).getFixerUpper(), DefaultTypeReferences.CHUNK, nbt, version);
                nbt.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
            }
            return nbt;
        }

        protected boolean isFullyGeneratedChunkData(@NonNull CompoundNBT nbt) {
            if (ChunkSerializer.getChunkTypeFromTag(nbt) != ChunkStatus.Type.LEVELCHUNK) { //chunk status isn't FULL
                return false;
            }

            CompoundNBT levelTag = nbt.getCompound("Level");
            if (!levelTag.getBoolean("isLightOn")) { //this field is poorly named, it should be called "isLightValid". if it's false, we want to delegate to the server thread in
                //  order to force lighting to be re-calculated (can happen on e.g. worldpainter worlds)
                return false;
            }

            return true;
        }

        @Override
        protected OffThreadChunk1_16 parseNBT(@NonNull Vec2i key, @NonNull Object param, @NonNull CompoundNBT nbt) {
            //upgrade chunk data if it's outdated
            nbt = this.dfu(nbt);

            return this.isFullyGeneratedChunkData(nbt)
                    ? new OffThreadChunk1_16(this.world, nbt)
                    : null; //chunk isn't fully generated, pretend it doesn't exist
        }

        @Override
        protected OffThreadChunk1_16 loadFromDisk(@NonNull Vec2i key, @NonNull Object param) {
            //load the raw NBT data from the IOWorker
            //  ((damn) that's a (lot) of ((casts) !)))
            CompoundNBT nbt = ((IMixinIOWorker1_16) ((ATChunkLoader1_16) this.world.getChunkSource().chunkMap).getWorker())
                    .fp2_IOWorker_loadFuture(new ChunkPos(key.x(), key.y())).join();
            return nbt != null
                    ? this.parseNBT(key, param, nbt) //we were able to read the data, delegate to parseNBT to do the actual parsing
                    : null; //chunk doesn't exist on disk
        }

        @Override
        protected void triggerGeneration(@NonNull Vec2i key, @NonNull Object param) {
            ((IMixinServerWorld1_16) this.world).fp2_levelServer().workerManager().workExecutor().run((ERunnable) () -> {
                int x = key.x();
                int z = key.y();

                Chunk chunk = this.world.getChunk(x, z);
                this.world.getChunk(x + 1, z);
                this.world.getChunk(x + 1, z + 1);
                this.world.getChunk(x, z + 1);

                ((ATChunkManager1_16) this.world.getChunkSource().chunkMap).invokeSave(chunk);
            }).join();
        }
    }
}
