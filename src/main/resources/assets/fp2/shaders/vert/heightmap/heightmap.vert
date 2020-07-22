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

#define HEIGHTMAP_TYPE ivec4

//
//
// VERTEX ATTRIBUTES
//
//

layout(location = 0) in ivec2 in_offset_absolute;
layout(location = 1) in ivec2 in_offset_chunk;
layout(location = 2) in int in_vertexID_chunk;

//
//
// OUTPUTS
//
//

out VS_OUT {
    vec3 pos;
    vec2 light;

    flat vec4 color;
    flat int state;
    flat int cancel;
} vs_out;

//
//
// BUFFERS
//
//

//positions

struct VertexPosition {
    ivec2 tilePos;
    int level;
};

layout(std430, binding = 2) buffer INSTANCE_POSITIONS {
    VertexPosition data[];
} instance_positions;

VertexPosition vertexPos()   {
    return instance_positions.data[gl_InstanceID];
}

ivec2 toWorldPos(VertexPosition vertex) {
    return (vertex.tilePos * HEIGHTMAP_VOXELS + in_offset_absolute) << vertex.level;
    //return ((vertex.tilePos * HEIGHTMAP_VOXELS)) + in_offset_absolute;
}

//tile index
layout(std430, binding = 3) buffer TILE_INDEX {
    ivec2 base;
    ivec2 size;
    int data[];
} tile_index;

int loadedTileIndex(ivec2 chunk)  {
    chunk -= tile_index.base;
    if (any(lessThan(chunk, ivec2(0))) || any(greaterThanEqual(chunk, tile_index.size)))    {
        return -1;
    }
    int index = chunk.x * tile_index.size.y + chunk.y;
    return tile_index.data[index];
}

//tile data
layout(std430, binding = 4) buffer TILE_DATA {
    HEIGHTMAP_TYPE data[][HEIGHTMAP_VOXELS * HEIGHTMAP_VOXELS];
} tile_data;

HEIGHTMAP_TYPE sampleHeightmap(ivec2 worldPos, int level)   {
    int tileIndex = loadedTileIndex(worldPos >> (HEIGHTMAP_SHIFT + level));
    if (tileIndex >= 0)  {
        vs_out.cancel = 0;
        return tile_data.data[tileIndex][in_vertexID_chunk];
    } else {
        vs_out.cancel = 1;
        return HEIGHTMAP_TYPE(0);
    }
}

//
//
// UTILITIES
//
//

//heightmap data unpacking

int unpackHeight(HEIGHTMAP_TYPE p)  {
    return p[0];
}

int unpackBlock(HEIGHTMAP_TYPE p)   {
    return p[1] & 0xFFFFFF;
}

vec2 unpackBlockLight(HEIGHTMAP_TYPE p)   {
    return unpackCombinedLight(p[1] >> 24);
}

vec4 unpackBlockColor(HEIGHTMAP_TYPE p)  {
    return fromARGB(p[2]);
}

vec2 unpackWaterLight(HEIGHTMAP_TYPE p) {
    return unpackCombinedLight(p[3] >> 24);
}

vec4 unpackWaterColor(HEIGHTMAP_TYPE p) {
    return fromRGB(p[3]);
}
