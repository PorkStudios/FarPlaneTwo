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

package net.daporkchop.fp2.gl.opengl.attribute.struct.layout;

import lombok.Data;
import lombok.NonNull;
import lombok.With;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentInterpretation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;
import org.objectweb.asm.MethodVisitor;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * @author DaPorkchop_
 */
@Data
@With
public final class LayoutComponentStorage {
    @Deprecated
    public static LayoutComponentStorage unchanged(@NonNull StructProperty.Components property) {
        return new LayoutComponentStorage(property.logicalStorageType(), property.logicalStorageType(), property.componentInterpretation());
    }

    public static LayoutComponentStorage unpacked(@NonNull StructProperty.Components property) {
        return new LayoutComponentStorage(property.logicalStorageType(), property.logicalStorageType().unpackedestForm(), property.componentInterpretation());
    }

    @NonNull
    private final ComponentType logicalStorageType;
    @NonNull
    private final ComponentType physicalStorageType;
    @NonNull
    private final ComponentInterpretation interpretation;

    public GLSLPrimitiveType glslInterpretedType() {
        return this.interpretation.outputType();
    }

    private void convertComponentTypes(@NonNull MethodVisitor mv, int lvtIndexAllocator, @NonNull ComponentType srcType, @NonNull ComponentType dstType) {
        if (srcType.integer()) {
            if (dstType.integer()) {
                if (!srcType.signed() || !dstType.signed()) { //i'm pretty sure this should never occur in practice
                    throw new UnsupportedOperationException();
                }

                //truncate source type to the destination type's level of precision
                dstType.truncateInteger(mv);
            } else { //need to convert source type to float
                srcType.convertToFloat(mv);
            }
        } else {
            if (dstType.integer()) { //need to convert source type from float
                dstType.convertFromFloat(mv);
            } else {
                //both types are already floats, nothing needs to be done
            }
        }
    }

    public void input2logical(@NonNull MethodVisitor mv, int lvtIndexAllocator, @NonNull ComponentType inputType) {
        this.convertComponentTypes(mv, lvtIndexAllocator, inputType, this.logicalStorageType);
    }

    public void logical2physical(@NonNull MethodVisitor mv, int lvtIndexAllocator) {
        this.convertComponentTypes(mv, lvtIndexAllocator, this.logicalStorageType, this.physicalStorageType);
    }

    public void input2physical(@NonNull MethodVisitor mv, int lvtIndexAllocator, @NonNull ComponentType inputType) {
        //convert to logical type first in order to truncate value to the expected level of precision
        this.input2logical(mv, lvtIndexAllocator, inputType);

        //convert logical type to physical
        this.logical2physical(mv, lvtIndexAllocator);
    }

    public void physical2interpreted(@NonNull MethodVisitor mv, int lvtIndexAllocator) {
        if (this.physicalStorageType.glslPrimitive() == this.glslInterpretedType()) { //no conversion is necessary
            return;
        }

        if (this.physicalStorageType.integer()) {
            if (this.glslInterpretedType().integer()) { //both types are integer types
                checkArg(this.physicalStorageType.signed() == this.glslInterpretedType().signed(), "physical storage type and interpreted type must have the same sign!");

                //no-op
            } else { //interpret as a float
                this.physicalStorageType.convertToFloat(mv);

                if (this.interpretation.normalized()) { //the type needs to be normalized
                    mv.visitLdcInsn(this.physicalStorageType.normalizationFactor());
                    mv.visitInsn(FMUL);
                }
            }
        } else {
            if (this.glslInterpretedType().integer()) {
                throw new IllegalStateException(); //impossible
            } else {
                //both types are already floats, nothing needs to be done
            }
        }
    }
}
