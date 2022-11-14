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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.exactfblocklevel;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.core.minecraft.util.threading.asynccache.AsyncCacheNBT;
import net.daporkchop.fp2.core.minecraft.world.cubes.AbstractCubesExactFBlockLevelHolder;
import net.daporkchop.fp2.core.minecraft.world.cubes.AbstractPrefetchedCubesExactFBlockLevel;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.TickEndEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.threading.futurecache.IAsyncCache;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.world.IMixinWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.biome.Column2dBiomeAccessWrapper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.biome.CubeBiomeAccessWrapper;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.cc.cube.CubeWithoutWorld;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.IBiomeAccess;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.math.vector.Vec2i;
import net.daporkchop.lib.math.vector.Vec3i;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Default implementation of {@link ExactFBlockLevelHolder} for cubic chunks worlds.
 *
 * @author DaPorkchop_
 */
public class CCExactFBlockLevelHolder1_12 extends AbstractCubesExactFBlockLevelHolder<ICube> {
    protected static final long ASYNCBATCHINGCUBEIO_STORAGE_OFFSET = PUnsafe.pork_getOffset(AsyncBatchingCubeIO.class, "storage");

    protected static ICubeIO getIO(@NonNull IFarLevelServer world) {
        return ((ICubeProviderInternal.Server) ((ICubicWorldInternal) world.implLevel()).getCubeCache()).getCubeIO();
    }

    protected static ICubicStorage getStorage(@NonNull ICubeIO io) {
        //noinspection RedundantClassCall
        return PUnsafe.getObject(AsyncBatchingCubeIO.class.cast(io), ASYNCBATCHINGCUBEIO_STORAGE_OFFSET);
    }

    protected final ICubeGenerator generator;

    private final NDimensionalIntSegtreeSet columnsExistCache;
    private final AsyncCacheNBT<Vec2i, ?, Chunk, ?> columnCache;

    protected final ExtendedBlockStorage emptyStorage;

    protected final AtomicInteger generatedCount = new AtomicInteger();

    public CCExactFBlockLevelHolder1_12(@NonNull IFarLevelServer level) {
        super(level, Integer.numberOfTrailingZeros(Coords.cubeToMinBlock(1)));

        this.generator = ((ICubicWorldServer) level.implLevel()).getCubeGenerator();

        this.emptyStorage = new ExtendedBlockStorage(0, ((WorldServer) level.implLevel()).provider.hasSkyLight());

        this.columnsExistCache = Datastructures.INSTANCE.nDimensionalIntSegtreeSet()
                .dimensions(2)
                .threadSafe(true)
                .initialPoints(() -> {
                    List<int[]> positions = new ArrayList<>();
                    getStorage(getIO(this.world())).forEachColumn(pos -> positions.add(new int[]{ pos.x, pos.z }));
                    return positions.stream();
                })
                .build();
        this.columnCache = new ColumnCache(this.world(), getIO(this.world()));
    }

    @Override
    protected NDimensionalIntSegtreeSet createCubesExistIndex(@NonNull IFarLevelServer world) {
        return Datastructures.INSTANCE.nDimensionalIntSegtreeSet()
                .dimensions(3)
                .threadSafe(true)
                .initialPoints(() -> {
                    List<int[]> positions = new ArrayList<>();
                    getStorage(getIO(world)).forEachCube(pos -> positions.add(new int[]{ pos.getX(), pos.getY(), pos.getZ() }));
                    return positions.stream();
                })
                .build();
    }

    @Override
    protected AsyncCacheNBT<Vec3i, ?, ICube, ?> createCubeCache(@NonNull IFarLevelServer world) {
        return new CubeCache(world, getIO(world));
    }

    @Override
    protected AbstractPrefetchedCubesExactFBlockLevel<ICube> prefetchedWorld(boolean generationAllowed, @NonNull List<ICube> cubes) {
        return new PrefetchedCubesCCFBlockLevel1_12(this, generationAllowed, cubes);
    }

    @Override
    protected void onColumnSaved(ColumnSavedEvent event) {
        //cache the column data for later use
        this.columnCache.notifyUpdate(event.pos(), uncheckedCast(event.data()));

        //the column at the given position was saved, therefore it must exist so we want it in the index
        this.columnsExistCache.add(event.pos().x(), event.pos().y());
    }

    @FEventHandler
    private void onTickEnd(TickEndEvent event) {
        //if 8192 things have been generated, run a full chunk gc!
        //  if we don't do this, the gc will only be triggered when a player has been moving for more than some number of ticks. the player might just be standing still
        //  while waiting for terrain to load in, so we need to do this to prevent a memory leak.
        if (this.generatedCount.get() >= 8192) {
            this.generatedCount.lazySet(0);

            ((ICubicWorldServer) this.world().implLevel()).unloadOldCubes();
        }
    }

