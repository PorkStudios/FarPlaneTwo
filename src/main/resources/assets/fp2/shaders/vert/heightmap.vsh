layout(location = 0) in vec2 vertexPosition_modelspace;
layout(location = 1) in int height;
layout(location = 2) in int color;
layout(location = 3) in int biome;
layout(location = 4) in int state;
layout(location = 5) in int light;

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_modelview = mat4(1.0);

uniform dvec2 camera_offset;

out vec3 vert_pos;
out vec2 vert_light;
out flat vec4 vert_color;
out flat int vert_state;

void main(){
    double fheight = double(height);
    dvec3 pos = dvec3(camera_offset.x + vertexPosition_modelspace.x, fheight + .5, camera_offset.y + vertexPosition_modelspace.y);
    vert_pos = vec3(pos);

    gl_Position = camera_projection * camera_modelview * vec4(pos, 1.);

    if (color == 1) { //grass
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
    } else if (color == 7)  { //foliage
        if (IS_SWAMP) {
            vert_color = fromRGB(6975545);
        } else if (IS_MESA) {
            vert_color = fromRGB(10387789);
        } else {
            vert_color = getFoliageColorAtPos(pos, biome);
        }
    } else if (color == 12)  { //water
        //constant color is taken from water_overlay.png, and should have an opacity of 179
        vert_color = /*fromARGB(0xB3212FAB) * */fromARGB(biome_watercolor[biome]);
    } else {
        vert_color = vec4(1.);
        //vert_color = fromARGB(map_colors[color]);
    }
    vert_state = state;

    vert_light = vec2(ivec2(light) >> ivec2(0, 16) & 0xF) / 16.;
}
