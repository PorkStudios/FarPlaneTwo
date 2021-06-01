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

package net.daporkchop.fp2.client.gl.vertex;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.lib.common.math.PMath;

import java.util.Deque;
import java.util.LinkedList;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * A group of {@link IVertexAttribute}s.
 *
 * @author DaPorkchop_
 */
@Getter
public class VertexFormat {
    protected final IVertexAttribute[] attributes;
    protected final int size; //the combined size of all of the vertex attributes, in bytes

    public VertexFormat(@NonNull IVertexAttribute attribute, int alignment) {
        //compute total size of a vertex
        this.size = PMath.roundUp(attribute.offset() + attribute.size(), positive(alignment, "alignment"));

        //enumerate all vertex attributes in the chain
        Deque<IVertexAttribute> attributes = new LinkedList<>();
        do {
            attributes.addFirst(attribute);
        } while ((attribute = attribute.parent()) != null);
        this.attributes = attributes.toArray(new IVertexAttribute[0]);
    }

    /**
     * Appends this vertex attribute to the given {@link VertexArrayObject} configuration.
     *
     * @param vao    the {@link VertexArrayObject}
     * @param buffer the {@link IGLBuffer} that will contain the vertex data
     */
    public void configureVAO(@NonNull VertexArrayObject vao, @NonNull IGLBuffer buffer) {
        for (IVertexAttribute attribute : this.attributes) {
            attribute.configureVAO(vao, buffer, this.size);
        }
    }

    /**
     * Appends a vertex to the given {@link ByteBuf}.
     * <p>
     * The vertex's contents are undefined.
     *
     * @param buf the {@link ByteBuf} to append the vertex to
     * @return the new vertex's starting offset inside the buffer
     */
    public int appendVertex(@NonNull ByteBuf buf) {
        int writerIndex = buf.ensureWritable(this.size).writerIndex();
        buf.writerIndex(writerIndex + this.size);
        return writerIndex;
    }

    /**
     * Duplicates an existing vertex from appends it to the given {@link ByteBuf}.
     *
     * @param buf the {@link ByteBuf} to duplicate the vertex in
     * @return the new vertex's starting offset inside the buffer
     */
    public int duplicateVertex(@NonNull ByteBuf buf, int srcBaseOffset) {
        int writerIndex = buf.writerIndex();
        buf.writeBytes(buf, srcBaseOffset, this.size);
        return writerIndex;
    }
}
