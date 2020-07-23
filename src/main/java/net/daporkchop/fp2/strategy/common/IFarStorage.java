/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
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

package net.daporkchop.fp2.strategy.common;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * Handles reading and writing of far terrain data.
 *
 * @author DaPorkchop_
 */
public interface IFarStorage<POS extends IFarPos> extends Closeable {
    /**
     * Loads the data at the given position.
     *
     * @param pos the position of the data to load
     * @return the loaded data, or {@code null} if it doesn't exist
     */
    ByteBuf load(@NonNull POS pos);

    /**
     * Stores the given data at the given position, atomically replacing any existing data.
     *
     * @param pos  the position to save the data at
     * @param data the data to save
     */
    void store(@NonNull POS pos, @NonNull ByteBuf data);

    /**
     * Closes this storage.
     * <p>
     * If write operations are queued, this method will block until they are completed.
     */
    @Override
    void close() throws IOException;
}
