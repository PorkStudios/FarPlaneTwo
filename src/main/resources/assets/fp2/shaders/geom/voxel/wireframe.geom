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

void pre(vec4 bbMin, vec4 bbMax, vec4 cam)    {
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

    vec4 neighborOffset = gs_in[0].other[14];
    bbMax = bbMin + neighborOffset + vec4(vec3(.025), 0.);
    bbMin = bbMin + neighborOffset - vec4(vec3(.025), 0.);

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
    /*for (int j = 0; j < 4; j++) {
        //vec4 worldPos = pos + gs_in[0].other[connections[i][j]];
        ivec3 vec = connections[i][j];
        int index = ((vec.x + 1) * 3 + vec.y + 1) * 3 + vec.z + 1;
        vec4 neighborOffset = gs_in[0].other[index];
        vec4 worldPos = bbMin + vec4(neighborOffset.xyz, 0.);
        gl_Position = cameraTransform(worldPos - cam);
        EmitVertex();
    }
    EndPrimitive();*/
}
