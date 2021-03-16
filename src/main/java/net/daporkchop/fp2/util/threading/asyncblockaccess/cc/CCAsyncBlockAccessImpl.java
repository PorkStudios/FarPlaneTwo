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

package net.daporkchop.fp2.util.threading.asyncblockaccess.cc;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.netty.util.concurrent.ImmediateEventExecutor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.compat.vanilla.IBlockHeightAccess;
import net.daporkchop.fp2.server.GlobalNBTWriteCache;
import net.daporkchop.fp2.util.reference.WeakLongKeySelfRemovingReference;
import net.daporkchop.fp2.util.reference.WeakSelfRemovingReference;
import net.daporkchop.fp2.util.threading.LazyRunnablePFuture;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.concurrent.PFuture;
import net.daporkchop.lib.concurrent.PFutures;
import net.daporkchop.lib.primitive.lambda.LongObjObjFunction;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.concurrent.LongObjConcurrentHashMap;
import net.daporkchop.lib.primitive.map.concurrent.ObjObjConcurrentHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import java.lang.ref.Reference;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Default implementation of {@link AsyncBlockAccess} for vanilla worlds.
 *
 * @author DaPorkchop_
 */
public class CCAsyncBlockAccessImpl implements AsyncBlockAccess {
    protected final WorldServer world;
    protected final ICubeIO io;

    protected final LongObjMap<Object> cache2d = new LongObjConcurrentHashMap<>();
    protected final Map<CubePos, Object> cache3d = new ObjObjConcurrentHashMap<>();

    public CCAsyncBlockAccessImpl(@NonNull WorldServer world) {
        this.world = world;
        this.io = ((ICubeProviderInternal.Server) ((ICubicWorldInternal) world).getCubeCache()).getCubeIO();
    }

    protected IColumn getColumn(int chunkX, int chunkZ) {
        class State implements LongObjObjFunction<Object, Object> {
            Object column;

            @Override
            public Object apply(long pos, Object column) {
                if (column instanceof Reference) {
                    this.column = PorkUtil.<Reference<IColumn>>uncheckedCast(column).get();
                } else if (column instanceof LazyRunnablePFuture) {
                    this.column = column;
                }

                if (this.column != null) { //column object or future was obtained successfully, keep existing value in cache
                    return column;
                } else { //column isn't loaded or was garbage collected, we need to load it anew
                    return this.column = new ColumnLoadingLazyFuture(pos);
                }
            }

            public IColumn resolve() {
                if (this.column instanceof IColumn) {
                    return (IColumn) this.column;
                } else if (this.column instanceof LazyRunnablePFuture) {
                    LazyRunnablePFuture<IColumn> future = uncheckedCast(this.column);
                    future.run();
                    return future.join();
                }

                throw new IllegalStateException(Objects.toString(this.column));
            }
        }

        State state = new State();
        this.cache2d.compute(BinMath.packXY(chunkX, chunkZ), state);
        return state.resolve();
    }

    protected PFuture<IColumn> getColumnFuture(int chunkX, int chunkZ) {
        class State implements LongObjObjFunction<Object, Object> {
            Object column;

            @Override
            public Object apply(long pos, Object column) {
                if (column instanceof Reference) {
                    this.column = PorkUtil.<Reference<IColumn>>uncheckedCast(column).get();
                } else if (column instanceof LazyRunnablePFuture) {
                    this.column = column;
                }

                if (this.column != null) { //column object or future was obtained successfully, keep existing value in cache
                    return column;
                } else { //column isn't loaded or was garbage collected, we need to load it anew
                    return this.column = new ColumnLoadingLazyFuture(pos);
                }
            }

            public PFuture<IColumn> resolve() {
                if (this.column instanceof IColumn) {
                    return PFutures.successful((IColumn) this.column, ImmediateEventExecutor.INSTANCE);
                } else if (this.column instanceof LazyRunnablePFuture) {
                    LazyRunnablePFuture<IColumn> future = uncheckedCast(this.column);
                    future.run();
                    return future;
                }

                throw new IllegalStateException(Objects.toString(this.column));
            }
        }

        State state = new State();
        this.cache2d.compute(BinMath.packXY(chunkX, chunkZ), state);
        return state.resolve();
    }

