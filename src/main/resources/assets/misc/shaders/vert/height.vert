#version 330 core

layout(location = 0) in vec2 vertexPosition_modelspace;
layout(location = 1) in int height;

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_modelview = mat4(1.0);

uniform vec2 offset = vec2(0.);

out float vert_height;

void main(){
    vec2 pos = offset + vertexPosition_modelspace;

    float fheight = float(height) / 256. * 2.;
    gl_Position = camera_projection * camera_modelview * vec4(pos.x, fheight + 70., pos.y, 1.);

    vert_height = fheight;
}
