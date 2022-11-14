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

#define FRAG_BLOCK

#include <"fp2:shaders/frag/common.frag">
#include <"fp2:shaders/frag/fog.frag">

void main() {
    vec3 normal = normalVector();

    vec4 frag_color;
#if FP2_DEBUG_COLORS_ENABLED
#if FP2_DEBUG_COLORS_MODE == FP2_DEBUG_COLORS_MODE_LEVEL || FP2_DEBUG_COLORS_MODE == FP2_DEBUG_COLORS_MODE_POSITION
    frag_color = vec4(fs_in.color * diffuseLight(normal), 1.);
#elif FP2_DEBUG_COLORS_MODE == FP2_DEBUG_COLORS_MODE_NORMAL
    frag_color = vec4(normal * normal, 1.);
#endif
#else
    //initial block texture sample
    frag_color = sampleTerrain(normal);

#ifdef FP2_CUTOUT
    //this is the cutout pass, emulate legacy opengl alpha testing
    if (frag_color.a <= u_alphaRefCutout) {
        discard;
    }
#endif

    //block/sky light
    frag_color *= t_lightmap(fs_in.light);

    //shading
    frag_color.rgb *= diffuseLight(normal);
#endif

    //fog
    frag_color = addFog(frag_color);

    color = frag_color;
}
