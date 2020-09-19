/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.netty.util.concurrent.ImmediateEventExecutor;
import lombok.NonNull;
import net.daporkchop.fp2.util.Constants;
import net.daporkchop.fp2.util.compat.vanilla.IBlockHeightAccess;
import net.daporkchop.fp2.util.threading.ServerThreadExecutor;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.common.util.PorkUtil;
import net.daporkchop.lib.concurrent.PFuture;
import net.daporkchop.lib.concurrent.PFutures;
import net.daporkchop.lib.primitive.map.LongObjMap;
import net.daporkchop.lib.primitive.map.concurrent.LongObjConcurrentHashMap;
import net.daporkchop.lib.primitive.map.concurrent.ObjObjConcurrentHashMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Default implementation of {@link AsyncBlockAccess} for vanilla worlds.
 *
 * @author DaPorkchop_
 */
//this makes the assumption that asynchronous read-only access to Chunk is safe, which seems to be the case although i'm not entirely sure. testing needed
public class CCAsyncBlockAccessImpl implements AsyncBlockAccess {
    protected final WorldServer world;

    protected final LongObjMap<Object> cache2d = new LongObjConcurrentHashMap<>();
    protected final LongFunction<PFuture<IColumn>> cacheMiss2d;

    protected final Map<CubePos, Object> cache3d = new ObjObjConcurrentHashMap<>();
    protected final Function<CubePos, PFuture<ICube>> cacheMiss3d;

    public CCAsyncBlockAccessImpl(@NonNull WorldServer world) {
        this.world = world;

        this.cacheMiss2d = l -> {
            PFuture<IColumn> future = PFutures.computeAsync(
                    () -> (IColumn) this.world.getChunk(BinMath.unpackX(l), BinMath.unpackY(l)),
                    ServerThreadExecutor.INSTANCE);
            future.thenAccept(column -> this.cache2d.replace(l, future, column)); //replace future with chunk instance in cache, to avoid indirection
            future.thenAccept(column -> Constants.LOGGER.info("loaded column ({},{}}) into cache", column.getX(), column.getZ()));
            return future;
        };

        this.cacheMiss3d = pos -> {
            PFuture<ICube> future = PFutures.computeAsync(
                    () -> ((ICubeProviderServer) ((ICubicWorld) this.world).getCubeCache()).getCube(pos.getX(), pos.getY(), pos.getZ(), ICubeProviderServer.Requirement.LIGHT),
                    ServerThreadExecutor.INSTANCE);
            future.thenAcceptAsync(cube -> this.cache3d.replace(pos, future, cube));
            return future;
        };
    }

    protected Object fetchColumn(int chunkX, int chunkZ) {
        return this.cache2d.computeIfAbsent(BinMath.packXY(chunkX, chunkZ), this.cacheMiss2d);
    }

    protected IColumn getColumn(int chunkX, int chunkZ) {
        Object o = this.fetchColumn(chunkX, chunkZ);
        if (o instanceof IColumn) {
            return (IColumn) o;
        } else if (o instanceof PFuture) {
            return PorkUtil.<PFuture<IColumn>>uncheckedCast(o).join();
        } else {
            throw new RuntimeException(Objects.toString(o));
        }
    }

    protected PFuture<IColumn> getColumnFuture(int chunkX, int chunkZ) {
        Object o = this.fetchColumn(chunkX, chunkZ);
        if (o instanceof IColumn) {
            return PFutures.successful((IColumn) o, ImmediateEventExecutor.INSTANCE);
        } else if (o instanceof PFuture) {
            return uncheckedCast(o);
        } else {
            throw new RuntimeException(Objects.toString(o));
        }
    }

    protected Object fetchCube(int chunkX, int cubeY, int chunkZ) {
        return this.cache3d.computeIfAbsent(new CubePos(chunkX, cubeY, chunkZ), this.cacheMiss3d);
    }

    protected ICube getCube(int chunkX, int cubeY, int chunkZ) {
        Object o = this.fetchCube(chunkX, cubeY, chunkZ);
        if (o instanceof ICube) {
            return (ICube) o;
        } else if (o instanceof PFuture) {
            return PorkUtil.<PFuture<ICube>>uncheckedCast(o).join();
        } else {
            throw new RuntimeException(Objects.toString(o));
        }
    }

    protected PFuture<ICube> getCubeFuture(int chunkX, int cubeY, int chunkZ) {
        Object o = this.fetchCube(chunkX, cubeY, chunkZ);
        if (o instanceof ICube) {
            return PFutures.successful((ICube) o, ImmediateEventExecutor.INSTANCE);
        } else if (o instanceof PFuture) {
            return uncheckedCast(o);
        } else {
            throw new RuntimeException(Objects.toString(o));
        }
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
    public void gc() {
        this.cache2d.values().removeIf(o -> o instanceof Chunk && !((Chunk) o).isLoaded());
        this.cache3d.values().removeIf(o -> o instanceof ICube && !((ICube) o).isCubeLoaded());
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
}
