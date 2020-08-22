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

layout(points) in;
layout(triangle_strip, max_vertices = 3) out;

in VS_OUT {
    vec3 pos;

    flat vec4 color;
} gs_in[];

in gl_PerVertex {
    vec4 gl_Position;
    float gl_PointSize;
    float gl_ClipDistance[];
} gl_in[];

out GS_OUT {
    vec3 pos;

    flat vec4 color;
} gs_out;

out gl_PerVertex {
    vec4 gl_Position;
};

void main() {
    gl_Position = cameraTransform(gl_in[0].gl_Position);
    gs_out.pos = gs_in[0].pos;
    gs_out.color = gs_in[0].color;
    EmitVertex();

    gl_Position = cameraTransform(gl_in[0].gl_Position + vec4(0., 1., 0., 0.));
    gs_out.pos = gs_in[0].pos + vec3(0., 1., 0.);
    EmitVertex();

    gl_Position = cameraTransform(gl_in[0].gl_Position + vec4(1., 0., 0., 0.));
    gs_out.pos = gs_in[0].pos + vec3(1., 0., 0.);
    EmitVertex();
}
