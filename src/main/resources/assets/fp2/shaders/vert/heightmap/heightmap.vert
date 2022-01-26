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

#define VERT_HEIGHTMAP_HEIGHTMAP

#include <"fp2:shaders/vert/common.vert">
#include <"fp2:shaders/vert/fog.vert">

ivec3 getLowOffsetPre(int level) {
    return ivec3(dl_posHoriz.x << level, dl_heightInt, dl_posHoriz.y << level);
}

vec3 getLowOffsetPost() {
    return vec3(0., dl_heightFrac / 256., 0.);
}

void main() {
    //convert position to vec3 afterwards to minimize precision loss
    ivec4 tile_position = ivec4(dg_tilePos.x, 0, dg_tilePos.yz);
    ivec3 relative_tile_position = (tile_position.xyz << tile_position.w << T_SHIFT) - u_positionFloor;
    vec3 relativePos = vec3(relative_tile_position + getLowOffsetPre(tile_position.w)) + getLowOffsetPost() - u_positionFrac;

    //set fog depth based on vertex distance to camera
    setFog(relativePos);

    //vertex position is detail mixed
    gl_Position = cameraTransform(relativePos);

    //pass relative position to fragment shader (used to compute face normal)
    vs_out.pos = vs_out.base_pos = vec3(relativePos);

    //copy trivial attributes
    vs_out.light = dl_light;
    vs_out.state = dl_state;
    vs_out.color = computeVertexColor(dl_color.rgb, ivec4(dg_tilePos.x, 0, dg_tilePos.yz));
}
