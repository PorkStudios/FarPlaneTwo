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

package net.daporkchop.fp2.gl.codegen.struct.method.parameter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;
import net.daporkchop.fp2.gl.codegen.util.LvtAlloc;
import org.objectweb.asm.MethodVisitor;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public abstract class MethodParameter {
    private final JavaPrimitiveType componentType;
    private final int components;

    public abstract void visitLoad(MethodVisitor mv, LvtAlloc lvtAlloc, Consumer<IntConsumer> callback);
}
