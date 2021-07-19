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

package net.daporkchop.fp2.util.threading.workergroup;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.daporkchop.fp2.util.threading.ThreadingHelper;
import net.daporkchop.lib.common.misc.threadfactory.PThreadFactories;
import net.minecraft.world.World;

import java.util.concurrent.ThreadFactory;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Builder for {@link WorldWorkerGroup} instances.
 *
 * @author DaPorkchop_
 * @see ThreadingHelper#workerGroupBuilder()
 */
@Getter
@Setter
public abstract class WorkerGroupBuilder {
    /**
     * The number of threads that this worker group will use.
     */
    protected int threads = -1;

    /**
     * The {@link ThreadFactory} that will be used for creating this worker group's threads.
     */
    @NonNull
    protected ThreadFactory threadFactory = PThreadFactories.DEFAULT_THREAD_FACTORY;

    /**
     * The {@link World} that this worker group belongs to.
     * <p>
     * This will affect the behavior of the task scheduling methods in {@link ThreadingHelper}.
     */
    @NonNull
    protected World world;

    public WorkerGroupBuilder threads(int threads) {
        this.threads = positive(threads, "threads");
        return this;
    }

    protected void validate() {
        positive(this.threads, "threads");
        checkArg(this.world != null, "world must be set!");
    }

    /**
     * Constructs a new {@link WorldWorkerGroup} using the settings configured in this builder.
     *
     * @param task the root task to be executed by the worker threads
     * @return the constructed {@link WorldWorkerGroup}
     */
    public abstract WorldWorkerGroup build(@NonNull Runnable task);
}
