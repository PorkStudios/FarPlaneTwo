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

void main(){
    TileIndexEntry entry = indexEntry();
    TileIndex index = entry.low[0];
    ivec3 blockPos = toWorldPos(index);

    //int slot = toSlot(index, blockPos);
    //index = entry.low[slot];

    VOXEL_TYPE voxel = sampleVoxel(index);

    dvec3 pos = dvec3(blockPos) + dvec3(voxel.xyz);

    //convert position to vec3 afterwards to minimize precision loss
    vec3 relativePos = vec3(pos - glState.camera.position);

    //give raw position to fragment shader
    vs_out.pos = vec3(pos);

    //set fog depth
    fog_out.depth = length(relativePos);

    //translate vertex position
    gl_Position = vec4(relativePos, 1.);

    vs_out.color = vec4(vec3(voxel.w), 1.);
}