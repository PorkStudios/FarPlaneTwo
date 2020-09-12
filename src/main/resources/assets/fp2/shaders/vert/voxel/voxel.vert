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
// MACROS
//
//

struct VoxelData {
    vec3 offset;
    int connections;
};

#define VOXEL_TYPE VoxelData

//
//
// CONSTANTS
//
//

const ivec3 otherOffsetCoords[7] = ivec3[](
    ivec3(0), ivec3(0, 0, 1), ivec3(0, 1, 0), ivec3(0, 1, 1), ivec3(1, 0, 0), ivec3(1, 0, 1), ivec3(1, 1, 0)
);

const vec3 otherOffsetVectors[7] = vec3[](
    vec3(0.), vec3(0., 0., 1.), vec3(0., 1., 0.), vec3(0., 1., 1.), vec3(1., 0., 0.), vec3(1., 0., 1.), vec3(1., 1., 0.)
);

//
//
// VERTEX ATTRIBUTES
//
//

layout(location = 0) in ivec3 in_offset_absolute;
layout(location = 1) in int in_vertexID_chunk;

//
//
// OUTPUTS
//
//

out VS_OUT {
    flat int connections;
    flat vec4 other[7];
} vs_out;

//
//
// BUFFERS
//
//

//positions
struct TileIndex {
    ivec3 tilePos;
    int level;
    int index;
};

struct TileIndexEntry {
    TileIndex low[8];
};

layout(std430, binding = 2) buffer TILE_INDEX {
    TileIndexEntry data[];
} tile_index;

TileIndexEntry indexEntry()   {
    return tile_index.data[gl_InstanceID];
}

ivec3 toWorldPos(TileIndex index) {
    return (index.tilePos * T_VOXELS + in_offset_absolute) << index.level;
}

//tile data
layout(std430, binding = 3) buffer TILE_DATA {
    VOXEL_TYPE data[][T_VOXELS * T_VOXELS * T_VOXELS];
} tile_data;

VOXEL_TYPE sampleVoxel(TileIndex index)   {
    return tile_data.data[index.index][in_vertexID_chunk];
}

VOXEL_TYPE sampleVoxel(TileIndex index, ivec3 blockPos)   {
    ivec3 p2 = (blockPos >> index.level) & T_MASK;
    return tile_data.data[index.index][(p2.x * T_VOXELS + p2.y) * T_VOXELS + p2.z];
}

int toSlot(TileIndex index, ivec3 blockPos)  {
    ivec3 p2 = (blockPos >> (index.level + T_SHIFT)) - index.tilePos;
    return (p2.x << 2) | (p2.y << 1) | p2.z;
}
