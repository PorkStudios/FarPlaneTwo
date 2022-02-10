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

package net.daporkchop.fp2.gl.opengl.attribute.struct.property.convert;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentInterpretation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import org.objectweb.asm.MethodVisitor;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class IntegerToFloatConversionProperty {
    /**
     * @author DaPorkchop_
     */
    public static class Real extends AbstractConversionProperty {
        public Real(@NonNull Components parent) {
            super(parent);

            checkArg(parent.componentType().integer(), "not an integer type: %s", parent.componentType());
        }

        @Override
        public ComponentType componentType() {
            return ComponentType.FLOAT;
        }

        @Override
        public ComponentInterpretation componentInterpretation() {
            return super.componentInterpretation().withOutputType(GLSLPrimitiveType.FLOAT).withNormalized(false);
        }

        @Override
        protected void convert(@NonNull MethodVisitor mv) {
            switch (this.parent().componentType()) {
                case UNSIGNED_BYTE:
                case BYTE:
                case UNSIGNED_SHORT:
                case SHORT:
                case INT:
                    mv.visitInsn(I2F);
                    break;
                case UNSIGNED_INT: //unsigned integers need special handling because java doesn't support them...
                    mv.visitInsn(I2L);
                    mv.visitLdcInsn(0xFFFFFFFFL);
                    mv.visitInsn(LAND);
                    mv.visitInsn(L2F);
                    break;
                default:
                    throw new IllegalArgumentException("unknown component type: " + this.parent().componentType());
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class Fake extends AbstractFakeConversionProperty {
        public Fake(@NonNull Components parent) {
            super(parent);
        }

        @Override
        public ComponentInterpretation componentInterpretation() {
            return super.componentInterpretation().withOutputType(GLSLPrimitiveType.FLOAT).withNormalized(false);
        }
    }
}
