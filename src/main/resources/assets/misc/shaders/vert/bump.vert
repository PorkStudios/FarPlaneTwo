#version 330 core

layout(location = 0) in vec2 vertexPosition_modelspace;
layout(location = 1) in int height;
layout(location = 2) in int c;

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_view = mat4(1.0);
uniform mat4 camera_model = mat4(1.0);

out float vert_height;
out vec3 vert_color;

void main(){
    float fheight = float(height) / 256.;
    gl_Position = camera_projection * camera_view * camera_model * vec4(vertexPosition_modelspace.x, fheight, vertexPosition_modelspace.y, 1.);

    vert_height = fheight;
    vert_color = vec3(ivec3(c) >> ivec3(16, 8, 0) & 0xFF) / 255.;
}
