#version 430 core

#define IS_MESA (biome == 37 || biome == 38 || biome == 39 || biome == 165 || biome == 166 || biome == 167)
#define IS_ROOFED_FOREST (biome == 29 || biome == 157)
#define IS_SWAMP (biome == 6 || biome == 134)

struct TextureUV {
    vec2 min;
    vec2 max;
};

layout(binding = 1) buffer global_info {
    vec2 biome_climate[256];
    int biome_watercolor[256];

    int colormap_grass[256 * 256];
    int colormap_foliage[256 * 256];

    int map_colors[64];

    TextureUV tex_uvs[4096 * 16];
};

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
        return biome_climate[biome].x - float(pos.y - 64.) * .05 / 30.;
    } else {
        return biome_climate[biome].x;
    }
}

vec4 getGrassColor(float temperature, float humidity){
    humidity = humidity * temperature;
    int i = int((1. - temperature) * 255.);
    int j = int((1. - humidity) * 255.);
    return fromARGB(colormap_grass[(j << 8) | i]);
}

vec4 getGrassColorAtPos(dvec3 pos, int biome){
    return getGrassColor(clamp(getTemperature(pos, biome), 0., 1.), clamp(biome_climate[biome].y, 0., 1.));
}

vec4 getFoliageColor(float temperature, float humidity){
    humidity = humidity * temperature;
    int i = int((1. - temperature) * 255.);
    int j = int((1. - humidity) * 255.);
    return fromARGB(colormap_foliage[(j << 8) | i]);
}

vec4 getFoliageColorAtPos(dvec3 pos, int biome){
    return getGrassColor(clamp(getTemperature(pos, biome), 0., 1.), clamp(biome_climate[biome].y, 0., 1.));
}
