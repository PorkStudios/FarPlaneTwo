out vec4 v_color;

void main() {
    gl_Position = vec4(((a_pos + a_offset) / 64.0 - 1.0) * u_scale, 0., 1.);
    v_color = a_color * vec4(ua_colorFactor(gl_VertexID), 1.0);
}
