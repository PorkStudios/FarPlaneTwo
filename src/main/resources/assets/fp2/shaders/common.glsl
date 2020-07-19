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

#version 430 core

#define IS_MESA (biome == 37 || biome == 38 || biome == 39 || biome == 165 || biome == 166 || biome == 167)
#define IS_ROOFED_FOREST (biome == 29 || biome == 157)
#define IS_SWAMP (biome == 6 || biome == 134)

layout(shared, binding = 0) buffer loaded_chunks {
    ivec4 loaded_base; //using 4d vectors because apparently GLSL is too stupid to handle 3d ones
    ivec4 loaded_size;
    int loaded_data[];
};

bool isLoaded(ivec3 chunk)  {
    chunk -= loaded_base.xyz;
    if (any(lessThan(chunk, ivec3(0))) || any(greaterThanEqual(chunk, loaded_size.xyz)))    {
        return false;
    }
    int index = (chunk.x * loaded_size.y + chunk.y) * loaded_size.z + chunk.z;
    return (loaded_data[index >> 5] & (1 << (index & 0x1F))) != 0;
}

struct TextureUV {
    vec2 min;
    vec2 max;
};

layout(shared, binding = 1) buffer GlobalInfo {
    vec2 biome_climate[256];
    int biome_watercolor[256];

    int colormap_grass[256 * 256];
    int colormap_foliage[256 * 256];

    int map_colors[64];

    TextureUV tex_uvs[];
} global_info;

vec4 fromARGB(uint argb)   {
    return vec4(uvec4(argb) >> uvec4(16, 8, 0, 24) & uint(0xFF)) / 255.;
}

vec4 fromARGB(int argb)   {
    return fromARGB(uint(argb));
}

vec4 fromRGB(uint rgb)   {
    return fromARGB(uint(0xFF000000) | rgb);
}

vec4 fromRGB(int rgb)   {
    return fromRGB(uint(rgb));
}

float getTemperature(dvec3 pos, int biome) {
    if (pos.y > 64.)   {
        return global_info.biome_climate[biome].x - float(pos.y - 64.) * .05 / 30.;
    } else {
        return global_info.biome_climate[biome].x;
    }
}

vec4 getGrassColor(float temperature, float humidity){
    humidity = humidity * temperature;
    int i = int((1. - temperature) * 255.);
    int j = int((1. - humidity) * 255.);
    return fromARGB(global_info.colormap_grass[(j << 8) | i]);
}

vec4 getGrassColorAtPos(dvec3 pos, int biome){
    return getGrassColor(clamp(getTemperature(pos, biome), 0., 1.), clamp(global_info.biome_climate[biome].y, 0., 1.));
}

vec4 getFoliageColor(float temperature, float humidity){
    humidity = humidity * temperature;
    int i = int((1. - temperature) * 255.);
    int j = int((1. - humidity) * 255.);
    return fromARGB(global_info.colormap_foliage[(j << 8) | i]);
}

vec4 getFoliageColorAtPos(dvec3 pos, int biome){
    return getGrassColor(clamp(getTemperature(pos, biome), 0., 1.), clamp(global_info.biome_climate[biome].y, 0., 1.));
}

int attrsToBiome(int attrs) {
    return attrs & 0x3F;
}

int attrsToLight(int attrs) {
    return (attrs >> 6) & 0xFF;
}

bool isGrass(int attrs) {
    return (attrs & (1 << (6 + 8 + 0))) != 0;
}

bool isFoliage(int attrs) {
    return (attrs & (1 << (6 + 8 + 1))) != 0;
}

bool isWater(int attrs) {
    return (attrs & (1 << (6 + 8 + 2))) != 0;
}
