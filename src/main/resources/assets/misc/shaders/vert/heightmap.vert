#version 330 core

#define PALETTE_OFFSET (0)
#define GRASS_BUFFER_OFFSET (0)
#define FOLIAGE_BUFFER_OFFSET (GRASS_BUFFER_OFFSET + 256 * 256)

layout(location = 0) in vec2 vertexPosition_modelspace;
layout(location = 1) in int height;
layout(location = 2) in int color;
layout(location = 3) in uint biome;

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_modelview = mat4(1.0);

uniform vec2 camera_offset = vec2(0.);

uniform vec2 biome_climate[256];

uniform samplerBuffer palettePlusClimate;
uniform samplerBuffer grassBuffer;

out float vert_height;
out vec4 vert_color;

float getTemperature(vec3 pos) {
    if (pos.y > 64.)   {
        //TODO: noise
        float f = 0.;
        return biome_climate[biome].x - (pos.y - 64. + f) * .05 / 30.;
    } else {
        return biome_climate[biome].x;
    }
}

vec4 getGrassColor(float temperature, float humidity){
    humidity = humidity * temperature;
    int i = int((1. - temperature) * 255.);
    int j = int((1. - humidity) * 255.);
    return texelFetch(grassBuffer, GRASS_BUFFER_OFFSET + ((j << 8) | i));
}

vec4 getGrassColorAtPos(vec3 pos){
    return getGrassColor(clamp(getTemperature(pos), 0., 1.), clamp(biome_climate[biome].y, 0., 1.));
}

vec4 getFoliageColor(float temperature, float humidity){
    humidity = humidity * temperature;
    int i = int((1. - temperature) * 255.);
    int j = int((1. - humidity) * 255.);
    return texelFetch(grassBuffer, FOLIAGE_BUFFER_OFFSET + ((j << 8) | i));
}

vec4 getFoliageColor(vec3 pos){
    return getGrassColor(clamp(getTemperature(pos), 0., 1.), clamp(biome_climate[biome].y, 0., 1.));
}

void main(){
    float fheight = float(height);
    vec3 pos = vec3(camera_offset.x + vertexPosition_modelspace.x, fheight + .5, camera_offset.y + vertexPosition_modelspace.y);

    gl_Position = camera_projection * camera_modelview * vec4(pos, 1.);

    vert_height = fheight;
    if (color == 1) { //grass
        vert_color = getGrassColorAtPos(pos);
    } else if (color == 7)  {
        vert_color = getFoliageColorAtPos(pos);
    } else {
        vert_color = texelFetch(palettePlusClimate, PALETTE_OFFSET + color);
    }
    //vert_color = vec4(uvec4(color) >> uvec4(16, 8, 0, 24) & uint(0xFF)) / 255.;
}
