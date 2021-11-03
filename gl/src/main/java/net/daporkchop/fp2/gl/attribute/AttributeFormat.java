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

package net.daporkchop.fp2.gl.attribute;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.instanced.InstancedAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.instanced.InstancedAttributeWriter;
import net.daporkchop.fp2.gl.attribute.uniform.UniformAttributeBuffer;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeBuffer;
import net.daporkchop.fp2.gl.attribute.local.LocalAttributeWriter;

import java.util.Map;

/**
 * A group of {@link Attribute}s which defines their layout in memory.
 *
 * @author DaPorkchop_
 */
public interface AttributeFormat {
    /**
     * @return all of the {@link Attribute}s in this format
     */
    Map<String, Attribute> attribs();

    /**
     * Creates a new {@link UniformAttributeBuffer} using this attribute format.
     *
     * @param usage the {@link BufferUsage} to use for the underlying OpenGL buffers
     * @return a new {@link UniformAttributeBuffer}
     */
    UniformAttributeBuffer createUniformBuffer(@NonNull BufferUsage usage);

    /**
     * Creates a new {@link InstancedAttributeWriter} using this attribute format.
     *
     * @return a new {@link InstancedAttributeWriter}
     */
    InstancedAttributeWriter createInstancedWriter();

    /**
     * Creates a new {@link InstancedAttributeBuffer} using this attribute format.
     *
     * @param usage the {@link BufferUsage} to use for the underlying OpenGL buffers
     * @return a new {@link InstancedAttributeBuffer}
     */
    InstancedAttributeBuffer createInstancedBuffer(@NonNull BufferUsage usage);

    /**
     * Creates a new {@link LocalAttributeWriter} using this attribute format.
     *
     * @return a new {@link LocalAttributeWriter}
     */
    LocalAttributeWriter createLocalWriter();

    /**
     * Creates a new {@link LocalAttributeBuffer} using this attribute format.
     *
     * @param usage the {@link BufferUsage} to use for the underlying OpenGL buffers
     * @return a new {@link LocalAttributeBuffer}
     */
    LocalAttributeBuffer createLocalBuffer(@NonNull BufferUsage usage);
}
