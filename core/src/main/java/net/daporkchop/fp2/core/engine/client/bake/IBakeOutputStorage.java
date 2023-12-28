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

package net.daporkchop.fp2.core.engine.client.bake;

import lombok.NonNull;
import net.daporkchop.fp2.core.debug.util.DebugStats;
import net.daporkchop.fp2.gl.draw.DrawLayout;
import net.daporkchop.fp2.gl.draw.binding.DrawBinding;
import net.daporkchop.fp2.gl.draw.binding.DrawBindingBuilder;
import net.daporkchop.fp2.gl.draw.list.DrawCommand;
import net.daporkchop.fp2.gl.transform.binding.TransformBindingBuilder;
import net.daporkchop.lib.common.misc.refcount.RefCounted;
import net.daporkchop.lib.common.util.exception.AlreadyReleasedException;

/**
 * Stores data contained in multiple {@link IBakeOutput}.
 *
 * @author DaPorkchop_
 */
public interface IBakeOutputStorage<BO extends IBakeOutput, DB extends DrawBinding, DC extends DrawCommand> extends RefCounted {
    /**
     * @return the number of render passes
     */
    int passes();

    /**
     * Adds the given {@link IBakeOutput} to this storage.
     *
     * @param output the {@link IBakeOutput} to add
     * @return a handle to the bake output
     */
    int add(@NonNull BO output);

    /**
     * Deletes the resources allocated by a previously allocated handle.
     *
     * @param handle the handle returned by {@link #add(IBakeOutput)}
     */
    void delete(int handle);

    /**
     * Gets the draw commands to be used for drawing the data associated with a previously added {@link IBakeOutput}.
     *
     * @param handle the handle returned by {@link #add(IBakeOutput)}
     * @return an array of {@link #passes()} {@link DC}s
     */
    DC[] toDrawCommands(int handle);

    DrawBindingBuilder<DB> createDrawBinding(@NonNull DrawLayout layout, int pass);

    TransformBindingBuilder createSelectionBinding(@NonNull TransformBindingBuilder builder);

    @Override
    int refCnt();

    @Override
    IBakeOutputStorage<BO, DB, DC> retain() throws AlreadyReleasedException;

    @Override
    boolean release() throws AlreadyReleasedException;

    DebugStats.Renderer stats();
}
