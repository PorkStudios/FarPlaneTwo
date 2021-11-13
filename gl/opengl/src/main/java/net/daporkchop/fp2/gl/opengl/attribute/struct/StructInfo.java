/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.Attrib;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Field;
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
public class StructInfo<T> {
    protected final Class<T> clazz;
    protected final List<StructMember<T>> members;

    public StructInfo(@NonNull Class<T> clazz) {
        this.clazz = clazz;

        ImmutableList.Builder<StructMember<T>> builder = ImmutableList.builder();
        Map<String, Field> fieldsByName = Stream.of(clazz.getFields())
                .collect(Collectors.toMap(Field::getName, Function.identity(), (a, b) -> {
                            throw new IllegalArgumentException(a + " " + b);
                        },
                        LinkedHashMap::new));

        while (!fieldsByName.isEmpty()) {
            Field field = fieldsByName.values().stream()
                    .filter(f -> f.getAnnotation(Attrib.class) != null)
                    .findAny()
                    .orElseThrow(() -> new IllegalStateException("no annotated fields remain: " + fieldsByName));

            String name = field.getName();
            Attrib attrib = field.getAnnotation(Attrib.class);

            if (attrib.vectorAxes().length == 0) {
                fieldsByName.remove(name);
                builder.add(new StructMember<>(clazz, name, attrib, ImmutableList.of(field)));
            } else {
                String[] vectorAxes = attrib.vectorAxes();
                checkArg(name.endsWith(vectorAxes[0]), "name doesn't end in %s: %s", vectorAxes[0], field);

                String baseName = name.substring(0, name.length() - vectorAxes[0].length());

                builder.add(new StructMember<>(clazz, name, attrib, Stream.of(vectorAxes)
                        .map(axisSuffix -> {
                            Field componentField = fieldsByName.remove(baseName + axisSuffix);
                            checkArg(componentField != null, "no such field: %s%s", baseName, axisSuffix);
                            return componentField;
                        })
                        .collect(Collectors.toList())));
            }
        }

        this.members = builder.build();
    }

    public void glslStructDefinition(@NonNull StringBuilder builder) {
        builder.append("struct ").append(this.clazz.getSimpleName()).append(" {\n");
        this.members.forEach(member -> builder.append("    ").append(member.unpackedStage.glslType().declaration(member.name)).append(";\n"));
        builder.append("};\n");
    }

    public void writePacked(@NonNull MethodVisitor mv, int structLvtIndex, int outputBaseLvtIndex, int outputOffsetLvtIndex) {
        this.members.forEach(member -> member.storeStageOutput(mv, member.packedStage, structLvtIndex, outputBaseLvtIndex, outputOffsetLvtIndex));
    }

    public void writeUnpacked(@NonNull MethodVisitor mv, int structLvtIndex, int outputBaseLvtIndex, int outputOffsetLvtIndex) {
        this.members.forEach(member -> member.storeStageOutput(mv, member.unpackedStage, structLvtIndex, outputBaseLvtIndex, outputOffsetLvtIndex));
    }
}
