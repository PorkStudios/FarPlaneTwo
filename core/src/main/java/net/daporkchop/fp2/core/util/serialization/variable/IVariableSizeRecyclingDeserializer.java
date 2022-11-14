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

package net.daporkchop.fp2.core.util.serialization.variable;

import lombok.NonNull;
import net.daporkchop.lib.binary.stream.DataIn;

import java.io.IOException;

/**
 * Decodes {@link T} instances from a binary representation. Serialized values have a variable size, which can be auto-detected during deserialization. When deserializing a
 * value, an existing instance of {@link T} is re-used and its state is overwritten.
 *
 * @author DaPorkchop_
 */
public interface IVariableSizeRecyclingDeserializer<T> {
    /**
     * @return the maximum size of a serialized position, in bytes
     */
    long maxSize();

    /**
     * Loads the position from the given {@link DataIn} instance into a {@link T} instance.
     *
     * @param value the {@link T} instance whose contents are to be overwritten with the deserialized data
     * @param in    the {@link DataIn} to read from
     */
    void load(@NonNull T value, @NonNull DataIn in) throws IOException;
}
