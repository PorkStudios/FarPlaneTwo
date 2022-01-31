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

package net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.convert;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.ComponentType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.StructProperty;
import org.objectweb.asm.MethodVisitor;

/**
 * @author DaPorkchop_
 */
@Getter
public class CompositeConversionProperty implements StructProperty.Components {
    private final Components parent;
    private final Components tail;

    public CompositeConversionProperty(@NonNull Components parent, @NonNull Attribute.Conversion[] conversions) {
        this.parent = parent;

        Components tail = parent;
        for (Attribute.Conversion conversion : conversions) {
            switch (conversion) {
                case TO_UNSIGNED:
                    tail = new IntegerToUnsignedIntegerConversionProperty(tail);
                    break;
                case TO_FLOAT:
                    tail = new IntegerToFloatConversionProperty(tail);
                    break;
                case TO_NORMALIZED_FLOAT:
                    tail = new IntegerToNormalizedFloatConversionProperty(tail);
                    break;
                default:
                    throw new IllegalArgumentException("unknown conversion: " + conversion);
            }
        }
        this.tail = tail;
    }

    @Override
    public ComponentType componentType() {
        return this.tail.componentType();
    }

    @Override
    public int components() {
        return this.tail.components();
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int structLvtIndexIn, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
        this.tail.load(mv, structLvtIndexIn, lvtIndexAllocatorIn, callback);
    }
}
