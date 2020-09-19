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

//
//
// TEXTURES
//
//

//textures
layout(binding = 0) uniform sampler2D terrain_texture;
layout(binding = 1) uniform sampler2D lightmap_texture;

//
//
// INPUTS
//
//

in VS_OUT {
    vec3 pos;
    vec2 light;

    flat vec3 color;
    flat vec3 base_pos;
    flat int state;
} fs_in;

//
//
// OUTPUTS
//
//

out vec4 color;

//
//
// UTILITIES
//
//

vec3 normalVector() {
    vec3 fdx = dFdx(fs_in.pos);
    vec3 fdy = dFdy(fs_in.pos);
    return normalize(cross(fdx, fdy));
}

/*vec2 texUv(int state, vec3 normal)  {
    vec3 delta = fs_in.pos - fs_in.base_pos;

    //does this have some special name? i have no idea, but i'm pretty proud of it
    vec2 uv_factor = vec2(0.);
    uv_factor += delta.xz * normal.y;
    uv_factor += delta.xy * normal.z;
    uv_factor += delta.zy * normal.x;

    TextureUV tex_uv = global_info.tex_uvs[state * 6 + normalToFaceIndex(normal)];
    return tex_uv.min + (tex_uv.max - tex_uv.min) * fract(uv_factor);
}*/

vec2 texUvFactor(vec3 normal)  {
    vec3 delta = fs_in.pos - fs_in.base_pos;

    //does this have some special name? i have no idea, but i'm pretty proud of it
    vec2 uv_factor = vec2(0.);
    uv_factor += delta.xz * normal.y;
    uv_factor += delta.xy * normal.z;
    uv_factor += delta.zy * normal.x;

    return fract(uv_factor);
}

vec4 sampleTerrain(vec3 normal)  {
    BakedQuad quad = quad_data[quad_indices[fs_in.state * 6 + normalToFaceIndex(normal)]];

    //raw color
    vec4 frag_color = texture(terrain_texture, lerp(quad.min, quad.max, texUvFactor(normal)));

    //apply tint if the quad allows it (branchless implementation)
    frag_color.rgb *= max(fs_in.color, vec3(quad.tintFactor));

    return frag_color;
}
