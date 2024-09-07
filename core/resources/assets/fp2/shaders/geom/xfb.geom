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

#define PROVOKING (2)

vec3 normalVector() {
    return normalize(cross(gs_in[PROVOKING].pos - gs_in[0].pos, gs_in[PROVOKING].pos - gs_in[1].pos));
}

//
//
// GEOMETRY SHADER-SPECIFIC CONFIGURATION
//
//

layout(triangles) in;
layout(triangle_strip, max_vertices = 3) out;

//
//
// OUTPUTS
//
//

out XFB_OUT {
    vec3 pos;
    uint color;
    vec2 uv;
    uint light;

#ifdef OPTIFINE_SHADER
    vec3 normal;
    vec2 midTexCoord;
    vec4 tangent;
    vec3 entity;
#endif
} xfb_out;

//
//
// CONSTANTS
//
//

//TODO: this needs to be re-designed somehow, as half the texture is off by 90°
const vec2 uv_factors[3] = vec2[](vec2(0., 0.), vec2(1., 0.), vec2(1., 1.));

uint rgb_uint(ivec4 color) {
    return color.a << 24 | color.r << 16 | color.g << 8 | color.b;
}

uint xy_uint(ivec2 pos) {
    return pos.x << 16 | pos.y;
}

void main() {
    vec3 normal = normalVector();
    float diffuse = diffuseLight(normal);

    ivec2 list = quad_lists[gs_in[PROVOKING].state * 6 + normalToFaceIndex(normal)];
    BakedQuad quad = quad_data[list[0]];

    uint color_uint = rgb_uint(ivec4(ivec3(max(gs_in[PROVOKING].color, vec3(quad.tintFactor)) * diffuse * 255.), 1));

    for (int i = 0; i < 3; i++) {
        xfb_out.pos = gs_in[i].pos;
        xfb_out.uv = mix(vec2(quad.minU, quad.minV), vec2(quad.maxU, quad.maxV), uv_factors[i]);
        xfb_out.light = xy_uint(ivec2(gs_in[i].light) << 8);

#ifdef OPTIFINE_SHADER
        xfb_out.normal = normal;
        xfb_out.midTexCoord = mix(vec2(quad.minU, quad.minV), vec2(quad.maxU, quad.maxV), .5);
        xfb_out.tangent = vec4(gs_in[PROVOKING].pos - gs_in[0].pos, 1.);
        xfb_out.entity = vec3(float(gs_in[PROVOKING].state & 0xFF), 0., 0.);
#endif

        xfb_out.color = color_uint;

        EmitVertex();
    }
    EndPrimitive();
}
