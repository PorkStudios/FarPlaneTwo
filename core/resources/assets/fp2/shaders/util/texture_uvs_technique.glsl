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

#pragma once

//
//
// MACROS
//
//

#ifndef FP2_TEXTURE_UVS_TECHNIQUE
#   error "FP2_TEXTURE_UVS_TECHNIQUE must be defined!"
#endif // FP2_TEXTURE_UVS_TECHNIQUE

//synced with net.daporkchop.fp2.core.client.render.textureuvs.gpu.GpuQuadLists$QuadsTechnique
#define FP2_TEXTURE_UVS_TECHNIQUE_SSBO (0)
#define FP2_TEXTURE_UVS_TECHNIQUE_BUFFER_TEXTURE (1)
#define FP2_TEXTURE_UVS_TECHNIQUE_TEXTURE_2D (2)

#if FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_SSBO
#   ifndef TEXTURE_UVS_LISTS_SSBO_NAME
#       error "TEXTURE_UVS_LISTS_SSBO_NAME must be defined!"
#   endif //TEXTURE_UVS_LISTS_SSBO_NAME
#   ifndef TEXTURE_UVS_LISTS_SSBO_LAYOUT
#       error "TEXTURE_UVS_LISTS_SSBO_LAYOUT must be defined!"
#   endif //TEXTURE_UVS_LISTS_SSBO_LAYOUT
#   ifndef TEXTURE_UVS_QUADS_SSBO_NAME
#       error "TEXTURE_UVS_QUADS_SSBO_NAME must be defined!"
#   endif //TEXTURE_UVS_QUADS_SSBO_NAME
#   ifndef TEXTURE_UVS_QUADS_SSBO_LAYOUT
#       error "TEXTURE_UVS_QUADS_SSBO_LAYOUT must be defined!"
#   endif //TEXTURE_UVS_QUADS_SSBO_LAYOUT
#elif FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_BUFFER_TEXTURE
#   ifndef TEXTURE_UVS_LISTS_SAMPLERBUFFER_NAME
#       error "TEXTURE_UVS_LISTS_SAMPLERBUFFER_NAME must be defined!"
#   endif //TEXTURE_UVS_LISTS_SAMPLERBUFFER_NAME
#   ifndef TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_NAME
#       error "TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_NAME must be defined!"
#   endif //TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_NAME
#   ifndef TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_NAME
#       error "TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_NAME must be defined!"
#   endif //TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_NAME
#elif FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_TEXTURE_2D
#   ifndef TEXTURE_UVS_LISTS_SAMPLER2D_NAME
#       error "TEXTURE_UVS_LISTS_SAMPLER2D_NAME must be defined!"
#   endif //TEXTURE_UVS_LISTS_SAMPLER2D_NAME
#   ifndef TEXTURE_UVS_QUADS_COORD_SAMPLER2D_NAME
#       error "TEXTURE_UVS_QUADS_COORD_SAMPLER2D_NAME must be defined!"
#   endif //TEXTURE_UVS_QUADS_COORD_SAMPLER2D_NAME
#   ifndef TEXTURE_UVS_QUADS_TINT_SAMPLER2D_NAME
#       error "TEXTURE_UVS_QUADS_TINT_SAMPLER2D_NAME must be defined!"
#   endif //TEXTURE_UVS_QUADS_TINT_SAMPLER2D_NAME
#   ifndef TEXTURE_UVS_TEXTURE_X_COORD_WRAP
#       error "TEXTURE_UVS_TEXTURE_X_COORD_WRAP must be defined!"
#   endif //TEXTURE_UVS_TEXTURE_X_COORD_WRAP
#else
#   error "FP2_TEXTURE_UVS_TECHNIQUE is set to an unsupported value!"
#endif

//
//
// STRUCTS
//
//

struct TexQuadList {
    uint first;
    uint last;
};

struct PackedBakedQuad {
    vec4 coords;
    float tint;
};

