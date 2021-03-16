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

package net.daporkchop.fp2.server;

import io.github.opencubicchunks.cubicchunks.api.world.CubeDataEvent;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.server.worldlistener.IWorldChangeListener;
import net.daporkchop.fp2.util.reference.WeakSelfRemovingReference;
import net.daporkchop.lib.primitive.map.concurrent.ObjObjConcurrentHashMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraftforge.event.world.ChunkDataEvent;

import java.lang.ref.Reference;
import java.util.Map;

/**
 * Caches the NBT root tag for columns/cubes that are about to be written.
 * <p>
 * This is to avoid a race condition caused by the fact that {@link ChunkDataEvent.Save} and {@link CubeDataEvent.Save} are fired <strong>before</strong> the NBT data is submitted to
 * the write queue, which means that notifications from {@link IWorldChangeListener} might not be visible in the world's {@link IChunkLoader}/{@link ICubeIO} by the time a processing
 * task for the respective columns/cubes is picked up by an async worker thread.
 * <p>
 * Since this class only uses automatically cleared weak references, the added GC pressure should be negligible.
 *
 * @author DaPorkchop_
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GlobalNBTWriteCache implements IWorldChangeListener {
    public static final GlobalNBTWriteCache INSTANCE = new GlobalNBTWriteCache();

    protected final Map<Object, Reference<NBTTagCompound>> cache = new ObjObjConcurrentHashMap<>();

    @Override
    public void onColumnSaved(@NonNull World world, int columnX, int columnZ, @NonNull NBTTagCompound nbt) {
        ColumnKey key = new ColumnKey(world, columnX, columnZ);
        this.cache.put(key, new WeakSelfRemovingReference<>(nbt, key, this.cache));
    }

    @Override
    public void onCubeSaved(@NonNull World world, int cubeX, int cubeY, int cubeZ, @NonNull NBTTagCompound nbt) {
        CubeKey key = new CubeKey(world, cubeX, cubeY, cubeZ);
        this.cache.put(key, new WeakSelfRemovingReference<>(nbt, key, this.cache));
    }

    /**
     * Gets the cached NBT data for the given column.
     *
     * @param world   the world that the column is in
     * @param columnX the column's X coordinate
     * @param columnZ the column's Z coordinate
     * @return the column's NBT data, or {@code null} if it isn't cached
     */
    public NBTTagCompound getColumnData(@NonNull World world, int columnX, int columnZ) {
        Reference<NBTTagCompound> reference = this.cache.get(new ColumnKey(world, columnX, columnZ));
        return reference != null ? reference.get() : null;
    }

    /**
     * Gets the cached NBT data for the given cube.
     *
     * @param world the world that the cube is in
     * @param cubeX the cube's X coordinate
     * @param cubeY the cube's Y coordinate
     * @param cubeZ the cube's Z coordinate
     * @return the cube's NBT data, or {@code null} if it isn't cached
     */
    public NBTTagCompound getCubeData(@NonNull World world, int cubeX, int cubeY, int cubeZ) {
        Reference<NBTTagCompound> reference = this.cache.get(new CubeKey(world, cubeX, cubeY, cubeZ));
        return reference != null ? reference.get() : null;
    }

    /**
     * Key used to identify columns stored in the NBT cache.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    protected static class ColumnKey {
        @NonNull
        protected final World world;
        protected final int columnX;
        protected final int columnZ;
    }

    /**
     * Key used to identify columns stored in the NBT cache.
     *
     * @author DaPorkchop_
     */
    @RequiredArgsConstructor
    @EqualsAndHashCode
    protected static class CubeKey {
        @NonNull
        protected final World world;
        protected final int cubeX;
        protected final int cubeY;
        protected final int cubeZ;
    }
}
