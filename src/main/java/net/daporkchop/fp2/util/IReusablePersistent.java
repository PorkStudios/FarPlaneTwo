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

package net.daporkchop.fp2.util;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;

/**
 * A type whose state may be persisted to a binary format, and restored later.
 * <p>
 * The state may also be reset, in order to allow instances to be re-used without reallocation.
 *
 * @author DaPorkchop_
 */
public interface IReusablePersistent {
    /**
     * Resets this instance's contents.
     * <p>
     * After being reset, the instance's state will be the same as if it were newly constructed.
     */
    void reset();

    /**
     * Restores this instance's state from the data in the given {@link ByteBuf}.
     * <p>
     * The instance is implicitly reset before the state is restored.
     *
     * @param src the {@link ByteBuf} to read from
     */
    void read(@NonNull ByteBuf src);

    /**
     * Writes this instance's state to the given {@link ByteBuf}.
     * <p>
     * This method returns a {@code boolean} indicating whether or not this instance was empty. Implementations may choose to treat 0 written bytes
     * differently from being empty, so if {@code true} is returned, the state may be restored later simply by resetting the tile - {@link #read(ByteBuf)}
     * must not be called.
     *
     * @param dst the {@link ByteBuf} to write to
     * @return whether or not this instance is empty
     */
    boolean write(@NonNull ByteBuf dst);
}
