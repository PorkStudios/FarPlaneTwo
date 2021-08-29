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

package net.daporkchop.fp2.client.gl.vertex.buffer;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.common.misc.refcount.AbstractRefCounted;
import net.daporkchop.lib.unsafe.util.exception.AlreadyReleasedException;

import static java.lang.Math.*;

/**
 * Base implementation of {@link IVertexBuilder}.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class AbstractVertexBuilder<L extends IVertexLayout> extends AbstractRefCounted implements IVertexBuilder {
    @NonNull
    protected final L layout;

    protected int size;
    protected int capacity;

    @Override
    public IVertexBuilder retain() throws AlreadyReleasedException {
        super.retain();
        return this;
    }

    @Override
    protected void doRelease() {
        this.layout.release();
    }

    @Override
    public int appendVertex() {
        int idx = this.size++;
        if (idx == this.capacity) { //we need to grow!
            this.resize(this.capacity = max(multiplyExact(this.capacity, 2), 64));
        }
        return idx;
    }

    protected abstract void resize(int capacity);

    @Override
    public void clear() {
        this.size = 0;
    }
}
