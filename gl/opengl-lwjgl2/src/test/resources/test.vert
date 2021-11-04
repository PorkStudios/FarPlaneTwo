uniform UNIFORM_0 {
    vec2 u_scale;
};

in vec4 a_color;
in vec2 a_pos;
in vec2 a_offset;

out vec4 v_color;

void main() {
    gl_Position = vec4(((a_pos + a_offset) / 64.0 - 1.0) * u_scale, 0., 1.);
    v_color = a_color;
}
