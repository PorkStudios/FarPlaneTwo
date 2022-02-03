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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.Transform;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.convert.IntegerToFloatConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.convert.IntegerToNormalizedFloatConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.convert.IntegerToUnsignedIntegerConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.input.SimplePrimitiveArrayInputProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.input.StructInputProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.input.VectorComponentsInputProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.transform.ArrayToMatrixTransformProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.transform.IntToARGBExpansionTransformProperty;
import net.daporkchop.lib.common.util.PorkUtil;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
                .collect(Collectors.toMap(Field::getName, Function.identity(), (a, b) -> {
                            throw new IllegalArgumentException(a + " " + b);
                        },
                        LinkedHashMap::new));

        nameOverrides = new HashMap<>(nameOverrides);
        Multimap<Integer, Map.Entry<String, StructProperty>> propertiesSorted = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);

        while (!fieldsByName.isEmpty()) {
            Field field = fieldsByName.values().stream()
                    .filter(f -> f.getAnnotation(Attribute.class) != null)
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("no annotated fields remain: " + fieldsByName));

            String name = field.getName();
            Attribute attribute = field.getAnnotation(Attribute.class);

            List<Field> fields;
            String attributeName = name;
            if (attribute.vectorAxes().length == 0) {
                fieldsByName.remove(name);
                fields = ImmutableList.of(field);
                attributeName = PorkUtil.fallbackIfNull(nameOverrides.remove(name), name);
            } else {
                String[] vectorAxes = attribute.vectorAxes();
                checkArg(name.endsWith(vectorAxes[0]), "name doesn't end in %s: %s", vectorAxes[0], field);

                String baseName = name.substring(0, name.length() - vectorAxes[0].length());
                attributeName = PorkUtil.fallbackIfNull(nameOverrides.remove(baseName), baseName);

                fields = Stream.of(vectorAxes)
                        .map(axisSuffix -> {
                            Field componentField = fieldsByName.remove(baseName + axisSuffix);
                            checkArg(componentField != null, "no such field: %s%s", baseName, axisSuffix);
                            return componentField;
                        })
                        .collect(Collectors.toList());
            }

            propertiesSorted.put(attribute.sort(), new AbstractMap.SimpleEntry<>(attributeName, attribute(options, attribute, fields)));
        }

        nameOverrides.forEach((oldName, newName) -> checkArg(false, "cannot rename attribute %s to %s: no such attribute %1$s", oldName, newName));

        return new StructInputProperty(Type.getInternalName(struct), propertiesSorted.values().toArray(uncheckedCast(new Map.Entry[0])));
    }

    public static StructProperty attribute(@NonNull Options options, @NonNull Attribute attribute, @NonNull List<Field> fields) {
        Class<?> type = fields.get(0).getType();

        //ensure all fields have the same type
        fields.forEach(field -> checkArg(field.getType() == type, "all fields must have the same type (expected: %s, mismatch at %s)", type, field));

        //input
        StructProperty initialProperty;
        if (type.isPrimitive()) { //primitive
            initialProperty = new VectorComponentsInputProperty(fields.toArray(new Field[0]));
        } else if (type.isArray() && type.getComponentType().isPrimitive()) {
            checkArg(attribute.arrayLength().length > 0, "arrayLength must be set!");
            checkArg(attribute.arrayLength().length == 1, "multi-dimensional arrays are not supported!");

            initialProperty = new SimplePrimitiveArrayInputProperty(options, fields.get(0), attribute.arrayLength()[0]);
        } else {
            throw new IllegalArgumentException("don't know how to handle attribute of type " + type);
        }

        if (initialProperty instanceof StructProperty.Components) {
            return convertComponents(options, attribute.convert(), (StructProperty.Components) transformComponents(options, attribute.transform(), (StructProperty.Components) initialProperty));
        } else if (initialProperty instanceof StructProperty.Elements) {
            //TODO: this can't deal with conversions
            return transformElements(options, attribute.transform(), (StructProperty.Elements) initialProperty);
        } else {
            throw new IllegalStateException("don't know what to do with " + initialProperty);
        }
    }

    public static StructProperty transformComponents(@NonNull Options options, @NonNull Transform[] transforms, @NonNull StructProperty.Components property) {
        for (int i = 0; i < transforms.length; i++) {
            Transform transform = transforms[i];

            StructProperty transformedProperty;
            switch (transform.value()) {
                case INT_ARGB8_TO_BYTE_VECTOR_RGB:
                case INT_ARGB8_TO_BYTE_VECTOR_RGBA:
                    transformedProperty = new IntToARGBExpansionTransformProperty(property, transform.value() == Transform.Type.INT_ARGB8_TO_BYTE_VECTOR_RGBA);
                    break;
                default:
                    throw new UnsupportedOperationException("transform unknown or not applicable to component type: " + transform.value());
            }

            if (transformedProperty instanceof StructProperty.Components) {
                property = (StructProperty.Components) transformedProperty;
            } else if (transformedProperty instanceof StructProperty.Elements) {
                return transformElements(options, Arrays.copyOfRange(transforms, i + 1, transforms.length), (StructProperty.Elements) transformedProperty);
            } else {
                throw new IllegalStateException("don't know what to do with " + transformedProperty);
            }
        }

        return property;
    }

    public static StructProperty transformElements(@NonNull Options options, @NonNull Transform[] transforms, @NonNull StructProperty.Elements property) {
        for (int i = 0; i < transforms.length; i++) {
            Transform transform = transforms[i];

            StructProperty transformedProperty;
            switch (transform.value()) {
                case ARRAY_TO_MATRIX:
                    checkArg(transform.matrixCols() >= 0, "matrixCols must be set!");
                    checkArg(transform.matrixRows() >= 0, "matrixRows must be set!");

                    transformedProperty = new ArrayToMatrixTransformProperty(property, transform.matrixCols(), transform.matrixRows());
                    break;
                default:
                    throw new UnsupportedOperationException("transform unknown or not applicable to element type: " + transform.value());
            }

            if (transformedProperty instanceof StructProperty.Elements) {
                property = (StructProperty.Elements) transformedProperty;
            } else if (transformedProperty instanceof StructProperty.Components) {
                return transformComponents(options, Arrays.copyOfRange(transforms, i + 1, transforms.length), (StructProperty.Components) transformedProperty);
            } else {
                throw new IllegalStateException("don't know what to do with " + transformedProperty);
            }
        }

        return property;
    }

    public static StructProperty.Components convertComponents(@NonNull Options options, @NonNull Attribute.Conversion[] conversions, @NonNull StructProperty.Components inputProperty) {
        StructProperty.Components convertedProperty = inputProperty;
        for (Attribute.Conversion conversion : conversions) {
            switch (conversion) {
                case TO_UNSIGNED:
                    //we still want to apply this stage even if the value is packed, and it's safe because it will always be the first conversion used
                    convertedProperty = new IntegerToUnsignedIntegerConversionProperty(convertedProperty);
                    break;
                case TO_FLOAT:
                    if (options.unpacked()) {
                        convertedProperty = new IntegerToFloatConversionProperty.Real(convertedProperty);
                    } else {
                        convertedProperty = new IntegerToFloatConversionProperty.Fake(convertedProperty);
                    }
                    break;
                case TO_NORMALIZED_FLOAT:
                    if (options.unpacked()) {
                        convertedProperty = new IntegerToNormalizedFloatConversionProperty.Real(convertedProperty);
                    } else {
                        convertedProperty = new IntegerToNormalizedFloatConversionProperty.Fake(convertedProperty);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown conversion: " + conversion);
            }
        }

        return convertedProperty;
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
