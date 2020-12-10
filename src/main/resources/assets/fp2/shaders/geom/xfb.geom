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

#define PROVOKING (0)

vec3 normalVector() {
    return normalize(cross(gs_in[PROVOKING].pos - gs_in[1].pos, gs_in[PROVOKING].pos - gs_in[2].pos));
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
    vec4 color;
    vec2 uv;
    vec2 light;
} xfb_out;

//
//
// CONSTANTS
//
//

const vec2 uv_factors[3] = vec2[](vec2(0., 1.), vec2(1., 0.), vec2(0., 0.));

void main() {
    vec3 normal = normalVector();
    float diffuse = diffuseLight(normal);

    ivec2 list = quad_lists[gs_in[PROVOKING].state * 6 + normalToFaceIndex(normal)];
    BakedQuad quad = quad_data[list[0]];

    vec4 color = vec4(max(gs_in[PROVOKING].color, vec3(quad.tintFactor)) * diffuse, 1.);

    for (int i = 0; i < 3; i++) {
        xfb_out.pos = gs_in[i].pos;
        xfb_out.uv = mix(vec2(quad.minU, quad.minV), vec2(quad.maxU, quad.maxV), uv_factors[i]);
        xfb_out.light = gs_in[i].light * 256.;

        xfb_out.color = color;

        EmitVertex();
    }
    EndPrimitive();
}
