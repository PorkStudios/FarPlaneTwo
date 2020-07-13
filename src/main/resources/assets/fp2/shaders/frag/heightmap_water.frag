in vec3 vert_pos;
in vec2 vert_light;
in flat vec4 vert_color;

layout(binding = 0) uniform sampler2D terrain_texture;
layout(binding = 1) uniform sampler2D lightmap_texture;

out vec4 color;

void main() {
    if (isLoaded(ivec3(floor(vert_pos)) >> 4)) {
        discard;//TODO: figure out the potential performance implications of this vs transparent output
        //color = vec4(0.);
    } else {
        TextureUV uvs = tex_uvs[9];
        //color = vert_color * texture(terrain_texture, uvs.min + (uvs.max - uvs.min) * fract(vert_pos.xz));
        color = vert_color * texture(lightmap_texture, vec2(0., 1.)) * texture(terrain_texture, uvs.min + (uvs.max - uvs.min) * fract(vert_pos.xz));
    }
}
