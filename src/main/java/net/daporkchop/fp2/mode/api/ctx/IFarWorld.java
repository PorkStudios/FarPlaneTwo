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

package net.daporkchop.fp2.mode.api.ctx;

import lombok.NonNull;
import net.daporkchop.fp2.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.minecraft.world.World;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Provides access to additional fp2 information in a {@link World}.
 *
 * @author DaPorkchop_
 */
public interface IFarWorld {
    /**
     * @return the tile coordinate limits in this world, indexed by detail level
     */
    IntAxisAlignedBB[] fp2_IFarWorld_coordLimits();

    /**
     * Called when the world is being loaded.
     */
    void fp2_IFarWorld_init();

    /**
     * Called when the world is being unloaded.
     */
    void fp2_IFarWorld_close();

    /**
     * Schedules a task to be run on this world's thread.
     *
     * @param task the task to run
     * @return a {@link CompletableFuture} which will be completed with the result of the task
     * @see ThreadingHelper#scheduleTaskInWorldThread(World, Runnable)
     */
    default CompletableFuture<Void> fp2_IFarWorld_scheduleTask(@NonNull Runnable task) {
        return ThreadingHelper.scheduleTaskInWorldThread(uncheckedCast(this), task);
    }

    /**
     * Schedules a task to be run on this world's thread.
     *
     * @param task the task to run
     * @return a {@link CompletableFuture} which will be completed with the result of the task
     * @see ThreadingHelper#scheduleTaskInWorldThread(World, Supplier)
     */
    default <T> CompletableFuture<T> fp2_IFarWorld_scheduleTask(@NonNull Supplier<T> task) {
        return ThreadingHelper.scheduleTaskInWorldThread(uncheckedCast(this), task);
    }

    /**
     * @return this world's dimension ID
     */
    default int fp2_IFarWorld_dimensionId() {
        return ((World) this).provider.getDimension();
    }
}
