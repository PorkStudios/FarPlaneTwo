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

void main() {
    //convert position to vec3 afterwards to minimize precision loss
    ivec3 relative_tile_position = (tile_position.xyz << tile_position.w << T_SHIFT) - glState.camera.position_floor;
    vec3 relativePos = vec3(relative_tile_position + getLowOffsetPre(tile_position.w)) + getLowOffsetPost() - glState.camera.position_fract;

#ifdef USE_LOD
    //LoD blending should only be done in 2D for heightmap mode to avoid seams
    float depth = length(relativePos.xz);

    //mix low and high vertex positions based on depth
    float cutoff_scale = float(fp2_state.view.levelCutoffDistance << tile_position.w);
    float start = cutoff_scale * fp2_state.view.transitionStart;
    float end = cutoff_scale * fp2_state.view.transitionEnd;

    vec3 relativePos_high = vec3(relative_tile_position + getHighOffsetPre(tile_position.w)) + getHighOffsetPost() - glState.camera.position_fract;
    relativePos = mix(relativePos_high, relativePos, clamp((end - depth) * (1. / (end - start)), 0., 1.));
#endif

    //set fog depth based on vertex distance to camera
    fog_out.depth = length(relativePos);

    //vertex position is detail mixed
    gl_Position = cameraTransform(relativePos) + glState.camera.anti_flicker_offset;

    //pass relative position to fragment shader (used to compute face normal)
    vs_out.pos = vs_out.base_pos = vec3(relativePos);

    //copy trivial attributes
    vs_out.light = in_light;
    vs_out.state = in_state;
    vs_out.color = in_color;
}
