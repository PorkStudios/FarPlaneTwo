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

package net.daporkchop.fp2.gl.codegen.struct.method.parameter.convert;

import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameter;
import org.objectweb.asm.MethodVisitor;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
public final class IntegerToFloatConvertingParameter extends AbstractConvertingParameter {
    private final boolean normalize;

    public IntegerToFloatConvertingParameter(MethodParameter parent, boolean normalize) {
        super(JavaPrimitiveType.FLOAT, parent);
        checkArg(parent.componentType().integer(), "not an integer type: %s", parent.componentType().integer());
        this.normalize = normalize;
    }

    @Override
    protected void visitConvert(MethodVisitor mv) {
        this.parent().componentType().convertToFloat(mv);

        if (this.normalize) {
            mv.visitLdcInsn(this.parent().componentType().normalizationFactor());
            mv.visitInsn(FMUL);
        }
    }
}
