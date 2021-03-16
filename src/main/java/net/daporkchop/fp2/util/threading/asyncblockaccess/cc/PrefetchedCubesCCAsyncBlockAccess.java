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

import io.github.opencubicchunks.cubicchunks.api.util.XYZMap;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import net.daporkchop.fp2.util.threading.asyncblockaccess.AsyncBlockAccess;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import java.util.function.Function;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Implementation of {@link AsyncBlockAccess} returned by {@link AsyncBlockAccess#prefetch(Stream, Function)}.
 *
 * @author DaPorkchop_
 */
public class PrefetchedCubesCCAsyncBlockAccess extends PrefetchedColumnsCCAsyncBlockAccess {
    protected final XYZMap<ICube> cubes = new XYZMap<>(0.75f, 16);

    public PrefetchedCubesCCAsyncBlockAccess(CCAsyncBlockAccessImpl parent, WorldServer world, Stream<IColumn> columns, Stream<ICube> cubes) {
        super(parent, world, columns);

        cubes.forEach(cube -> {
            checkArg(this.cubes.put(cube) == null, "duplicate cube at (%d, %d, %d)", cube.getX(), cube.getY(), cube.getZ());
        });
    }

    @Override
    public int getCombinedLight(BlockPos pos, int defaultBlockLightValue) {
        if (!this.world.isValid(pos))    {
            return this.world.provider.hasSkyLight() ? 0xF << 20 : 0;
        } else {
            ICube cube = this.cubes.get(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            if (cube != null) {
                int skyLight = this.world.provider.hasSkyLight() ? cube.getLightFor(EnumSkyBlock.SKY, pos) : 0;
                int blockLight = cube.getLightFor(EnumSkyBlock.BLOCK, pos);
                return (skyLight << 20) | (blockLight << 4);
            }
            return super.getCombinedLight(pos, defaultBlockLightValue);
        }
    }

    @Override
    public int getBlockLight(BlockPos pos) {
        if (!this.world.isValid(pos))    {
            return 0;
        } else {
            ICube cube = this.cubes.get(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            if (cube != null) {
                return cube.getLightFor(EnumSkyBlock.BLOCK, pos);
            }
            return super.getBlockLight(pos);
        }
    }

    @Override
    public int getSkyLight(BlockPos pos) {
        if (!this.world.provider.hasSkyLight()) {
            return 0;
        } else if (!this.world.isValid(pos))    {
            return 15;
        } else {
            ICube cube = this.cubes.get(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
            if (cube != null) {
                return cube.getLightFor(EnumSkyBlock.SKY, pos);
            }
            return super.getSkyLight(pos);
        }
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        ICube cube = this.cubes.get(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        if (cube != null) {
            return cube.getBlockState(pos);
        }
        return super.getBlockState(pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        ICube cube = this.cubes.get(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        if (cube != null) {
            return cube.getBiome(pos);
        }
        return super.getBiome(pos);
    }
}
