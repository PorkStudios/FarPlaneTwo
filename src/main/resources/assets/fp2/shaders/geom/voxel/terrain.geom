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

#define TYPE triangle_strip

void pre(vec4 bbMin, vec4 bbMax, vec4 cam)    {
}

void fancyClamp(inout vec3 pos)   {
    //pos = clamp(pos, vec3(0.), vec3(1.));
}

void quad(vec4 bbMin, vec4 bbMax, vec4 cam, int i)    {
    gs_out.color = vec4(1.);

    i = (i >> 1) | (i & 1);

    for (int j = 0; j < 3; j++) {
        ivec3 vec = connections[i][j];
        vec4 neighborOffset = gs_in[0].other[((vec.x + 1) * 3 + vec.y + 1) * 3 + vec.z + 1];
        vec4 worldPos = neighborOffset + vec4(vec3(vec), 0.);

        fancyClamp(worldPos.xyz);
        worldPos += bbMin;

        gs_out.pos = worldPos.xyz;
        gl_Position = cameraTransform(worldPos - cam);
        EmitVertex();
    }
    EndPrimitive();
}
