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

#ifndef FRAG_FOG
#define FRAG_FOG

#include <"fp2:shaders/common.glsl">

#include <"fp2:shaders/util/draw_state_uniforms.glsl"> // u_fogMode, u_fogColor, u_fogDensity, u_fogStart, u_fogEnd, u_fogScale

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

vec4 addFog(in vec4 color) {
    //compute the fog factor (formula depends on the current fog mode)
    float fogFactor;
    switch (u_fogMode) {
        case FP2_FOG_MODE_DISABLED:
            //fog is disabled, don't modify the fragment color
            return color;
        case FP2_FOG_MODE_LINEAR:
            fogFactor = (u_fogEnd - fog_in.depth) * u_fogScale;
            break;
        case FP2_FOG_MODE_EXP:
            fogFactor = exp(-u_fogDensity * fog_in.depth);
            break;
        case FP2_FOG_MODE_EXP2:
            fogFactor = exp(-u_fogDensity * (fog_in.depth * fog_in.depth));
            break;
    }

    //mix fog colors
    return mix(u_fogColor, color, clamp(fogFactor, 0., 1.));
}

#endif //FRAG_FOG
