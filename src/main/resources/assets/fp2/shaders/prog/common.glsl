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

//
//
// MACROS
//
//

#define LAYER_SOLID (1 << 0)
#define LAYER_CUTOUT_MIPPED (1 << 1)
#define LAYER_CUTOUT (1 << 2)
#define LAYER_TRANSLUCENT (1 << 3)

#define GL_LINEAR (9729)
#define GL_EXP (2048)
#define GL_EXP2 (2049)

//
//
// UNIFORMS
//
//

//OpenGL state

struct Camera {
    mat4 projection;
    mat4 modelview;

    dvec3 position;
};

struct Fog {
    vec4 color;

    float density;
    float start;
    float end;
    float scale;

    int mode;
};

layout(std140, binding = 0) uniform GL_STATE {
    Camera camera;
    Fog fog;
} gl_state;

//
//
// BUFFERS
//
//

layout(std430, binding = 0) buffer RENDERABLE_CHUNKS {
    ivec4 base;//using 4d vectors because apparently GLSL is too stupid to handle 3d ones
    ivec4 size;
    int data[];
} renderable_chunks;

bool isChunkSectionRenderable(ivec3 chunk)  {
    chunk -= renderable_chunks.base.xyz;
    if (any(lessThan(chunk, ivec3(0))) || any(greaterThanEqual(chunk, renderable_chunks.size.xyz)))    {
        return false;
    }
    int index = (chunk.x * renderable_chunks.size.y + chunk.y) * renderable_chunks.size.z + chunk.z;
    return (renderable_chunks.data[index >> 5] & (1 << (index & 0x1F))) != 0;
}

struct TextureUV {
    vec2 min;
    vec2 max;
};

layout(std430, binding = 1) buffer GLOBAL_INFO {
    vec2 biome_climate[256];
    int biome_watercolor[256];

    int colormap_grass[256 * 256];
    int colormap_foliage[256 * 256];

    int map_colors[64];

    TextureUV tex_uvs[];
} global_info;

//
//
// UTILITIES
//
//

// color unpacking

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

// biome climate data access

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

// vertex transformation

vec4 cameraTransform(vec4 point) {
    return gl_state.camera.projection * gl_state.camera.modelview * point;
}

vec4 cameraTransform(vec3 point)   {
    return cameraTransform(vec4(point, 1.));
}

// lighting

float diffuseLight(vec3 normal) {
    return min(normal.x * normal.x * .6 + normal.y * normal.y * ((3. + normal.y) / 4.) + normal.z * normal.z * .8, 1.);
}
