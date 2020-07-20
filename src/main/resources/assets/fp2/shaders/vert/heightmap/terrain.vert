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

out VS_OUT {
    vec3 pos;
    vec2 light;

    flat vec4 color;
    flat int state;
    flat int cancel;
} vs_out;

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
    HEIGHTMAP_TYPE data[][64 * 64];
} tile_data;

void main(){
    //ivec2 posXZ = position_offset + (ivec2(gl_VertexID) / ivec2(65, 1) % 65);
    ivec2 posXZ = position_offset + in_offset_absolute;

    int tile_Index = tileIndex(posXZ >> 6);
    if (tile_Index < 0)  {
        vs_out.cancel = 1;
    } else {
        vs_out.cancel = 0;
    }

    //HeightmapData center = unpackData(center_in);
    //ivec3 center = tile_data.slots[tile_Index][0];
    int blockIndex = in_vertexID_chunk;
    HEIGHTMAP_TYPE center = tile_data.data[tile_Index][blockIndex];

    dvec3 pos = dvec3(double(posXZ.x), double(unpackHeight(center)) + .5, double(posXZ.y));
    //give raw position to fragment shader
    vs_out.pos = vec3(pos);

    //translate vertex position
    gl_Position = camera_projection * camera_modelview * vec4(pos, 1.);

    //decode block and sky light
    vs_out.light = vec2(ivec2(unpackLight(center)) >> ivec2(0, 4) & 0xF) / 16.;

    //store block state
    vs_out.state = unpackBlock(center);

    int biome = unpackBiome(center);
    if (isGrass(unpackFlags(center))) { //grass
        if (IS_SWAMP) {
            vs_out.color = fromRGB(-1. < -.1 ? 0x4C763C : 0x6A7039);
        } else if (IS_ROOFED_FOREST)    {
            vec4 original = getGrassColorAtPos(pos, biome);
            vs_out.color = vec4(((original + fromARGB(0x0028340A)) * .5).rgb, original.a);
        } else if (IS_MESA) {
            vs_out.color = fromRGB(0x90814D);
        } else {
            vs_out.color = getGrassColorAtPos(pos, biome);
        }
    } else if (isFoliage(unpackFlags(center)))  { //foliage
        if (IS_SWAMP) {
            vs_out.color = fromRGB(0x6A7039);
        } else if (IS_MESA) {
            vs_out.color = fromRGB(0x9E814D);
        } else {
            vs_out.color = getFoliageColorAtPos(pos, biome);
        }
    } else {
        vs_out.color = vec4(1.);
    }
}
