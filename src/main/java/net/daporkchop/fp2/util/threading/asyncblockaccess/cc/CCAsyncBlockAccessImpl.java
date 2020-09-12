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
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import lombok.NonNull;
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
import net.minecraft.util.math.AxisAlignedBB;
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
import java.util.stream.Stream;

/**
 * Default implementation of {@link AsyncBlockAccess} for vanilla worlds.
 *
 * @author DaPorkchop_
 */
//this makes the assumption that asynchronous read-only access to Chunk is safe, which seems to be the case although i'm not entirely sure. testing needed
public class CCAsyncBlockAccessImpl implements AsyncBlockAccess {
    protected final WorldServer world;

    protected final LongObjMap<Object> cache2d = new LongObjConcurrentHashMap<>();
    protected final LongFunction<PFuture<Chunk>> cacheMiss2d;

    protected final Map<CubePos, Object> cache3d = new ObjObjConcurrentHashMap<>();
    protected final Function<CubePos, PFuture<ICube>> cacheMiss3d;

    public CCAsyncBlockAccessImpl(@NonNull WorldServer world) {
        this.world = world;

        this.cacheMiss2d = l -> {
            PFuture<Chunk> future = PFutures.computeAsync(
                    () -> this.world.getChunk(BinMath.unpackX(l), BinMath.unpackY(l)),
                    ServerThreadExecutor.INSTANCE);
            future.thenAcceptAsync(chunk -> this.cache2d.replace(l, future, chunk)); //replace future with chunk instance in cache, to avoid indirection
            return future;
        };

        this.cacheMiss3d = pos -> {
            PFuture<ICube> future = PFutures.computeAsync(
                    () -> ((ICubicWorld) this.world).getCubeFromCubeCoords(pos),
                    ServerThreadExecutor.INSTANCE);
            future.thenAcceptAsync(cube -> this.cache3d.replace(pos, future, cube));
            return future;
        };
    }

    protected Object fetchChunk(int chunkX, int chunkZ) {
        return this.cache2d.computeIfAbsent(BinMath.packXY(chunkX, chunkZ), this.cacheMiss2d);
    }

    protected Chunk getChunk(int chunkX, int chunkZ) {
        Object o = this.fetchChunk(chunkX, chunkZ);
        if (o instanceof Chunk) {
            return (Chunk) o;
        } else if (o instanceof PFuture) {
            return PorkUtil.<PFuture<Chunk>>uncheckedCast(o).join();
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

    @Override
    public PFuture<IBlockHeightAccess> prefetchAsync(@NonNull Stream<ChunkPos> chunks) {
        return null; //TODO: implement this
    }

    @Override
    public PFuture<IBlockHeightAccess> prefetchAsync(@NonNull Stream<ChunkPos> chunks, @NonNull Function<IBlockHeightAccess, Stream<Vec3i>> cubesMappingFunction) {
        return null; //TODO: implement this
    }

    @Override
    public void gc() {
        this.cache2d.values().removeIf(o -> o instanceof Chunk && !((Chunk) o).isLoaded());
        this.cache3d.values().removeIf(o -> o instanceof ICube && !((ICube) o).isCubeLoaded());
    }

    @Override
    public int getTopBlockY(int blockX, int blockZ) {
        return ((IColumn) this.getChunk(blockX >> 4, blockZ >> 4)).getOpacityIndex().getTopBlockY(blockX & 0xF, blockZ & 0xF);
    }

    @Override
    public int getTopBlockYBelow(int blockX, int blockY, int blockZ) {
        return ((IColumn) this.getChunk(blockX >> 4, blockZ >> 4)).getOpacityIndex().getTopBlockYBelow(blockX & 0xF, blockY, blockZ & 0xF);
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        return (this.getLightFromNeighborsFor(EnumSkyBlock.SKY, pos) << 20)
               | (Math.max(this.getLightFromNeighborsFor(EnumSkyBlock.BLOCK, pos), lightValue) << 4);
    }

    @Override
    public int getBlockLight(BlockPos pos) {
        return this.getCube(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4).getLightFor(EnumSkyBlock.BLOCK, pos);
    }

    @Override
    public int getSkyLight(BlockPos pos) {
        if (!this.world.provider.hasSkyLight()) {
            return 0;
        }
        return this.getCube(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4).getLightFor(EnumSkyBlock.SKY, pos);
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

    public int getLightFromNeighborsFor(EnumSkyBlock type, BlockPos pos) {
        if (!this.world.provider.hasSkyLight() && type == EnumSkyBlock.SKY) {
            return 0;
        } else {
            if (pos.getY() < 0) {
                pos = new BlockPos(pos.getX(), 0, pos.getZ());
            }

            if (!this.world.isValid(pos)) {
                return type.defaultLightValue;
            }
            Chunk chunk = this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk.getBlockState(pos).useNeighborBrightness()) {
                int i1 = this.getLightFor(type, pos.up());
                int i = this.getLightFor(type, pos.east());
                int j = this.getLightFor(type, pos.west());
                int k = this.getLightFor(type, pos.south());
                int l = this.getLightFor(type, pos.north());

                if (i > i1) {
                    i1 = i;
                }
                if (j > i1) {
                    i1 = j;
                }
                if (k > i1) {
                    i1 = k;
                }
                if (l > i1) {
                    i1 = l;
                }

                return i1;
            } else {
                return chunk.getLightFor(type, pos);
            }
        }
    }

    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        if (pos.getY() < 0) {
            pos = new BlockPos(pos.getX(), 0, pos.getZ());
        }

        if (!this.world.isValid(pos)) {
            return type.defaultLightValue;
        } else {
            return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getLightFor(type, pos);
        }
    }
}
