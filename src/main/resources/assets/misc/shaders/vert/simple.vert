#version 330 core

layout(location = 0) in vec3 vertexPosition_modelspace;

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_view = mat4(1.0);
uniform mat4 camera_model = mat4(1.0);

out vec4 vert_color;

void main(){
    gl_Position = camera_projection * camera_view * camera_model * vec4(vertexPosition_modelspace, 1.0);
    vert_color = vec4(1.);
}
