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

package net.daporkchop.fp2.gl.opengl.attribute.struct.attribute;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLArrayType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLBasicType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLStructType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLTypeFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

/**
 * Defines a property in a struct, in such a way as it would be visible in shader code.
 *
 * @author DaPorkchop_
 */
public interface AttributeType {
    void with(@NonNull Callback callback);

    <T> T with(@NonNull TypedCallback<T> callback);

    GLSLType glslType();

    /**
     * @author DaPorkchop_
     */
    interface Callback {
        void withComponents(@NonNull Components componentsType);

        void withElements(@NonNull Elements elementsType);

        void withFields(@NonNull Fields fieldsType);
    }

    /**
     * @author DaPorkchop_
     */
    interface TypedCallback<T> {
        T withComponents(@NonNull Components componentsType);

        T withElements(@NonNull Elements elementsType);

        T withFields(@NonNull Fields fieldsType);
    }

    /**
     * @author DaPorkchop_
     */
    interface Components extends AttributeType {
        @Override
        default void with(@NonNull Callback callback) {
            callback.withComponents(this);
        }

        @Override
        default <T> T with(@NonNull TypedCallback<T> callback) {
            return callback.withComponents(this);
        }

        /**
         * @return the component type which the property is expected to be stored as
         */
        ComponentType logicalStorageType();

        default ComponentInterpretation componentInterpretation() {
            return new ComponentInterpretation(this.logicalStorageType().glslPrimitive(), false);
        }

        /**
         * @return the GLSL type formed by the combination of this property's components
         */
        GLSLBasicType glslType();

        /**
         * @return the number of columns this property has. If this property is not a matrix, this value is always {@code 1}
         */
        int cols();

        /**
         * @return the number of rows this property has. If this property is not a matrix, this value is always {@link #components()}
         */
        int rows();

        /**
         * @return the total number of components. Must always be equal to {@code this.cols() * this.rows()}
         */
        default int components() {
            return this.cols() * this.rows();
        }
    }

    /**
     * @author DaPorkchop_
     */
    interface Elements extends AttributeType {
        @Override
        default void with(@NonNull Callback callback) {
            callback.withElements(this);
        }

        @Override
        default <T> T with(@NonNull TypedCallback<T> callback) {
            return callback.withElements(this);
        }

        int elements();

        @Deprecated
        AttributeType element(int elementIndex);

        AttributeType componentType();

        @Override
        default GLSLArrayType glslType() {
            return GLSLTypeFactory.array(this.componentType().glslType(), this.elements());
        }
    }

    /**
     * @author DaPorkchop_
     */
    interface Fields extends AttributeType, Iterable<Map.Entry<String, AttributeType>> {
        @Override
        default void with(@NonNull Callback callback) {
            callback.withFields(this);
        }

        @Override
        default <T> T with(@NonNull TypedCallback<T> callback) {
            return callback.withFields(this);
        }

        int fields();

        Map.Entry<String, AttributeType> field(int fieldIndex);

        default String fieldName(int fieldIndex) {
            return this.field(fieldIndex).getKey();
        }

        default AttributeType fieldProperty(int fieldIndex) {
            return this.field(fieldIndex).getValue();
        }

        default int fieldNameIndex(@NonNull String fieldName) {
            return IntStream.range(0, this.fields()).filter(i -> fieldName.equals(this.fieldName(i))).findAny().orElseThrow(() -> new NoSuchElementException("no such field: '" + fieldName + '\''));
        }

        @Override
        default Iterator<Map.Entry<String, AttributeType>> iterator() {
            return IntStream.range(0, this.fields()).mapToObj(this::field).iterator();
        }

        String structName();

        @Override
        default GLSLType glslType() {
            ImmutableMap.Builder<String, GLSLType> fields = ImmutableMap.builder();
            for (Map.Entry<String, AttributeType> entry : this) {
                fields.put(entry.getKey(), entry.getValue().glslType());
            }
            return new GLSLStructType(this.structName(), fields.build());
        }
    }
}
