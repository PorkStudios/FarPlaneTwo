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
    FLOAT(GL_FLOAT, GLSLTypeCategory.VALUE),// 	float
    FLOAT_VEC2(GL_FLOAT_VEC2, GLSLTypeCategory.VALUE),// 	vec2
    FLOAT_VEC3(GL_FLOAT_VEC3, GLSLTypeCategory.VALUE),// 	vec3
    FLOAT_VEC4(GL_FLOAT_VEC4, GLSLTypeCategory.VALUE),// 	vec4
    DOUBLE(GL_DOUBLE, GLSLTypeCategory.VALUE),// 	double
    DOUBLE_VEC2(GL_DOUBLE_VEC2, GLSLTypeCategory.VALUE),// 	dvec2
    DOUBLE_VEC3(GL_DOUBLE_VEC3, GLSLTypeCategory.VALUE),// 	dvec3
    DOUBLE_VEC4(GL_DOUBLE_VEC4, GLSLTypeCategory.VALUE),// 	dvec4
    INT(GL_INT, GLSLTypeCategory.VALUE),// 	int
    INT_VEC2(GL_INT_VEC2, GLSLTypeCategory.VALUE),// 	ivec2
    INT_VEC3(GL_INT_VEC3, GLSLTypeCategory.VALUE),// 	ivec3
    INT_VEC4(GL_INT_VEC4, GLSLTypeCategory.VALUE),// 	ivec4
    UNSIGNED_INT(GL_UNSIGNED_INT, GLSLTypeCategory.VALUE),// 	unsigned int
    UNSIGNED_INT_VEC2(GL_UNSIGNED_INT_VEC2, GLSLTypeCategory.VALUE),// 	uvec2
    UNSIGNED_INT_VEC3(GL_UNSIGNED_INT_VEC3, GLSLTypeCategory.VALUE),// 	uvec3
    UNSIGNED_INT_VEC4(GL_UNSIGNED_INT_VEC4, GLSLTypeCategory.VALUE),// 	uvec4
    BOOL(GL_BOOL, GLSLTypeCategory.VALUE),// 	bool
    BOOL_VEC2(GL_BOOL_VEC2, GLSLTypeCategory.VALUE),// 	bvec2
    BOOL_VEC3(GL_BOOL_VEC3, GLSLTypeCategory.VALUE),// 	bvec3
    BOOL_VEC4(GL_BOOL_VEC4, GLSLTypeCategory.VALUE),// 	bvec4
    FLOAT_MAT2(GL_FLOAT_MAT2, GLSLTypeCategory.VALUE),// 	mat2
    FLOAT_MAT3(GL_FLOAT_MAT3, GLSLTypeCategory.VALUE),// 	mat3
    FLOAT_MAT4(GL_FLOAT_MAT4, GLSLTypeCategory.VALUE),// 	mat4
    FLOAT_MAT2x3(GL_FLOAT_MAT2x3, GLSLTypeCategory.VALUE),// 	mat2x3
    FLOAT_MAT2x4(GL_FLOAT_MAT2x4, GLSLTypeCategory.VALUE),// 	mat2x4
    FLOAT_MAT3x2(GL_FLOAT_MAT3x2, GLSLTypeCategory.VALUE),// 	mat3x2
    FLOAT_MAT3x4(GL_FLOAT_MAT3x4, GLSLTypeCategory.VALUE),// 	mat3x4
    FLOAT_MAT4x2(GL_FLOAT_MAT4x2, GLSLTypeCategory.VALUE),// 	mat4x2
    FLOAT_MAT4x3(GL_FLOAT_MAT4x3, GLSLTypeCategory.VALUE),// 	mat4x3
    DOUBLE_MAT2(GL_DOUBLE_MAT2, GLSLTypeCategory.VALUE),// 	dmat2
    DOUBLE_MAT3(GL_DOUBLE_MAT3, GLSLTypeCategory.VALUE),// 	dmat3
    DOUBLE_MAT4(GL_DOUBLE_MAT4, GLSLTypeCategory.VALUE),// 	dmat4
    DOUBLE_MAT2x3(GL_DOUBLE_MAT2x3, GLSLTypeCategory.VALUE),// 	dmat2x3
    DOUBLE_MAT2x4(GL_DOUBLE_MAT2x4, GLSLTypeCategory.VALUE),// 	dmat2x4
    DOUBLE_MAT3x2(GL_DOUBLE_MAT3x2, GLSLTypeCategory.VALUE),// 	dmat3x2
    DOUBLE_MAT3x4(GL_DOUBLE_MAT3x4, GLSLTypeCategory.VALUE),// 	dmat3x4
    DOUBLE_MAT4x2(GL_DOUBLE_MAT4x2, GLSLTypeCategory.VALUE),// 	dmat4x2
    DOUBLE_MAT4x3(GL_DOUBLE_MAT4x3, GLSLTypeCategory.VALUE),// 	dmat4x3

    SAMPLER_1D(GL_SAMPLER_1D, GLSLTypeCategory.SAMPLER),// 	sampler1D
    SAMPLER_2D(GL_SAMPLER_2D, GLSLTypeCategory.SAMPLER),// 	sampler2D
    SAMPLER_3D(GL_SAMPLER_3D, GLSLTypeCategory.SAMPLER),// 	sampler3D
    SAMPLER_CUBE(GL_SAMPLER_CUBE, GLSLTypeCategory.SAMPLER),// 	samplerCube
    SAMPLER_1D_SHADOW(GL_SAMPLER_1D_SHADOW, GLSLTypeCategory.SAMPLER),// 	sampler1DShadow
    SAMPLER_2D_SHADOW(GL_SAMPLER_2D_SHADOW, GLSLTypeCategory.SAMPLER),// 	sampler2DShadow
    SAMPLER_1D_ARRAY(GL_SAMPLER_1D_ARRAY, GLSLTypeCategory.SAMPLER),// 	sampler1DArray
    SAMPLER_2D_ARRAY(GL_SAMPLER_2D_ARRAY, GLSLTypeCategory.SAMPLER),// 	sampler2DArray
    SAMPLER_1D_ARRAY_SHADOW(GL_SAMPLER_1D_ARRAY_SHADOW, GLSLTypeCategory.SAMPLER),// 	sampler1DArrayShadow
    SAMPLER_2D_ARRAY_SHADOW(GL_SAMPLER_2D_ARRAY_SHADOW, GLSLTypeCategory.SAMPLER),// 	sampler2DArrayShadow
    SAMPLER_2D_MULTISAMPLE(GL_SAMPLER_2D_MULTISAMPLE, GLSLTypeCategory.SAMPLER),// 	sampler2DMS
    SAMPLER_2D_MULTISAMPLE_ARRAY(GL_SAMPLER_2D_MULTISAMPLE_ARRAY, GLSLTypeCategory.SAMPLER),// 	sampler2DMSArray
    SAMPLER_CUBE_SHADOW(GL_SAMPLER_CUBE_SHADOW, GLSLTypeCategory.SAMPLER),// 	samplerCubeShadow
    SAMPLER_BUFFER(GL_SAMPLER_BUFFER, GLSLTypeCategory.SAMPLER),// 	samplerBuffer
    SAMPLER_2D_RECT(GL_SAMPLER_2D_RECT, GLSLTypeCategory.SAMPLER),// 	sampler2DRect
    SAMPLER_2D_RECT_SHADOW(GL_SAMPLER_2D_RECT_SHADOW, GLSLTypeCategory.SAMPLER),// 	sampler2DRectShadow
    INT_SAMPLER_1D(GL_INT_SAMPLER_1D, GLSLTypeCategory.SAMPLER),// 	isampler1D
    INT_SAMPLER_2D(GL_INT_SAMPLER_2D, GLSLTypeCategory.SAMPLER),// 	isampler2D
    INT_SAMPLER_3D(GL_INT_SAMPLER_3D, GLSLTypeCategory.SAMPLER),// 	isampler3D
    INT_SAMPLER_CUBE(GL_INT_SAMPLER_CUBE, GLSLTypeCategory.SAMPLER),// 	isamplerCube
    INT_SAMPLER_1D_ARRAY(GL_INT_SAMPLER_1D_ARRAY, GLSLTypeCategory.SAMPLER),// 	isampler1DArray
    INT_SAMPLER_2D_ARRAY(GL_INT_SAMPLER_2D_ARRAY, GLSLTypeCategory.SAMPLER),// 	isampler2DArray
    INT_SAMPLER_2D_MULTISAMPLE(GL_INT_SAMPLER_2D_MULTISAMPLE, GLSLTypeCategory.SAMPLER),// 	isampler2DMS
    INT_SAMPLER_2D_MULTISAMPLE_ARRAY(GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY, GLSLTypeCategory.SAMPLER),// 	isampler2DMSArray
    INT_SAMPLER_BUFFER(GL_INT_SAMPLER_BUFFER, GLSLTypeCategory.SAMPLER),// 	isamplerBuffer
    INT_SAMPLER_2D_RECT(GL_INT_SAMPLER_2D_RECT, GLSLTypeCategory.SAMPLER),// 	isampler2DRect
    UNSIGNED_INT_SAMPLER_1D(GL_UNSIGNED_INT_SAMPLER_1D, GLSLTypeCategory.SAMPLER),// 	usampler1D
    UNSIGNED_INT_SAMPLER_2D(GL_UNSIGNED_INT_SAMPLER_2D, GLSLTypeCategory.SAMPLER),// 	usampler2D
    UNSIGNED_INT_SAMPLER_3D(GL_UNSIGNED_INT_SAMPLER_3D, GLSLTypeCategory.SAMPLER),// 	usampler3D
    UNSIGNED_INT_SAMPLER_CUBE(GL_UNSIGNED_INT_SAMPLER_CUBE, GLSLTypeCategory.SAMPLER),// 	usamplerCube
    UNSIGNED_INT_SAMPLER_1D_ARRAY(GL_UNSIGNED_INT_SAMPLER_1D_ARRAY, GLSLTypeCategory.SAMPLER),// 	usampler2DArray
    UNSIGNED_INT_SAMPLER_2D_ARRAY(GL_UNSIGNED_INT_SAMPLER_2D_ARRAY, GLSLTypeCategory.SAMPLER),// 	usampler2DArray
    UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE(GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE, GLSLTypeCategory.SAMPLER),// 	usampler2DMS
    UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY(GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY, GLSLTypeCategory.SAMPLER),// 	usampler2DMSArray
    UNSIGNED_INT_SAMPLER_BUFFER(GL_UNSIGNED_INT_SAMPLER_BUFFER, GLSLTypeCategory.SAMPLER),// 	usamplerBuffer
    UNSIGNED_INT_SAMPLER_2D_RECT(GL_UNSIGNED_INT_SAMPLER_2D_RECT, GLSLTypeCategory.SAMPLER),//  usampler2DRect

    IMAGE_1D(GL_IMAGE_1D, GLSLTypeCategory.IMAGE),//  image1D
    IMAGE_2D(GL_IMAGE_2D, GLSLTypeCategory.IMAGE),//  image2D
    IMAGE_3D(GL_IMAGE_3D, GLSLTypeCategory.IMAGE),//  image3D
    IMAGE_2D_RECT(GL_IMAGE_2D_RECT, GLSLTypeCategory.IMAGE),//  image2DRect
    IMAGE_CUBE(GL_IMAGE_CUBE, GLSLTypeCategory.IMAGE),//  imageCube
    IMAGE_BUFFER(GL_IMAGE_BUFFER, GLSLTypeCategory.IMAGE),//  imageBuffer
    IMAGE_1D_ARRAY(GL_IMAGE_1D_ARRAY, GLSLTypeCategory.IMAGE),//  image1DArray
    IMAGE_2D_ARRAY(GL_IMAGE_2D_ARRAY, GLSLTypeCategory.IMAGE),//  image2DArray
    IMAGE_CUBE_MAP_ARRAY(GL_IMAGE_CUBE_MAP_ARRAY, GLSLTypeCategory.IMAGE),//  imageCubeArray
    IMAGE_2D_MULTISAMPLE(GL_IMAGE_2D_MULTISAMPLE, GLSLTypeCategory.IMAGE),//  image2DMS
    IMAGE_2D_MULTISAMPLE_ARRAY(GL_IMAGE_2D_MULTISAMPLE_ARRAY, GLSLTypeCategory.IMAGE),//  image2DMSArray
    INT_IMAGE_1D(GL_INT_IMAGE_1D, GLSLTypeCategory.IMAGE),//  iimage1D
    INT_IMAGE_2D(GL_INT_IMAGE_2D, GLSLTypeCategory.IMAGE),//  iimage2D
    INT_IMAGE_3D(GL_INT_IMAGE_3D, GLSLTypeCategory.IMAGE),//  iimage3D
    INT_IMAGE_2D_RECT(GL_INT_IMAGE_2D_RECT, GLSLTypeCategory.IMAGE),//  iimage2DRect
    INT_IMAGE_CUBE(GL_INT_IMAGE_CUBE, GLSLTypeCategory.IMAGE),//  iimageCube
    INT_IMAGE_BUFFER(GL_INT_IMAGE_BUFFER, GLSLTypeCategory.IMAGE),//  iimageBuffer
    INT_IMAGE_1D_ARRAY(GL_INT_IMAGE_1D_ARRAY, GLSLTypeCategory.IMAGE),//  iimage1DArray
    INT_IMAGE_2D_ARRAY(GL_INT_IMAGE_2D_ARRAY, GLSLTypeCategory.IMAGE),//  iimage2DArray
    INT_IMAGE_CUBE_MAP_ARRAY(GL_INT_IMAGE_CUBE_MAP_ARRAY, GLSLTypeCategory.IMAGE),//  iimageCubeArray
    INT_IMAGE_2D_MULTISAMPLE(GL_INT_IMAGE_2D_MULTISAMPLE, GLSLTypeCategory.IMAGE),//  iimage2DMS
    INT_IMAGE_2D_MULTISAMPLE_ARRAY(GL_INT_IMAGE_2D_MULTISAMPLE_ARRAY, GLSLTypeCategory.IMAGE),//  iimage2DMSArray
    UNSIGNED_INT_IMAGE_1D(GL_UNSIGNED_INT_IMAGE_1D, GLSLTypeCategory.IMAGE),//  uimage1D
    UNSIGNED_INT_IMAGE_2D(GL_UNSIGNED_INT_IMAGE_2D, GLSLTypeCategory.IMAGE),//  uimage2D
    UNSIGNED_INT_IMAGE_3D(GL_UNSIGNED_INT_IMAGE_3D, GLSLTypeCategory.IMAGE),//  uimage3D
    UNSIGNED_INT_IMAGE_2D_RECT(GL_UNSIGNED_INT_IMAGE_2D_RECT, GLSLTypeCategory.IMAGE),//  uimage2DRect
    UNSIGNED_INT_IMAGE_CUBE(GL_UNSIGNED_INT_IMAGE_CUBE, GLSLTypeCategory.IMAGE),//  uimageCube
    UNSIGNED_INT_IMAGE_BUFFER(GL_UNSIGNED_INT_IMAGE_BUFFER, GLSLTypeCategory.IMAGE),//  uimageBuffer
    UNSIGNED_INT_IMAGE_1D_ARRAY(GL_UNSIGNED_INT_IMAGE_1D_ARRAY, GLSLTypeCategory.IMAGE),//  uimage1DArray
    UNSIGNED_INT_IMAGE_2D_ARRAY(GL_UNSIGNED_INT_IMAGE_2D_ARRAY, GLSLTypeCategory.IMAGE),//  uimage2DArray
    UNSIGNED_INT_IMAGE_CUBE_MAP_ARRAY(GL_UNSIGNED_INT_IMAGE_CUBE_MAP_ARRAY, GLSLTypeCategory.IMAGE),//  uimageCubeArray
    UNSIGNED_INT_IMAGE_2D_MULTISAMPLE(GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE, GLSLTypeCategory.IMAGE),//  uimage2DMS
    UNSIGNED_INT_IMAGE_2D_MULTISAMPLE_ARRAY(GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE_ARRAY, GLSLTypeCategory.IMAGE),//  uimage2DMSArray
    ;

    private final int id;
    private final GLSLTypeCategory category;

    public static GLSLType get(int name) {
        for (GLSLType type : values()) {
            if (type.id() == name) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown shader type: " + name);
    }
}
