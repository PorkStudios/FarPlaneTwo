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

package net.daporkchop.fp2.gl.codegen.struct.layout.assignment;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.AttributeTarget;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ArrayAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.AttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.MatrixAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ShaderPrimitiveType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.VectorAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.layout.ArrayLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.AttributeLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.LayoutInfo;
import net.daporkchop.fp2.gl.codegen.struct.layout.MatrixLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.VectorLayout;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.lib.common.math.PMath;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@UtilityClass
public class SharedBlockMemoryLayout {
    public static EnumSet<AttributeTarget> compatibleTargets(OpenGL gl, StructAttributeType type) {
        return EnumSet.of(AttributeTarget.VERTEX_ATTRIBUTE, AttributeTarget.UBO, AttributeTarget.SSBO);
    }

    @SneakyThrows
    public static LayoutInfo computeLayout(OpenGL gl, StructAttributeType type) {
        //compile a dummy shader
        StringBuilder source = new StringBuilder().append("uniform DUMMY_UNIFORM_BLOCK");
        toGLSL(source, type, new HashSet<>());
        source.insert(0, "#version " + gl.version().glsl() + "\n\n");
        source.append("void main() {\n}\n");

        int shader = gl.glCreateShader(GL_VERTEX_SHADER);
        int program = gl.glCreateProgram();
        try {
            gl.glShaderSource(shader, source);
            gl.glCompileShader(shader);

            if (gl.glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                throw new ShaderCompilationException(gl.glGetShaderInfoLog(shader));
            }

            gl.glAttachShader(program, shader);
            gl.glLinkProgram(program);
            gl.glDetachShader(program, shader);

            //check for errors
            if (gl.glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                throw new ShaderLinkageException(gl.glGetProgramInfoLog(program));
            }

            return new LayoutInfo(type, layout(type, introspectLayout(type, gl, program, "")), "shared", true, SharedBlockMemoryLayout::compatibleTargets);
        } finally {
            gl.glDeleteProgram(program);
            gl.glDeleteShader(shader);
        }
    }

