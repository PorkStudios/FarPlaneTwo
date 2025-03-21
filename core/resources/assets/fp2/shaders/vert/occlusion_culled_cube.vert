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

#include <"fp2:shaders/common.glsl">

#include <"fp2:shaders/util/camera_state_uniforms.glsl"> // u_positionFrac, u_positionFloor
#include <"fp2:shaders/util/camera_transform.glsl"> // cameraTransform()

layout(std140) readonly restrict buffer B_TilePositions {
    ivec4 b_tilePositions[];
};

flat out uint vs_out_tileIndex;
//out vec3 vs_out_color;

const ivec3 CUBE_VERTEX_OFFSETS[8] = {
    ivec3(0, 0, 0),
    ivec3(0, 0, 1),
    ivec3(0, 1, 0),
    ivec3(0, 1, 1),
    ivec3(1, 0, 0),
    ivec3(1, 0, 1),
    ivec3(1, 1, 0),
    ivec3(1, 1, 1),
};

void main() {
    uint tileIndex = gl_InstanceID;
    uint vertexIndex = gl_VertexID & 7;

    //get the tile position
    ivec4 tilePos = b_tilePositions[tileIndex];

    //convert position to vec3 afterwards to minimize precision loss
    ivec3 relativePosFloor = tilePos.xyz;
    tilePos.xyz += CUBE_VERTEX_OFFSETS[vertexIndex];
    tilePos.xyz <<= tilePos.w + T_SHIFT;
    tilePos.xyz -= u_positionFloor;
    vec3 relativePos = vec3(tilePos.xyz) - u_positionFrac;

    gl_Position = cameraTransform(relativePos);
    vs_out_tileIndex = tileIndex;
    //vs_out_color = vec3(CUBE_VERTEX_OFFSETS[vertexIndex]);
}
