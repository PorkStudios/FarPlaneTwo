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

layout(location = 0) in ivec3 in_tile_position;

layout(location = 1) in int in_state;
layout(location = 2) in vec2 in_light;
layout(location = 3) in vec3 in_color;

layout(location = 4) in ivec2 in_pos_horiz;
layout(location = 5) in int in_height_int;
layout(location = 6) in float in_height_frac;

ivec3 getLowOffsetPre(int level) {
    return ivec3(in_pos_horiz.x << level, in_height_int, in_pos_horiz.y << level);
}

vec3 getLowOffsetPost() {
    return vec3(0., in_height_frac / 256., 0.);
}

void main() {
    //convert position to vec3 afterwards to minimize precision loss
    ivec4 tile_position = ivec4(in_tile_position.x, 0, in_tile_position.yz);
    ivec3 relative_tile_position = (tile_position.xyz << tile_position.w << T_SHIFT) - glState.camera.position_floor;
    vec3 relativePos = vec3(relative_tile_position + getLowOffsetPre(tile_position.w)) + getLowOffsetPost() - glState.camera.position_fract;

    //set fog depth based on vertex distance to camera
    fog_out.depth = length(relativePos);

    //vertex position is detail mixed
    gl_Position = cameraTransform(relativePos);

    //pass relative position to fragment shader (used to compute face normal)
    vs_out.pos = vs_out.base_pos = vec3(relativePos);

    //copy trivial attributes
    vs_out.light = in_light;
    vs_out.state = in_state;
    vs_out.color = computeVertexColor(in_color.rgb);
}
