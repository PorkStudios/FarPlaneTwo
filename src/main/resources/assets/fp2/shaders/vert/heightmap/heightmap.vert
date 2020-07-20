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

#define HEIGHTMAP_TYPE ivec4

//
//
// INPUTS
//
//

//vertex attributes
layout(location = 0) in ivec2 in_offset_absolute;
layout(location = 1) in ivec2 in_offset_chunk;
layout(location = 2) in int in_vertexID_chunk;

//uniforms
uniform ivec2 position_offset;

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
// DATA ACCESS
//
//

//tile index
layout(shared, binding = 2) buffer TILE_INDEX {
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
layout(shared, binding = 3) buffer TILE_DATA {
    HEIGHTMAP_TYPE data[][64 * 64];
} tile_data;

HEIGHTMAP_TYPE sampleHeightmap(ivec2 pos)   {
    int tileIndex = loadedTileIndex(pos >> 6);
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
    return p.x;
}

int unpackBlock(HEIGHTMAP_TYPE p)   {
    return p.y;
}

int unpackBiome(HEIGHTMAP_TYPE p)   {
    return p.z & 0x3F;
}

int unpackLight(HEIGHTMAP_TYPE p)   {
    return (p.z >> 6) & 0xFF;
}

int unpackFlags(HEIGHTMAP_TYPE p)    {
    return p.z >> 14;
}

bool isGrass(int flags) {
    return (flags & 1) != 0;
}

bool isFoliage(int flags) {
    return (flags & 2) != 0;
}

bool isWater(int flags) {
    return (flags & 4) != 0;
}
