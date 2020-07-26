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
// UNIFORMS
//
//

uniform int current_base_level;

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
struct TileIndexEntry {
    ivec2 tilePos;
    int level;
    int index;

    ivec4 _padding;
};

layout(std430, binding = 2) buffer TILE_INDEX {
    TileIndexEntry data[];
} tile_index;

TileIndexEntry indexEntry()   {
    return tile_index.data[gl_InstanceID];
}

ivec2 toWorldPos(TileIndexEntry entry) {
    return (entry.tilePos * HEIGHTMAP_VOXELS + in_offset_absolute) << entry.level;
}

//tile data
layout(std430, binding = 3) buffer TILE_DATA {
    HEIGHTMAP_TYPE data[][HEIGHTMAP_VOXELS * HEIGHTMAP_VOXELS];
} tile_data;

HEIGHTMAP_TYPE sampleHeightmap(TileIndexEntry entry)   {
    if (all(lessThan(in_offset_absolute, ivec2(16))))  {
        vs_out.cancel = 0;
        return tile_data.data[entry.index][in_vertexID_chunk];
    } else {
        vs_out.cancel = 1;
        return HEIGHTMAP_TYPE(0);
    }
    /*int tileIndex = loadedTileIndex(worldPos >> (HEIGHTMAP_SHIFT + level), level - current_base_level);
    if (tileIndex >= 0)  {
        vs_out.cancel = 0;
        return tile_data[level - current_base_level].data[tileIndex][in_vertexID_chunk];
    } else {
        vs_out.cancel = 1;
        return HEIGHTMAP_TYPE(0);
    }*/
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
