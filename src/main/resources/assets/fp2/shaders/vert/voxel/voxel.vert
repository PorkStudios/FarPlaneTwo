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

#define DEBUG_DISTANCES

#ifdef DEBUG_DISTANCES
const vec3[][3] DEBUG_DISTANCE_COLORS = vec3[][](
vec3[3](vec3(0., 1., 0.), vec3(1., 1., 0.), vec3(1., 0., 0.)),
vec3[3](vec3(0., 0., 1.), vec3(1., 0., 1.), vec3(0., 1., 1.)),
vec3[3](vec3(0.), vec3(.5), vec3(1.))
);
#endif

//
//
// VERTEX ATTRIBUTES
//
//

layout(location = 1) in int in_state;
layout(location = 2) in vec2 in_light;
layout(location = 3) in vec3 in_color;

layout(location = 4) in vec3 in_pos_low;
layout(location = 5) in vec3 in_pos_high;

void main() {
    //convert position to vec3 afterwards to minimize precision loss
    ivec3 relative_tile_position = (tile_position.xyz << tile_position.w << T_SHIFT) - glState.camera.position_floor;
    vec3 relativePos = vec3(relative_tile_position) + in_pos_low * float(1 << tile_position.w) / 8. - glState.camera.position_fract;

    float depth = length(relativePos);

    //set fog depth here, simply because it's going to change by at most a few blocks (negligable) and this prevents us from having to compute the depth twice
    fog_out.depth = depth;

    //mix low and high vertex positions based on depth
    float cutoff_scale = float(fp2_state.view.levelCutoffDistance << tile_position.w);
    float start = cutoff_scale * fp2_state.view.transitionStart;
    float end = cutoff_scale * fp2_state.view.transitionEnd;

    vec3 relativePos_high = vec3(relative_tile_position) + in_pos_high * float(1 << tile_position.w) / 8. - glState.camera.position_fract;
    relativePos = mix(relativePos_high, relativePos, clamp((end - depth) / (end - start), 0., 1.));

#ifdef DEBUG_DISTANCES
    if (depth < start) {
        vs_out.color = DEBUG_DISTANCE_COLORS[tile_position.w][0];
    } else if (depth > end) {
        vs_out.color = DEBUG_DISTANCE_COLORS[tile_position.w][2];
    } else {
        vs_out.color = DEBUG_DISTANCE_COLORS[tile_position.w][1];
    }
#endif

    //vertex position is detail mixed
    gl_Position = cameraTransform(relativePos) + glState.camera.anti_flicker_offset * vec4(31. - float(tile_position.w));

    //pass relative position to fragment shader (used to compute face normal)
    vs_out.pos = vs_out.base_pos = vec3(relativePos);

    //copy trivial attributes
    vs_out.light = in_light;
    vs_out.state = in_state;
#ifndef DEBUG_DISTANCES
    vs_out.color = in_color;
#endif
}
