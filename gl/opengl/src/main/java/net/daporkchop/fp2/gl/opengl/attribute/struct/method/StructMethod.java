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

package net.daporkchop.fp2.gl.opengl.attribute.struct.method;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.ComponentType;
import org.objectweb.asm.MethodVisitor;

import java.util.BitSet;
import java.util.function.IntConsumer;

/**
 * @author DaPorkchop_
 */
public interface StructMethod {
    int lvtIndexStart();

    /**
     * @author DaPorkchop_
     */
    interface Setter extends StructMethod {
        <M extends StructLayout.Member<M, C>, C extends StructLayout.Component> void visit(@NonNull MethodVisitor mv, int lvtIndexAllocator, @NonNull M rootMember, @NonNull Callback<M, C> callback);

        /**
         * @author DaPorkchop_
         */
        interface Callback<M extends StructLayout.Member<M, C>, C extends StructLayout.Component> {
            void visitComponentFixed(int lvtIndexAllocator, @NonNull M parent, int localComponentIndex, @NonNull C component, @NonNull ComponentType inputComponentType, @NonNull LoaderCallback componentValueLoader);

            void visitComponentIndexed(int lvtIndexAllocator, @NonNull M parent, @NonNull BitSet possibleLocalComponentIndices, @NonNull LoaderCallback localComponentIndexLoader, @NonNull ComponentType inputComponentType, @NonNull LoaderCallback componentValueLoader);

            void visitChildFixed(int lvtIndexAllocator, @NonNull M parent, int localChildIndex, @NonNull M child, @NonNull ChildCallback<M, C> childCallback);

            void visitChildIndexed(int lvtIndexAllocator, @NonNull M parent, @NonNull BitSet possibleLocalChildIndices, @NonNull LoaderCallback localChildIndexLoader, @NonNull ChildCallback<M, C> childCallback);
        }

        /**
         * @author DaPorkchop_
         */
        @FunctionalInterface
        interface ChildCallback<M extends StructLayout.Member<M, C>, C extends StructLayout.Component> {
            void visitChild(int lvtIndexAllocator, @NonNull Callback<M, C> next);
        }

        /**
         * @author DaPorkchop_
         */
        @FunctionalInterface
        interface LoaderCallback {
            void load(int lvtIndexAllocator);
        }
    }
}
