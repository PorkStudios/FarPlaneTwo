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

package net.daporkchop.fp2.gl.opengl.attribute.struct.property.transform;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentInterpretation;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLBasicType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLTypeFactory;
import org.objectweb.asm.MethodVisitor;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class ArrayToVectorArrayTransformProperty implements StructProperty.Elements {
    private final Elements parent;

    private final ComponentType logicalStorageType;
    private final ComponentInterpretation componentInterpretation;

    private final int vectorComponents;

    public ArrayToVectorArrayTransformProperty(@NonNull StructProperty.Elements parent, int vectorComponents) {
        this.parent = parent;
        this.vectorComponents = positive(vectorComponents, "vectorComponents");

        checkArg(parent.elements() % vectorComponents == 0, "vectorComponents (%d) must be a multiple of the array element count (%d)", vectorComponents, parent.elements());

        //ensure element type only has 1 component
        StructProperty element0 = parent.element(0);
        this.logicalStorageType = element0.with(new TypedPropertyCallback<ComponentType>() {
            @Override
            public ComponentType withComponents(@NonNull Components componentsProperty) {
                checkArg(componentsProperty.components() == 1, "cannot construct vectors when the elements have %d compoenents!", componentsProperty.components());

                return componentsProperty.logicalStorageType();
            }

            @Override
            public ComponentType withElements(@NonNull Elements elementsProperty) {
                throw new IllegalArgumentException("cannot construct vectors when one of the elements is a struct!");
            }

            @Override
            public ComponentType withFields(@NonNull Fields fieldsProperty) {
                throw new IllegalArgumentException("cannot construct vectors when one of the elements is a struct!");
            }
        });
        this.componentInterpretation = ((StructProperty.Components) element0).componentInterpretation();
    }

    @Override
    public int elements() {
        return this.parent().elements() / this.vectorComponents();
    }

    @Override
    public StructProperty element(int elementIndex) {
        checkIndex(this.elements(), elementIndex);

        return new StructProperty.Components() {
            @Override
            public ComponentType logicalStorageType() {
                return ArrayToVectorArrayTransformProperty.this.logicalStorageType();
            }

            @Override
            public ComponentInterpretation componentInterpretation() {
                return ArrayToVectorArrayTransformProperty.this.componentInterpretation();
            }

            @Override
            public GLSLBasicType glslType() {
                return GLSLTypeFactory.vec(this.logicalStorageType().glslPrimitive(), this.components());
            }

            @Override
            public int cols() {
                return 1;
            }

            @Override
            public int rows() {
                return ArrayToVectorArrayTransformProperty.this.vectorComponents();
            }

            @Override
            public void load(@NonNull MethodVisitor mv, int structLvtIndexIn, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
                callback.accept(structLvtIndexIn, lvtIndexAllocatorIn, (structLvtIndex, lvtIndexAllocator, componentIndex) -> {
                    int vectorComponents = ArrayToVectorArrayTransformProperty.this.vectorComponents();
                    int parentElementIndex = elementIndex * vectorComponents + checkIndex(vectorComponents, componentIndex);

                    ((Components) ArrayToVectorArrayTransformProperty.this.parent().element(parentElementIndex)).load(mv, structLvtIndex, lvtIndexAllocator, (structLvtIndex1, lvtIndexAllocator1, loader) -> {
                        //each child element has exactly one component
                        loader.load(structLvtIndex1, lvtIndexAllocator1, 0);
                    });
                });
            }
        };
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int structLvtIndexIn, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
        //delegate to parent, no additional translation needed
        this.parent().load(mv, structLvtIndexIn, lvtIndexAllocatorIn, callback);
    }
}
