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

//
//
// MACROS
//
//

#define GL_LINEAR (9729)
#define GL_EXP (2048)
#define GL_EXP2 (2049)

//
//
// UNIFORMS
//
//

//fog
layout(std140, binding = 1) uniform FOG {
    vec4 color;
    int mode;
    float density;
    float start;
    float end;
    float scale;
} fog;

//
//
// INPUTS
//
//

in FOG {
    float depth;
} fog_in;

//
//
// UTILITIES
//
//

float getFogFactor()    {
    if (fog.mode == GL_LINEAR) {
        return clamp((fog.end - fog_in.depth) * fog.scale, 0., 1.);
    } else {
        return 1.;
    }
}

vec4 addFog(vec4 color) {
    return mix(fog.color, color, getFogFactor());
}
