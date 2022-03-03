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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.exactfblockworld;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldServer;
import io.github.opencubicchunks.cubicchunks.api.world.storage.ICubicStorage;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.AsyncBatchingCubeIO;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.event.TickEndEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockWorldHolder;
import net.daporkchop.fp2.core.server.world.IFarWorldServer;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.threading.futurecache.IAsyncCache;
import net.daporkchop.fp2.core.util.threading.lazy.LazyFutureTask;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.world.IMixinWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.biome.Column2dBiomeAccessWrapper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.biome.CubeBiomeAccessWrapper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.cube.CubeWithoutWorld;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.IBiomeAccess;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.asyncblockaccess.AsyncCacheNBTBase;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Default implementation of {@link ExactFBlockWorldHolder} for cubic chunks worlds.
 *
 * @author DaPorkchop_
 */
public class CCExactFBlockWorldHolder1_12 implements ExactFBlockWorldHolder {
    protected static final long ASYNCBATCHINGCUBEIO_STORAGE_OFFSET = PUnsafe.pork_getOffset(AsyncBatchingCubeIO.class, "storage");

    protected final WorldServer world;
    protected final IFarWorldServer farWorld;

    protected final ICubeIO io;
    protected final ICubicStorage storage;
    protected final ICubeGenerator generator;

    protected final ColumnCache columns = new ColumnCache();
    protected final CubeCache cubes = new CubeCache();

    protected final ExtendedBlockStorage emptyStorage;

    protected final NDimensionalIntSegtreeSet columnsExistCache;
    protected final NDimensionalIntSegtreeSet cubesExistCache;

    protected final AtomicInteger generatedCount = new AtomicInteger();

    public CCExactFBlockWorldHolder1_12(@NonNull WorldServer world) {
        this.world = world;
        this.farWorld = ((IMixinWorldServer) world).fp2_farWorldServer();

        this.io = ((ICubeProviderInternal.Server) ((ICubicWorldInternal) world).getCubeCache()).getCubeIO();
        this.storage = PUnsafe.getObject(AsyncBatchingCubeIO.class.cast(this.io), ASYNCBATCHINGCUBEIO_STORAGE_OFFSET);
        this.generator = ((ICubicWorldServer) world).getCubeGenerator();

        this.emptyStorage = new ExtendedBlockStorage(0, world.provider.hasSkyLight());

        this.farWorld.fp2_IFarWorldServer_eventBus().registerWeak(this);

        this.columnsExistCache = Datastructures.INSTANCE.nDimensionalIntSegtreeSet()
                .dimensions(2)
                .threadSafe(true)
                .initialPoints(() -> {
                    List<int[]> positions = new ArrayList<>();
                    CCExactFBlockWorldHolder1_12.this.storage.forEachColumn(pos -> positions.add(new int[]{ pos.x, pos.z }));
                    return positions.stream();
                })
                .build();
        this.cubesExistCache = Datastructures.INSTANCE.nDimensionalIntSegtreeSet()
                .dimensions(3)
                .threadSafe(true)
                .initialPoints(() -> {
                    List<int[]> positions = new ArrayList<>();
                    CCExactFBlockWorldHolder1_12.this.storage.forEachCube(pos -> positions.add(new int[]{ pos.getX(), pos.getY(), pos.getZ() }));
                    return positions.stream();
                })
                .build();
    }

    @Override
    public FBlockWorld worldFor(@NonNull AllowGenerationRequirement requirement) {
        return new CCExactFBlockWorld1_12(this, requirement == AllowGenerationRequirement.ALLOWED);
    }

    @Override
    public void close() {
        this.columnsExistCache.release();
        this.cubesExistCache.release();
    }

    @FEventHandler
    private void onColumnSaved(ColumnSavedEvent event) {
        this.columnsExistCache.add(event.pos().x(), event.pos().y());
        this.columns.notifyUpdate(new ChunkPos(event.pos().x(), event.pos().y()), (NBTTagCompound) event.data());
    }

