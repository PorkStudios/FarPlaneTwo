#version 430 core

in vec3 vert_pos;
in vec4 vert_color;

layout(binding = 0) buffer loaded_chunks {
    /*ivec3 base;
    ivec3 size;
    int data[];*/
    int count;
    int data[];
};

out vec4 color;

bool isLoaded(ivec3 chunk)  {
    /*chunk -= base;
    if (any(greaterThan(chunk, size - 1)) || any(lessThan(chunk, ivec3(0))))    {
        return false;
    }
    int index = (chunk.x * size.y + chunk.y) * size.z + chunk.z;
    return data[index] != 0;*/
    for (int i = 0; i < count; i++) {
        if (data[i * 2] == chunk.x && data[i * 2 + 1] == chunk.z)    {
            return true;
        }
    }
    return false;
}

void main() {
    if (isLoaded(ivec3(vert_pos) >> 4)) {
        color = vec4(0.);
    } else {
        color = vert_color;
    }
}