    /**
     * {@link IAsyncCache} for columns.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class ColumnCache extends AsyncCacheNBT<Vec2i, Object, Chunk, NBTTagCompound> {
        @NonNull
        protected final IFarLevelServer world;
        @NonNull
        protected final ICubeIO io;

        @Override
        protected Chunk parseNBT(@NonNull Vec2i key, @NonNull Object param, @NonNull NBTTagCompound nbt) {
            ICubeIO.PartialData<Chunk> data = new ICubeIO.PartialData<>(null, nbt);
            this.io.loadColumnAsyncPart(data, key.x(), key.y());
            return data.getObject();
        }

        @Override
        @SneakyThrows(IOException.class)
        protected Chunk loadFromDisk(@NonNull Vec2i key, @NonNull Object param) {
            ICubeIO.PartialData<Chunk> data = this.io.loadColumnNbt(key.x(), key.y());
            this.io.loadColumnAsyncPart(data, key.x(), key.y());
            return data.getObject();
        }

        @Override
        protected void triggerGeneration(@NonNull Vec2i key, @NonNull Object param) {
            //spin until the generator reports that it's ready
            while (CCExactFBlockLevelHolder1_12.this.generator.pollAsyncColumnGenerator(key.x(), key.y()) != ICubeGenerator.GeneratorReadyState.READY) {
                PorkUtil.sleep(1L);
            }

            //load and immediately save column on server thread
            this.world.workerManager().workExecutor().run(() -> {
                Chunk column = ((ICubicWorldServer) this.world.implLevel())
                        .getCubeCache().getColumn(key.x(), key.y(), ICubeProviderServer.Requirement.POPULATE);
                if (column != null && !column.isEmpty()) {
                    this.io.saveColumn(column);
                    CCExactFBlockLevelHolder1_12.this.generatedCount.incrementAndGet();
                }
            }).join();
        }
    }

    /**
     * {@link IAsyncCache} for cubes.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class CubeCache extends AsyncCacheNBT<Vec3i, Chunk, ICube, NBTTagCompound> {
        @NonNull
        protected final IFarLevelServer world;
        @NonNull
        protected final ICubeIO io;

        @Override
        protected Chunk getParamFor(@NonNull Vec3i key, boolean allowGeneration) {
            return CCExactFBlockLevelHolder1_12.this.columnCache.get(Vec2i.of(key.x(), key.z()), allowGeneration).join();
        }

        @Override
        protected ICube parseNBT(@NonNull Vec3i key, @NonNull Chunk param, @NonNull NBTTagCompound nbt) {
            ICubeIO.PartialData<ICube> data = new ICubeIO.PartialData<>(null, nbt);
            this.io.loadCubeAsyncPart(data, param, key.y());
            return data.getObject() != null && data.getObject().isInitialLightingDone() ? data.getObject() : null;
        }

        @Override
        @SneakyThrows(IOException.class)
        protected ICube loadFromDisk(@NonNull Vec3i key, @NonNull Chunk param) {
            ICubeIO.PartialData<ICube> data = this.io.loadCubeNbt(param, key.y());
            this.io.loadCubeAsyncPart(data, param, key.y());
            return data.getObject() != null && data.getObject().isInitialLightingDone() ? data.getObject() : null;
        }

        @Override
        protected void triggerGeneration(@NonNull Vec3i key, @NonNull Chunk param) {
            //spin until the generator reports that it's ready
            while (CCExactFBlockLevelHolder1_12.this.generator.pollAsyncCubeGenerator(key.x(), key.y(), key.z()) != ICubeGenerator.GeneratorReadyState.READY
                   || CCExactFBlockLevelHolder1_12.this.generator.pollAsyncCubePopulator(key.x(), key.y(), key.z()) != ICubeGenerator.GeneratorReadyState.READY) {
                PorkUtil.sleep(1L);
            }

            this.world.workerManager().workExecutor().run(() -> {
                //TODO: save column as well if needed
                ICube cube = ((ICubicWorldServer) this.world.implLevel())
                        .getCubeCache().getCube(key.x(), key.y(), key.z(), ICubeProviderServer.Requirement.LIGHT);
                if (cube != null && cube.isInitialLightingDone()) {
                    this.io.saveCube((Cube) cube);
                    CCExactFBlockLevelHolder1_12.this.generatedCount.incrementAndGet();
                }
            }).join();
        }

        @Override
        protected ICube bakeValue(@NonNull Vec3i key, @NonNull Chunk param, @NonNull ICube value) {
            //override per-cube biomes if necessary
            IBiomeAccess biomeAccess = value instanceof Cube && ((Cube) value).getBiomeArray() != null
                    ? new CubeBiomeAccessWrapper(((Cube) value).getBiomeArray())
                    : new Column2dBiomeAccessWrapper(value.getColumn().getBiomeArray());

            return new CubeWithoutWorld(PorkUtil.fallbackIfNull(value.getStorage(), CCExactFBlockLevelHolder1_12.this.emptyStorage), biomeAccess, new CubePos(key.x(), key.y(), key.z()));
        }
    }
}