    protected ICube getCube(int cubeX, int cubeY, int cubeZ) {
        class State extends CubePos implements BiFunction<CubePos, Object, Object> {
            Object cube;

            public State(int cubeX, int cubeY, int cubeZ) {
                super(cubeX, cubeY, cubeZ);
            }

            @Override
            public Object apply(CubePos pos, Object cube) {
                if (cube instanceof Reference) {
                    this.cube = PorkUtil.<Reference<ICube>>uncheckedCast(cube).get();
                } else if (cube instanceof LazyRunnablePFuture) {
                    this.cube = cube;
                }

                if (this.cube != null) { //cube object or future was obtained successfully, keep existing value in cache
                    return cube;
                } else { //cube isn't loaded or was garbage collected, we need to load it anew
                    return this.cube = new CubeLoadingLazyFuture(pos);
                }
            }

            public ICube resolve() {
                if (this.cube instanceof ICube) {
                    return (ICube) this.cube;
                } else if (this.cube instanceof LazyRunnablePFuture) {
                    LazyRunnablePFuture<ICube> future = uncheckedCast(this.cube);
                    future.run();
                    return future.join();
                }

                throw new IllegalStateException(Objects.toString(this.cube));
            }
        }

        State state = new State(cubeX, cubeY, cubeZ);
        this.cache3d.compute(state, state);
        return state.resolve();
    }

    protected PFuture<ICube> getCubeFuture(int cubeX, int cubeY, int cubeZ) {
        class State extends CubePos implements BiFunction<CubePos, Object, Object> {
            Object cube;

            public State(int cubeX, int cubeY, int cubeZ) {
                super(cubeX, cubeY, cubeZ);
            }

            @Override
            public Object apply(CubePos pos, Object cube) {
                if (cube instanceof Reference) {
                    this.cube = PorkUtil.<Reference<ICube>>uncheckedCast(cube).get();
                } else if (cube instanceof LazyRunnablePFuture) {
                    this.cube = cube;
                }

                if (this.cube != null) { //cube object or future was obtained successfully, keep existing value in cache
                    return cube;
                } else { //cube isn't loaded or was garbage collected, we need to load it anew
                    return this.cube = new CubeLoadingLazyFuture(pos);
                }
            }

            public PFuture<ICube> resolve() {
                if (this.cube instanceof ICube) {
                    return PFutures.successful((ICube) this.cube, ImmediateEventExecutor.INSTANCE);
                } else if (this.cube instanceof LazyRunnablePFuture) {
                    LazyRunnablePFuture<ICube> future = uncheckedCast(this.cube);
                    future.run();
                    return future;
                }

                throw new IllegalStateException(Objects.toString(this.cube));
            }
        }

        State state = new State(cubeX, cubeY, cubeZ);
        this.cache3d.compute(state, state);
        return state.resolve();
    }

    @Override
    public PFuture<IBlockHeightAccess> prefetchAsync(@NonNull Stream<ChunkPos> columns) {
        return PFutures.mergeToList(columns.map(pos -> this.getColumnFuture(pos.x, pos.z)).collect(Collectors.toList()))
                .thenApply(columnList -> new PrefetchedColumnsCCAsyncBlockAccess(this, this.world, columnList.stream()));
    }

    @Override
    public PFuture<IBlockHeightAccess> prefetchAsync(@NonNull Stream<ChunkPos> columns, @NonNull Function<IBlockHeightAccess, Stream<Vec3i>> cubesMappingFunction) {
        return PFutures.mergeToList(columns.map(pos -> this.getColumnFuture(pos.x, pos.z)).collect(Collectors.toList()))
                .thenCompose(columnList -> {
                    IBlockHeightAccess tempColumnsAccess = new PrefetchedColumnsCCAsyncBlockAccess(this, this.world, columnList.stream());
                    return PFutures.mergeToList(cubesMappingFunction.apply(tempColumnsAccess)
                            .map(pos -> this.getCubeFuture(pos.getX(), pos.getY(), pos.getZ())).collect(Collectors.toList()))
                            .thenApply(cubeList -> new PrefetchedCubesCCAsyncBlockAccess(this, this.world, columnList.stream(), cubeList.stream()));
                });
    }

