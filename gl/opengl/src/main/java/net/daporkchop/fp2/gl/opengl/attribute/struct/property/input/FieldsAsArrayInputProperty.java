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

package net.daporkchop.fp2.gl.opengl.attribute.struct.property.input;

import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentInterpretation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructPropertyFactory;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
public class FieldsAsArrayInputProperty implements StructProperty.Elements {
    private final StructPropertyFactory.Options options;
    private final Field[] fields;

    public FieldsAsArrayInputProperty(@NonNull StructPropertyFactory.Options options, @NonNull Field[] fields) {
        this.options = options;
        this.fields = fields;

        this.element(0).with(new PropertyCallback() {
            @Override
            public void withComponents(@NonNull Components componentsProperty) {
                int components = componentsProperty.components();
                ComponentInterpretation componentInterpretation = componentsProperty.componentInterpretation();

                for (int i = 1; i < FieldsAsArrayInputProperty.this.elements(); i++) {
                    Field field = fields[i];
                    FieldsAsArrayInputProperty.this.element(i).with(new PropertyCallback() {
                        @Override
                        public void withComponents(@NonNull Components componentsProperty) {
                            checkArg(components == componentsProperty.components(), "mismatched fields in array: %s is incompatible with %s (mismatched component count)", fields[0], field);
                            checkArg(componentInterpretation.equals(componentsProperty.componentInterpretation()), "mismatched fields in array: %s is incompatible with %s (mismatched component interpretation)", fields[0], field);
                        }

                        @Override
                        public void withElements(@NonNull Elements elementsProperty) {
                            throw new IllegalArgumentException("mismatched fields in array: " + fields[0] + " is incompatible with " + field);
                        }

                        @Override
                        public void withFields(@NonNull Fields fieldsProperty) {
                            throw new IllegalArgumentException("mismatched fields in array: " + fields[0] + " is incompatible with " + field);
                        }
                    });
                }
            }

            @Override
            public void withElements(@NonNull Elements elementsProperty) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void withFields(@NonNull Fields fieldsProperty) {
                throw new UnsupportedOperationException();
            }
        });
    }

    @Override
    public int elements() {
        return this.fields.length;
    }

    @Override
    public StructProperty element(int elementIndex) {
        return StructPropertyFactory.attributeFromField(this.options, this.fields[elementIndex]);
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int structLvtIndex, int lvtIndexAllocator, @NonNull LoadCallback callback) {
        callback.accept(structLvtIndex, lvtIndexAllocator);
    }
}
