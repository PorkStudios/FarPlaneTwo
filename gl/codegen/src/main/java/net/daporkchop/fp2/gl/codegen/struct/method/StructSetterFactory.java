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

import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayIndex;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeIgnore;
import net.daporkchop.fp2.gl.attribute.annotation.AttributeSetter;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ArrayAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.AttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.MatrixAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.VectorAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.layout.ArrayLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.MatrixLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.VectorLayout;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.codegen.struct.method.parameter.MethodParameterFactory;
import net.daporkchop.lib.common.util.PorkUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;
import static org.objectweb.asm.Type.*;

/**
 * Constructs {@link StructSetter}s from {@link Method}s.
 *
 * @author DaPorkchop_
 */
@UtilityClass
public class StructSetterFactory {
    public static StructSetter<StructLayout> createFromMethod(StructAttributeType structType, Method method, String fieldName) {
        checkArg(method.getReturnType() == method.getDeclaringClass(), "method %s must have the same return type as the class in which it is declared!", method);
        checkArg((method.getModifiers() & Modifier.STATIC) == 0, "method %s may not be static!", method);

        int fieldIndex = structType.fieldIndexByName(fieldName);
        return structField(structType, fieldIndex, digestParametersForSetter(structType.fieldType(fieldIndex), 1, Arrays.asList(method.getParameters()).iterator()));
    }

    private static StructSetter<?> digestParametersForSetter(AttributeType attributeType, int lvtIndex, Iterator<Parameter> iterator) {
        Parameter parameter = iterator.next();
        if (parameter.isAnnotationPresent(ArrayIndex.class)) { //the parameter is an array index
            checkArg(parameter.getType() == int.class, "parameter annotated as @%s must be int", ArrayIndex.class);

            checkArg(attributeType instanceof ArrayAttributeType, "%s attribute doesn't have elements, cannot be used with @%s", PorkUtil.className(attributeType), ArrayIndex.class);
            ArrayAttributeType realAttributeType = (ArrayAttributeType) attributeType;

            return arrayElementIndexed(realAttributeType, lvtIndex, digestParametersForSetter(realAttributeType.elementType(), lvtIndex + 1, iterator));
        }

        //consume all the remaining parameters and gather them into an array
        List<MethodParameter> methodParameters = new ArrayList<>();
        do {
            methodParameters.add(MethodParameterFactory.createFromType(parameter.getAnnotatedType(), lvtIndex));
            lvtIndex += getType(parameter.getType()).getSize();
        } while (iterator.hasNext() && (parameter = iterator.next()) != null);

        //group all the method parameters into a single virtual parameter containing all the components
        MethodParameter methodParameter = MethodParameterFactory.union(methodParameters.toArray(new MethodParameter[0]));

        if (attributeType instanceof MatrixAttributeType) {
            MatrixAttributeType matrixType = (MatrixAttributeType) attributeType;
            return matrixColumnsAll(matrixType, colIndex -> vector(matrixType.colType(), MethodParameterFactory.slice(methodParameter, colIndex * matrixType.rows(), matrixType.rows())));
        } else if (attributeType instanceof VectorAttributeType) {
            return vector((VectorAttributeType) attributeType, methodParameter);
        } else {
            throw new IllegalArgumentException(String.valueOf(attributeType));
        }
    }

    private static StructSetter<StructLayout> structField(StructAttributeType structType, int fieldIndex, StructSetter<?> next) {
        return (mv, lvtAlloc, layout, callback) -> next.visit(mv, lvtAlloc, uncheckedCast(layout.fieldLayout(fieldIndex)), callback.visitStructField(structType, layout, fieldIndex));
    }

    private static StructSetter<ArrayLayout> arrayElementsAll(ArrayAttributeType arrayType, IntFunction<StructSetter<?>> next) {
        return (mv, lvtAlloc, layout, callback) -> {
            for (int elementIndex = 0; elementIndex < arrayType.elementCount(); elementIndex++) {
                next.apply(elementIndex).visit(mv, lvtAlloc, uncheckedCast(layout.elementLayout()), callback.visitArrayElementConstant(arrayType, layout, elementIndex));
            }
        };
    }

    private static StructSetter<ArrayLayout> arrayElementConstant(ArrayAttributeType arrayType, int elementIndex, StructSetter<?> next) {
        return (mv, lvtAlloc, layout, callback) -> next.visit(mv, lvtAlloc, uncheckedCast(layout.elementLayout()), callback.visitArrayElementConstant(arrayType, layout, elementIndex));
    }

    private static StructSetter<ArrayLayout> arrayElementIndexed(ArrayAttributeType arrayType, int elementIndexLvt, StructSetter<?> next) {
        return (mv, lvtAlloc, layout, callback) -> next.visit(mv, lvtAlloc, uncheckedCast(layout.elementLayout()), callback.visitArrayElementIndexed(arrayType, layout, elementIndexLvt));
    }

    private static StructSetter<MatrixLayout> matrixColumnsAll(MatrixAttributeType matrixType, IntFunction<StructSetter<?>> next) {
        return (mv, lvtAlloc, layout, callback) -> {
            for (int colIndex = 0; colIndex < matrixType.cols(); colIndex++) {
                next.apply(colIndex).visit(mv, lvtAlloc, uncheckedCast(layout.colLayout()), callback.visitMatrixColumnConstant(matrixType, layout, colIndex));
            }
        };
    }

    /*private static StructSetter<MatrixLayout> matrixColumnsAll(MatrixAttributeType matrixType, MethodParameter parameter) {
        checkArg(matrixType.rows() * matrixType.cols() == parameter.components());
        return (mv, lvtAlloc, layout, callback) -> parameter.visitLoad(mv, lvtAlloc, componentLoader -> {
            for (int colIndex = 0; colIndex < matrixType.cols(); colIndex++) {
                callback.visitMatrixColumnConstant(matrixType, layout, colIndex)
                                .visitVector(matrixType.colType(), layout.colLayout(), );
                next.apply(colIndex).visit(mv, lvtAlloc, uncheckedCast(layout.colLayout()), callback.visitMatrixColumnConstant(matrixType, layout, colIndex));
            }
        });
    }*/

    private static StructSetter<MatrixLayout> matrixColumnConstant(MatrixAttributeType matrixType, int colIndex, StructSetter<?> next) {
        return (mv, lvtAlloc, layout, callback) -> next.visit(mv, lvtAlloc, uncheckedCast(layout.colLayout()), callback.visitMatrixColumnConstant(matrixType, layout, colIndex));
    }

    private static StructSetter<MatrixLayout> matrixColumnIndexed(MatrixAttributeType matrixType, int colIndexLvt, StructSetter<?> next) {
        return (mv, lvtAlloc, layout, callback) -> next.visit(mv, lvtAlloc, uncheckedCast(layout.colLayout()), callback.visitMatrixColumnIndexed(matrixType, layout, colIndexLvt));
    }

    private static StructSetter<VectorLayout> vector(VectorAttributeType vectorType, MethodParameter parameter) {
        return (mv, lvtAlloc, layout, callback) -> parameter.visitLoad(mv, lvtAlloc, loader -> callback.visitVector(vectorType, layout, parameter, loader));
    }
}
