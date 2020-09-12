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

    VOXEL_TYPE voxel = sampleVoxel(index, blockPos);

    vs_out.connections = voxel.connections;

    vec3 baseOffset = vec3(dvec3(blockPos) - glState.camera.position);

    for (int i = 0; i < 7; i++) {
        ivec3 delta = otherOffsetCoords[i];
        ivec3 otherWorldPos = blockPos + delta;
        int slot = toSlot(index, otherWorldPos);

        VOXEL_TYPE voxel = sampleVoxel(entry.low[slot], otherWorldPos);
        vs_out.other[i] = vec4(baseOffset + otherOffsetVectors[i] + voxel.offset, 1.);
    }
}
