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

import lombok.NonNull;
import net.daporkchop.fp2.client.gl.object.IGLBuffer;
import net.daporkchop.fp2.client.gl.object.VertexArrayObject;
import net.daporkchop.fp2.client.gl.vertex.buffer.IVertexBuilder;

/**
 * Provides write-only access to a vertex attribute.
 *
 * @author DaPorkchop_
 */
public interface IVertexAttribute {
    /**
     * @return this vertex attribute's index
     */
    int index();

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
     *
     * @param vao    the {@link VertexArrayObject}
     * @param buffer the {@link IGLBuffer} that will contain the vertex data
     * @param offset
     * @param stride
     */
    void configureVAO(@NonNull VertexArrayObject vao, @NonNull IGLBuffer buffer, long offset, int stride);

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
         * Sets this vertex attribute to the given value.
         *
         * @param builder     the {@link IVertexBuilder} containing the vertex
         * @param vertexIndex the index of the vertex
         */
        default void set(@NonNull IVertexBuilder builder, int vertexIndex, int v0) {
            this.set(builder.addressFor(this, vertexIndex), v0);
        }

        /**
         * Sets this vertex attribute to the given value.
         *
         * @param addr the memory address
         */
        void set(long addr, int v0);
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
         * Sets this vertex attribute to the given value.
         *
         * @param builder     the {@link IVertexBuilder} containing the vertex
         * @param vertexIndex the index of the vertex
         */
        default void set(@NonNull IVertexBuilder builder, int vertexIndex, int v0, int v1) {
            this.set(builder.addressFor(this, vertexIndex), v0, v1);
        }

        /**
         * Sets this vertex attribute to the given value.
         *
         * @param addr the memory address
         */
        void set(long addr, int v0, int v1);
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
         * Sets this vertex attribute to the given value.
         *
         * @param builder     the {@link IVertexBuilder} containing the vertex
         * @param vertexIndex the index of the vertex
         */
        default void set(@NonNull IVertexBuilder builder, int vertexIndex, int v0, int v1, int v2) {
            this.set(builder.addressFor(this, vertexIndex), v0, v1, v2);
        }

        /**
         * Sets this vertex attribute to the given value.
         *
         * @param addr the memory address
         */
        void set(long addr, int v0, int v1, int v2);

        /**
         * Sets this vertex attribute to the given RGB value.
         *
         * @param builder     the {@link IVertexBuilder} containing the vertex
         * @param vertexIndex the index of the vertex
         */
        default void setRGB(@NonNull IVertexBuilder builder, int vertexIndex, int val) {
            this.setRGB(builder.addressFor(this, vertexIndex), val);
        }

        /**
         * Sets this vertex attribute to the given RGB value.
         *
         * @param addr the memory address
         */
        default void setRGB(long addr, int val) {
            this.set(addr, val >>> 16, val >>> 8, val);
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
         * Sets this vertex attribute to the given value.
         *
         * @param builder     the {@link IVertexBuilder} containing the vertex
         * @param vertexIndex the index of the vertex
         */
        default void set(@NonNull IVertexBuilder builder, int vertexIndex, int v0, int v1, int v2, int v3) {
            this.set(builder.addressFor(this, vertexIndex), v0, v1, v2, v3);
        }

        /**
         * Sets this vertex attribute to the given value.
         *
         * @param addr the memory address
         */
        void set(long addr, int v0, int v1, int v2, int v3);

        /**
         * Sets this vertex attribute to the given ARGB value.
         *
         * @param builder     the {@link IVertexBuilder} containing the vertex
         * @param vertexIndex the index of the vertex
         */
        default void setARGB(@NonNull IVertexBuilder builder, int vertexIndex, int val) {
            this.setARGB(builder.addressFor(this, vertexIndex), val);
        }

        /**
         * Sets this vertex attribute to the given ARGB value.
         *
         * @param addr the memory address
         */
        default void setARGB(long addr, int val) {
            this.set(addr, val, val >>> 8, val >>> 16, val >>> 24);
        }
    }
}
