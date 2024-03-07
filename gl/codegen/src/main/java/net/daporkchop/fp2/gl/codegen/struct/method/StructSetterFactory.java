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

package net.daporkchop.fp2.gl.codegen.struct.method;

import com.google.common.collect.Iterators;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayIndex;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeIgnore;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ArrayAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.AttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameterFactory;
import net.daporkchop.lib.common.util.PorkUtil;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static net.daporkchop.lib.common.util.PValidation.*;
import static org.objectweb.asm.Type.*;

/**
 * Constructs {@link StructSetter}s from {@link Method}s.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StructSetterFactory {
    /*public static Optional<StructSetter> createFromMethod(StructAttributeType property, Method method) {
        checkArg(method.getReturnType() == method.getDeclaringClass(), "method %s must have the same return type as the class in which it is declared!", method);
        checkArg((method.getModifiers() & Modifier.STATIC) == 0, "method %s may not be static!", method);

        if (method.isAnnotationPresent(AttributeSetter.class)) {
            AttributeSetter annotation = method.getAnnotation(AttributeSetter.class);
            String attributeName = annotation.value();
            if (attributeName.isEmpty()) { //fall back to method name if attribute name is unset
                attributeName = method.getName();
            }

            int fieldIndex = property.fieldIndexByName(attributeName);

            return Optional.of(new ChildSpecificSetter(digestParametersForSetter(property.fieldType(fieldIndex), 1, Arrays.asList(method.getParameters()).iterator()), fieldIndex));
        } else if (method.isAnnotationPresent(AttributeIgnore.class)) {
            return Optional.empty();
        }

        throw new UnsupportedOperationException(method.toString());
    }

    private static StructSetter digestParametersForSetter(AttributeType attributeType, int lvtIndex, Iterator<Parameter> iterator) {
        Parameter parameter = iterator.next();
        if (parameter.isAnnotationPresent(ArrayIndex.class)) { //the parameter is an array index
            checkArg(parameter.getType() == int.class, "parameter annotated as @%s must be int", ArrayIndex.class);

            checkArg(attributeType instanceof ArrayAttributeType, "%s attribute doesn't have elements, cannot be used with @%s", PorkUtil.className(attributeType), ArrayIndex.class);
            ArrayAttributeType realAttributeType = (ArrayAttributeType) attributeType;

            return new IndexedSetterImpl(digestParametersForSetter(realAttributeType.elementType(), lvtIndex + 1, iterator), lvtIndex);
        }

        //consume all the remaining parameters and gather them into an array
        List<MethodParameter> methodParameters = new ArrayList<>();
        do {
            methodParameters.add(MethodParameterFactory.createFromType(parameter.getAnnotatedType(), lvtIndex));
            lvtIndex += getType(parameter.getType()).getSize();
        } while (iterator.hasNext() && (parameter = iterator.next()) != null);

        //group all the method parameters into a single virtual parameter containing all the components
        MethodParameter methodParameter = MethodParameterFactory.union(methodParameters.toArray(new MethodParameter[0]));

        return new BasicSetterImpl(methodParameter, lvtIndex);
    }*/
}
