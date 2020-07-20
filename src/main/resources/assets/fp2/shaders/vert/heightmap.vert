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

layout(location = 0) in ivec3 center_in;
layout(location = 1) in ivec3 detail_in[4];

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_modelview = mat4(1.0);

uniform ivec2 position_offset;
uniform dvec3 player_position;

out vec3 vert_pos;
out vec2 vert_light;
out flat vec4 vert_color;
out flat int vert_state;

/*struct HeightmapData {
    int height;
    int block;
    int biome;
    int light;
    int flags;
};

HeightmapData unpackData(ivec3 p)  {
    HeightmapData data;
    data.height = p.x;
    data.block = p.y;
    data.biome = p.z & 0x3F;
    data.light = (p.z >> 6) & 0xFF;
    data.flags = p.z >> 14;
    return data;
}*/

int unpackHeight(in ivec3 p)  {
    return p.x;
}

int unpackBlock(in ivec3 p)   {
    return p.y;
}

int unpackBiome(in ivec3 p)   {
    return p.z & 0x3F;
}

int unpackLight(in ivec3 p)   {
    return (p.z >> 6) & 0xFF;
}

int unpackFlags(in ivec3 p)    {
    return p.z >> 4;
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

layout(shared, binding = 2) buffer TileIndex {
    ivec2 base;
    ivec2 size;
    int data[];
} tile_index;

int tileIndex(ivec2 chunk)  {
    chunk -= tile_index.base;
    if (any(lessThan(chunk, ivec2(0))) || any(greaterThanEqual(chunk, tile_index.size)))    {
        return -1;
    }
    int index = chunk.x * tile_index.size.y + chunk.y;
    return tile_index.data[index];
}

layout(shared, binding = 3) buffer TileData {
    ivec3 slots[][64 * 64];
} tile_data;

void main(){
    ivec2 posXZ = position_offset + (ivec2(gl_VertexID) / ivec2(65, 1) % 65);

    int tile_Index = tileIndex(posXZ >> 6);
    if (tile_Index < 0)  {
        return;
    }

    //HeightmapData center = unpackData(center_in);
    ivec3 center = tile_data[tile_Index][gl_VertexID];

    dvec3 pos = dvec3(double(posXZ.x), double(unpackHeight(center)) + .5, double(posXZ.y));
    vert_pos = vec3(pos);

    gl_Position = camera_projection * camera_modelview * vec4(pos, 1.);

    vert_light = vec2(ivec2(unpackLight(center)) >> ivec2(0, 4) & 0xF) / 16.;
    vert_state = unpackBlock(center);

    int biome = unpackBiome(center);
    if (isGrass(unpackFlags(center))) { //grass
        if (IS_SWAMP) {
            vert_color = fromRGB(-1. < -.1 ? 5011004 : 6975545);
        } else if (IS_ROOFED_FOREST)    {
            vec4 original = getGrassColorAtPos(pos, biome);
            vert_color = vec4(((original + fromARGB(0x0028340A)) * .5).rgb, original.a);
        } else if (IS_MESA) {
            vert_color = fromRGB(9470285);
        } else {
            vert_color = getGrassColorAtPos(pos, biome);
        }
    } else if (isFoliage(unpackFlags(center)))  { //foliage
        if (IS_SWAMP) {
            vert_color = fromRGB(6975545);
        } else if (IS_MESA) {
            vert_color = fromRGB(10387789);
        } else {
            vert_color = getFoliageColorAtPos(pos, biome);
        }
    } else {
        vert_color = vec4(1.);
    }
}
