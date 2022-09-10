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

/**
 * @author DaPorkchop_
 */
@Getter
public class ArrayToVectorTransformProperty implements StructProperty.Components {
    private final Elements parent;

    private final ComponentType logicalStorageType;
    private final ComponentInterpretation componentInterpretation;

    public ArrayToVectorTransformProperty(@NonNull StructProperty.Elements parent) {
        this.parent = parent;

        //find component type of first element
        StructProperty element0 = parent.element(0);
        this.logicalStorageType = element0.with(new TypedPropertyCallback<ComponentType>() {
            @Override
            public ComponentType withComponents(@NonNull Components componentsProperty) {
                return componentsProperty.logicalStorageType();
            }

            @Override
            public ComponentType withElements(@NonNull Elements elementsProperty) {
                throw new IllegalArgumentException("cannot construct vector if any of the elements is an array!");
            }

            @Override
            public ComponentType withFields(@NonNull Fields fieldsProperty) {
                throw new IllegalArgumentException("cannot construct vector when one of the elements is a struct!");
            }
        });
        this.componentInterpretation = ((StructProperty.Components) element0).componentInterpretation();
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
        return this.parent().elements();
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
