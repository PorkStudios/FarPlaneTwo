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

#if GL_FOG_ENABLED
in FOG {
    float depth;
} fog_in;
#endif //GL_FOG_ENABLED

//
//
// UTILITIES
//
//

vec4 addFog(in vec4 color) {
#if GL_FOG_ENABLED
    //compute the fog factor (formula depends on the current fog mode)
    float fogFactor;

#if GL_FOG_MODE == GL_FOG_MODE_LINEAR
    fogFactor = (glState.fog.end - fog_in.depth) * glState.fog.scale;
#elif GL_FOG_MODE == GL_FOG_MODE_EXP
    fogFactor = exp(-glState.fog.density * fog_in.depth);
#elif GL_FOG_MODE == GL_FOG_MODE_EXP2
    float depth = fog_in.depth;
    fogFactor = exp(-glState.fog.density * (depth * depth));
#else
#error unsupported fog mode!
#endif

    //mix fog colors
    return mix(glState.fog.color, color, clamp(fogFactor, 0., 1.));
#else
    //fog is disabled, don't modify the fragment color
    return color;
#endif
}
