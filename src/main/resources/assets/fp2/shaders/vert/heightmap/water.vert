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

uniform float seaLevel;

void main(){
    ivec3 fullPos = vertexPos();
    ivec2 posXZ = fullPos.xz;

    HEIGHTMAP_TYPE center = sampleHeightmap(fullPos);

    dvec3 pos = dvec3(double(posXZ.x), seaLevel - .125, double(posXZ.y));
    vec3 relativePos = vec3(pos - camera.position); //convert to vec3 afterwards to minimize precision loss

    //give raw position to fragment shader
    vs_out.pos = vec3(pos);

    //set fog depth
    fog_out.depth = length(relativePos);

    //translate vertex position
    gl_Position = transformPoint(relativePos);

    //decode sky and block light
    vs_out.light = unpackWaterLight(center);

    vs_out.color = unpackWaterColor(center);
}
