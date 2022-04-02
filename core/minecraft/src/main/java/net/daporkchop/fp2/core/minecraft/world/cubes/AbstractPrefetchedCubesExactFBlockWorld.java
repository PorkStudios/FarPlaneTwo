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

package net.daporkchop.fp2.core.minecraft.world.cubes;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.lib.common.math.BinMath;
import net.daporkchop.lib.math.vector.Vec3i;
import net.daporkchop.lib.primitive.map.open.ObjObjOpenHashMap;

import java.util.List;
import java.util.Map;

/**
 * Base implementation of an {@link FBlockWorld} which serves a Minecraft-style world made up of cubes.
 * <p>
 * This forms the internal API implementation used by {@link AbstractCubesExactFBlockWorldHolder}. It contains a group of chunks which have been prefetched and can be quickly accessed
 * without blocking.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractPrefetchedCubesExactFBlockWorld<CUBE> implements FBlockWorld {
    private final AbstractCubesExactFBlockWorldHolder<CUBE> holder;
    private final boolean generationAllowed;

    private final int cubeShift;

    private final Map<Vec3i, CUBE> cubes; //TODO: some kind of 3int -> obj hashmap

    private final FGameRegistry registry;

    public AbstractPrefetchedCubesExactFBlockWorld(@NonNull AbstractCubesExactFBlockWorldHolder<CUBE> holder, boolean generationAllowed, @NonNull List<CUBE> cubes) {
        this.holder = holder;
        this.generationAllowed = generationAllowed;

        this.cubeShift = holder.cubeShift();

        this.registry = holder.registry();

        this.cubes = new ObjObjOpenHashMap<>(cubes.size());
        cubes.forEach(cube -> this.cubes.put(this.cubePosition(cube), cube));
    }

    /**
     * Gets the given {@link CUBE}'s position.
     *
     * @param cube the {@link CUBE}
     * @return the {@link CUBE}'s position
     */
    protected abstract Vec3i cubePosition(@NonNull CUBE cube);

    @Override
    public void close() {
        //no-op, all resources are owned by AbstractCubesExactFBlockWorldHolder
    }

    @Override
    public IntAxisAlignedBB dataLimits() {
        return this.holder.bounds();
    }

    @Override
    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.holder.containsAnyData(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public IntAxisAlignedBB guaranteedDataAvailableVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.holder.guaranteedDataAvailableVolume(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Gets the prefetched {@link CUBE} which contains the given voxel position.
     *
     * @param x the voxel position's X coordinate
     * @param y the voxel position's Y coordinate
     * @param z the voxel position's Z coordinate
     * @return the prefetched {@link CUBE}, or {@code null} if the chunk at the given position wasn't prefetched
     */
    protected CUBE getPrefetchedCube(int x, int y, int z) {
        return this.cubes().get(Vec3i.of(x >> this.cubeShift(), y >> this.cubeShift(), z >> this.cubeShift()));
    }

    /**
     * Gets the {@link CUBE} which contains the given voxel position, and attempts to load it from the parent {@link AbstractCubesExactFBlockWorldHolder holder} if it isn't prefetched.
     *
     * @param x the voxel position's X coordinate
     * @param y the voxel position's Y coordinate
     * @param z the voxel position's Z coordinate
     * @return the {@link CUBE}
     * @throws GenerationNotAllowedException if the cube wasn't prefetched, generation is disallowed and the cube is ungenerated
     */
    protected CUBE getOrLoadCube(int x, int y, int z) throws GenerationNotAllowedException {
        CUBE chunk = this.cubes().get(Vec3i.of(x >> this.cubeShift(), y >> this.cubeShift(), z >> this.cubeShift()));

        if (chunk == null) {
            //this instance doesn't have the cube prefetched, try to retrieve it from the holder...
            chunk = this.holder().getCube(x >> this.cubeShift(), y >> this.cubeShift(), z >> this.cubeShift(), this.generationAllowed());

            //don't bother saving the loaded cube into the cache:
            //- we don't want to modify this instance's state, in order to avoid causing future regular block accesses to be succeed when the would otherwise have failed
            //- this is very much an edge case which doesn't necessarily need to be fast
        }

        return chunk;
    }

    @Override
    public int getState(int x, int y, int z) throws GenerationNotAllowedException {
        if (!this.holder.isValidPosition(x, y, z)) { //position is outside world, return 0
            return 0;
        }

        CUBE cube = this.getPrefetchedCube(x, y, z);
        assert cube != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        return this.getState(x, y, z, cube);
    }

    protected abstract int getState(int x, int y, int z, CUBE cube) throws GenerationNotAllowedException;

    @Override
    public int getBiome(int x, int y, int z) throws GenerationNotAllowedException {
        if (!this.holder.isValidPosition(x, y, z)) { //position is outside world, return 0
            return 0;
        }

        CUBE cube = this.getPrefetchedCube(x, y, z);
        assert cube != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        return this.getBiome(x, y, z, cube);
    }

    protected abstract int getBiome(int x, int y, int z, CUBE cube) throws GenerationNotAllowedException;

    @Override
    public byte getLight(int x, int y, int z) throws GenerationNotAllowedException {
        if (!this.holder.isValidPosition(x, y, z)) { //position is outside world, return 0
            return 0;
        }

        CUBE cube = this.getPrefetchedCube(x, y, z);
        assert cube != null : "position outside prefetched area: " + x + ',' + y + ',' + z;

        return this.getLight(x, y, z, cube);
    }

    protected abstract byte getLight(int x, int y, int z, CUBE cube) throws GenerationNotAllowedException;
}
