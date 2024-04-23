#version 330

layout(std140) uniform Uniforms {
    vec2 u_scale;
};

in vec2 a_pos;

in vec2 a_offset;
in vec4 a_color;

out vec4 v_color;

void main() {
    gl_Position = vec4(((a_pos + a_offset) / 64. - 1.) * u_scale, 0., 1.);
    v_color = a_color;// * vec4(ua_colorFactor(gl_VertexID), 1.0);
}
