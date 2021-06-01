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
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.type.Int2_10_10_10_Rev;

/**
 * Provides write-only access to a vertex attribute.
 *
 * @author DaPorkchop_
 */
public interface IVertexAttribute {
    /**
     * @return the offset of this vertex attribute from the vertex base, in bytes
     */
    int offset();

    /**
     * @return the size of this vertex attribute, in bytes
     */
    int size();

    /**
     * @return the number of components in this vertex attribute
     */
    int components();

    /**
     * @return the {@link IVertexAttribute} that appears before this one in the vertex layout, or {@code null} if there is none
     */
    IVertexAttribute parent();

    /**
     * Appends this vertex attribute to the given {@link VertexArrayObject} configuration.
     * @param vao    the {@link VertexArrayObject}
     * @param buffer the {@link IGLBuffer} that will contain the vertex data
     * @param stride
     */
    void configureVAO(@NonNull VertexArrayObject vao, @NonNull IGLBuffer buffer, int stride);

    /**
     * @author DaPorkchop_
     */
    interface Int1 extends IVertexAttribute {
        static VertexAttributeBuilder<Int1> builder() {
            return VertexAttributeBuilder.fromMap(VertexAttributesImpl.FACTORIES_INT1).components(1);
        }

        static VertexAttributeBuilder<Int1> builder(IVertexAttribute parent) {
            return builder().parent(parent);
        }

        @Override
        default int components() {
            return 1;
        }

        /**
         * Sets the this vertex attribute to the given value.
         *
         * @param buf        the {@link ByteBuf} containing the vertex
         * @param vertexBase the index of the beginning of the vertex
         */
        void set(@NonNull ByteBuf buf, int vertexBase, int v0);
    }

    /**
     * @author DaPorkchop_
     */
    interface Int2 extends IVertexAttribute {
        static VertexAttributeBuilder<Int2> builder() {
            return VertexAttributeBuilder.fromMap(VertexAttributesImpl.FACTORIES_INT2).components(2);
        }

        static VertexAttributeBuilder<Int2> builder(IVertexAttribute parent) {
            return builder().parent(parent);
        }

        @Override
        default int components() {
            return 2;
        }

        /**
         * Sets the this vertex attribute to the given value.
         *
         * @param buf        the {@link ByteBuf} containing the vertex
         * @param vertexBase the index of the beginning of the vertex
         */
        void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1);
    }

    /**
     * @author DaPorkchop_
     */
    interface Int3 extends IVertexAttribute {
        static VertexAttributeBuilder<Int3> builder() {
            return VertexAttributeBuilder.fromMap(VertexAttributesImpl.FACTORIES_INT3).components(3);
        }

        static VertexAttributeBuilder<Int3> builder(IVertexAttribute parent) {
            return builder().parent(parent);
        }

        @Override
        default int components() {
            return 3;
        }

        /**
         * Sets the this vertex attribute to the given value.
         *
         * @param buf        the {@link ByteBuf} containing the vertex
         * @param vertexBase the index of the beginning of the vertex
         */
        void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1, int v2);

        /**
         * Sets the this vertex attribute to the given RGB value.
         *
         * @param buf        the {@link ByteBuf} containing the vertex
         * @param vertexBase the index of the beginning of the vertex
         */
        default void setRGB(@NonNull ByteBuf buf, int vertexBase, int val) {
            this.set(buf, vertexBase, val, val >> 8, val >>> 16);
        }
    }

    /**
     * @author DaPorkchop_
     */
    interface Int4 extends IVertexAttribute {
        static VertexAttributeBuilder<Int4> builder() {
            return VertexAttributeBuilder.fromMap(VertexAttributesImpl.FACTORIES_INT4).components(4);
        }

        static VertexAttributeBuilder<Int4> builder(IVertexAttribute parent) {
            return builder().parent(parent);
        }

        @Override
        default int components() {
            return 4;
        }

        /**
         * Sets the this vertex attribute to the given value.
         *
         * @param buf        the {@link ByteBuf} containing the vertex
         * @param vertexBase the index of the beginning of the vertex
         */
        void set(@NonNull ByteBuf buf, int vertexBase, int v0, int v1, int v2, int v3);

        /**
         * Sets the this vertex attribute to the given ARGB value.
         *
         * @param buf        the {@link ByteBuf} containing the vertex
         * @param vertexBase the index of the beginning of the vertex
         */
        default void setARGB(@NonNull ByteBuf buf, int vertexBase, int val) {
            this.set(buf, vertexBase, val, val >>> 8, val >>> 16, val >>> 24);
        }

        /**
         * Sets the this vertex attribute to the given {@link Int2_10_10_10_Rev} value.
         *
         * @param buf        the {@link ByteBuf} containing the vertex
         * @param vertexBase the index of the beginning of the vertex
         */
        default void setInt2_10_10_10_rev(@NonNull ByteBuf buf, int vertexBase, int val) {
            this.set(buf, vertexBase, Int2_10_10_10_Rev.unpackX(val), Int2_10_10_10_Rev.unpackY(val), Int2_10_10_10_Rev.unpackZ(val), Int2_10_10_10_Rev.unpackW(val));
        }

        /**
         * Sets the this vertex attribute to the given {@link Int2_10_10_10_Rev} value.
         *
         * @param buf        the {@link ByteBuf} containing the vertex
         * @param vertexBase the index of the beginning of the vertex
         */
        default void setUnsignedInt2_10_10_10_rev(@NonNull ByteBuf buf, int vertexBase, int val) {
            this.set(buf, vertexBase, Int2_10_10_10_Rev.unpackUnsignedX(val), Int2_10_10_10_Rev.unpackUnsignedY(val), Int2_10_10_10_Rev.unpackUnsignedZ(val), Int2_10_10_10_Rev.unpackUnsignedW(val));
        }
    }
}
