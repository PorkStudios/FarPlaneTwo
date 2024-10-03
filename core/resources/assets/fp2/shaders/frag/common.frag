/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-$today.year DaPorkchop_
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

#pragma once

#include <"fp2:shaders/common.glsl">
#include <"fp2:shaders/util/texture_uvs_technique.glsl"> //stateAndFaceIndexToTexQuadList(), quadIndexToQuad()

//
//
// INPUTS
//
//

in vec3 vs_out_pos;
in vec2 vs_out_light;

flat in vec3 vs_out_color;
flat in vec3 vs_out_base_pos;
flat in uint vs_out_state;

#define fs_in_pos (vs_out_pos)
#define fs_in_light (vs_out_light)

#define fs_in_color (vs_out_color)
#define fs_in_base_pos (vs_out_base_pos)
#define fs_in_state (uint(vs_out_state))

//
//
// OUTPUTS
//
//

out vec4 color;

//
//
// TEXTURES
//
//

uniform sampler2D TEXTURE_ATLAS_SAMPLER_NAME;
uniform sampler2D LIGHTMAP_SAMPLER_NAME;

//
//
// UTILITIES
//
//

vec3 normalVector() {
    vec3 fdx = dFdx(fs_in_pos);
    vec3 fdy = dFdy(fs_in_pos);
    return normalize(cross(fdx, fdy));
}

vec2 texUvFactor(vec3 normal, vec3 pos)  {
    vec3 delta = pos - fs_in_base_pos;

    vec3 s = sign(normal);
    normal = -abs(normal);

    //does this have some special name? i have no idea, but i'm pretty proud of it
    vec2 uv_factor = vec2(0.);
    uv_factor += vec2(1. - delta.x, -s.y * delta.z) * normal.y;
    uv_factor += vec2(-s.z * delta.x, delta.y) * normal.z;
    uv_factor += vec2(s.x * delta.z, delta.y) * normal.x;

    return uv_factor;
}

vec4 sampleTerrain(vec3 normal)  {
    vec2 factor = texUvFactor(normal, fs_in_pos);

    uint state = fs_in_state;
    uint faceIndex = normalToFaceIndex(normal);
    TexQuadList list = stateAndFaceIndexToTexQuadList(state, faceIndex);

    PackedBakedQuad quad = quadIndexToQuad(list.first);
    vec4 quadCoords = quad.coords;
    float tint = quad.tint;

    vec2 uv = mix(quadCoords.st, quadCoords.pq, factor);
    vec4 color_out = textureGrad(TEXTURE_ATLAS_SAMPLER_NAME, mix(quadCoords.st, quadCoords.pq, fract(factor)), dFdx(uv), dFdy(uv));

    //apply tint if the quad allows it (branchless implementation)
    color_out.rgb *= max(fs_in_color, vec3(tint));

    //this shouldn't be too bad performance-wise, because in all likelihood it'll have the same number of loops for all neighboring fragments
    // almost all the time
    for (uint i = list.first + 1u; i < list.last; i++) {
        quad = quadIndexToQuad(i);
        quadCoords = quad.coords;
        tint = quad.tint;

        //raw color
        uv = mix(quadCoords.st, quadCoords.pq, factor);
        vec4 frag_color = textureGrad(TEXTURE_ATLAS_SAMPLER_NAME, mix(quadCoords.st, quadCoords.pq, fract(factor)), dFdx(uv), dFdy(uv));

        //possibly apply tint (branchless implementation)
        frag_color.rgb *= max(fs_in_color, vec3(tint));

        //apply texture over previous layers if possible (branchless implementation)
        color_out = color_out * (1. - frag_color.a) + frag_color * frag_color.a;
    }

    return color_out;
}
