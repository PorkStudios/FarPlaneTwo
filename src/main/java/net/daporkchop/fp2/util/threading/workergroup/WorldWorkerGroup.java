/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 DaPorkchop_
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

package net.daporkchop.fp2.util.threading.workergroup;

import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.daporkchop.fp2.util.threading.futureexecutor.FutureExecutor;
import net.daporkchop.lib.common.misc.release.Releasable;
import net.minecraft.world.World;

import java.util.Collection;

/**
 * A group of worker threads in a {@link World}.
 *
 * @author DaPorkchop_
 * @see ThreadingHelper#workerGroupBuilder()
 */
public interface WorldWorkerGroup extends Releasable {
    /**
     * @return the {@link World} that this group belongs to
     */
    World world();

    /**
     * @return the individual worker threads in this group
     */
    Collection<Thread> threads();

    /**
     * @return the {@link FutureExecutor} to be used by this group's threads to execute tasks on the world thread
     */
    FutureExecutor worldExecutor();
}
