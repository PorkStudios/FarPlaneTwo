/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.gl.opengl;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * All known OpenGL extensions.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public enum GLExtension {
    GL_ARB_compatibility(GLVersion.OpenGL30, false) {
        @Override
        public boolean supported(@NonNull OpenGL gl) {
            return gl.extensions().contains(this);
        }
    },

    //OpenGL 3.1
    GL_ARB_draw_instanced(GLVersion.OpenGL31, false),
    GL_ARB_copy_buffer(GLVersion.OpenGL31, false),
    GL_NV_primitive_restart(GLVersion.OpenGL31, false),
    GL_ARB_texture_buffer_object(GLVersion.OpenGL31, false),
    GL_ARB_texture_rectangle(GLVersion.OpenGL31, false),
    GL_ARB_uniform_buffer_object(GLVersion.OpenGL31, true),

    //OpenGL 3.2
    GL_ARB_geometry_shader4(GLVersion.OpenGL32, false),
    GL_ARB_sync(GLVersion.OpenGL32, false),
    GL_ARB_depth_clamp(GLVersion.OpenGL32, false),
    GL_ARB_texture_multisample(GLVersion.OpenGL32, false),
    GL_ARB_seamless_cube_map(GLVersion.OpenGL32, false),
    GL_ARB_provoking_vertex(GLVersion.OpenGL32, false),
    GL_ARB_fragment_coord_conventions(GLVersion.OpenGL32, false),
    GL_ARB_draw_elements_base_vertex(GLVersion.OpenGL32, false),
    GL_ARB_vertex_array_bgra(GLVersion.OpenGL32, false),

    //OpenGL 3.3
    GL_ARB_vertex_type_2_10_10_10_rev(GLVersion.OpenGL33, false),
    GL_ARB_instanced_arrays(GLVersion.OpenGL33, false),
    GL_ARB_timer_query(GLVersion.OpenGL33, false),
    GL_ARB_texture_swizzle(GLVersion.OpenGL33, false),
    GL_ARB_texture_rgb10_a2ui(GLVersion.OpenGL33, false),
    GL_ARB_sampler_objects(GLVersion.OpenGL33, false),
    GL_ARB_occlusion_query2(GLVersion.OpenGL33, false),
    GL_ARB_explicit_attrib_location(GLVersion.OpenGL33, false),
    GL_ARB_blend_func_extended(GLVersion.OpenGL33, false),
    GL_ARB_shader_bit_encoding(GLVersion.OpenGL33, true),

    //OpenGL 4.0
    GL_ARB_draw_buffers_blend(GLVersion.OpenGL40, false),
    GL_ARB_transform_feedback2(GLVersion.OpenGL40, false),
    GL_ARB_transform_feedback3(GLVersion.OpenGL40, false),
    GL_ARB_texture_cube_map_array(GLVersion.OpenGL40, false),
    GL_ARB_texture_buffer_object_rgb32(GLVersion.OpenGL40, false),
    GL_ARB_tessellation_shader(GLVersion.OpenGL40, false),
    GL_ARB_sample_shading(GLVersion.OpenGL40, false),
    GL_ARB_draw_indirect(GLVersion.OpenGL40, false),
    GL_ARB_texture_query_lod(GLVersion.OpenGL40, true),
    GL_ARB_gpu_shader5(GLVersion.OpenGL40, true),
    GL_ARB_gpu_shader_fp64(GLVersion.OpenGL40, true),
    GL_ARB_shader_subroutine(GLVersion.OpenGL40, true),
    GL_ARB_texture_gather(GLVersion.OpenGL40, true),

    //OpenGL 4.1
    GL_ARB_viewport_array(GLVersion.OpenGL41, false),
    GL_ARB_vertex_attrib_64_bit(GLVersion.OpenGL41, true),
    GL_ARB_shader_precision(GLVersion.OpenGL41, false),
    GL_ARB_ES2_compatibility(GLVersion.OpenGL41, false),
    GL_ARB_separate_shader_objects(GLVersion.OpenGL41, false),
    GL_ARB_get_program_binary(GLVersion.OpenGL41, false),

    //OpenGL 4.2
    GL_ARB_texture_compression_BPTC(GLVersion.OpenGL42, false),
    GL_ARB_conservative_depth(GLVersion.OpenGL42, false),
    GL_ARB_map_buffer_alignment(GLVersion.OpenGL42, false),
    GL_ARB_shading_language_packing(GLVersion.OpenGL42, false),
    GL_ARB_compressed_texture_pixel_storage(GLVersion.OpenGL42, false),
    GL_ARB_internalformat_query(GLVersion.OpenGL42, false),
    GL_ARB_base_instance(GLVersion.OpenGL42, false),
    GL_ARB_shading_language_420pack(GLVersion.OpenGL42, true),
    GL_ARB_transform_feedback_instanced(GLVersion.OpenGL42, false),
    GL_ARB_texture_storage(GLVersion.OpenGL42, false),
    GL_ARB_shader_image_load_store(GLVersion.OpenGL42, false),
    GL_ARB_shader_atomic_counters(GLVersion.OpenGL42, false),

    //OpenGL 4.3
    GL_ARB_robustness_isolation(GLVersion.OpenGL43, false),
    GL_ARB_robust_buffer_access_behavior(GLVersion.OpenGL43, false),
    GL_ARB_vertex_attrib_binding(GLVersion.OpenGL43, false),
    GL_ARB_texture_view(GLVersion.OpenGL43, false),
    GL_ARB_texture_storage_multisample(GLVersion.OpenGL43, false),
    GL_ARB_texture_query_levels(GLVersion.OpenGL43, false),
    GL_ARB_texture_buffer_range(GLVersion.OpenGL43, false),
    GL_ARB_stencil_texturing(GLVersion.OpenGL43, false),
    GL_ARB_shader_storage_buffer_object(GLVersion.OpenGL43, true),
    GL_ARB_shader_image_size(GLVersion.OpenGL43, false),
    GL_ARB_program_interface_query(GLVersion.OpenGL43, false),
    GL_ARB_multi_draw_indirect(GLVersion.OpenGL43, false),
    GL_ARB_invalidate_subdata(GLVersion.OpenGL43, false),
    GL_ARB_internalformat_query2(GLVersion.OpenGL43, false),
    GL_ARB_framebuffer_no_attachments(GLVersion.OpenGL43, false),
    GL_ARB_fragment_layer_viewport(GLVersion.OpenGL43, false),
    GL_ARB_explicit_uniform_location(GLVersion.OpenGL43, false),
    GL_ARB_ES3_compatibility(GLVersion.OpenGL43, false),
    GL_ARB_copy_image(GLVersion.OpenGL43, false),
    GL_ARB_compute_shader(GLVersion.OpenGL43, false),
    GL_ARB_clear_buffer_object(GLVersion.OpenGL43, false),
    GL_ARB_arrays_of_arrays(GLVersion.OpenGL43, false),
    GL_KHR_debug(GLVersion.OpenGL43, false),

    //OpenGL 4.4
    GL_ARB_vertex_type_10f_11f_11f_rev(GLVersion.OpenGL44, false),
    GL_ARB_texture_stencil8(GLVersion.OpenGL44, false),
    GL_ARB_texture_mirror_clamp_to_edge(GLVersion.OpenGL44, false),
    GL_ARB_query_buffer_object(GLVersion.OpenGL44, false),
    GL_ARB_multi_bind(GLVersion.OpenGL44, false),
    GL_ARB_enhanced_layouts(GLVersion.OpenGL44, false),
    GL_ARB_clear_texture(GLVersion.OpenGL44, false),
    GL_ARB_buffer_storage(GLVersion.OpenGL44, false),

    //OpenGL 4.5
    GL_ARB_texture_barrier(GLVersion.OpenGL45, false),
    GL_ARB_shader_texture_image_samples(GLVersion.OpenGL45, false),
    GL_KHR_robustness(GLVersion.OpenGL45, false),
    GL_ARB_get_texture_sub_image(GLVersion.OpenGL45, false),
    GL_ARB_direct_state_access(GLVersion.OpenGL45, false),
    GL_ARB_derivative_control(GLVersion.OpenGL45, false),
    GL_ARB_conditional_render_inverted(GLVersion.OpenGL45, false),
    GL_ARB_ES3_1_compatibility(GLVersion.OpenGL45, false),
    GL_ARB_cull_distance(GLVersion.OpenGL45, false),
    GL_ARB_clip_control(GLVersion.OpenGL45, false),

    //OpenGL 4.6
    GL_ARB_shader_group_vote(GLVersion.OpenGL46, false),
    GL_ARB_shader_atomic_counter_ops(GLVersion.OpenGL46, false),
    GL_KHR_no_error(GLVersion.OpenGL46, false),
    GL_ARB_polygon_offset_clamp(GLVersion.OpenGL46, false),
    GL_ARB_texture_filter_anisotropic(GLVersion.OpenGL46, false),
    GL_ARB_pipeline_statistics_query(GLVersion.OpenGL46, false),
    GL_ARB_transform_feedback_overflow_query(GLVersion.OpenGL46, false),
    GL_ARB_indirect_parameters(GLVersion.OpenGL46, false),
    GL_ARB_shader_draw_parameters(GLVersion.OpenGL46, true),
    GL_ARB_gl_spirv(GLVersion.OpenGL46, false),
    GL_ARB_spirv_extensions(GLVersion.OpenGL46, false),
    ;

    private final GLVersion coreVersion;

    @Getter
    private final boolean glsl;

    /**
     * Checks whether or not this extension is a core feature in the given OpenGL context.
     *
     * @param gl the context
     */
    public boolean core(@NonNull OpenGL gl) {
        return this.coreVersion != null && gl.version().compareTo(this.coreVersion) >= 0;
    }

    /**
     * Checks whether or not this extension is supported in the given OpenGL context.
     *
     * @param gl the context
     */
    public boolean supported(@NonNull OpenGL gl) {
        return this.core(gl) || gl.extensions().contains(this);
    }
}
