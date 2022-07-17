/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.core.mode.api;

import net.daporkchop.fp2.core.util.serialization.IConstructingDeserializer;
import net.daporkchop.fp2.core.util.serialization.ISerializer;

/**
 * An {@link ISerializer} specifically for {@link POS tile position}s.
 * <p>
 * All {@link POS tile position}s are the same size when serialized. As such, {@link #maxSize()} is not an upper bound, but the exact size of every serialized position, as
 * returned by {@link #posSize()}. Additionally, all {@link #store} methods will always return the exact value of {@link #posSize()}.
 *
 * @author DaPorkchop_
 */
public interface IFarPosCodec<POS extends IFarPos> extends ISerializer<POS>, IConstructingDeserializer<POS> {
    /**
     * @deprecated positions are always serialized with a fixed size, use {@link #posSize()}
     */
    @Deprecated
    @Override
    default long maxSize() {
        return this.posSize();
    }

    /**
     * @return the size of a position, in bytes
     */
    long posSize();
}
