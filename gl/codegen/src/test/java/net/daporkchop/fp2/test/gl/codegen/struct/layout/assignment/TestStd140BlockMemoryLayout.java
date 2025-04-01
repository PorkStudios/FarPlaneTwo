/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2025 DaPorkchop_
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

package net.daporkchop.fp2.test.gl.codegen.struct.layout.assignment;

import net.daporkchop.fp2.gl.attribute.AttributeStruct;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.MatrixType;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarType;
import net.daporkchop.fp2.gl.attribute.annotation.VectorType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ComponentType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.JavaPrimitiveType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.ShaderPrimitiveType;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeFactory;
import net.daporkchop.fp2.gl.codegen.struct.attribute.StructAttributeType;
import net.daporkchop.fp2.gl.codegen.struct.layout.AttributeLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.LayoutInfo;
import net.daporkchop.fp2.gl.codegen.struct.layout.MatrixLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.StructLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.VectorLayout;
import net.daporkchop.fp2.gl.codegen.struct.layout.assignment.Std140BlockMemoryLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author DaPorkchop_
 */
public class TestStd140BlockMemoryLayout {
    @Attribute(name = "mat4_0", typeMatrix = @MatrixType(rows = 4, cols = 4, componentType = @ScalarType(float.class)))
    @Attribute(name = "mat4_1", typeMatrix = @MatrixType(rows = 4, cols = 4, componentType = @ScalarType(float.class)))
    @Attribute(name = "float_0", typeScalar = @ScalarType(float.class))
    @Attribute(name = "float_1", typeScalar = @ScalarType(float.class))
    @Attribute(name = "vec2_0", typeVector = @VectorType(components = 2, componentType = @ScalarType(float.class)))
    @Attribute(name = "vec4_0", typeVector = @VectorType(components = 4, componentType = @ScalarType(float.class)))
    private interface Simple extends AttributeStruct {
    }

    @Test
    public void testSimple() {
        testStruct(Simple.class,
                new StructLayout(
                        128 + 16 + 16, 16,
                        new AttributeLayout[] {
                                new MatrixLayout(64, 16, new VectorLayout(16, 16, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 4), 16, 4),
                                new MatrixLayout(64, 16, new VectorLayout(16, 16, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 4), 16, 4),
                                new VectorLayout(4, 4, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 1),
                                new VectorLayout(4, 4, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 1),
                                new VectorLayout(8, 8, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 2),
                                new VectorLayout(16, 16, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 4),
                        },
                        new long[] {
                                0,
                                64,
                                128,
                                132,
                                136,
                                144,
                        }));
    }

    @Attribute(name = "float_0", typeScalar = @ScalarType(float.class))
    @Attribute(name = "vec2_0", typeVector = @VectorType(components = 2, componentType = @ScalarType(float.class)))
    @Attribute(name = "vec2_1", typeVector = @VectorType(components = 2, componentType = @ScalarType(float.class)))
    @Attribute(name = "float_1", typeScalar = @ScalarType(float.class))
    private interface Float_Vec2 extends AttributeStruct {
    }

    @Test
    public void testFloat_Vec2() {
        testStruct(Float_Vec2.class,
                new StructLayout(
                        32, 16,
                        new AttributeLayout[] {
                                new VectorLayout(4, 4, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 1),
                                new VectorLayout(8, 8, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 2),
                                new VectorLayout(8, 8, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 2),
                                new VectorLayout(4, 4, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 1),
                        },
                        new long[] {
                                0,
                                8,
                                16,
                                24,
                        }));
    }

    @Attribute(name = "float_0", typeScalar = @ScalarType(float.class))
    @Attribute(name = "vec3_0", typeVector = @VectorType(components = 3, componentType = @ScalarType(float.class)))
    @Attribute(name = "vec3_1", typeVector = @VectorType(components = 3, componentType = @ScalarType(float.class)))
    @Attribute(name = "float_1", typeScalar = @ScalarType(float.class))
    private interface Float_Vec3 extends AttributeStruct {
    }

    @Test
    public void testFloat_Vec3() {
        testStruct(Float_Vec3.class,
                new StructLayout(
                        48, 16,
                        new AttributeLayout[] {
                                new VectorLayout(4, 4, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 1),
                                new VectorLayout(12, 16, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 3),
                                new VectorLayout(12, 16, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 3),
                                new VectorLayout(4, 4, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 1),
                        },
                        new long[] {
                                0,
                                16,
                                32,
                                44,
                        }));
    }

    @Attribute(name = "float_0", typeScalar = @ScalarType(float.class))
    @Attribute(name = "vec4_0", typeVector = @VectorType(components = 4, componentType = @ScalarType(float.class)))
    @Attribute(name = "vec4_1", typeVector = @VectorType(components = 4, componentType = @ScalarType(float.class)))
    @Attribute(name = "float_1", typeScalar = @ScalarType(float.class))
    private interface Float_Vec4 extends AttributeStruct {
    }

    @Test
    public void testFloat_Vec4() {
        testStruct(Float_Vec4.class,
                new StructLayout(
                        64, 16,
                        new AttributeLayout[] {
                                new VectorLayout(4, 4, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 1),
                                new VectorLayout(16, 16, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 4),
                                new VectorLayout(16, 16, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 4),
                                new VectorLayout(4, 4, new ComponentType(JavaPrimitiveType.FLOAT, ShaderPrimitiveType.FLOAT, false), JavaPrimitiveType.FLOAT, 1),
                        },
                        new long[] {
                                0,
                                16,
                                32,
                                48,
                        }));
    }

    private static void testStruct(Class<? extends AttributeStruct> structClass, AttributeLayout expectedLayout) {
        testStruct(StructAttributeFactory.struct(structClass), expectedLayout);
    }

    private static void testStruct(StructAttributeType type, AttributeLayout expectedLayout) {
        LayoutInfo layoutInfo = Std140BlockMemoryLayout.computeLayout(type);

        assertTrue(layoutInfo.interleaved());
        assertEquals("std140", layoutInfo.name());
        assertEquals(expectedLayout, layoutInfo.rootLayout());
    }
}
