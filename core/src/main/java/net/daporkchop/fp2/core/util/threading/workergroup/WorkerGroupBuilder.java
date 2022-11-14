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

package net.daporkchop.fp2.core.util.threading.workergroup;

import lombok.NonNull;

import java.util.concurrent.ThreadFactory;

/**
 * Builder for constructing {@link WorkerGroup}s.
 *
 * @author DaPorkchop_
 */
public interface WorkerGroupBuilder {
    /**
     * Sets the number of threads which the {@link WorkerGroup} will have.
     *
     * @param threads the number of threads
     */
    WorkerGroupBuilder threads(int threads);

    /**
     * Sets the {@link ThreadFactory} which the {@link WorkerGroup}'s threads will be created by.
     *
     * @param threadFactory the {@link ThreadFactory}
     */
    WorkerGroupBuilder threadFactory(@NonNull ThreadFactory threadFactory);

    /**
     * Builds the {@link WorkerGroup} using the configured settings.
     *
     * @param action the {@link Runnable} which the workers will run
     * @return the constructed {@link WorkerGroup}
     */
    WorkerGroup build(@NonNull Runnable action);
}
