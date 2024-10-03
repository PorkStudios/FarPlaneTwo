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

#include <"fp2:shaders/util/camera_state_uniforms.glsl"> // u_positionFrac, u_positionFloor
#include <"fp2:shaders/util/camera_transform.glsl"> // cameraTransform()
#include <"fp2:shaders/util/tile_pos_technique.glsl"> // FP2_TILE_POS_TECHNIQUE_*
#include <"fp2:shaders/vert/common.vert">
#include <"fp2:shaders/vert/fog.vert">

//per-vertex attributes
in uint a_state;
in vec2 a_light;
in vec3 a_color;
in vec3 a_pos;

//shader inputs for accessing the tile position
#if FP2_TILE_POS_TECHNIQUE == FP2_TILE_POS_TECHNIQUE_VERTEX_ATTRIBUTE
    //instanced vertex attributes
    in ivec4 a_tilePos;
#elif FP2_TILE_POS_TECHNIQUE == FP2_TILE_POS_TECHNIQUE_UNIFORM_ARRAY_DRAWID
#include <"fp2:shaders/util/arb/shader_draw_parameters.glsl"> // get_gl_DrawID()
    //uniforms
    layout(TILE_POS_ARRAY_UBO_LAYOUT) uniform TILE_POS_ARRAY_UBO_NAME {
        ivec4 u_tilePosArray[TILE_POS_ARRAY_UBO_ELEMENTS];
    };
#elif FP2_TILE_POS_TECHNIQUE == FP2_TILE_POS_TECHNIQUE_UNIFORM
    //uniforms
    uniform ivec4 u_TilePos;
#else
#   error
#endif

//get the tile position
//  (how we do this depends on the configured tile position technique)
ivec4 getTilePosition() {
#if FP2_TILE_POS_TECHNIQUE == FP2_TILE_POS_TECHNIQUE_VERTEX_ATTRIBUTE
    //simply read the tile position from the vertex attribute
    return a_tilePos;
#elif FP2_TILE_POS_TECHNIQUE == FP2_TILE_POS_TECHNIQUE_UNIFORM_ARRAY_DRAWID
    //load the tile position from the corresponding index of the tile positions uniform array
    return u_tilePosArray[get_gl_DrawID()];
#elif FP2_TILE_POS_TECHNIQUE == FP2_TILE_POS_TECHNIQUE_UNIFORM
    //simply read the tile position from the uniform variable
    return u_TilePos;
#else
#   error
#endif
}

void main() {
    //get the tile position
    //  (how we do this depends on the configured tile position technique)
    ivec4 tilePos = getTilePosition();

    //convert position to vec3 afterwards to minimize precision loss
    ivec3 relative_tile_position = (tilePos.xyz << (tilePos.w + T_SHIFT)) - u_positionFloor;
    vec3 relativePos = vec3(relative_tile_position) + a_pos * float(1 << tilePos.w) / 8. - u_positionFrac;

    //set fog depth based on vertex distance to camera
    setFog(relativePos);

    //vertex position is detail mixed
    gl_Position = cameraTransform(relativePos);

    //pass relative position to fragment shader (used to compute face normal)
    vs_out_pos = vs_out_base_pos = vec3(relativePos);

    //copy trivial attributes
    vs_out_light = a_light;
    vs_out_state = a_state;
    vs_out_color = computeVertexColor(a_color, tilePos);
}
