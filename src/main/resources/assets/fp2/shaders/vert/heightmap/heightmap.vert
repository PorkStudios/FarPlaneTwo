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
// VERTEX ATTRIBUTES
//
//

layout(location = 1) in int in_state;
layout(location = 2) in vec2 in_light;
layout(location = 3) in vec3 in_color;

layout(location = 4) in ivec2 in_pos_low;
layout(location = 5) in int in_height_int_low;
layout(location = 6) in float in_height_frac_low;

layout(location = 7) in ivec2 in_pos_high;
layout(location = 8) in int in_height_int_high;
layout(location = 9) in float in_height_frac_high;

ivec3 getLowOffsetPre(int level) {
    return ivec3(in_pos_low.x << level, in_height_int_low, in_pos_low.y << level);
}

vec3 getLowOffsetPost() {
    return vec3(0., in_height_frac_low / 256., 0.);
}

ivec3 getHighOffsetPre(int level) {
    return ivec3(in_pos_high.x << level, in_height_int_high, in_pos_high.y << level);
}

vec3 getHighOffsetPost() {
    return vec3(0., in_height_frac_high / 256., 0.);
}

void main() {
    //convert position to vec3 afterwards to minimize precision loss
    ivec3 relative_tile_position = (tile_position.xyz << tile_position.w << T_SHIFT) - glState.camera.position_floor;
    vec3 relativePos = vec3(relative_tile_position + getLowOffsetPre(tile_position.w)) + getLowOffsetPost() - glState.camera.position_fract;

    //LoD blending should only be done in 2D for heightmap mode to avoid seams
    float depth = length(relativePos.xz);

    //mix low and high vertex positions based on depth
    float cutoff_scale = float(fp2_state.view.levelCutoffDistance << tile_position.w);
    float start = cutoff_scale * fp2_state.view.transitionStart;
    float end = cutoff_scale * fp2_state.view.transitionEnd;

    vec3 relativePos_high = vec3(relative_tile_position + getHighOffsetPre(tile_position.w)) + getHighOffsetPost() - glState.camera.position_fract;
    relativePos = mix(relativePos_high, relativePos, clamp((end - depth) / (end - start), 0., 1.));

    //set fog depth based on vertex distance to camera
    fog_out.depth = length(relativePos);

    //vertex position is detail mixed
    gl_Position = cameraTransform(relativePos) + glState.camera.anti_flicker_offset;

    //pass relative position to fragment shader (used to compute face normal)
    vs_out.pos = vs_out.base_pos = vec3(relativePos);

    //copy trivial attributes
    vs_out.light = in_light;
    vs_out.state = in_state;
    vs_out.color = computeVertexColor(in_color.rgb, start, end, depth);
}
