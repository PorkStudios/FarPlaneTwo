layout(location = 0) in vec2 vertexPosition_modelspace;
layout(location = 3) in int biome;
layout(location = 4) in int state;
layout(location = 5) in int light;

uniform mat4 camera_projection = mat4(1.0);
uniform mat4 camera_modelview = mat4(1.0);

uniform dvec2 camera_offset;

uniform float seaLevel;

out vec3 vert_pos;
out vec2 vert_light;
out flat vec4 vert_color;

void main(){
    dvec3 pos = dvec3(camera_offset.x + vertexPosition_modelspace.x, seaLevel - .125, camera_offset.y + vertexPosition_modelspace.y);
    vert_pos = vec3(pos);

    gl_Position = camera_projection * camera_modelview * vec4(pos, 1.);

    vert_color = fromARGB(biome_watercolor[biome]);

    vert_light = vec2(ivec2(light) >> ivec2(0, 16) & 0xF) / 16.;
}
