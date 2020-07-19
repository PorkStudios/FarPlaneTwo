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

layout(location = 0) in int height;
layout(location = 1) in int block;
layout(location = 2) in int biome;
layout(location = 3) in int light;

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_modelview = mat4(1.0);

uniform dvec2 camera_offset;

out vec3 vert_pos;
out vec2 vert_light;
out flat vec4 vert_color;
out flat int vert_state;

void main(){
    vec2 posXZ = vec2(ivec2(gl_VertexID) / ivec2(65, 1) % 65);
    dvec3 pos = dvec3(camera_offset.x + posXZ.x, double(height) + .5, camera_offset.y + posXZ.y);
    vert_pos = vec3(pos);

    gl_Position = camera_projection * camera_modelview * vec4(pos, 1.);

    /*if (color == 1) { //grass
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
        vert_color = fromARGB(global_info.biome_watercolor[biome]);
    } else {*/
        vert_color = vec4(1.);
    //}
    vert_state = block;

    vert_light = vec2(ivec2(light) >> ivec2(0, 4) & 0xF) / 16.;
}
