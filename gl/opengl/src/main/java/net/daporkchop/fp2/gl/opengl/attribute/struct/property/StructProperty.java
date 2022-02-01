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

package net.daporkchop.fp2.gl.opengl.attribute.struct.property;

import lombok.NonNull;
import org.objectweb.asm.MethodVisitor;

import java.util.Iterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/**
 * @author DaPorkchop_
 */
public interface StructProperty {
    void with(@NonNull PropertyCallback callback);

    /**
     * @author DaPorkchop_
     */
    interface PropertyCallback {
        void withComponents(@NonNull Components componentsProperty);

        void withElements(@NonNull Elements elementsProperty);
    }

    /**
     * @author DaPorkchop_
     */
    interface Components extends StructProperty {
        @Override
        default void with(@NonNull PropertyCallback callback) {
            callback.withComponents(this);
        }

        ComponentType componentType();

        default ComponentInterpretation interpretation() {
            return new ComponentInterpretation(this.componentType(), this.componentType().integer(), false);
        }

        int components();

        void load(@NonNull MethodVisitor mv, int structLvtIndex, int lvtIndexAllocator, @NonNull LoadCallback callback);

        /**
         * @author DaPorkchop_
         */
        @FunctionalInterface
        interface LoadCallback {
            void accept(int structLvtIndex, int lvtIndexAllocator, @NonNull IntConsumer loader);
        }
    }

    /**
     * @author DaPorkchop_
     */
    interface Elements extends StructProperty, Iterable<StructProperty> {
        @Override
        default void with(@NonNull PropertyCallback callback) {
            callback.withElements(this);
        }

        int elements();

        StructProperty element(int elementIndex);

        @Override
        default Iterator<StructProperty> iterator() {
            return IntStream.range(0, this.elements()).mapToObj(this::element).iterator();
        }

        void load(@NonNull MethodVisitor mv, int structLvtIndex, int lvtIndexAllocator, @NonNull LoadCallback callback);

        /**
         * @author DaPorkchop_
         */
        @FunctionalInterface
        interface LoadCallback {
            void accept(int structLvtIndex, int lvtIndexAllocator);
        }
    }

}
