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

package net.daporkchop.fp2.gl.shader.introspection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static net.daporkchop.fp2.gl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum GLSLType {
    FLOAT(false),// 	float
    FLOAT_VEC2(false),// 	vec2
    FLOAT_VEC3(false),// 	vec3
    FLOAT_VEC4(false),// 	vec4
    DOUBLE(false),// 	double
    DOUBLE_VEC2(false),// 	dvec2
    DOUBLE_VEC3(false),// 	dvec3
    DOUBLE_VEC4(false),// 	dvec4
    INT(false),// 	int
    INT_VEC2(false),// 	ivec2
    INT_VEC3(false),// 	ivec3
    INT_VEC4(false),// 	ivec4
    UNSIGNED_INT(false),// 	unsigned int
    UNSIGNED_INT_VEC2(false),// 	uvec2
    UNSIGNED_INT_VEC3(false),// 	uvec3
    UNSIGNED_INT_VEC4(false),// 	uvec4
    BOOL(false),// 	bool
    BOOL_VEC2(false),// 	bvec2
    BOOL_VEC3(false),// 	bvec3
    BOOL_VEC4(false),// 	bvec4
    FLOAT_MAT2(false),// 	mat2
    FLOAT_MAT3(false),// 	mat3
    FLOAT_MAT4(false),// 	mat4
    FLOAT_MAT2x3(false),// 	mat2x3
    FLOAT_MAT2x4(false),// 	mat2x4
    FLOAT_MAT3x2(false),// 	mat3x2
    FLOAT_MAT3x4(false),// 	mat3x4
    FLOAT_MAT4x2(false),// 	mat4x2
    FLOAT_MAT4x3(false),// 	mat4x3
    DOUBLE_MAT2(false),// 	dmat2
    DOUBLE_MAT3(false),// 	dmat3
    DOUBLE_MAT4(false),// 	dmat4
    DOUBLE_MAT2x3(false),// 	dmat2x3
    DOUBLE_MAT2x4(false),// 	dmat2x4
    DOUBLE_MAT3x2(false),// 	dmat3x2
    DOUBLE_MAT3x4(false),// 	dmat3x4
    DOUBLE_MAT4x2(false),// 	dmat4x2
    DOUBLE_MAT4x3(false),// 	dmat4x3
    SAMPLER_1D(true),// 	sampler1D
    SAMPLER_2D(true),// 	sampler2D
    SAMPLER_3D(true),// 	sampler3D
    SAMPLER_CUBE(true),// 	samplerCube
    SAMPLER_1D_SHADOW(true),// 	sampler1DShadow
    SAMPLER_2D_SHADOW(true),// 	sampler2DShadow
    SAMPLER_1D_ARRAY(true),// 	sampler1DArray
    SAMPLER_2D_ARRAY(true),// 	sampler2DArray
    SAMPLER_1D_ARRAY_SHADOW(true),// 	sampler1DArrayShadow
    SAMPLER_2D_ARRAY_SHADOW(true),// 	sampler2DArrayShadow
    SAMPLER_2D_MULTISAMPLE(true),// 	sampler2DMS
    SAMPLER_2D_MULTISAMPLE_ARRAY(true),// 	sampler2DMSArray
    SAMPLER_CUBE_SHADOW(true),// 	samplerCubeShadow
    SAMPLER_BUFFER(true),// 	samplerBuffer
    SAMPLER_2D_RECT(true),// 	sampler2DRect
    SAMPLER_2D_RECT_SHADOW(true),// 	sampler2DRectShadow
    INT_SAMPLER_1D(true),// 	isampler1D
    INT_SAMPLER_2D(true),// 	isampler2D
    INT_SAMPLER_3D(true),// 	isampler3D
    INT_SAMPLER_CUBE(true),// 	isamplerCube
    INT_SAMPLER_1D_ARRAY(true),// 	isampler1DArray
    INT_SAMPLER_2D_ARRAY(true),// 	isampler2DArray
    INT_SAMPLER_2D_MULTISAMPLE(true),// 	isampler2DMS
    INT_SAMPLER_2D_MULTISAMPLE_ARRAY(true),// 	isampler2DMSArray
    INT_SAMPLER_BUFFER(true),// 	isamplerBuffer
    INT_SAMPLER_2D_RECT(true),// 	isampler2DRect
    UNSIGNED_INT_SAMPLER_1D(true),// 	usampler1D
    UNSIGNED_INT_SAMPLER_2D(true),// 	usampler2D
    UNSIGNED_INT_SAMPLER_3D(true),// 	usampler3D
    UNSIGNED_INT_SAMPLER_CUBE(true),// 	usamplerCube
    UNSIGNED_INT_SAMPLER_1D_ARRAY(true),// 	usampler2DArray
    UNSIGNED_INT_SAMPLER_2D_ARRAY(true),// 	usampler2DArray
    UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE(true),// 	usampler2DMS
    UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY(true),// 	usampler2DMSArray
    UNSIGNED_INT_SAMPLER_BUFFER(true),// 	usamplerBuffer
    UNSIGNED_INT_SAMPLER_2D_RECT(true),//  usampler2DRect
    ;

    private final boolean isSampler;

    public static GLSLType get(int name) {
        switch (name) {
            case GL_FLOAT:
                return FLOAT;
            case GL_FLOAT_VEC2:
                return FLOAT_VEC2;
            case GL_FLOAT_VEC3:
                return FLOAT_VEC3;
            case GL_FLOAT_VEC4:
                return FLOAT_VEC4;
            case GL_DOUBLE:
                return DOUBLE;
            case GL_DOUBLE_VEC2:
                return DOUBLE_VEC2;
            case GL_DOUBLE_VEC3:
                return DOUBLE_VEC3;
            case GL_DOUBLE_VEC4:
                return DOUBLE_VEC4;
            case GL_INT:
                return INT;
            case GL_INT_VEC2:
                return INT_VEC2;
            case GL_INT_VEC3:
                return INT_VEC3;
            case GL_INT_VEC4:
                return INT_VEC4;
            case GL_UNSIGNED_INT:
                return UNSIGNED_INT;
            case GL_UNSIGNED_INT_VEC2:
                return UNSIGNED_INT_VEC2;
            case GL_UNSIGNED_INT_VEC3:
                return UNSIGNED_INT_VEC3;
            case GL_UNSIGNED_INT_VEC4:
                return UNSIGNED_INT_VEC4;
            case GL_BOOL:
                return BOOL;
            case GL_BOOL_VEC2:
                return BOOL_VEC2;
            case GL_BOOL_VEC3:
                return BOOL_VEC3;
            case GL_BOOL_VEC4:
                return BOOL_VEC4;
            case GL_FLOAT_MAT2:
                return FLOAT_MAT2;
            case GL_FLOAT_MAT3:
                return FLOAT_MAT3;
            case GL_FLOAT_MAT4:
                return FLOAT_MAT4;
            case GL_FLOAT_MAT2x3:
                return FLOAT_MAT2x3;
            case GL_FLOAT_MAT2x4:
                return FLOAT_MAT2x4;
            case GL_FLOAT_MAT3x2:
                return FLOAT_MAT3x2;
            case GL_FLOAT_MAT3x4:
                return FLOAT_MAT3x4;
            case GL_FLOAT_MAT4x2:
                return FLOAT_MAT4x2;
            case GL_FLOAT_MAT4x3:
                return FLOAT_MAT4x3;
            case GL_DOUBLE_MAT2:
                return DOUBLE_MAT2;
            case GL_DOUBLE_MAT3:
                return DOUBLE_MAT3;
            case GL_DOUBLE_MAT4:
                return DOUBLE_MAT4;
            case GL_DOUBLE_MAT2x3:
                return DOUBLE_MAT2x3;
            case GL_DOUBLE_MAT2x4:
                return DOUBLE_MAT2x4;
            case GL_DOUBLE_MAT3x2:
                return DOUBLE_MAT3x2;
            case GL_DOUBLE_MAT3x4:
                return DOUBLE_MAT3x4;
            case GL_DOUBLE_MAT4x2:
                return DOUBLE_MAT4x2;
            case GL_DOUBLE_MAT4x3:
                return DOUBLE_MAT4x3;
            case GL_SAMPLER_1D:
                return SAMPLER_1D;
            case GL_SAMPLER_2D:
                return SAMPLER_2D;
            case GL_SAMPLER_3D:
                return SAMPLER_3D;
            case GL_SAMPLER_CUBE:
                return SAMPLER_CUBE;
            case GL_SAMPLER_1D_SHADOW:
                return SAMPLER_1D_SHADOW;
            case GL_SAMPLER_2D_SHADOW:
                return SAMPLER_2D_SHADOW;
            case GL_SAMPLER_1D_ARRAY:
                return SAMPLER_1D_ARRAY;
            case GL_SAMPLER_2D_ARRAY:
                return SAMPLER_2D_ARRAY;
            case GL_SAMPLER_1D_ARRAY_SHADOW:
                return SAMPLER_1D_ARRAY_SHADOW;
            case GL_SAMPLER_2D_ARRAY_SHADOW:
                return SAMPLER_2D_ARRAY_SHADOW;
            case GL_SAMPLER_2D_MULTISAMPLE:
                return SAMPLER_2D_MULTISAMPLE;
            case GL_SAMPLER_2D_MULTISAMPLE_ARRAY:
                return SAMPLER_2D_MULTISAMPLE_ARRAY;
            case GL_SAMPLER_CUBE_SHADOW:
                return SAMPLER_CUBE_SHADOW;
            case GL_SAMPLER_BUFFER:
                return SAMPLER_BUFFER;
            case GL_SAMPLER_2D_RECT:
                return SAMPLER_2D_RECT;
            case GL_SAMPLER_2D_RECT_SHADOW:
                return SAMPLER_2D_RECT_SHADOW;
            case GL_INT_SAMPLER_1D:
                return INT_SAMPLER_1D;
            case GL_INT_SAMPLER_2D:
                return INT_SAMPLER_2D;
            case GL_INT_SAMPLER_3D:
                return INT_SAMPLER_3D;
            case GL_INT_SAMPLER_CUBE:
                return INT_SAMPLER_CUBE;
            case GL_INT_SAMPLER_1D_ARRAY:
                return INT_SAMPLER_1D_ARRAY;
            case GL_INT_SAMPLER_2D_ARRAY:
                return INT_SAMPLER_2D_ARRAY;
            case GL_INT_SAMPLER_2D_MULTISAMPLE:
                return INT_SAMPLER_2D_MULTISAMPLE;
            case GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY:
                return INT_SAMPLER_2D_MULTISAMPLE_ARRAY;
            case GL_INT_SAMPLER_BUFFER:
                return INT_SAMPLER_BUFFER;
            case GL_INT_SAMPLER_2D_RECT:
                return INT_SAMPLER_2D_RECT;
            case GL_UNSIGNED_INT_SAMPLER_1D:
                return UNSIGNED_INT_SAMPLER_1D;
            case GL_UNSIGNED_INT_SAMPLER_2D:
                return UNSIGNED_INT_SAMPLER_2D;
            case GL_UNSIGNED_INT_SAMPLER_3D:
                return UNSIGNED_INT_SAMPLER_3D;
            case GL_UNSIGNED_INT_SAMPLER_CUBE:
                return UNSIGNED_INT_SAMPLER_CUBE;
            case GL_UNSIGNED_INT_SAMPLER_1D_ARRAY:
                return UNSIGNED_INT_SAMPLER_1D_ARRAY;
            case GL_UNSIGNED_INT_SAMPLER_2D_ARRAY:
                return UNSIGNED_INT_SAMPLER_2D_ARRAY;
            case GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE:
                return UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE;
            case GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY:
                return UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY;
            case GL_UNSIGNED_INT_SAMPLER_BUFFER:
                return UNSIGNED_INT_SAMPLER_BUFFER;
            case GL_UNSIGNED_INT_SAMPLER_2D_RECT:
                return UNSIGNED_INT_SAMPLER_2D_RECT;
            default:
                throw new IllegalArgumentException("unknown shader type: " + name);
        }
    }
}
