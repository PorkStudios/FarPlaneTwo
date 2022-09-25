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

package net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.transform;

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentType;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Data
public class UnionMethodParameter implements MethodParameter {
    protected final MethodParameter[] delegates;

    protected final ComponentType componentType;

    protected final int components;
    protected final int[] componentsBinarySearchIndex;

    public UnionMethodParameter(@NonNull MethodParameter... delegates) {
        checkArg(delegates.length > 0, "at least one delegate must be given!");

        this.componentType = delegates[0].componentType();

        int components = 0;
        this.componentsBinarySearchIndex = new int[delegates.length];
        for (int i = 0; i < delegates.length; i++) {
            MethodParameter delegate = delegates[i];
            checkArg(delegate.componentType() == this.componentType, "all delegates must have the same component type");

            this.componentsBinarySearchIndex[i] = components;
            components += delegate.components();
        }
        this.components = components;

        this.delegates = delegates.clone();
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
        callback.accept(lvtIndexAllocatorIn, (lvtIndexAllocator, componentIndex) -> {
            checkIndex(this.components(), componentIndex);

            //find the corresponding component in the list
            int delegateIndex = Arrays.binarySearch(this.componentsBinarySearchIndex, componentIndex);
            int mask = delegateIndex >> 31; //branchless implementation of (delegateIndex >= 0 ? delegateIndex : (-(delegateIndex + 1) - 1))
            delegateIndex = (delegateIndex ^ mask) + mask;

            //TODO: generate more optimized code by not exiting the load callback for every component access
            int realComponentIndex = componentIndex - this.componentsBinarySearchIndex[delegateIndex];
            this.delegates[delegateIndex].load(mv, lvtIndexAllocator,
                    (lvtIndexAllocator1, loader) -> loader.load(lvtIndexAllocator1, realComponentIndex));
        });
    }
}