    @FEventHandler
    private void onCubeSaved(CubeSavedEvent event) {
        this.cubesExistCache.add(event.pos().x(), event.pos().y(), event.pos().z());
        this.cubes.notifyUpdate(new CubePos(event.pos().x(), event.pos().y(), event.pos().z()), (NBTTagCompound) event.data());
    }

    @FEventHandler
    private void onTickEnd(TickEndEvent event) {
        //if 8192 things have been generated, run a full chunk gc!
        //  if we don't do this, the gc will only be triggered when a player has been moving for more than some number of ticks. the player might just be standing still
        //  while waiting for terrain to load in, so we need to do this to prevent a memory leak.
        if (this.generatedCount.get() >= 8192) {
            this.generatedCount.lazySet(0);

            ((ICubicWorldServer) this.world).unloadOldCubes();
        }
    }

    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int minCubeX = Coords.blockToCube(minX);
        int minCubeY = Coords.blockToCube(minY);
        int minCubeZ = Coords.blockToCube(minZ);
        int maxCubeX = Coords.blockToCube(maxX) + 1; //rounded up because maximum positions are inclusive
        int maxCubeY = Coords.blockToCube(maxY) + 1;
        int maxCubeZ = Coords.blockToCube(maxZ) + 1;

        return this.cubesExistCache.containsAny(minCubeX, minCubeY, minCubeZ, maxCubeX, maxCubeY, maxCubeZ);
    }

    protected ICube getCube(int cubeX, int cubeY, int cubeZ, boolean allowGeneration) throws GenerationNotAllowedException {
        return GenerationNotAllowedException.uncheckedThrowIfNull(this.cubes.get(new CubePos(cubeX, cubeY, cubeZ), allowGeneration).join());
    }

    protected Stream<ICube> multiGetCubes(@NonNull Stream<CubePos> cubePositions, boolean allowGeneration) throws GenerationNotAllowedException {
        //collect all futures into an array first in order to issue all tasks at once before blocking, thus ensuring maximum parallelism
        LazyFutureTask<ICube>[] cubeFutures = uncheckedCast(cubePositions.map(pos -> this.cubes.get(pos, allowGeneration)).toArray(LazyFutureTask[]::new));

        return LazyFutureTask.scatterGather(cubeFutures).stream()
                .peek(GenerationNotAllowedException.uncheckedThrowIfNull());
    }

    /**
     * {@link IAsyncCache} for columns.
     *
     * @author DaPorkchop_
     */
    protected class ColumnCache extends AsyncCacheNBTBase<ChunkPos, Object, IColumn> {
        @Override
        protected IColumn parseNBT(@NonNull ChunkPos key, @NonNull Object param, @NonNull NBTTagCompound nbt) {
            ICubeIO.PartialData<Chunk> data = new ICubeIO.PartialData<>(null, nbt);
            CCExactFBlockWorldHolder1_12.this.io.loadColumnAsyncPart(data, key.x, key.z);
            return (IColumn) data.getObject();
        }

        @Override
        @SneakyThrows(IOException.class)
        protected IColumn loadFromDisk(@NonNull ChunkPos key, @NonNull Object param) {
            ICubeIO.PartialData<Chunk> data = CCExactFBlockWorldHolder1_12.this.io.loadColumnNbt(key.x, key.z);
            CCExactFBlockWorldHolder1_12.this.io.loadColumnAsyncPart(data, key.x, key.z);
            return (IColumn) data.getObject();
        }

        @Override
        protected void triggerGeneration(@NonNull ChunkPos key, @NonNull Object param) {
            //spin until the generator reports that it's ready
            while (CCExactFBlockWorldHolder1_12.this.generator.pollAsyncColumnGenerator(key.x, key.z) != ICubeGenerator.GeneratorReadyState.READY) {
                PorkUtil.sleep(1L);
            }

            //load and immediately save column on server thread
            ((IMixinWorldServer) CCExactFBlockWorldHolder1_12.this.world).fp2_farWorldServer().fp2_IFarWorld_workerManager().workExecutor().run(() -> {
                Chunk column = ((ICubicWorldServer) CCExactFBlockWorldHolder1_12.this.world)
                        .getCubeCache().getColumn(key.x, key.z, ICubeProviderServer.Requirement.POPULATE);
                if (column != null && !column.isEmpty()) {
                    CCExactFBlockWorldHolder1_12.this.io.saveColumn(column);
                    CCExactFBlockWorldHolder1_12.this.generatedCount.incrementAndGet();
                }
            }).join();
        }
    }

    /**
     * {@link IAsyncCache} for cubes.
     *
     * @author DaPorkchop_
     */
    protected class CubeCache extends AsyncCacheNBTBase<CubePos, Chunk, ICube> {
        @Override
        protected Chunk getParamFor(@NonNull CubePos key, boolean allowGeneration) {
            Object o = CCExactFBlockWorldHolder1_12.this.columns.get(key.chunkPos(), allowGeneration).join();
            return (Chunk) o;
        }

        @Override
        protected ICube parseNBT(@NonNull CubePos key, @NonNull Chunk param, @NonNull NBTTagCompound nbt) {
            ICubeIO.PartialData<ICube> data = new ICubeIO.PartialData<>(null, nbt);
            CCExactFBlockWorldHolder1_12.this.io.loadCubeAsyncPart(data, param, key.getY());
            return data.getObject() != null && data.getObject().isInitialLightingDone() ? data.getObject() : null;
        }

        @Override
        @SneakyThrows(IOException.class)
        protected ICube loadFromDisk(@NonNull CubePos key, @NonNull Chunk param) {
            ICubeIO.PartialData<ICube> data = CCExactFBlockWorldHolder1_12.this.io.loadCubeNbt(param, key.getY());
            CCExactFBlockWorldHolder1_12.this.io.loadCubeAsyncPart(data, param, key.getY());
            return data.getObject() != null && data.getObject().isInitialLightingDone() ? data.getObject() : null;
        }

        @Override
        protected void triggerGeneration(@NonNull CubePos key, @NonNull Chunk param) {
            //spin until the generator reports that it's ready
            while (CCExactFBlockWorldHolder1_12.this.generator.pollAsyncCubeGenerator(key.getX(), key.getY(), key.getZ()) != ICubeGenerator.GeneratorReadyState.READY
                   || CCExactFBlockWorldHolder1_12.this.generator.pollAsyncCubePopulator(key.getX(), key.getY(), key.getZ()) != ICubeGenerator.GeneratorReadyState.READY) {
                PorkUtil.sleep(1L);
            }

            ((IMixinWorldServer) CCExactFBlockWorldHolder1_12.this.world).fp2_farWorldServer().fp2_IFarWorld_workerManager().workExecutor().run(() -> {
                //TODO: save column as well if needed
                ICube cube = ((ICubicWorldServer) CCExactFBlockWorldHolder1_12.this.world)
                        .getCubeCache().getCube(key.getX(), key.getY(), key.getZ(), ICubeProviderServer.Requirement.LIGHT);
                if (cube != null && cube.isInitialLightingDone()) {
                    CCExactFBlockWorldHolder1_12.this.io.saveCube((Cube) cube);
                    CCExactFBlockWorldHolder1_12.this.generatedCount.incrementAndGet();
                }
            }).join();
        }

        @Override
        protected ICube bakeValue(@NonNull CubePos key, @NonNull Chunk param, @NonNull ICube value) {
            //override per-cube biomes if necessary
            IBiomeAccess biomeAccess = value instanceof Cube && ((Cube) value).getBiomeArray() != null
                    ? new CubeBiomeAccessWrapper(((Cube) value).getBiomeArray())
                    : new Column2dBiomeAccessWrapper(value.getColumn().getBiomeArray());

            return new CubeWithoutWorld(PorkUtil.fallbackIfNull(value.getStorage(), CCExactFBlockWorldHolder1_12.this.emptyStorage), biomeAccess, key);
        }
    }
}