in vec4 v_color;

void main() {
    f_color = v_color * t_colorFactor(gl_FragCoord.xy / 512.0);
}
