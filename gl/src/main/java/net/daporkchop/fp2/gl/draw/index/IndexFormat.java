/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.gl.draw.index;

import lombok.SneakyThrows;
import net.daporkchop.fp2.common.GlobalProperties;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.util.AbstractTypedFormat;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * @author DaPorkchop_
 */
public abstract class IndexFormat extends AbstractTypedFormat {
    /**
     * Gets an {@link IndexFormat} for the given index type.
     *
     * @param type the index type
     * @return an {@link IndexFormat}
     */
    @SneakyThrows
    public static IndexFormat get(IndexType type) {
        return (IndexFormat) MethodHandles.publicLookup()
                .findStatic(
                        GlobalProperties.find(IndexFormat.class, "format").getClass("factory"),
                        "get",
                        MethodType.methodType(IndexFormat.class, IndexType.class))
                .invokeExact(type);
    }

    private final IndexType type;

    protected IndexFormat(IndexType type) {
        super(type.size());
        this.type = type;
    }

    /**
     * @return the type of index stored by this format
     */
    public final IndexType type() {
        return this.type;
    }

    /**
     * Creates a new {@link IndexBuffer} for storing indices using this index format.
     *
     * @param gl the OpenGL context to create the buffer in
     * @return the created {@link IndexBuffer}
     */
    public abstract IndexBuffer createBuffer(OpenGL gl);

    /**
     * Creates a new {@link IndexWriter} for writing indices using this index format.
     *
     * @param alloc a {@link DirectMemoryAllocator} to use for allocating memory
     * @return the created {@link IndexWriter}
     */
    public abstract IndexWriter createWriter(DirectMemoryAllocator alloc);
}
