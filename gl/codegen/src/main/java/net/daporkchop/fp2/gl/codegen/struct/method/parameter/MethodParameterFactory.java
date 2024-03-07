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

package net.daporkchop.fp2.gl.codegen.struct.method.parameter;

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayLength;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarExpand;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarTransform;
import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.convert.IntegerToFloatConvertingParameter;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.convert.IntegerToUnsignedConvertingParameter;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.input.ArrayElementLoadingInputParameter;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.input.ScalarArgumentInputParameter;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.transform.Int2ARGBExpansionTransformParameter;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.transform.UnionTransformParameter;
import net.daporkchop.lib.common.util.PorkUtil;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class MethodParameterFactory {
    public static MethodParameter union(MethodParameter... parameters) {
        switch (parameters.length) {
            case 0:
                throw new IllegalArgumentException("at least one parameter must be given");
            case 1:
                return parameters[0];
        }

        //ensure all parameters have the same component type
        JavaPrimitiveType componentType = parameters[0].componentType();
        for (MethodParameter parameter : parameters) {
            checkArg(parameter.componentType() == componentType, "wrong component type: expected %s, found %s", componentType, parameter.componentType());
        }

        return new UnionTransformParameter(parameters);
    }

    public static MethodParameter createFromType(AnnotatedType annotatedType, int argumentLvtIndex) {
        if (annotatedType instanceof AnnotatedArrayType) { //special handling for array types
            return createFromArray((AnnotatedArrayType) annotatedType, argumentLvtIndex);
        } else {
            return createFromScalar(annotatedType, argumentLvtIndex);
        }
    }

    private static MethodParameter createFromArray(AnnotatedArrayType annotatedArrayType, int argumentLvtIndex) {
        int[] arrayLengths = PorkUtil.EMPTY_INT_ARRAY;

        do {
            ArrayLength lengthAnnotation = annotatedArrayType.getAnnotation(ArrayLength.class);
            checkArg(lengthAnnotation != null, "array must be annotated with @%s", ArrayLength.class);

            //append the current array dimension's length to the arrayLengths array
            arrayLengths = Arrays.copyOf(arrayLengths, arrayLengths.length + 1);
            arrayLengths[arrayLengths.length - 1] = positive(lengthAnnotation.value(), "length");

            AnnotatedType annotatedComponentType = annotatedArrayType.getAnnotatedGenericComponentType();
            if (annotatedComponentType instanceof AnnotatedArrayType) { //component type is an array, making this an array of arrays!
                annotatedArrayType = (AnnotatedArrayType) annotatedComponentType;
            } else { //this is the bottom array level
                Class<?> rawComponentType = (Class<?>) annotatedComponentType.getType(); //assume it's a class
                checkArg(rawComponentType.isPrimitive(), "input parameter type must be primitive! %s", rawComponentType);

                JavaPrimitiveType componentType = JavaPrimitiveType.from(rawComponentType);
                return scalarTransform(new ArrayElementLoadingInputParameter(componentType, argumentLvtIndex, arrayLengths), annotatedComponentType.getAnnotation(ScalarTransform.class));
            }
        } while (true);
    }

    private static MethodParameter createFromScalar(AnnotatedType annotatedType, int argumentLvtIndex) {
        Class<?> type = (Class<?>) annotatedType.getType(); //assume it's a class
        checkArg(type.isPrimitive(), "input parameter type must be primitive! %s", annotatedType);

        JavaPrimitiveType componentType = JavaPrimitiveType.from(type);
        return scalarTransform(new ScalarArgumentInputParameter(componentType, argumentLvtIndex), annotatedType.getAnnotation(ScalarTransform.class));
    }

    private static MethodParameter scalarTransform(MethodParameter parameter, ScalarTransform transform) {
        if (transform == null) { //no transform annotation is present, nothing to do
            return parameter;
        }

        MethodParameter converted = scalarConvert(parameter, transform.interpret());
        MethodParameter expanded = scalarExpand(converted, transform.expand());
        return expanded;
    }

    private static MethodParameter scalarConvert(MethodParameter parameter, ScalarConvert... conversions) {
        for (ScalarConvert conversion : conversions) {
            switch (conversion.value()) {
                case TO_UNSIGNED:
                    //we still want to apply this stage even if the value is packed(((, and it's safe because it will always be the first conversion used)))
                    parameter = new IntegerToUnsignedConvertingParameter(parameter);
                    break;
                case TO_FLOAT:
                    parameter = new IntegerToFloatConvertingParameter(parameter, conversion.normalized());
                default:
                    throw new IllegalArgumentException("unknown conversion: " + conversion);
            }
        }
        return parameter;
    }

    private static MethodParameter scalarExpand(MethodParameter parameter, ScalarExpand... expansions) {
        for (ScalarExpand expansion : expansions) {
            switch (expansion.value()) {
                case INT_ARGB8_TO_BYTE_VECTOR_RGBA:
                    parameter = new Int2ARGBExpansionTransformParameter(parameter,
                            expansion.alpha() ? Int2ARGBExpansionTransformParameter.UnpackOrder.RGBA : Int2ARGBExpansionTransformParameter.UnpackOrder.RGB,
                            false);
                    break;
                default:
                    throw new IllegalArgumentException("unknown expansion: " + expansion);
            }

            //run any subsequent conversions on the resulting components
            parameter = scalarConvert(parameter, expansion.thenConvert());
        }
        return parameter;
    }
}
