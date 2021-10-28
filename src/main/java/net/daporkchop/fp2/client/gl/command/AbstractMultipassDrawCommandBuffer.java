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

package net.daporkchop.fp2.client.gl.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.DrawMode;
import net.daporkchop.fp2.common.util.alloc.Allocator;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * Base implementation of {@link IMultipassDrawCommandBuffer}.
 *
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractMultipassDrawCommandBuffer<C extends IDrawCommand> extends AbstractRefCounted implements IMultipassDrawCommandBuffer<C> {
    @Getter(AccessLevel.NONE)
    protected final Allocator alloc;
    protected final DrawMode mode;
    protected final int passes;

    protected int capacity;

    protected boolean dirty = false; //extra field for use by implementations, may be ignored if not needed

    public AbstractMultipassDrawCommandBuffer(@NonNull Allocator alloc, int passes, @NonNull DrawMode mode) {
        this.alloc = alloc;
        this.mode = mode;
        this.passes = positive(passes, "passes");
    }

    @Override
    public IMultipassDrawCommandBuffer<C> retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    public void resize(int capacity) {
        this.resize0(this.capacity, this.capacity = notNegative(capacity, "capacity"));
    }

    protected abstract void resize0(int oldCapacity, int newCapacity);
}
