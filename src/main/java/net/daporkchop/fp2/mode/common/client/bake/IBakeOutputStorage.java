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

package net.daporkchop.fp2.mode.common.client.bake;

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.command.IDrawCommand;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

/**
 * Stores data contained in a {@link IBakeOutputStorage}.
 *
 * @author DaPorkchop_
 */
public interface IBakeOutputStorage<B extends IBakeOutput, C extends IDrawCommand> extends RefCounted {
    /**
     * Adds the given {@link IBakeOutput} to this storage.
     *
     * @param output the {@link IBakeOutput} to add
     * @return a handle to the bake output
     */
    int add(@NonNull B output);

    /**
     * Deletes the resources allocated by a previously allocated
     *
     * @param handle the handle returned by {@link #add(IBakeOutput)}
     */
    void delete(int handle);

    /**
     * Gets the draw commands to be used for drawing the data associated with a previously added {@link IBakeOutput}.
     *
     * @param handle   the handle returned by {@link #add(IBakeOutput)}
     * @param commands an array of {@link C}s to be configured
     */
    void toDrawCommands(int handle, @NonNull C[] commands);

    @Override
    int refCnt();

    @Override
    IBakeOutputStorage<B, C> retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;
}
