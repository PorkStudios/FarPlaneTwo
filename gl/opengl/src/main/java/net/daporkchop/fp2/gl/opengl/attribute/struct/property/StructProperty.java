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

package net.daporkchop.fp2.gl.opengl.attribute.struct.property;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLArrayType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLBasicType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLStructType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLTypeFactory;
import org.objectweb.asm.MethodVisitor;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Defines a property in a struct, in such a way as it would be visible in shader code.
 *
 * @author DaPorkchop_
 */
//TODO: rename to AttributeType
public interface StructProperty {
    void with(@NonNull PropertyCallback callback);

    <T> T with(@NonNull TypedPropertyCallback<T> callback);

    GLSLType glslType();

    /**
     * @author DaPorkchop_
     */
    interface PropertyCallback {
        void withComponents(@NonNull Components componentsProperty);

        void withElements(@NonNull Elements elementsProperty);

        void withFields(@NonNull Fields fieldsProperty);
    }

    /**
     * @author DaPorkchop_
     */
    interface TypedPropertyCallback<T> {
        T withComponents(@NonNull Components componentsProperty);

        T withElements(@NonNull Elements elementsProperty);

        T withFields(@NonNull Fields fieldsProperty);
    }

    /**
     * @author DaPorkchop_
     */
    interface Components extends StructProperty {
        @Override
        default void with(@NonNull PropertyCallback callback) {
            callback.withComponents(this);
        }

        @Override
        default <T> T with(@NonNull TypedPropertyCallback<T> callback) {
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

        @Deprecated
        default void load(@NonNull MethodVisitor mv, int structLvtIndex, int lvtIndexAllocator, @NonNull LoadCallback callback) {
            throw new UnsupportedOperationException(this.getClass().getTypeName());
        }

        /**
         * @author DaPorkchop_
         */
        @Deprecated
        @FunctionalInterface
        interface LoadCallback {
            @Deprecated
            void accept(int structLvtIndex, int lvtIndexAllocator, @NonNull Loader loader);
        }

        /**
         * @author DaPorkchop_
         */
        @Deprecated
        @FunctionalInterface
        interface Loader {
            @Deprecated
            void load(int structLvtIndex, int lvtIndexAllocator, int componentIndex);
        }
    }

    /**
     * @author DaPorkchop_
     */
    interface Elements extends StructProperty, Iterable<StructProperty> {
        @Override
        default void with(@NonNull PropertyCallback callback) {
            callback.withElements(this);
        }

        @Override
        default <T> T with(@NonNull TypedPropertyCallback<T> callback) {
            return callback.withElements(this);
        }

        int elements();

        StructProperty element(int elementIndex);

        @Override
        default GLSLArrayType glslType() {
            return GLSLTypeFactory.array(this.element(0).glslType(), this.elements());
        }

        @Override
        default Iterator<StructProperty> iterator() {
            return IntStream.range(0, this.elements()).mapToObj(this::element).iterator();
        }

        @Deprecated
        default void load(@NonNull MethodVisitor mv, int structLvtIndex, int lvtIndexAllocator, @NonNull LoadCallback callback) {
            throw new UnsupportedOperationException(this.getClass().getTypeName());
        }

        /**
         * @author DaPorkchop_
         */
        @FunctionalInterface
        interface LoadCallback {
            @Deprecated
            void accept(int structLvtIndex, int lvtIndexAllocator);
        }
    }

    /**
     * @author DaPorkchop_
     */
    interface Fields extends StructProperty, Iterable<Map.Entry<String, StructProperty>> {
        @Override
        default void with(@NonNull PropertyCallback callback) {
            callback.withFields(this);
        }

        @Override
        default <T> T with(@NonNull TypedPropertyCallback<T> callback) {
            return callback.withFields(this);
        }

        int fields();

        Map.Entry<String, StructProperty> field(int fieldIndex);

        default String fieldName(int fieldIndex) {
            return this.field(fieldIndex).getKey();
        }

        default StructProperty fieldProperty(int fieldIndex) {
            return this.field(fieldIndex).getValue();
        }

        default int fieldNameIndex(@NonNull String fieldName) {
            return IntStream.range(0, this.fields()).filter(i -> fieldName.equals(this.fieldName(i))).findAny().getAsInt();
        }

        @Override
        default Iterator<Map.Entry<String, StructProperty>> iterator() {
            return IntStream.range(0, this.fields()).mapToObj(this::field).iterator();
        }

        String structName();

        @Override
        default GLSLType glslType() {
            ImmutableMap.Builder<String, GLSLType> fields = ImmutableMap.builder();
            for (Map.Entry<String, StructProperty> entry : this) {
                fields.put(entry.getKey(), entry.getValue().glslType());
            }
            return new GLSLStructType(this.structName(), fields.build());
        }

        default void load(@NonNull MethodVisitor mv, int structLvtIndex, int lvtIndexAllocator, @NonNull LoadCallback callback) {
            throw new UnsupportedOperationException(this.getClass().getTypeName());
        }

        /**
         * @author DaPorkchop_
         */
        @FunctionalInterface
        interface LoadCallback {
            void accept(int structLvtIndex, int lvtIndexAllocator);
        }
    }
}
