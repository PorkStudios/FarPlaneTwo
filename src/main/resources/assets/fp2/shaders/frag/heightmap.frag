in vec3 vert_pos;
in vec2 vert_light;
in flat vec4 vert_color;
in flat int vert_state;

layout(binding = 0) uniform sampler2D terrain_texture;
layout(binding = 1) uniform sampler2D lightmap_texture;

out vec4 color;

float rand(vec2 co){
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    if (isLoaded(ivec3(floor(vert_pos)) >> 4)) {
        discard;//TODO: figure out the potential performance implications of this vs transparent output
        //color = vec4(0.);
    } else {
        TextureUV uvs = tex_uvs[vert_state];
        vec4 textured_color = vert_color * texture(terrain_texture, uvs.min + (uvs.max - uvs.min) * fract(vert_pos.xz));
        if (textured_color.a < 1.)  {
            textured_color = vec4(0.); //TODO: this is hilariously slow lol
            float i = 0.;
            for (int x = 0; x < 16; x++)    {
                for (int z = 0; z < 16; z++)    {
                    vec4 c = vert_color * texture(terrain_texture, uvs.min + (uvs.max - uvs.min) * vec2(x, z) / 16.);
                    if (c.a == 1.)  {
                        textured_color += c;
                        i++;
                    }
                }
            }
            textured_color /= i;
        }
        color = texture(lightmap_texture, vert_light) * textured_color;
    }
}
