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

package net.daporkchop.fp2.gl.opengl.attribute.struct;

import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.opengl.attribute.AttributeFormatBuilderImpl;
import net.daporkchop.fp2.gl.opengl.attribute.struct.GLSLField;
import net.daporkchop.fp2.gl.opengl.attribute.struct.StructMember;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructProperty;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.StructPropertyFactory;
import net.daporkchop.lib.common.util.PorkUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
@EqualsAndHashCode(of = "clazz")
public class StructInfo<S> {
    protected final Class<S> clazz;
    @Deprecated
    protected final List<StructMember<S>> members;

    protected final StructProperty packedProperty;
    protected final StructProperty unpackedProperty;

    @Deprecated
    public StructInfo(@NonNull AttributeFormatBuilderImpl<S> builder) {
        this(builder.clazz(), builder.nameOverrides());
    }

    public StructInfo(@NonNull Class<S> clazz, @NonNull Map<String, String> nameOverrides) {
        this.clazz = clazz;
        this.packedProperty = StructPropertyFactory.struct(StructPropertyFactory.Options.builder().unpacked(false).build(), clazz);
        this.unpackedProperty = StructPropertyFactory.struct(StructPropertyFactory.Options.builder().unpacked(true).build(), clazz);

        nameOverrides = new HashMap<>(nameOverrides);

        List<StructMember<S>> memberListBuilder = new ArrayList<>();
        Map<String, Field> fieldsByName = Stream.of(this.clazz.getFields())
                .collect(Collectors.toMap(Field::getName, Function.identity(), (a, b) -> {
                            throw new IllegalArgumentException(a + " " + b);
                        },
                        LinkedHashMap::new));

        while (!fieldsByName.isEmpty()) {
            Field field = fieldsByName.values().stream()
                    .filter(f -> f.getAnnotation(Attribute.class) != null)
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("no annotated fields remain: " + fieldsByName));

            String name = field.getName();
            Attribute attribute = field.getAnnotation(Attribute.class);

            if (attribute.vectorAxes().length == 0) {
                fieldsByName.remove(name);
                memberListBuilder.add(new StructMember<>(this.clazz, name, attribute, ImmutableList.of(field)));
            } else {
                String[] vectorAxes = attribute.vectorAxes();
                checkArg(name.endsWith(vectorAxes[0]), "name doesn't end in %s: %s", vectorAxes[0], field);

                String baseName = name.substring(0, name.length() - vectorAxes[0].length());

                memberListBuilder.add(new StructMember<>(this.clazz, PorkUtil.fallbackIfNull(nameOverrides.remove(baseName), baseName), attribute, Stream.of(vectorAxes)
                        .map(axisSuffix -> {
                            Field componentField = fieldsByName.remove(baseName + axisSuffix);
                            checkArg(componentField != null, "no such field: %s%s", baseName, axisSuffix);
                            return componentField;
                        })
                        .collect(Collectors.toList())));
            }
        }

        nameOverrides.forEach((oldName, newName) -> checkArg(false, "cannot rename attribute %s to %s: no such attribute %1$s", oldName, newName));

        memberListBuilder.sort(Comparator.comparingInt(StructMember::sort));
        this.members = ImmutableList.copyOf(memberListBuilder);
    }

    public String name() {
        return this.clazz.getSimpleName();
    }

    public List<GLSLField<?>> memberFields() {
        return this.members.stream()
                .map(member -> new GLSLField<>(member.unpackedStage().glslType(), member.name()))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
    }

    public void glslStructDefinition(@NonNull StringBuilder builder) {
        builder.append("struct ").append(this.clazz.getSimpleName()).append(" {\n");
        this.members.forEach(member -> builder.append("    ").append(member.unpackedStage().glslType().declaration(member.name())).append(";\n"));
        builder.append("};\n");
    }
}
