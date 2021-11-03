in vec4 a_color;
in vec2 a_pos;
in vec2 a_offset;

out VS_OUT {
    vec4 color;
} vs_out;

void main() {
    gl_Position = vec4((a_pos + a_offset) / 64.0 - 1.0, 0., 1.);
    vs_out.color = a_color;
}
