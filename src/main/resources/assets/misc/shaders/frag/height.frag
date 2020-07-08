#version 330 core

in float vert_height;
in vec4 vert_color;

out vec4 color;

void main() {
    color = vert_color;
}