//
//
// TEXTURES
//
//

#if FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_BUFFER_TEXTURE
    uniform usamplerBuffer TEXTURE_UVS_LISTS_SAMPLERBUFFER_NAME;
    uniform samplerBuffer TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_NAME;
    uniform samplerBuffer TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_NAME;
#elif FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_TEXTURE_2D
    uniform usampler2D TEXTURE_UVS_LISTS_SAMPLER2D_NAME;
    uniform sampler2D TEXTURE_UVS_QUADS_COORD_SAMPLER2D_NAME;
    uniform sampler2D TEXTURE_UVS_QUADS_TINT_SAMPLER2D_NAME;
#endif

//
//
// SSBOs
//
//

#if FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_SSBO
    layout(TEXTURE_UVS_LISTS_SSBO_LAYOUT) readonly buffer TEXTURE_UVS_LISTS_SSBO_NAME {
        TexQuadList b_texQuadList[];
    };

    layout(TEXTURE_UVS_QUADS_SSBO_LAYOUT) readonly buffer TEXTURE_UVS_QUADS_SSBO_NAME {
        PackedBakedQuad b_texQuad[];
    };
#endif //FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_SSBO

//
//
// UTILITIES
//
//

/**
 * Gets the TexQuadList describing the textures used by the given block state on the given face.
 *
 * @param state     the block state ID
 * @param faceIndex the index of the block face
 */
TexQuadList stateAndFaceIndexToTexQuadList(uint state, uint faceIndex) {
    uint listIndex = state * 6 + faceIndex;

#if FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_SSBO
    return b_texQuadList[listIndex];
#elif FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_BUFFER_TEXTURE
    uvec2 list = texelFetch(TEXTURE_UVS_LISTS_SAMPLERBUFFER_NAME, int(listIndex)).xy;

    TexQuadList result;
    result.first = list.x;
    result.last = list.y;
    return result;
#elif FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_TEXTURE_2D
    uvec2 texCoord = uvec2(listIndex % uint(TEXTURE_UVS_TEXTURE_X_COORD_WRAP), listIndex / uint(TEXTURE_UVS_TEXTURE_X_COORD_WRAP));
    uvec2 list = texelFetch(TEXTURE_UVS_LISTS_SAMPLER2D_NAME, ivec2(texCoord), 0).xy;

    TexQuadList result;
    result.first = list.x;
    result.last = list.y;
    return result;
#else
#   error
#endif
}

/**
 * Gets the PackedBakedQuad with the given index.
 *
 * @param quadIndex the quad index
 */
PackedBakedQuad quadIndexToQuad(uint quadIndex) {
#if FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_SSBO
    return b_texQuad[quadIndex];
#elif FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_BUFFER_TEXTURE
    vec4 coord = texelFetch(TEXTURE_UVS_QUADS_COORD_SAMPLERBUFFER_NAME, int(quadIndex));
    float tint = texelFetch(TEXTURE_UVS_QUADS_TINT_SAMPLERBUFFER_NAME, int(quadIndex)).x;

    PackedBakedQuad result;
    result.coords = coord;
    result.tint = tint;
    return result;
#elif FP2_TEXTURE_UVS_TECHNIQUE == FP2_TEXTURE_UVS_TECHNIQUE_TEXTURE_2D
    uvec2 texCoord = uvec2(quadIndex % uint(TEXTURE_UVS_TEXTURE_X_COORD_WRAP), quadIndex / uint(TEXTURE_UVS_TEXTURE_X_COORD_WRAP));
    vec4 coord = texelFetch(TEXTURE_UVS_QUADS_COORD_SAMPLER2D_NAME, ivec2(texCoord), 0);
    float tint = texelFetch(TEXTURE_UVS_QUADS_TINT_SAMPLER2D_NAME, ivec2(texCoord), 0).x;

    PackedBakedQuad result;
    result.coords = coord;
    result.tint = tint;
    return result;
#else
#   error
#endif
}
