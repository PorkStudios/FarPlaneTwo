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

package net.daporkchop.fp2.gl.opengl.attribute.struct;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructPropertyFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author DaPorkchop_
 */
@Getter
@EqualsAndHashCode(of = "clazz")
public class StructInfo<S> {
    protected final Class<S> clazz;

    protected final StructProperty property;

    public StructInfo(@NonNull Class<S> clazz) {
        this.clazz = clazz;
        this.property = StructPropertyFactory.struct(clazz);
    }

    public String name() {
        return this.clazz.getSimpleName();
    }

    public List<GLSLField<?>> memberFields() {
        return this.property().with(new StructProperty.TypedPropertyCallback<List<GLSLField<?>>>() {
            @Override
            public List<GLSLField<?>> withComponents(@NonNull StructProperty.Components componentsProperty) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<GLSLField<?>> withElements(@NonNull StructProperty.Elements elementsProperty) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<GLSLField<?>> withFields(@NonNull StructProperty.Fields fieldsProperty) {
                return IntStream.range(0, fieldsProperty.fields()).mapToObj(fieldsProperty::field)
                        .map(entry -> new GLSLField<>(entry.getValue().glslType(), entry.getKey()))
                        .collect(Collectors.toList());
            }
        });
    }
}