    @Override
    public int getTopBlockY(int blockX, int blockZ) {
        return this.getColumn(blockX >> 4, blockZ >> 4).getOpacityIndex().getTopBlockY(blockX & 0xF, blockZ & 0xF);
    }

    @Override
    public int getTopBlockYBelow(int blockX, int blockY, int blockZ) {
        return this.getColumn(blockX >> 4, blockZ >> 4).getOpacityIndex().getTopBlockYBelow(blockX & 0xF, blockY, blockZ & 0xF);
    }

    @Override
    public int getBlockLight(BlockPos pos) {
        if (!this.world.isValid(pos)) {
            return 0;
        } else {
            return this.getCube(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4).getLightFor(EnumSkyBlock.BLOCK, pos);
        }
    }

    @Override
    public int getSkyLight(BlockPos pos) {
        if (!this.world.provider.hasSkyLight()) {
            return 0;
        } else if (!this.world.isValid(pos)) {
            return 15;
        } else {
            return this.getCube(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4).getLightFor(EnumSkyBlock.SKY, pos);
        }
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return this.getCube(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4).getBlockState(pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return this.getCube(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4).getBiome(pos);
    }

    @Override
    public WorldType getWorldType() {
        return this.world.getWorldType();
    }

    /**
     * Lazily loads columns.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class ColumnLoadingLazyFuture extends LazyRunnablePFuture<IColumn> {
        protected final long pos;

        @Override
        protected IColumn run0() throws Exception {
            NBTTagCompound nbt = GlobalNBTWriteCache.INSTANCE.getColumnData(CCAsyncBlockAccessImpl.this.world, BinMath.unpackX(this.pos), BinMath.unpackY(this.pos));

            ICubeIO.PartialData<Chunk> data;
            if (nbt == null) { //not in write cache, load it from disk
                data = CCAsyncBlockAccessImpl.this.io.loadColumnNbt(BinMath.unpackX(this.pos), BinMath.unpackY(this.pos));
            } else { //re-use cached nbt
                data = new ICubeIO.PartialData<>(null, nbt);
            }

            CCAsyncBlockAccessImpl.this.io.loadColumnAsyncPart(data, BinMath.unpackX(this.pos), BinMath.unpackY(this.pos));
            return (IColumn) data.getObject();
        }

        @Override
        protected void postRun() {
            //swap out self in cache with a weak reference to self
            CCAsyncBlockAccessImpl.this.cache2d.replace(this.pos, this, new WeakLongKeySelfRemovingReference<>(this.getNow(), this.pos, CCAsyncBlockAccessImpl.this.cache2d));
        }
    }

    /**
     * Lazily loads cubes.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    protected class CubeLoadingLazyFuture extends LazyRunnablePFuture<ICube> {
        @NonNull
        protected final CubePos pos;

        @Override
        protected ICube run0() throws Exception {
            Chunk column = (Chunk) CCAsyncBlockAccessImpl.this.getColumn(this.pos.getX(), this.pos.getZ());
            if (column == null) { //a cube cannot exist without a column, so we assume that the cube is non-existent as well
                return null;
            }

            NBTTagCompound nbt = GlobalNBTWriteCache.INSTANCE.getCubeData(CCAsyncBlockAccessImpl.this.world, this.pos.getX(), this.pos.getY(), this.pos.getZ());

            ICubeIO.PartialData<ICube> data;
            if (nbt == null) { //not in write cache, load it from disk
                data = CCAsyncBlockAccessImpl.this.io.loadCubeNbt(column, this.pos.getY());
            } else { //re-use cached nbt
                data = new ICubeIO.PartialData<>(null, nbt);
            }

            CCAsyncBlockAccessImpl.this.io.loadCubeAsyncPart(data, column, this.pos.getY());
            return data.getObject();
        }

        @Override
        protected void postRun() {
            //swap out self in cache with a weak reference to self
            CCAsyncBlockAccessImpl.this.cache3d.replace(this.pos, this, new WeakSelfRemovingReference<>(this.getNow(), this.pos, CCAsyncBlockAccessImpl.this.cache3d));
        }
    }
}
