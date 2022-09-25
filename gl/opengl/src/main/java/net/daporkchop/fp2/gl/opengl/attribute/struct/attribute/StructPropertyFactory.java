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

package net.daporkchop.fp2.gl.opengl.attribute.struct.attribute;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.attribute.annotation.AArrayType;
import net.daporkchop.fp2.gl.attribute.annotation.AAttribute;
import net.daporkchop.fp2.gl.attribute.annotation.AAttributes;
import net.daporkchop.fp2.gl.attribute.annotation.AMatrixType;
import net.daporkchop.fp2.gl.attribute.annotation.AScalarType;
import net.daporkchop.fp2.gl.attribute.annotation.AVectorType;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.basic.BasicArrayAttributeType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.basic.BasicMatrixAttributeType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.basic.BasicScalarAttributeType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.basic.BasicAttributeType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.attribute.basic.BasicVectorAttributeType;
import net.daporkchop.fp2.gl.opengl.attribute.struct.type.GLSLPrimitiveType;

import java.util.Map;

import static net.daporkchop.lib.common.util.PValidation.*;
import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class StructPropertyFactory {
    public static AttributeType struct(@NonNull Class<?> struct) {
        checkArg(struct.isInterface(), "struct type %s must be an interface", struct.getTypeName());
        checkArg(struct.getInterfaces().length == 0, "struct type %s may not extend any interfaces", struct.getTypeName());

        AAttribute[] attributes = null;
        {
            AAttributes annotation = struct.getAnnotation(AAttributes.class);
            if (annotation != null) {
                attributes = annotation.value();
            }
        }
        if (attributes == null) {
            AAttribute annotation = struct.getAnnotation(AAttribute.class);
            if (annotation != null) {
                attributes = new AAttribute[]{ annotation };
            }
        }
        checkArg(attributes != null, "struct type %s has no attribute annotations", struct.getTypeName());

        ImmutableMap.Builder<String, AttributeType> propertiesBuilder = ImmutableMap.builder();
        for (AAttribute attribute : attributes) {
            propertiesBuilder.put(attribute.name(), of(attribute));
        }

        return new BasicAttributeType(struct.getName(), uncheckedCast(propertiesBuilder.build().entrySet().toArray(new Map.Entry[0])));
    }

    private static AttributeType of(@NonNull AAttribute attribute) {
        //determine the attribute type
        int declaredAttributes = 0;

        AArrayType[] typeArray = attribute.typeArray();
        checkArg(typeArray.length <= 1, "more than one array attribute may not be declared");
        if (typeArray.length == 1) {
            declaredAttributes++;
        }
        AVectorType[] typeVector = attribute.typeVector();
        checkArg(typeVector.length <= 1, "more than one vector attribute may not be declared");
        if (typeVector.length == 1) {
            declaredAttributes++;
        }
        AMatrixType[] typeMatrix = attribute.typeMatrix();
        checkArg(typeMatrix.length <= 1, "more than one matrix attribute may not be declared");
        if (typeMatrix.length == 1) {
            declaredAttributes++;
        }
        AScalarType[] typeScalar = attribute.typeScalar();
        checkArg(typeScalar.length <= 1, "more than one scalar attribute may not be declared");
        if (typeScalar.length == 1) {
            declaredAttributes++;
        }

        checkArg(declaredAttributes == 1, "exactly one attribute must be declared");

        //parse the declared attribute type
        if (typeArray.length == 1) {
            return of(typeArray[0]);
        } else if (typeVector.length == 1) {
            return of(typeVector[0]);
        } else if (typeMatrix.length == 1) {
            return of(typeMatrix[0]);
        } else /*if (typeScalar.length == 1)*/ {
            return of(typeScalar[0]);
        }
    }

    private static AttributeType.Elements of(@NonNull AArrayType type) {
        //determine the component type
        int declaredComponentTypes = 0;

        AVectorType[] typeVector = type.componentTypeVector();
        checkArg(typeVector.length <= 1, "more than one vector component type may not be declared");
        if (typeVector.length == 1) {
            declaredComponentTypes++;
        }
        AMatrixType[] typeMatrix = type.componentTypeMatrix();
        checkArg(typeMatrix.length <= 1, "more than one matrix component type may not be declared");
        if (typeMatrix.length == 1) {
            declaredComponentTypes++;
        }
        AScalarType[] typeScalar = type.componentTypeScalar();
        checkArg(typeScalar.length <= 1, "more than one scalar component type may not be declared");
        if (typeScalar.length == 1) {
            declaredComponentTypes++;
        }

        checkArg(declaredComponentTypes == 1, "exactly one component type must be declared");

        //parse the component type
        AttributeType.Components bottomComponentType;
        if (typeVector.length == 1) {
            bottomComponentType = of(typeVector[0]);
        } else if (typeMatrix.length == 1) {
            bottomComponentType = of(typeMatrix[0]);
        } else /*if (typeScalar.length == 1)*/ {
            bottomComponentType = of(typeScalar[0]);
        }

        //get and validate length
        int[] lengths = type.length();
        checkArg(lengths.length > 0, "array may not have 0 dimensions");
        for (int length : lengths) {
            checkArg(length > 0, "array may not have negative length");
        }

        //unroll array into a (chain) of BasicArrayProperty
        AttributeType.Elements arrayType = new BasicArrayAttributeType(bottomComponentType, lengths[lengths.length - 1]);
        for (int i = lengths.length - 2; i >= 0; i--) {
            arrayType = new BasicArrayAttributeType(arrayType, lengths[i]);
        }
        return arrayType;
    }

    private static AttributeType.Components of(@NonNull AVectorType type) {
        int components = type.components();
        checkArg(components >= 1 && components <= 4, "illegal vector component count: %d", components);

        return new BasicVectorAttributeType(of(type.componentType()), components);
    }

    private static AttributeType.Components of(@NonNull AMatrixType type) {
        int cols = type.cols();
        checkArg(cols >= 2 && cols <= 4, "illegal vector column count: %d", cols);

        int rows = type.rows();
        checkArg(rows >= 2 && rows <= 4, "illegal vector row count: %d", rows);

        return new BasicMatrixAttributeType(of(type.componentType()), cols, rows);
    }

    private static AttributeType.Components of(@NonNull AScalarType type) {
        ComponentType logicalStorageType;
        Class<?> rawLogicalStorageType = type.value();
        checkArg(rawLogicalStorageType.isPrimitive(), "scalar type value must be a primitive class!");
        if (rawLogicalStorageType == byte.class) {
            logicalStorageType = ComponentType.BYTE;
        } else if (rawLogicalStorageType == char.class) {
            logicalStorageType = ComponentType.UNSIGNED_SHORT;
        } else if (rawLogicalStorageType == short.class) {
            logicalStorageType = ComponentType.SHORT;
        } else if (rawLogicalStorageType == int.class) {
            logicalStorageType = ComponentType.INT;
        } else if (rawLogicalStorageType == float.class) {
            logicalStorageType = ComponentType.FLOAT;
        } else {
            throw new IllegalArgumentException("unsupported scalar type value: " + rawLogicalStorageType);
        }

        ComponentInterpretation componentInterpretation = new ComponentInterpretation(logicalStorageType.glslPrimitive(), false);
        for (ScalarConvert convert : type.interpret()) {
            switch (convert.value()) {
                case TO_UNSIGNED:
                    checkArg(componentInterpretation.outputType().integer(), "floating-point type cannot be converted to unsigned!");
                    checkArg(componentInterpretation.outputType().signed(), "unsigned type is already unsigned!");
                    //componentInterpretation = componentInterpretation.withOutputType(GLSLPrimitiveType.UINT);
                    logicalStorageType = logicalStorageType.toUnsigned();
                    break;
                case TO_FLOAT:
                    checkArg(componentInterpretation.outputType().integer(), "floating-point type cannot be converted to floating-point!");
                    componentInterpretation = new ComponentInterpretation(GLSLPrimitiveType.FLOAT, convert.normalized());
                    break;
                default:
                    throw new IllegalArgumentException("unknown conversion: " + convert);
            }
        }

        return new BasicScalarAttributeType(logicalStorageType, componentInterpretation);
    }
}
