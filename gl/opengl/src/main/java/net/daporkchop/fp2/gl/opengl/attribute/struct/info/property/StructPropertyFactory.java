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

package net.daporkchop.fp2.gl.opengl.attribute.struct.info.property;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.Transform;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.convert.IntegerToFloatConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.convert.IntegerToNormalizedFloatConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.convert.IntegerToUnsignedIntegerConversionProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.input.StructInputProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.input.VectorComponentsInputProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.info.property.transform.IntToARGBExpansionTransformProperty;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class StructPropertyFactory {
    public static StructProperty struct(@NonNull Options options, @NonNull Class<?> struct) {
        Map<String, Field> fieldsByName = Stream.of(struct.getFields())
                .collect(Collectors.toMap(Field::getName, Function.identity(), (a, b) -> {
                            throw new IllegalArgumentException(a + " " + b);
                        },
                        LinkedHashMap::new));

        Multimap<Integer, StructProperty> propertiesSorted = Multimaps.newListMultimap(new TreeMap<>(), ArrayList::new);

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
            } else {
                String[] vectorAxes = attribute.vectorAxes();
                checkArg(name.endsWith(vectorAxes[0]), "name doesn't end in %s: %s", vectorAxes[0], field);

                String baseName = attributeName = name.substring(0, name.length() - vectorAxes[0].length());

                fields = Stream.of(vectorAxes)
                        .map(axisSuffix -> {
                            Field componentField = fieldsByName.remove(baseName + axisSuffix);
                            checkArg(componentField != null, "no such field: %s%s", baseName, axisSuffix);
                            return componentField;
                        })
                        .collect(Collectors.toList());
            }

            propertiesSorted.put(attribute.sort(), attribute(options, struct, attributeName, attribute, fields));
        }

        return new StructInputProperty(propertiesSorted.values().toArray(new StructProperty[0]));
    }

    public static StructProperty attribute(@NonNull Options options, @NonNull Class<?> struct, @NonNull String name, @NonNull Attribute attribute, @NonNull List<Field> fields) {
        Class<?> type = fields.get(0).getType();

        //ensure all fields have the same type
        fields.forEach(field -> checkArg(field.getType() == type, "all fields must have the same type (expected: %s, mismatch at %s)", type, field));

        //input
        StructProperty.Components initialProperty;
        if (type.isPrimitive()) {
            initialProperty = new VectorComponentsInputProperty(fields.toArray(new Field[0]));
        } else {
            throw new IllegalArgumentException("don't know how to handle attribute of type " + type);
        }

        //transform
        StructProperty.Components transformProperty = initialProperty;
        for (Transform transform : attribute.transform()) {
            switch (transform.value()) {
                case INT_ARGB8_TO_BYTE_VECTOR_RGB:
                case INT_ARGB8_TO_BYTE_VECTOR_RGBA:
                    transformProperty = new IntToARGBExpansionTransformProperty(transformProperty, transform.value() == Transform.Type.INT_ARGB8_TO_BYTE_VECTOR_RGBA);
                    break;
                default:
                    throw new UnsupportedOperationException("unknown transform: " + transform.value());
            }
        }

        //convert
        StructProperty.Components convertProperty = transformProperty;
        for (Attribute.Conversion conversion : attribute.convert()) {
            switch (conversion) {
                case TO_UNSIGNED:
                    //we still want to apply this stage even if the value is packed, and it's safe because it will always be the first conversion used
                    convertProperty = new IntegerToUnsignedIntegerConversionProperty(convertProperty);
                    break;
                case TO_FLOAT:
                    if (options.unpacked()) {
                        convertProperty = new IntegerToFloatConversionProperty(convertProperty);
                    }
                    break;
                case TO_NORMALIZED_FLOAT:
                    if (options.unpacked()) {
                        convertProperty = new IntegerToNormalizedFloatConversionProperty(convertProperty);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unknown conversion: " + conversion);
            }
        }

        return convertProperty;
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
