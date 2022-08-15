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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.exactfblocklevel;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.core.minecraft.util.threading.asynccache.AsyncCacheNBT;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractChunksExactFBlockLevelHolder;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractPrefetchedChunksExactFBlockLevel;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.threading.futurecache.IAsyncCache;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.at.world.chunk.storage.ATAnvilChunkLoader1_12;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.world.IMixinWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.region.ThreadSafeRegionFileCache;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.world.level.FLevelServer1_12;
import net.daporkchop.lib.common.function.exception.ERunnable;
import net.daporkchop.lib.math.vector.Vec2i;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import java.io.IOException;
import java.util.List;

/**
 * Default implementation of {@link ExactFBlockLevelHolder} for vanilla worlds.
 *
 * @author DaPorkchop_
 */
public class VanillaExactFBlockLevelHolder1_12 extends AbstractChunksExactFBlockLevelHolder<Chunk> {
    public VanillaExactFBlockLevelHolder1_12(@NonNull FLevelServer1_12 level) {
        super(level, 4);
    }

    @Override
    protected NDimensionalIntSegtreeSet createChunksExistIndex(@NonNull IFarLevelServer level) {
        AnvilChunkLoader io = (AnvilChunkLoader) ((WorldServer) level.implLevel()).getChunkProvider().chunkLoader;
        return Datastructures.INSTANCE.nDimensionalIntSegtreeSet()
                .dimensions(2)
                .threadSafe(true)
                .initialPoints(() -> ThreadSafeRegionFileCache.INSTANCE.allChunks(io.chunkSaveLocation.toPath().resolve("region"))
                        .map(pos -> new int[]{ pos.x, pos.z })
                        .parallel())
                .build();
    }

    @Override
    protected AsyncCacheNBT<Vec2i, ?, Chunk, ?> createChunkCache(@NonNull IFarLevelServer level) {
        return new ChunkCache((WorldServer) level.implLevel(), (AnvilChunkLoader) ((WorldServer) level.implLevel()).getChunkProvider().chunkLoader);
    }

    @Override
    protected AbstractPrefetchedChunksExactFBlockLevel<Chunk> prefetchedWorld(boolean generationAllowed, @NonNull List<Chunk> chunks) {
        return new PrefetchedChunksFBlockLevel1_12(this, generationAllowed, chunks);
    }

    /**
     * {@link IAsyncCache} for chunks.
     *
     * @author DaPorkchop_
     */
    //TODO: this doesn't handle the difference between "chunk is populated" and "chunk and its neighbors are populated", which is important because vanilla is very dumb
    @RequiredArgsConstructor
    @Getter
    protected static class ChunkCache extends AsyncCacheNBT<Vec2i, Object, Chunk, NBTTagCompound> {
        @NonNull
        private final WorldServer world;
        @NonNull
        private final AnvilChunkLoader io;

        @Override
        protected Chunk parseNBT(@NonNull Vec2i key, @NonNull Object param, @NonNull NBTTagCompound nbt) {
            Chunk chunk = ((ATAnvilChunkLoader1_12) this.io).invokeCheckedReadChunkFromNBT(this.world, key.x(), key.y(), nbt);
            return chunk.isTerrainPopulated() ? chunk : null;
        }

        @Override
        @SneakyThrows(IOException.class)
        protected Chunk loadFromDisk(@NonNull Vec2i key, @NonNull Object param) {
            Object[] data = this.io.loadChunk__Async(this.world, key.x(), key.y());
            Chunk chunk = data != null ? (Chunk) data[0] : null;
            return chunk != null && chunk.isTerrainPopulated() ? chunk : null;
        }

        @Override
        protected void triggerGeneration(@NonNull Vec2i key, @NonNull Object param) {
            ((IMixinWorldServer) this.world).fp2_levelServer().workerManager().workExecutor().run((ERunnable) () -> {
                int x = key.x();
                int z = key.y();

                //see net.minecraftforge.server.command.ChunkGenWorker#doWork() for more info

                Chunk chunk = this.world.getChunk(x, z);
                this.world.getChunk(x + 1, z);
                this.world.getChunk(x + 1, z + 1);
                this.world.getChunk(x, z + 1);

                this.io.saveChunk(this.world, chunk);
            }).join();
        }
    }
}
