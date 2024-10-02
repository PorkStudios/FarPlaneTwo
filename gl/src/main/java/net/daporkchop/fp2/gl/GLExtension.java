/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 DaPorkchop_
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
 */

package net.daporkchop.fp2.gl;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * All known OpenGL extensions.
 *
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
@Getter
public enum GLExtension {
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_compatibility.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_compatibility.txt</a>
     */
    GL_ARB_compatibility(null, false),

    //OpenGL 3.1
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/EXT/EXT_texture_snorm.txt">https://registry.khronos.org/OpenGL/extensions/EXT/EXT_texture_snorm.txt</a>
     */
    GL_EXT_texture_snorm(GLVersion.OpenGL31, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_draw_instanced.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_draw_instanced.txt</a>
     */
    GL_ARB_draw_instanced(GLVersion.OpenGL31, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_copy_buffer.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_copy_buffer.txt</a>
     */
    GL_ARB_copy_buffer(GLVersion.OpenGL31, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/NV/NV_primitive_restart.txt">https://registry.khronos.org/OpenGL/extensions/NV/NV_primitive_restart.txt</a>
     */
    GL_NV_primitive_restart(GLVersion.OpenGL31, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_buffer_object.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_buffer_object.txt</a>
     */
    GL_ARB_texture_buffer_object(GLVersion.OpenGL31, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_rectangle.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_rectangle.txt</a>
     */
    GL_ARB_texture_rectangle(GLVersion.OpenGL31, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_uniform_buffer_object.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_uniform_buffer_object.txt</a>
     */
    GL_ARB_uniform_buffer_object(GLVersion.OpenGL31, true),

    //OpenGL 3.2
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_geometry_shader4.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_geometry_shader4.txt</a>
     */
    GL_ARB_geometry_shader4(GLVersion.OpenGL32, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_sync.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_sync.txt</a>
     */
    GL_ARB_sync(GLVersion.OpenGL32, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_depth_clamp.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_depth_clamp.txt</a>
     */
    GL_ARB_depth_clamp(GLVersion.OpenGL32, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_multisample.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_multisample.txt</a>
     */
    GL_ARB_texture_multisample(GLVersion.OpenGL32, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_seamless_cube_map.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_seamless_cube_map.txt</a>
     */
    GL_ARB_seamless_cube_map(GLVersion.OpenGL32, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_provoking_vertex.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_provoking_vertex.txt</a>
     */
    GL_ARB_provoking_vertex(GLVersion.OpenGL32, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_fragment_coord_conventions.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_fragment_coord_conventions.txt</a>
     */
    GL_ARB_fragment_coord_conventions(GLVersion.OpenGL32, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_draw_elements_base_vertex.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_draw_elements_base_vertex.txt</a>
     */
    GL_ARB_draw_elements_base_vertex(GLVersion.OpenGL32, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_array_bgra.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_array_bgra.txt</a>
     */
    GL_ARB_vertex_array_bgra(GLVersion.OpenGL32, false),

    //OpenGL 3.3
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_type_2_10_10_10_rev.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_type_2_10_10_10_rev.txt</a>
     */
    GL_ARB_vertex_type_2_10_10_10_rev(GLVersion.OpenGL33, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_instanced_arrays.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_instanced_arrays.txt</a>
     */
    GL_ARB_instanced_arrays(GLVersion.OpenGL33, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_timer_query.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_timer_query.txt</a>
     */
    GL_ARB_timer_query(GLVersion.OpenGL33, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_swizzle.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_swizzle.txt</a>
     */
    GL_ARB_texture_swizzle(GLVersion.OpenGL33, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_rgb10_a2ui.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_rgb10_a2ui.txt</a>
     */
    GL_ARB_texture_rgb10_a2ui(GLVersion.OpenGL33, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_sampler_objects.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_sampler_objects.txt</a>
     */
    GL_ARB_sampler_objects(GLVersion.OpenGL33, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_occlusion_query2.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_occlusion_query2.txt</a>
     */
    GL_ARB_occlusion_query2(GLVersion.OpenGL33, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_explicit_attrib_location.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_explicit_attrib_location.txt</a>
     */
    GL_ARB_explicit_attrib_location(GLVersion.OpenGL33, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_blend_func_extended.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_blend_func_extended.txt</a>
     */
    GL_ARB_blend_func_extended(GLVersion.OpenGL33, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_bit_encoding.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_bit_encoding.txt</a>
     */
    GL_ARB_shader_bit_encoding(GLVersion.OpenGL33, true),

    //OpenGL 4.0
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_draw_buffers_blend.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_draw_buffers_blend.txt</a>
     */
    GL_ARB_draw_buffers_blend(GLVersion.OpenGL40, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_transform_feedback2.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_transform_feedback2.txt</a>
     */
    GL_ARB_transform_feedback2(GLVersion.OpenGL40, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_transform_feedback3.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_transform_feedback3.txt</a>
     */
    GL_ARB_transform_feedback3(GLVersion.OpenGL40, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_cube_map_array.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_cube_map_array.txt</a>
     */
    GL_ARB_texture_cube_map_array(GLVersion.OpenGL40, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_buffer_object_rgb32.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_buffer_object_rgb32.txt</a>
     */
    GL_ARB_texture_buffer_object_rgb32(GLVersion.OpenGL40, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_tessellation_shader.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_tessellation_shader.txt</a>
     */
    GL_ARB_tessellation_shader(GLVersion.OpenGL40, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_sample_shading.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_sample_shading.txt</a>
     */
    GL_ARB_sample_shading(GLVersion.OpenGL40, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_draw_indirect.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_draw_indirect.txt</a>
     */
    GL_ARB_draw_indirect(GLVersion.OpenGL40, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_query_lod.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_query_lod.txt</a>
     */
    GL_ARB_texture_query_lod(GLVersion.OpenGL40, true),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_gpu_shader5.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_gpu_shader5.txt</a>
     */
    GL_ARB_gpu_shader5(GLVersion.OpenGL40, true),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_gpu_shader_fp64.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_gpu_shader_fp64.txt</a>
     */
    GL_ARB_gpu_shader_fp64(GLVersion.OpenGL40, true),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_subroutine.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_subroutine.txt</a>
     */
    GL_ARB_shader_subroutine(GLVersion.OpenGL40, true),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_gather.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_gather.txt</a>
     */
    GL_ARB_texture_gather(GLVersion.OpenGL40, true),

    //OpenGL 4.1
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_viewport_array.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_viewport_array.txt</a>
     */
    GL_ARB_viewport_array(GLVersion.OpenGL41, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_attrib_64bit.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_attrib_64bit.txt</a>
     */
    GL_ARB_vertex_attrib_64_bit(GLVersion.OpenGL41, true),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_precision.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_precision.txt</a>
     */
    GL_ARB_shader_precision(GLVersion.OpenGL41, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_ES2_compatibility.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_ES2_compatibility.txt</a>
     */
    GL_ARB_ES2_compatibility(GLVersion.OpenGL41, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_separate_shader_objects.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_separate_shader_objects.txt</a>
     */
    GL_ARB_separate_shader_objects(GLVersion.OpenGL41, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_get_program_binary.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_get_program_binary.txt</a>
     */
    GL_ARB_get_program_binary(GLVersion.OpenGL41, false),

    //OpenGL 4.2
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_compression_bptc.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_compression_bptc.txt</a>
     */
    GL_ARB_texture_compression_BPTC(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_conservative_depth.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_conservative_depth.txt</a>
     */
    GL_ARB_conservative_depth(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_map_buffer_alignment.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_map_buffer_alignment.txt</a>
     */
    GL_ARB_map_buffer_alignment(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shading_language_packing.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shading_language_packing.txt</a>
     */
    GL_ARB_shading_language_packing(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_compressed_texture_pixel_storage.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_compressed_texture_pixel_storage.txt</a>
     */
    GL_ARB_compressed_texture_pixel_storage(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_internalformat_query.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_internalformat_query.txt</a>
     */
    GL_ARB_internalformat_query(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_base_instance.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_base_instance.txt</a>
     */
    GL_ARB_base_instance(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shading_language_420pack.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shading_language_420pack.txt</a>
     */
    GL_ARB_shading_language_420pack(GLVersion.OpenGL42, true),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_transform_feedback_instanced.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_transform_feedback_instanced.txt</a>
     */
    GL_ARB_transform_feedback_instanced(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_storage.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_storage.txt</a>
     */
    GL_ARB_texture_storage(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_image_load_store.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_image_load_store.txt</a>
     */
    GL_ARB_shader_image_load_store(GLVersion.OpenGL42, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_atomic_counters.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_atomic_counters.txt</a>
     */
    GL_ARB_shader_atomic_counters(GLVersion.OpenGL42, false),

    //OpenGL 4.3
    // TODO: The heirarchy of robustness extensions is quite confusing, and I don't use them anyway.
    ///**
    // * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_robustness_application_isolation.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_robustness_application_isolation.txt</a>
    // */
    //GL_ARB_robustness_isolation(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_robust_buffer_access_behavior.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_robust_buffer_access_behavior.txt</a>
     */
    GL_ARB_robust_buffer_access_behavior(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_attrib_binding.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_attrib_binding.txt</a>
     */
    GL_ARB_vertex_attrib_binding(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_view.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_view.txt</a>
     */
    GL_ARB_texture_view(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_storage_multisample.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_storage_multisample.txt</a>
     */
    GL_ARB_texture_storage_multisample(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_query_levels.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_query_levels.txt</a>
     */
    GL_ARB_texture_query_levels(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_buffer_range.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_buffer_range.txt</a>
     */
    GL_ARB_texture_buffer_range(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_stencil_texturing.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_stencil_texturing.txt</a>
     */
    GL_ARB_stencil_texturing(GLVersion.OpenGL43, false),
    /**
     * Note that this also implies support for the {@code std430} interface block layout in GLSL.
     * <p>
     * Support for this extension implies support for {@link #GL_ARB_program_interface_query}.
     *
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_storage_buffer_object.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_storage_buffer_object.txt</a>
     */
    GL_ARB_shader_storage_buffer_object(GLVersion.OpenGL43, true),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_image_size.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_image_size.txt</a>
     */
    GL_ARB_shader_image_size(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_program_interface_query.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_program_interface_query.txt</a>
     */
    GL_ARB_program_interface_query(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_multi_draw_indirect.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_multi_draw_indirect.txt</a>
     */
    GL_ARB_multi_draw_indirect(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_invalidate_subdata.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_invalidate_subdata.txt</a>
     */
    GL_ARB_invalidate_subdata(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_internalformat_query2.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_internalformat_query2.txt</a>
     */
    GL_ARB_internalformat_query2(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_framebuffer_no_attachments.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_framebuffer_no_attachments.txt</a>
     */
    GL_ARB_framebuffer_no_attachments(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_fragment_layer_viewport.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_fragment_layer_viewport.txt</a>
     */
    GL_ARB_fragment_layer_viewport(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_explicit_uniform_location.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_explicit_uniform_location.txt</a>
     */
    GL_ARB_explicit_uniform_location(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_ES3_compatibility.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_ES3_compatibility.txt</a>
     */
    GL_ARB_ES3_compatibility(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_copy_image.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_copy_image.txt</a>
     */
    GL_ARB_copy_image(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_compute_shader.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_compute_shader.txt</a>
     */
    GL_ARB_compute_shader(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_clear_buffer_object.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_clear_buffer_object.txt</a>
     */
    GL_ARB_clear_buffer_object(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_arrays_of_arrays.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_arrays_of_arrays.txt</a>
     */
    GL_ARB_arrays_of_arrays(GLVersion.OpenGL43, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/KHR/KHR_debug.txt">https://registry.khronos.org/OpenGL/extensions/KHR/KHR_debug.txt</a>
     */
    GL_KHR_debug(GLVersion.OpenGL43, false),

    //OpenGL 4.4
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_type_10f_11f_11f_rev.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_vertex_type_10f_11f_11f_rev.txt</a>
     */
    GL_ARB_vertex_type_10f_11f_11f_rev(GLVersion.OpenGL44, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_stencil8.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_stencil8.txt</a>
     */
    GL_ARB_texture_stencil8(GLVersion.OpenGL44, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_mirror_clamp_to_edge.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_mirror_clamp_to_edge.txt</a>
     */
    GL_ARB_texture_mirror_clamp_to_edge(GLVersion.OpenGL44, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_query_buffer_object.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_query_buffer_object.txt</a>
     */
    GL_ARB_query_buffer_object(GLVersion.OpenGL44, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_multi_bind.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_multi_bind.txt</a>
     */
    GL_ARB_multi_bind(GLVersion.OpenGL44, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_enhanced_layouts.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_enhanced_layouts.txt</a>
     */
    GL_ARB_enhanced_layouts(GLVersion.OpenGL44, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_clear_texture.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_clear_texture.txt</a>
     */
    GL_ARB_clear_texture(GLVersion.OpenGL44, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_buffer_storage.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_buffer_storage.txt</a>
     */
    GL_ARB_buffer_storage(GLVersion.OpenGL44, false),

    //OpenGL 4.5
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_barrier.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_barrier.txt</a>
     */
    GL_ARB_texture_barrier(GLVersion.OpenGL45, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_texture_image_samples.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_texture_image_samples.txt</a>
     */
    GL_ARB_shader_texture_image_samples(GLVersion.OpenGL45, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/KHR/KHR_robustness.txt">https://registry.khronos.org/OpenGL/extensions/KHR/KHR_robustness.txt</a>
     */
    GL_KHR_robustness(GLVersion.OpenGL45, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_get_texture_sub_image.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_get_texture_sub_image.txt</a>
     */
    GL_ARB_get_texture_sub_image(GLVersion.OpenGL45, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_direct_state_access.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_direct_state_access.txt</a>
     */
    GL_ARB_direct_state_access(GLVersion.OpenGL45, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_derivative_control.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_derivative_control.txt</a>
     */
    GL_ARB_derivative_control(GLVersion.OpenGL45, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_conditional_render_inverted.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_conditional_render_inverted.txt</a>
     */
    GL_ARB_conditional_render_inverted(GLVersion.OpenGL45, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_ES3_1_compatibility.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_ES3_1_compatibility.txt</a>
     */
    GL_ARB_ES3_1_compatibility(GLVersion.OpenGL45, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_cull_distance.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_cull_distance.txt</a>
     */
    GL_ARB_cull_distance(GLVersion.OpenGL45, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_clip_control.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_clip_control.txt</a>
     */
    GL_ARB_clip_control(GLVersion.OpenGL45, false),

    //OpenGL 4.6
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_group_vote.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_group_vote.txt</a>
     */
    GL_ARB_shader_group_vote(GLVersion.OpenGL46, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_atomic_counter_ops.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_atomic_counter_ops.txt</a>
     */
    GL_ARB_shader_atomic_counter_ops(GLVersion.OpenGL46, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/KHR/KHR_no_error.txt">https://registry.khronos.org/OpenGL/extensions/KHR/KHR_no_error.txt</a>
     */
    GL_KHR_no_error(GLVersion.OpenGL46, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_polygon_offset_clamp.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_polygon_offset_clamp.txt</a>
     */
    GL_ARB_polygon_offset_clamp(GLVersion.OpenGL46, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_filter_anisotropic.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_texture_filter_anisotropic.txt</a>
     */
    GL_ARB_texture_filter_anisotropic(GLVersion.OpenGL46, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_pipeline_statistics_query.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_pipeline_statistics_query.txt</a>
     */
    GL_ARB_pipeline_statistics_query(GLVersion.OpenGL46, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_transform_feedback_overflow_query.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_transform_feedback_overflow_query.txt</a>
     */
    GL_ARB_transform_feedback_overflow_query(GLVersion.OpenGL46, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_indirect_parameters.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_indirect_parameters.txt</a>
     */
    GL_ARB_indirect_parameters(GLVersion.OpenGL46, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_draw_parameters.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_shader_draw_parameters.txt</a>
     */
    GL_ARB_shader_draw_parameters(GLVersion.OpenGL46, true),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_gl_spirv.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_gl_spirv.txt</a>
     */
    GL_ARB_gl_spirv(GLVersion.OpenGL46, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_spirv_extensions.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_spirv_extensions.txt</a>
     */
    GL_ARB_spirv_extensions(GLVersion.OpenGL46, false),

    //No OpenGL version
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_debug_output.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_debug_output.txt</a>
     */
    GL_ARB_debug_output(null, false),
    /**
     * @see <a href="https://registry.khronos.org/OpenGL/extensions/ARB/ARB_sparse_buffer.txt">https://registry.khronos.org/OpenGL/extensions/ARB/ARB_sparse_buffer.txt</a>
     */
    GL_ARB_sparse_buffer(null, false),
    ;

    private final GLVersion coreVersion;
    private final boolean glsl;

    /**
     * Checks whether or not this extension is a core feature in the given OpenGL version.
     *
     * @param version the OpenGL version
     */
    public final boolean core(@NonNull GLVersion version) {
        return this.coreVersion != null && version.compareTo(this.coreVersion) >= 0;
    }
}
