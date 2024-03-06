/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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

package net.daporkchop.fp2.gl.codegen.struct.attribute;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.lib.primitive.lambda.IntObjObjConsumer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
public final class StructAttributeType extends AttributeType {
    public static StructAttributeType create(ImmutableMap<String, ? extends AttributeType> attributes) {
        return new StructAttributeType(attributes);
    }

    public static StructAttributeType create(LinkedHashMap<String, ? extends AttributeType> attributes) {
        return new StructAttributeType(attributes);
    }

    private final String[] names;
    private final AttributeType[] types;

    private StructAttributeType(Map<String, ? extends AttributeType> attributes) {
        this.names = attributes.keySet().toArray(new String[0]);
        this.types = attributes.values().toArray(new AttributeType[0]);
    }

    /**
     * @return the number of fields in this struct type
     */
    public int fieldCount() {
        return this.names.length;
    }

    /**
     * Gets the name of the field with the given index.
     *
     * @param index the field index
     * @return the field's name
     */
    public String fieldName(int index) {
        return this.names[index];
    }

    /**
     * Gets the type of the field with the given index.
     *
     * @param index the field index
     * @return the field's type
     */
    public AttributeType fieldType(int index) {
        return this.types[index];
    }

    /**
     * Gets the index of the field in this struct type with the given name.
     *
     * @param name the field's name
     * @return the corresponding field index
     * @throws NoSuchElementException if there is no field in this struct format with the given name
     */
    public int fieldIndexByName(@NonNull String name) {
        for (int index = 0; index < this.names.length; index++) {
            if (this.names[index].equals(name)) {
                return index;
            }
        }
        throw new NoSuchElementException("no field exists with name: " + name);
    }

    /**
     * Runs the given function on each of the fields in this struct type.
     *
     * @param action the action. For each field in this struct type, the action will be invoked once with the field index, the field name and the field type
     */
    public void forEachField(@NonNull IntObjObjConsumer<String, AttributeType> action) {
        for (int index = 0; index < this.names.length; index++) {
            action.accept(index, this.names[index], this.types[index]);
        }
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", this.getClass().getSimpleName() + '{', "}");
        this.forEachField((index, name, type) -> joiner.add(name + " = " + type));
        return joiner.toString();
    }
}
