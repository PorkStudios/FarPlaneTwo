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

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.ComponentType;
import org.objectweb.asm.MethodVisitor;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
public class IntToARGBExpansionTransformMethodParameter implements MethodParameter {
    private final MethodParameter parent;
    private final UnpackOrder order;
    private final boolean signed;

    public IntToARGBExpansionTransformMethodParameter(@NonNull MethodParameter parent, @NonNull UnpackOrder order, boolean signed) {
        checkArg(parent.componentType() == ComponentType.INT, "parent component type must be %s (given: %s)", ComponentType.INT, parent.componentType());
        checkArg(parent.components() == 1, "parent must have exactly one component (given: %d)", parent.components());

        this.parent = parent;
        this.order = order;
        this.signed = signed;
    }

    @Override
    public ComponentType componentType() {
        return this.signed ? ComponentType.BYTE : ComponentType.UNSIGNED_BYTE;
    }

    @Override
    public int components() {
        return this.order.components();
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
        this.parent.load(mv, lvtIndexAllocatorIn, (lvtIndexAllocatorFromParent, loader) -> {
            int argbLvtIndex = lvtIndexAllocatorFromParent++;

            //load the 0th component (which is an int) and store it in the LVT
            loader.load(lvtIndexAllocatorFromParent, 0);
            mv.visitVarInsn(ISTORE, argbLvtIndex);

            callback.accept(lvtIndexAllocatorFromParent, (lvtIndexAllocator, componentIndex) -> {
                checkIndex(this.order.components(), componentIndex);

                mv.visitVarInsn(ILOAD, argbLvtIndex);

                int shift = this.order.mapComponentIndex(componentIndex) << 3;
                if (shift != 0) { //unsigned shift right if necessary
                    mv.visitLdcInsn(shift);
                    mv.visitInsn(this.signed ? ISHR : IUSHR);
                }
                if (this.signed) {
                    if (shift != 24) { //truncate and sign-extend if needed (not necessary for component at bits [24, 32), as it is already sign-extended during shifting)
                        mv.visitInsn(I2B);
                    }
                } else {
                    if (shift != 24) { //truncate if needed (not necessary for component at bits [24, 32), as all other bits are already discarded during shifting)
                        mv.visitLdcInsn(0xFF);
                        mv.visitInsn(IAND);
                    }
                }
            });
        });
    }

    /**
     * The different orders in which
     */
    @RequiredArgsConstructor
    @Getter
    public enum UnpackOrder {
        ARGB(4) {
            @Override
            protected int mapComponentIndex(int componentIndex) {
                //[0, 1, 2, 3] -> [3, 2, 1, 0]
                return componentIndex ^ 3;
            }
        },
        RGBA(4) {
            @Override
            protected int mapComponentIndex(int componentIndex) {
                //[0, 1, 2, 3] -> [2, 1, 0, 3]
                return (2 - componentIndex) & 3;
            }
        },
        RGB(3) {
            @Override
            protected int mapComponentIndex(int componentIndex) {
                //[0, 1, 2] -> [2, 1, 0]
                return 2 - componentIndex;
            }
        };

        private final int components;

        protected abstract int mapComponentIndex(int componentIndex);
    }
}
