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

package net.daporkchop.fp2.gl.opengl.attribute.struct.property.transform;

import lombok.Getter;
import lombok.NonNull;
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
public class ArrayToMatrixTransformProperty implements StructProperty.Components {
    private final StructProperty.Elements parent;

    private final ComponentType componentType;

    private final int cols;
    private final int rows;

    public ArrayToMatrixTransformProperty(@NonNull StructProperty.Elements parent, int cols, int rows) {
        checkArg(cols >= 2 && cols <= 4 && rows >= 2 && rows <= 4, "cannot create %dx%d matrix", cols, rows);
        checkArg(parent.elements() == cols * rows, "%dx%d matrix requires a parent with %d elements, but found %d", cols, rows, cols * rows, parent.elements());

        this.parent = parent;
        this.cols = cols;
        this.rows = rows;

        //find component type of first element
        this.componentType = parent.element(0).with(new TypedPropertyCallback<ComponentType>() {
            @Override
            public ComponentType withComponents(@NonNull Components componentsProperty) {
                return componentsProperty.componentType();
            }

            @Override
            public ComponentType withElements(@NonNull Elements elementsProperty) {
                throw new IllegalArgumentException("cannot construct matrix if any of the elements is an array!");
            }

            @Override
            public ComponentType withFields(@NonNull Fields fieldsProperty) {
                throw new IllegalArgumentException("cannot construct matrix when one of the elements is a struct!");
            }
        });

        //validate arguments
        for (int i = 0; i < rows * cols; i++) {
            parent.element(0).with(new PropertyCallback() {
                @Override
                public void withComponents(@NonNull Components componentsProperty) {
                    checkArg(componentsProperty.components() == 1, "cannot construct matrix if any of the elements has more than one component!");
                    checkArg(componentsProperty.componentType() == ArrayToMatrixTransformProperty.this.componentType, "cannot construct matrix if the component type is not uniform across all elements! expected: %s, found: %s", ArrayToMatrixTransformProperty.this.componentType, componentsProperty.componentType());
                }

                @Override
                public void withElements(@NonNull Elements elementsProperty) {
                    throw new IllegalArgumentException("cannot construct matrix when one of the elements is an array!");
                }

                @Override
                public void withFields(@NonNull Fields fieldsProperty) {
                    throw new IllegalArgumentException("cannot construct matrix when one of the elements is a struct!");
                }
            });
        }
    }

    @Override
    public GLSLBasicType glslType() {
        return GLSLTypeFactory.mat(this.componentType.glslPrimitive(), this.cols, this.rows);
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int structLvtIndexIn, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
        this.parent.load(mv, structLvtIndexIn, lvtIndexAllocatorIn, (structLvtIndexFromParent, lvtIndexAllocatorFromParent) ->
                callback.accept(structLvtIndexFromParent, lvtIndexAllocatorFromParent, (structLvtIndex, lvtIndexAllocator, componentIndex) ->
                        this.parent.element(componentIndex).with(new PropertyCallback() {
                            @Override
                            public void withComponents(@NonNull Components componentsProperty) {
                                componentsProperty.load(mv, structLvtIndexFromParent, lvtIndexAllocator, (structLvtIndexForLoader, lvtIndexAllocatorForLoader, loader) ->
                                        loader.load(structLvtIndexForLoader, lvtIndexAllocatorForLoader, 0));
                            }

                            //the other two cases are impossible, we assert in the constructor that all elements are component properties

                            @Override
                            public void withElements(@NonNull Elements elementsProperty) {
                                throw new IllegalStateException();
                            }

                            @Override
                            public void withFields(@NonNull Fields fieldsProperty) {
                                throw new IllegalStateException();
                            }
                        })));
    }
}
