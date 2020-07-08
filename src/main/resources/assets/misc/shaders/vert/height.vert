#version 330 core

layout(location = 0) in vec2 vertexPosition_modelspace;
layout(location = 1) in int height;
layout(location = 2) in int color;

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_modelview = mat4(1.0);

uniform vec2 offset = vec2(0.);

uniform samplerBuffer palette;

out float vert_height;
out vec4 vert_color;

void main(){
    vec2 pos = offset + vertexPosition_modelspace;

    gl_Position = camera_projection * camera_modelview * vec4(pos.x, float(height) + .5, pos.y, 1.);
    vert_height = float(height);

    vert_color = vec4(texelFetch(palette, color));
}
