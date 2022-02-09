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

package net.daporkchop.fp2.gl.opengl.attribute.struct.property;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayTransform;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayType;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.FieldsAsArrayAttribute;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarExpand;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.convert.IntegerToFloatConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.convert.IntegerToNormalizedFloatConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.convert.IntegerToUnsignedIntegerConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.convert.UnpackingConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.input.FieldsAsArrayInputProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.input.ScalarInputProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.input.SimplePrimitiveArrayInputProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.input.StructInputProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.transform.ArrayToMatrixTransformProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.transform.ArrayToVectorArrayTransformProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.transform.ArrayToVectorTransformProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.transform.IntToARGBExpansionTransformProperty;
import net.daporkchop.lib.common.util.PorkUtil;
import org.objectweb.asm.Type;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class StructPropertyFactory {
    public static StructProperty struct(@NonNull Options options, @NonNull Class<?> struct, @NonNull Map<String, String> nameOverrides) {
        Map<String, Field> fieldsByName = Stream.of(struct.getFields())
                .filter(field -> (field.getModifiers() & Modifier.STATIC) == 0) //skip static fields
                .collect(Collectors.toMap(Field::getName, Function.identity(), (a, b) -> {
                            throw new IllegalArgumentException(a + " " + b);
                        },
                        LinkedHashMap::new));

        nameOverrides = new HashMap<>(nameOverrides);
        Multimap<Integer, Map.Entry<String, StructProperty>> propertiesSorted = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);

        TOP:
        while (!fieldsByName.isEmpty()) {
            for (Iterator<Field> itr = fieldsByName.values().iterator(); itr.hasNext(); ) {
                Field field = itr.next();

                Attribute attribute;
                String name;
                StructProperty property;

                if (field.isAnnotationPresent(Attribute.class)) {
                    itr.remove();

                    attribute = field.getAnnotation(Attribute.class);
                    name = field.getName();
                    property = attributeFromField(options, field);
                } else if (field.isAnnotationPresent(FieldsAsArrayAttribute.class)) {
                    FieldsAsArrayAttribute fieldsAsArrayAttribute = field.getAnnotation(FieldsAsArrayAttribute.class);

                    attribute = fieldsAsArrayAttribute.attribute();
                    name = attribute.name();
                    checkArg(!name.isEmpty(), "%s on %s must set an attribute name!", FieldsAsArrayAttribute.class, field);

                    String[] names = fieldsAsArrayAttribute.names();
                    checkArg(names.length > 0, "%s on %s must have at least one field name!", FieldsAsArrayAttribute.class, field);
                    checkArg(Arrays.asList(names).contains(field.getName()), "%s on %s must include name of annotated field (%s) (given: %s)", FieldsAsArrayAttribute.class, field, field.getName(), Arrays.asList(names));

                    Field[] fields = Stream.of(names)
                            .map(n -> {
                                Field f = fieldsByName.remove(n);
                                checkArg(f != null, "no field \"%s\" could be found!", n);
                                return f;
                            })
                            .toArray(Field[]::new);

                    property = arrayTransform(options, new FieldsAsArrayInputProperty(options, fields, fieldsAsArrayAttribute.scalarType()), fieldsAsArrayAttribute.transform());
                } else {
                    continue;
                }

                //use attribute name from annotation if set
                if (!attribute.name().isEmpty()) {
                    name = attribute.name();
                }

                name = PorkUtil.fallbackIfNull(nameOverrides.remove(name), name);
                propertiesSorted.put(attribute.sort(), new AbstractMap.SimpleEntry<>(name, property));
                continue TOP;
            }

            throw new IllegalArgumentException("no annotated fields remain: " + fieldsByName);
        }

        nameOverrides.forEach((oldName, newName) -> checkArg(false, "cannot rename attribute %s to %s: no such attribute %1$s", oldName, newName));

        return new StructInputProperty(Type.getInternalName(struct), propertiesSorted.values().toArray(uncheckedCast(new Map.Entry[0])));
    }

    public static StructProperty attributeFromField(@NonNull Options options, @NonNull Field field) {
        AnnotatedType annotatedType = field.getAnnotatedType();

        if (annotatedType instanceof AnnotatedArrayType) {
            AnnotatedArrayType annotatedArrayType = (AnnotatedArrayType) annotatedType;
            AnnotatedType componentType = annotatedArrayType.getAnnotatedGenericComponentType();

            ArrayType arrayType = annotatedArrayType.getAnnotation(ArrayType.class);
            checkArg(arrayType != null, "%s must be annotated with %s", field, ArrayType.class);

            StructProperty.Elements property;
            if (componentType.getType() instanceof Class && ((Class<?>) componentType.getType()).isPrimitive()) {
                property = new SimplePrimitiveArrayInputProperty(options, field, arrayType.length());
            } else {
                throw new UnsupportedOperationException("don't know how to process type: " + annotatedArrayType.getType());
            }

            return arrayTransform(options, property, arrayType.transform());
        } else {
            StructProperty.Components rawProperty = new ScalarInputProperty(field);

            return annotatedType.isAnnotationPresent(ScalarType.class)
                    ? scalarType(options, rawProperty, annotatedType.getAnnotation(ScalarType.class))
                    : rawProperty;
        }
    }

    public static StructProperty processAnnotated(@NonNull Options options, @NonNull StructProperty property, @NonNull AnnotatedType annotatedType) {
        if (annotatedType instanceof AnnotatedArrayType) {
            throw new UnsupportedOperationException("don't know how to process type: " + annotatedType.getType());
        } else {
            return annotatedType.isAnnotationPresent(ScalarType.class)
                    ? scalarType(options, (StructProperty.Components) property, annotatedType.getAnnotation(ScalarType.class))
                    : property;
        }
    }

    public static StructProperty.Components scalarType(@NonNull Options options, @NonNull StructProperty.Components property, @NonNull ScalarType scalarType) {
        StructProperty.Components converted = scalarConvert(options, property, scalarType.convert());
        StructProperty.Components expanded = scalarExpand(options, converted, scalarType.expand());

        return options.unpacked() ? new UnpackingConversionProperty(expanded) : expanded;
    }

    public static StructProperty.Components scalarConvert(@NonNull Options options, @NonNull StructProperty.Components property, @NonNull ScalarConvert... conversions) {
        for (ScalarConvert conversion : conversions) {
            switch (conversion.value()) {
                case TO_UNSIGNED:
                    //we still want to apply this stage even if the value is packed(((, and it's safe because it will always be the first conversion used)))
                    property = new IntegerToUnsignedIntegerConversionProperty(property);
                    break;
                case TO_FLOAT:
                    if (conversion.normalized()) {
                        property = options.unpacked()
                                ? new IntegerToNormalizedFloatConversionProperty.Real(property)
                                : new IntegerToNormalizedFloatConversionProperty.Fake(property);
                    } else {
                        property = options.unpacked()
                                ? new IntegerToFloatConversionProperty.Real(property)
                                : new IntegerToFloatConversionProperty.Fake(property);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown conversion: " + conversion);
            }
        }
        return property;
    }

    public static StructProperty.Components scalarExpand(@NonNull Options options, @NonNull StructProperty.Components property, @NonNull ScalarExpand... expansions) {
        for (ScalarExpand expansion : expansions) {
            switch (expansion.value()) {
                case INT_ARGB8_TO_BYTE_VECTOR_RGBA:
                    property = new IntToARGBExpansionTransformProperty(property, expansion.alpha());
                    break;
                default:
                    throw new IllegalArgumentException("unknown expansion: " + expansion);
            }

            //run any subsequent conversions on the resulting components
            property = scalarConvert(options, property, expansion.thenConvert());
        }
        return property;
    }

    public static StructProperty arrayTransform(@NonNull Options options, @NonNull StructProperty.Elements property, @NonNull ArrayTransform... transforms) {
        StructProperty transformedProperty = property;
        for (ArrayTransform transform : transforms) {
            property = (StructProperty.Elements) transformedProperty;

            switch (transform.value()) {
                case TO_MATRIX:
                    checkArg(transform.matrixCols() >= 0, "matrixCols must be set!");
                    checkArg(transform.matrixRows() >= 0, "matrixRows must be set!");

                    transformedProperty = new ArrayToMatrixTransformProperty(property, transform.matrixCols(), transform.matrixRows());
                    break;
                case TO_VECTOR:
                    transformedProperty = new ArrayToVectorTransformProperty(property);
                    break;
                case TO_VECTOR_ARRAY:
                    checkArg(transform.vectorComponents() >= 0, "vectorComponents must be set!");

                    transformedProperty = new ArrayToVectorArrayTransformProperty(property, transform.vectorComponents());
                    break;
                default:
                    throw new IllegalArgumentException("unknown transform: " + transform);
            }
        }
        return transformedProperty;
    }

    /**
     * @author DaPorkchop_
     */
    @Builder
    @Data
    @With
    public static final class Options {
        @Builder.Default
        private final boolean unpacked = false;
    }
}
