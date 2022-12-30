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

package net.daporkchop.fp2.core.minecraft.test.world.level;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.event.FEventBus;
import net.daporkchop.fp2.api.storage.external.FStorageCategory;
import net.daporkchop.fp2.api.test.world.level.TestFBlockLevel;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.event.EventBus;
import net.daporkchop.fp2.core.minecraft.util.threading.asynccache.AsyncCacheNBT;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractChunksExactFBlockLevelHolder;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractPrefetchedChunksExactFBlockLevel;
import net.daporkchop.fp2.core.mode.api.IFarPos;
import net.daporkchop.fp2.core.mode.api.IFarRenderMode;
import net.daporkchop.fp2.core.mode.api.IFarTile;
import net.daporkchop.fp2.core.mode.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.server.world.FBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.math.vector.Vec2i;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;

import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;

/**
 * @author DaPorkchop_
 */
public class TestExactFBlockLevelChunks extends TestFBlockLevel {
    @Override
    protected FBlockLevel dummyLevelOpaque(IntAxisAlignedBB bounds) {
        return new DummyChunksIFarLevelServer(bounds, 4, this.alwaysOpaque()).exactBlockLevelHolder().blockLevel(FBlockLevelHolder.AllowGenerationRequirement.DONT_CARE);
    }

    @Override
    protected FBlockLevel dummyLevelCheckerboard(IntAxisAlignedBB bounds) {
        return new DummyChunksIFarLevelServer(bounds, 4, this.checkerboard()).exactBlockLevelHolder().blockLevel(FBlockLevelHolder.AllowGenerationRequirement.DONT_CARE);
    }

    protected static class DummyChunksExactFBlockLevelHolder extends AbstractChunksExactFBlockLevelHolder<DummyChunk> {
        protected final IntIntIntToIntFunction blockFunction;

        public DummyChunksExactFBlockLevelHolder(@NonNull IFarLevelServer level, int chunkShift, @NonNull IntIntIntToIntFunction blockFunction) {
            super(level, chunkShift);

            this.blockFunction = blockFunction;
        }

        @Override
        protected NDimensionalIntSegtreeSet createChunksExistIndex(@NonNull IFarLevelServer world) {
            return null;
        }

        @Override
        protected AsyncCacheNBT<Vec2i, ?, DummyChunk, ?> createChunkCache(@NonNull IFarLevelServer world) {
            return new AsyncCacheNBT<Vec2i, Object, DummyChunk, Object>() {
                @Override
                protected DummyChunk parseNBT(@NonNull Vec2i key, @NonNull Object param, @NonNull Object o) {
                    throw new UnsupportedOperationException();
                }

                @Override
                protected DummyChunk loadFromDisk(@NonNull Vec2i key, @NonNull Object param) {
                    return new DummyChunk(key);
                }

                @Override
                protected void triggerGeneration(@NonNull Vec2i key, @NonNull Object param) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        protected AbstractPrefetchedChunksExactFBlockLevel<DummyChunk> prefetchedWorld(boolean generationAllowed, @NonNull List<DummyChunk> dummyChunks) {
            return new AbstractPrefetchedChunksExactFBlockLevel<DummyChunk>(this, generationAllowed, dummyChunks) {
                @Override
                protected long packedChunkPosition(@NonNull DummyChunk dummyChunk) {
                    return BinMath.packXY(dummyChunk.pos().x(), dummyChunk.pos().y());
                }

                @Override
                protected int getState(int x, int y, int z, DummyChunk dummyChunk) throws GenerationNotAllowedException {
                    //TODO: this should account for the case where part of a chunk extends outside the bounds
                    return this.holder().dataLimits().contains(x, y, z) ? DummyChunksExactFBlockLevelHolder.this.blockFunction.apply(x, y, z) : BLOCK_TYPE_INVISIBLE;
                }

                @Override
                protected int getBiome(int x, int y, int z, DummyChunk dummyChunk) throws GenerationNotAllowedException {
                    return 0;
                }

                @Override
                protected byte getLight(int x, int y, int z, DummyChunk dummyChunk) throws GenerationNotAllowedException {
                    return 0;
                }
            };
        }
    }

    @Data
    protected static class DummyChunk {
        @NonNull
        private final Vec2i pos;
    }

    @Getter
    protected static class DummyChunksIFarLevelServer implements IFarLevelServer {
        protected final IntAxisAlignedBB coordLimits;
        protected final FBlockLevelHolder.Exact exactBlockLevelHolder;

        protected final FEventBus eventBus = new EventBus();

        public DummyChunksIFarLevelServer(@NonNull IntAxisAlignedBB coordLimits, int chunkShift, @NonNull IntIntIntToIntFunction blockFunction) {
            this.coordLimits = coordLimits;
            this.exactBlockLevelHolder = new DummyChunksExactFBlockLevelHolder(this, chunkShift, blockFunction);
        }

        @Override
        public Object implLevel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Identifier id() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long timestamp() {
            throw new UnsupportedOperationException();
        }

        @Override
        public FGameRegistry registry() {
            return DummyTypeRegistry.INSTANCE;
        }

        @Override
        public void close() {
            this.exactBlockLevelHolder.close();
        }

        @Override
        public FStorageCategory storageCategory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public WorkerManager workerManager() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <POS extends IFarPos, T extends IFarTile> IFarTileProvider<POS, T> tileProviderFor(@NonNull IFarRenderMode<POS, T> mode) throws NoSuchElementException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void forEachTileProvider(@NonNull BiConsumer<? super IFarRenderMode<?, ?>, ? super IFarTileProvider<?, ?>> action) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path levelDirectory() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TerrainGeneratorInfo terrainGeneratorInfo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int seaLevel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FBlockLevelHolder.Rough> roughBlockLevelHolder() {
            throw new UnsupportedOperationException();
        }
    }
}
