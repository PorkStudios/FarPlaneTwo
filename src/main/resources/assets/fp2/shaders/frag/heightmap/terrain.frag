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

void main() {
    color = vec4(1., 0., 0., 1.);

    /*if (shouldCancel()) {
        discard;
    } else {
        TextureUV uvs = global_info.tex_uvs[fs_in.state];
        vec2 uv = uvs.min + (uvs.max - uvs.min) * fract(fs_in.pos.xz);

        //initial block texture sample
        vec4 frag_color = fs_in.color * texture(terrain_texture, uv);

        //block/sky light
        frag_color *= texture(lightmap_texture, fs_in.light);

        //shading
        frag_color.rgb *= diffuseLight(normalVector());

        //fog
        //frag_color = addFog(frag_color);

        color = frag_color;
    }*/
}
