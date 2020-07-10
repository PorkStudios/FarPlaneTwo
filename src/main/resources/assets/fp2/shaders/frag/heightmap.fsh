in vec3 vert_pos;
in flat vec4 vert_color;
in flat int vert_state;

layout(binding = 0) uniform sampler2D terrain_texture;

layout(binding = 0) buffer loaded_chunks {
    ivec4 loaded_base; //using 4d vectors because apparently GLSL is too stupid to handle 3d ones
    ivec4 loaded_size;
    int loaded_data[];
};

out vec4 color;

bool isLoaded(ivec3 chunk)  {
    chunk -= loaded_base.xyz;
    if (any(lessThan(chunk, ivec3(0))) || any(greaterThanEqual(chunk, loaded_size.xyz)))    {
        return false;
    }
    int index = (chunk.x * loaded_size.y + chunk.y) * loaded_size.z + chunk.z;
    return (loaded_data[index >> 5] & (1 << (index & 0x1F))) != 0;
}

void main() {
    if (true || isLoaded(ivec3(vert_pos) >> 4)) {
        discard;//TODO: figure out the potential performance implications of this vs transparent output
        //color = vec4(0.);
    } else {
        TextureUV uvs = tex_uvs[vert_state];
        color = vert_color * texture(terrain_texture, uvs.min + (uvs.max - uvs.min) * fract(vert_pos.xz));
    }
}
