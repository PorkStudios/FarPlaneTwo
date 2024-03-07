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

package net.daporkchop.fp2.gl.codegen.struct.method.parameter.transform;

import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameter;
import org.objectweb.asm.MethodVisitor;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public final class UnionTransformParameter extends MethodParameter {
    private final MethodParameter[] delegates;
    private final int[] componentsBinarySearchIndex;

    public UnionTransformParameter(MethodParameter... delegates) {
        super(Arrays.stream(delegates)
                        .map(MethodParameter::componentType).reduce((a, b) -> {
                            checkArg(a == b, "all delegates must have the same component type");
                            return a;
                        }).get(),
                Arrays.stream(delegates).mapToInt(MethodParameter::components).sum());

        this.delegates = delegates.clone();

        this.componentsBinarySearchIndex = new int[delegates.length];
        this.componentsBinarySearchIndex[0] = 0;
        for (int i = 1; i < delegates.length; i++) {
            this.componentsBinarySearchIndex[i] = this.componentsBinarySearchIndex[i - 1] + delegates[i].components();
        }
    }

    @Override
    public void visitLoad(MethodVisitor mv, int[] lvtAlloc, Consumer<IntConsumer> callback) {
        callback.accept(componentIndex -> {
            checkIndex(this.components(), componentIndex);

            //find the corresponding component in the list
            int delegateIndex = Arrays.binarySearch(this.componentsBinarySearchIndex, componentIndex);
            int mask = delegateIndex >> 31; //branchless implementation of (delegateIndex >= 0 ? delegateIndex : (-(delegateIndex + 1) - 1))
            delegateIndex = (delegateIndex ^ mask) + mask;

            //TODO: generate more optimized code by not exiting the load callback for every component access
            int realComponentIndex = componentIndex - this.componentsBinarySearchIndex[delegateIndex];
            this.delegates[delegateIndex].visitLoad(mv, lvtAlloc, loader -> loader.accept(realComponentIndex));
        });
    }
}