    private static void toGLSL(StringBuilder builder, AttributeType type, String name, Set<String> visitedStructs) {
        if (type instanceof VectorAttributeType) {
            VectorAttributeType vectorType = (VectorAttributeType) type;
            ShaderPrimitiveType shaderType = vectorType.componentType().interpretedType();
            if (vectorType.components() == 1) {
                builder.append(shaderType.name().toLowerCase(Locale.ROOT));
            } else {
                builder.append(shaderType.glslPrefix()).append("vec").append(vectorType.components());
            }
            builder.append(' ').append(name);
        } else if (type instanceof MatrixAttributeType) {
            MatrixAttributeType matrixType = (MatrixAttributeType) type;
            builder.append(matrixType.colType().componentType().interpretedType().glslPrefix()).append("mat").append(matrixType.cols());
            if (matrixType.cols() != matrixType.rows()) {
                builder.append('x').append(matrixType.rows());
            }
            builder.append(' ').append(name);
        } else if (type instanceof ArrayAttributeType) {
            StringBuilder suffixBuilder = new StringBuilder();
            do {
                ArrayAttributeType arrayType = (ArrayAttributeType) type;
                suffixBuilder.append('[').append(arrayType.elementCount()).append(']');
                type = arrayType.elementType();
            } while (type instanceof ArrayAttributeType);

            toGLSL(builder, type, name, visitedStructs);
            builder.append(suffixBuilder);
        } else if (type instanceof StructAttributeType) {
            StructAttributeType structType = (StructAttributeType) type;
            if (visitedStructs.add(structType.structName())) {
                StringBuilder structBuilder = new StringBuilder().append("struct ").append(structType.structName());
                toGLSL(structBuilder, structType, visitedStructs);
                builder.insert(0, structBuilder);
            }
            builder.append(structType.structName()).append(' ').append(name);
        } else {
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    private static void toGLSL(StringBuilder builder, StructAttributeType structType, Set<String> visitedStructs) {
        builder.append(" {\n");
        for (int field = 0; field < structType.fieldCount(); field++) {
            toGLSL(builder.append("  "), structType.fieldType(field), structType.fieldName(field), visitedStructs);
            builder.append(";\n");
        }
        builder.append("\n};\n\n");
    }

    private static Offset introspectLayout(AttributeType type, OpenGL gl, int program, String path) {
        if (type instanceof StructAttributeType) {
            return introspectLayout((StructAttributeType) type, gl, program, path);
        } else if (type instanceof ArrayAttributeType) {
            return introspectLayout((ArrayAttributeType) type, gl, program, path);
        } else if (type instanceof MatrixAttributeType) {
            return introspectLayout((MatrixAttributeType) type, gl, program, path);
        } else if (type instanceof VectorAttributeType) {
            return introspectLayout((VectorAttributeType) type, gl, program, path);
        } else {
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    private static OffsetFields introspectLayout(StructAttributeType type, OpenGL gl, int program, String path) {
        Offset[] fields = IntStream.range(0, type.fieldCount())
                .mapToObj(i -> introspectLayout(type.fieldType(i), gl, program, path.isEmpty() ? type.fieldName(i) : path + '.' + type.fieldName(i)))
                .toArray(Offset[]::new);
        return new OffsetFields(Arrays.stream(fields).mapToInt(offset -> offset.offset).min().getAsInt(), fields);
    }

    private static Offset introspectLayout(ArrayAttributeType type, OpenGL gl, int program, String path) {
        int uniformIndex = gl.glGetUniformIndices(program, path);
        checkArg(uniformIndex != GL_INVALID_INDEX, path);

        int offset = gl.glGetActiveUniformsi(program, uniformIndex, GL_UNIFORM_OFFSET);
        checkArg(offset >= 0, "offset is negative: %s (%s)", offset, path);
        int length = gl.glGetActiveUniformsi(program, uniformIndex, GL_UNIFORM_SIZE);
        checkArg(length == type.elementCount(), "compiled array length doesn't match: %s != %s", length, type.elementType());
        int stride = gl.glGetActiveUniformsi(program, uniformIndex, GL_UNIFORM_ARRAY_STRIDE);
        checkArg(stride >= 0, "stride is negative: %s (%s)", stride, path);

        if (type.elementType() instanceof StructAttributeType || type.elementType() instanceof ArrayAttributeType) {
            Offset[] children = new Offset[type.elementCount()];
            for (int index = 0; index < children.length; index++) {
                children[index] = introspectLayout(type.elementType(), gl, program, path + '[' + index + ']');
            }
            return new OffsetFields(offset, children);
        } else if (type.elementType() instanceof MatrixAttributeType) {
            Offset[] children = new Offset[type.elementCount()];
            OffsetStride base = introspectLayout((MatrixAttributeType) type.elementType(), gl, program, path + "[0]");
            for (int index = 0; index < children.length; index++) {
                children[index] = new OffsetStride(offset + index * stride, base.stride);
            }
            return new OffsetFields(offset, children);
        } else {
            return new OffsetStride(offset, stride);
        }
    }

    private static OffsetStride introspectLayout(MatrixAttributeType type, OpenGL gl, int program, String path) {
        int uniformIndex = gl.glGetUniformIndices(program, path);
        checkArg(uniformIndex != GL_INVALID_INDEX, path);

        int offset = gl.glGetActiveUniformsi(program, uniformIndex, GL_UNIFORM_OFFSET);
        checkArg(offset >= 0, "offset is negative: %s (%s)", offset, path);
        int stride = gl.glGetActiveUniformsi(program, uniformIndex, GL_UNIFORM_MATRIX_STRIDE);
        checkArg(stride >= 0, "stride is negative: %s (%s)", stride, path);
        boolean columnMajor = gl.glGetActiveUniformsi(program, uniformIndex, GL_UNIFORM_IS_ROW_MAJOR) == GL_FALSE;
        checkArg(columnMajor, "row-major matrices aren't supported!");

        return new OffsetStride(offset, stride);
    }

    private static Offset introspectLayout(VectorAttributeType type, OpenGL gl, int program, String path) {
        int uniformIndex = gl.glGetUniformIndices(program, path);
        checkArg(uniformIndex != GL_INVALID_INDEX, path);

        int offset = gl.glGetActiveUniformsi(program, uniformIndex, GL_UNIFORM_OFFSET);
        checkArg(offset >= 0, "offset is negative: %s (%s)", offset, path);
        return new Offset(offset);
    }

    private static AttributeLayout layout(AttributeType type, Offset introspected) {
        if (type instanceof StructAttributeType) {
            return layout((StructAttributeType) type, (OffsetFields) introspected);
        } else if (type instanceof ArrayAttributeType) {
            return layout((ArrayAttributeType) type, introspected);
        } else if (type instanceof MatrixAttributeType) {
            return layout((MatrixAttributeType) type, (OffsetStride) introspected);
        } else if (type instanceof VectorAttributeType) {
            return layout((VectorAttributeType) type);
        } else {
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    private static StructLayout layout(StructAttributeType type, OffsetFields introspected) {
        AttributeLayout[] fieldLayouts = IntStream.range(0, type.fieldCount())
                .mapToObj(i -> layout(type.fieldType(i), introspected.fields[i]))
                .toArray(AttributeLayout[]::new);

        long[] fieldOffsets = new long[fieldLayouts.length];
        long size = 0L;
        for (int i = 0; i < fieldLayouts.length; i++) {
            fieldOffsets[i] = introspected.fields[i].offset - introspected.offset;
            size = Math.max(size, introspected.fields[i].offset - introspected.offset + fieldLayouts[i].size());
        }

        return new StructLayout(size, 0L, fieldLayouts, fieldOffsets);
    }

    private static ArrayLayout layout(ArrayAttributeType type, Offset introspected) {
        AttributeLayout elementLayout;
        int stride;
        if (introspected instanceof OffsetStride) {
            elementLayout = layout(type.elementType(), new Offset(0));
            stride = ((OffsetStride) introspected).stride;
        } else {
            OffsetFields offsetFields = (OffsetFields) introspected;
            checkArg(offsetFields.fields[0].offset == introspected.offset);
            stride = notNegative(offsetFields.fields[1].offset - offsetFields.fields[0].offset);
            elementLayout = layout(type.elementType(), offsetFields.fields[0]);

            for (int index = 1; index < type.elementCount(); index++) {
                checkArg(offsetFields.fields[index].offset == introspected.offset + index * stride);
                checkArg(elementLayout.equals(layout(type.elementType(), offsetFields.fields[index])), "array elements have incompatible layouts!");
            }
        }
        return new ArrayLayout(type.elementCount() * (long) stride, 1L, elementLayout, stride, type.elementCount());
    }

    private static MatrixLayout layout(MatrixAttributeType type, OffsetStride introspected) {
        return new MatrixLayout(type.cols() * (long) introspected.stride, 1L, layout(type.colType()), introspected.stride, type.cols());
    }

    private static VectorLayout layout(VectorAttributeType type) {
        return new VectorLayout(type.componentType().interpretedType().size() * (long) type.components(), 1L, type.componentType(),
                JavaPrimitiveType.from(type.componentType().interpretedType()),
                type.components());
    }

    @RequiredArgsConstructor
    private static class Offset {
        public final int offset;
    }

    private static class OffsetStride extends Offset {
        public final int stride;

        public OffsetStride(int offset, int stride) {
            super(offset);
            this.stride = stride;
        }
    }

    private static class OffsetFields extends Offset {
        public final Offset[] fields;

        public OffsetFields(int offset, Offset[] fields) {
            super(offset);
            this.fields = fields;
        }
    }
}
