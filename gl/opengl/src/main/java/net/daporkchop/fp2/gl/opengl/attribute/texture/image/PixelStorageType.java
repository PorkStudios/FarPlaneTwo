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

package net.daporkchop.fp2.gl.opengl.attribute.texture.image;

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.texture.image.PixelFormatChannelType;
import org.objectweb.asm.MethodVisitor;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * The {@code type} parameter of a pixel transfer function defines how many bits each of the components defined by the {@code format} take up. There are two kinds of
 * {@code type} values: values that specify each component as a separate byte value, or values that pack multiple components into a single value.
 *
 * @author DaPorkchop_
 */
public interface PixelStorageType {
    int glType();

    PixelFormatChannelType genericType();

    int components();

    ImmutableList<Integer> bitDepths();

    int totalSizeBytes();

    default void loadAllToLvt(@NonNull MethodVisitor mv, int lvtIndexAllocator, @NonNull int[] dstLvtIndices) {
        checkArg(dstLvtIndices.length == this.components());

        this.load(mv, lvtIndexAllocator, (lvtIndexAllocator1, loader) -> {
            for (int i = 0; i < dstLvtIndices.length; i++) {
                loader.loadComponent(i);
                mv.visitVarInsn(this.genericType() == PixelFormatChannelType.FLOATING_POINT ? FSTORE : ISTORE, dstLvtIndices[i]);
            }
        });
    }

    void load(@NonNull MethodVisitor mv, int lvtIndexAllocator, @NonNull LoadCallback callback);

    /**
     * @author DaPorkchop_
     */
    @FunctionalInterface
    interface LoadCallback {
        void withComponents(int lvtIndexAllocator, @NonNull Loader loader);
    }

    /**
     * @author DaPorkchop_
     */
    @FunctionalInterface
    interface Loader {
        void loadComponent(int componentIndex);
    }

    default void storeAllFromLvt(@NonNull MethodVisitor mv, int lvtIndexAllocator, @NonNull int[] srcLvtIndices) {
        checkArg(srcLvtIndices.length == this.components());

        this.store(mv, lvtIndexAllocator, (lvtIndexAllocator1, storer) -> {
            for (int i = 0; i < srcLvtIndices.length; i++) {
                mv.visitVarInsn(this.genericType() == PixelFormatChannelType.FLOATING_POINT ? FLOAD : ILOAD, srcLvtIndices[i]);
                storer.storeComponent(i);
            }
        });
    }

    void store(@NonNull MethodVisitor mv, int lvtIndexAllocator, @NonNull StoreCallback callback);

    /**
     * @author DaPorkchop_
     */
    @FunctionalInterface
    interface StoreCallback {
        void withComponents(int lvtIndexAllocator, @NonNull Storer storer);
    }

    /**
     * @author DaPorkchop_
     */
    @FunctionalInterface
    interface Storer {
        void storeComponent(int componentIndex);
    }
}
