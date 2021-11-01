in vec4 a_color;
in vec2 a_pos;

out VS_OUT {
    vec4 color;
} vs_out;

void main() {
    gl_Position = vec4(a_pos / 64.0 - 1.0, 0., 1.);
    vs_out.color = a_color;
}
