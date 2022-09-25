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

package net.daporkchop.fp2.gl.attribute;

import net.daporkchop.fp2.common.util.capability.CloseableResource;

/**
 * A buffer in client memory which is used for building sequences of attribute data.
 *
 * @author DaPorkchop_
 */
public interface AttributeWriter<S> extends CloseableResource {
    /**
     * @return the {@link AttributeFormat} used by this writer
     */
    AttributeFormat<S> format();

    /**
     * @return the number of vertices written so far
     */
    int size();

    /**
     * @return the writer's current position
     */
    int position();

    /**
     * @return the {@link S} instance at the current position
     */
    S current();

    /**
     * @return a {@link S} instance for struct data to be written to
     */
    S append();

    /**
     * Copies the attribute values at the given source index to the given destination index.
     *
     * @param src the source index
     * @param dst the destination index
     */
    AttributeWriter<S> copy(int src, int dst);

    /**
     * Copies the attribute values at the given source index to the given destination index.
     *
     * @param src the source index
     * @param dst the destination index
     */
    AttributeWriter<S> copy(int src, int dst, int length);
}
