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

package net.daporkchop.fp2.server.worldlistener;

import lombok.NonNull;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Listens for events in a world.
 *
 * @author DaPorkchop_
 */
public interface IWorldChangeListener {
    /**
     * Fired immediately before a column is saved.
     *
     * @param world   the world that the column is in
     * @param columnX the column's X coordinate
     * @param columnZ the column's Z coordinate
     * @param nbt     the column's NBT data
     */
    void onColumnSaved(@NonNull World world, int columnX, int columnZ, @NonNull NBTTagCompound nbt);

    /**
     * Fired immediately before a cube is saved.
     *
     * @param world the world that the cube is in
     * @param cubeX the cube's X coordinate
     * @param cubeY the cube's Y coordinate
     * @param cubeZ the cube's Z coordinate
     * @param nbt   the cube's NBT data
     */
    void onCubeSaved(@NonNull World world, int cubeX, int cubeY, int cubeZ, @NonNull NBTTagCompound nbt);

    /**
     * Fired after a world tick is completed.
     */
    @Deprecated
    default void onTickEnd() {
        //no-op
    }
}
