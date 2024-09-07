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
#include <"fp2:shaders/vert/common.vert">
#include <"fp2:shaders/vert/fog.vert">

//per-vertex attributes
in int a_state;
in vec2 a_light;
in vec3 a_color;
in vec3 a_pos;

//instanced vertex attributes
in ivec4 a_tilePos;

void main() {
    //convert position to vec3 afterwards to minimize precision loss
    ivec3 relative_tile_position = (a_tilePos.xyz << a_tilePos.w << T_SHIFT) - u_positionFloor;
    vec3 relativePos = vec3(relative_tile_position) + a_pos * float(1 << a_tilePos.w) / 8. - u_positionFrac;

    //set fog depth based on vertex distance to camera
    setFog(relativePos);

    //vertex position is detail mixed
    gl_Position = cameraTransform(relativePos);

    //pass relative position to fragment shader (used to compute face normal)
    vs_out.pos = vs_out.base_pos = vec3(relativePos);

    //copy trivial attributes
    vs_out.light = a_light;
    vs_out.state = a_state;
    vs_out.color = computeVertexColor(a_color, a_tilePos);
}
