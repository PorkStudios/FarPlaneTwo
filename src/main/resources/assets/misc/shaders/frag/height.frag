#version 330 core

const float OFFSET = 0.5;
const float PI     = 3.1415926535897932384626433832795;
const float TWO_PI = 6.2831853071795864769252867665590;
const vec3  BASE   = PI * vec3(0., 0.66666666666666, 1.33333333333333);

in float vert_height;
in vec4 vert_color;

out vec4 color;

void main() {
    //color = vec4(OFFSET + sin(BASE + vert_height * PI), 1.);
    color = vert_color;
}
