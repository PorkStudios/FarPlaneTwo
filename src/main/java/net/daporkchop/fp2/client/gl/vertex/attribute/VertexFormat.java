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

package net.daporkchop.fp2.client.gl.vertex.attribute;

import lombok.Getter;
import lombok.NonNull;

import java.util.Deque;
import java.util.LinkedList;
import java.util.stream.Stream;

import static net.daporkchop.fp2.util.Constants.*;

/**
 * A group of {@link IVertexAttribute}s.
 *
 * @author DaPorkchop_
 */
@Getter
public class VertexFormat {
    protected final IVertexAttribute[] attributes;
    protected final int vertexSize; //the combined size of all of the vertex attributes, in bytes

    public VertexFormat(@NonNull String name, @NonNull IVertexAttribute attribute) {
        //enumerate all vertex attributes in the chain
        Deque<IVertexAttribute> attributes = new LinkedList<>();
        do {
            attributes.addFirst(attribute);
        } while ((attribute = attribute.parent()) != null);
        this.attributes = attributes.toArray(new IVertexAttribute[0]);

        //add all the sizes together
        this.vertexSize = Stream.of(this.attributes).mapToInt(IVertexAttribute::size).sum();

        FP2_LOG.info("{} vertex size: {} bytes", name, this.vertexSize());
    }
}