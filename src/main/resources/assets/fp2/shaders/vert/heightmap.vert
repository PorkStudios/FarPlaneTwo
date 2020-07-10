#version 430 core

#define PALETTE_OFFSET (0)

#define IS_MESA (biome == 37 || biome == 38 || biome == 39 || biome == 165 || biome == 166 || biome == 167)
#define IS_ROOFED_FOREST (biome == 29 || biome == 157)
#define IS_SWAMP (biome == 6 || biome == 134)

layout(location = 0) in vec2 vertexPosition_modelspace;
layout(location = 1) in int height;
layout(location = 2) in int color;
layout(location = 3) in int biome;

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_modelview = mat4(1.0);

uniform dvec2 camera_offset;

layout(binding = 1) buffer global_info {
    vec2 biome_climate[256];

    int colormap_grass[256 * 256];
    int colormap_foliage[256 * 256];
    int biome_watercolor[256];
    int map_colors[64];
};

out vec3 vert_pos;
out vec4 vert_color;

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

float getTemperature(dvec3 pos) {
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

vec4 getGrassColorAtPos(dvec3 pos){
    return getGrassColor(clamp(getTemperature(pos), 0., 1.), clamp(biome_climate[biome].y, 0., 1.));
}

vec4 getFoliageColor(float temperature, float humidity){
    humidity = humidity * temperature;
    int i = int((1. - temperature) * 255.);
    int j = int((1. - humidity) * 255.);
    return fromARGB(colormap_foliage[(j << 8) | i]);
}

vec4 getFoliageColorAtPos(dvec3 pos){
    return getGrassColor(clamp(getTemperature(pos), 0., 1.), clamp(biome_climate[biome].y, 0., 1.));
}

void main(){
    double fheight = double(height);
    dvec3 pos = dvec3(camera_offset.x + vertexPosition_modelspace.x, fheight + .5, camera_offset.y + vertexPosition_modelspace.y);
    vert_pos = vec3(pos);

    gl_Position = camera_projection * camera_modelview * vec4(pos, 1.);

    if (color == 1) { //grass
        if (IS_SWAMP) {
            vert_color = fromRGB(-1. < -.1 ? 5011004 : 6975545);
        } else if (IS_ROOFED_FOREST)    {
            vec4 original = getGrassColorAtPos(pos);
            vert_color = vec4(((original + fromARGB(0x0028340A)) * .5).rgb, original.a);
        } else if (IS_MESA) {
            vert_color = fromRGB(9470285);
        } else {
            vert_color = getGrassColorAtPos(pos);
        }
    } else if (color == 7)  { //foliage
        if (IS_SWAMP) {
            vert_color = fromRGB(6975545);
        } else if (IS_MESA) {
            vert_color = fromRGB(10387789);
        } else {
            vert_color = getFoliageColorAtPos(pos);
        }
    } else if (color == 12)  { //water
        //constant color is taken from water_overlay.png, and should have an opacity of 179
        vert_color = fromARGB(0xB3212FAB) * fromARGB(biome_watercolor[biome]);
    } else {
        vert_color = fromARGB(map_colors[color]);
    }
}
