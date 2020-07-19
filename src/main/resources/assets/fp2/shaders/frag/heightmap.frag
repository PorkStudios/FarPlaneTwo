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

layout(binding = 0) uniform sampler2D terrain_texture;
layout(binding = 1) uniform sampler2D lightmap_texture;

out vec4 color;

in vec3 vert_pos;
in vec2 vert_light;
in flat vec4 vert_color;
in flat int vert_state;

float rand(vec2 co){
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    if (isLoaded(ivec3(floor(vert_pos)) >> 4)) {
        discard;//TODO: figure out the potential performance implications of this vs transparent output
        //color = vec4(0.);
    } else {
        TextureUV uvs = global_info.tex_uvs[vert_state];
        vec4 textured_color = vert_color * texture(terrain_texture, uvs.min + (uvs.max - uvs.min) * fract(vert_pos.xz));
        textured_color.a = 1.;
        color = texture(lightmap_texture, vert_light) * textured_color;
    }
}
