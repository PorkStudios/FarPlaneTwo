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
 *
 */

package net.daporkchop.fp2.api.util;

/**
 * An object that holds some kind of resource until it is closed.
 * <p>
 * Once closed, the object should be considered to be in an unsafe state. Attempting to use it in any way will result in undefined behavior.
 * <p>
 * If not manually closed, a resource will be automatically closed when the instance is garbage-collected. However, since there are no guarantees as to when
 * garbage collection will occur, relying on this behavior can lead to poor performance and lead to resource exhaustion. It is strongly recommended to manually
 * close the resource, either by explicitly invoking the {@link #close()} method or wrapping the object in a {@code try}-with-resources block.
 *
 * @author DaPorkchop_
 */
public interface CloseableResource extends AutoCloseable {
    /**
     * Closes this resource, immediately releasing any internally allocated resources.
     */
    @Override
    void close();
}
