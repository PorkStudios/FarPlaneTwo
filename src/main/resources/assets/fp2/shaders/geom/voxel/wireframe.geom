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

#define TYPE line_strip

void vertex(vec4 pos)   {
    gl_Position = cameraTransform(pos);
    EmitVertex();
}

const int lines[24] = int[](
0, 4, 1, 5, 2, 6, 3, 7,    // x-axis
0, 2, 1, 3, 4, 6, 5, 7,    // y-axis
0, 1, 2, 3, 4, 5, 6, 7
);

void line(int i, vec4 a, vec4 b, vec4 cam) {
    gs_out.color = (gs_in[0].connections & (1 << i)) == 0 ? vec4(0.) : vec4(1., 0., 0., 0.);
    vertex(a + (b - a) * vec4(ivec3(lines[i << 1]) >> ivec3(2, 1, 0) & 1, 0.) - cam);
    vertex(a + (b - a) * vec4(ivec3(lines[(i << 1) | 1]) >> ivec3(2, 1, 0) & 1, 0.) - cam);
    EndPrimitive();
}

void pre(vec4 bbMin, vec4 bbMax, vec4 cam)    {
    for (int i = 0; i < 12; i++)    {
        line(i, bbMin + vec4(.05), bbMax - vec4(.05), cam);
    }

    gs_out.color = vec4(0.);
    vec4 ownOffset = gs_in[0].other[0];
    bbMax = ownOffset + vec4(vec3(.025), 0.);
    bbMin = ownOffset - vec4(vec3(.025), 0.);

    vertex(vec4(bbMin.x, bbMin.y, bbMin.z, 1.) - cam);
    vertex(vec4(bbMin.x, bbMin.y, bbMax.z, 1.) - cam);
    vertex(vec4(bbMin.x, bbMax.y, bbMax.z, 1.) - cam);
    vertex(vec4(bbMin.x, bbMax.y, bbMin.z, 1.) - cam);
    EndPrimitive();

    vertex(vec4(bbMax.x, bbMin.y, bbMax.z, 1.) - cam);
    vertex(vec4(bbMax.x, bbMin.y, bbMin.z, 1.) - cam);
    vertex(vec4(bbMax.x, bbMax.y, bbMin.z, 1.) - cam);
    vertex(vec4(bbMax.x, bbMax.y, bbMax.z, 1.) - cam);
    EndPrimitive();

    vertex(vec4(bbMax.x, bbMin.y, bbMin.z, 1.) - cam);
    vertex(vec4(bbMin.x, bbMin.y, bbMin.z, 1.) - cam);
    vertex(vec4(bbMin.x, bbMax.y, bbMin.z, 1.) - cam);
    vertex(vec4(bbMax.x, bbMax.y, bbMin.z, 1.) - cam);
    EndPrimitive();

    vertex(vec4(bbMin.x, bbMin.y, bbMax.z, 1.) - cam);
    vertex(vec4(bbMax.x, bbMin.y, bbMax.z, 1.) - cam);
    vertex(vec4(bbMax.x, bbMax.y, bbMax.z, 1.) - cam);
    vertex(vec4(bbMin.x, bbMax.y, bbMax.z, 1.) - cam);
    EndPrimitive();
}

void quad(vec4 bbMin, vec4 bbMax, vec4 cam, int i)    {
}
