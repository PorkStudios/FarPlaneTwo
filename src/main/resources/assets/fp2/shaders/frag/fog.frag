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

float getFogFactor() { //this shouldn't cause any significant performance drop because the result is constant for every invocation
    if (glState.fog.mode == FOG_LINEAR) {
        return clamp((glState.fog.end - fog_in.depth) * glState.fog.scale, 0., 1.);
    } else if (glState.fog.mode == FOG_EXP)  {
        return clamp(exp(-glState.fog.end * fog_in.depth), 0., 1.);
    } else if (glState.fog.mode == FOG_EXP2)  {
        return clamp(exp(-glState.fog.end * pow(fog_in.depth, 2.)), 0., 1.);
    } else {
        return 1.;
    }
}

vec4 addFog(vec4 color) {
    return mix(glState.fog.color, color, getFogFactor());
}
