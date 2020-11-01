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
    ivec4 tile_position = tile_positions[gl_DrawID];
    ivec3 relativePos_floor_base = (tile_position.xyz << tile_position.w << T_SHIFT) - glState.camera.position_floor;
    vec3 relativePos = vec3(relativePos_floor + getLowOffsetPre(tile_position.w)) + getLowOffsetPost(tile_position.w) - glState.camera.position_fract;

    float depth = length(relativePos);

    //set fog depth here, simply because it's going to change by at most a few blocks (negligable) and this prevents us from having to compute the depth twice
    fog_out.depth = depth;

#ifdef USE_LOD
    //mix low and high vertex positions based on depth
    /*float start = float(fp2_state.view.levelCutoffDistance) * in_level_scale * fp2_state.view.transitionStart;
    float end = float(fp2_state.view.levelCutoffDistance) * in_level_scale * fp2_state.view.transitionEnd;
    dvec3 mixedPos = mix(in_pos_low, in_pos_high, 1. - clamp((end - depth) * (1. / (end - start)), 0., 1.));
    relativePos = vec3(mixedPos - glState.camera.position);*/
#endif

    //vertex position is detail mixed
    gl_Position = cameraTransform(relativePos) - vec4(0., 0., 0.0001, 0.);

    //pass relative position to fragment shader (used to compute face normal)
    vs_out.pos = vs_out.base_pos = vec3(relativePos);

    //copy trivial attributes
    vs_out.light = in_light;
    vs_out.state = in_state;
    vs_out.color = in_color;
}
