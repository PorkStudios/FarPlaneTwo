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

package net.daporkchop.fp2.core.engine.client.index;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.alloc.DirectMemoryAllocator;
import net.daporkchop.fp2.core.client.render.GlobalRenderer;
import net.daporkchop.fp2.core.client.render.state.CameraStateUniforms;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.client.bake.storage.BakeStorage;
import net.daporkchop.fp2.core.engine.client.index.attribdivisor.CPUCulledBaseInstanceRenderIndex;
import net.daporkchop.fp2.core.engine.client.index.attribdivisor.CPUCulledUniformRenderIndex;
import net.daporkchop.fp2.core.engine.client.index.attribdivisor.GPUCulledBaseInstanceRenderIndex;
import net.daporkchop.fp2.core.engine.client.struct.VoxelLocalAttributes;
import net.daporkchop.fp2.gl.GLExtensionSet;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.gl.attribute.UniformBuffer;

/**
 * The different {@link RenderIndex} implementations.
 *
 * @author DaPorkchop_
 */
public enum RenderIndexType {
    GPU_CULLED_INDIRECT_MULTIDRAW {
        @Override
        public boolean enabled(@NonNull FP2Config config) {
            return config.performance().gpuFrustumCulling();
        }

        @Override
        public GLExtensionSet requiredExtensions() {
            return GPUCulledBaseInstanceRenderIndex.REQUIRED_EXTENSIONS;
        }

        @Override
        public RenderIndex<VoxelLocalAttributes> createRenderIndex(OpenGL gl, BakeStorage<VoxelLocalAttributes> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
            gl.checkSupported(this.requiredExtensions());
            return new GPUCulledBaseInstanceRenderIndex<>(gl, bakeStorage, alloc, globalRenderer.voxelInstancedAttributesFormat, globalRenderer, cameraStateUniformsBuffer);
        }
    },

    CPU_CULLED_INDIRECT_MULTIDRAW {
        @Override
        public boolean enabled(@NonNull FP2Config config) {
            return true;
        }

        @Override
        public GLExtensionSet requiredExtensions() {
            return CPUCulledBaseInstanceRenderIndex.REQUIRED_EXTENSIONS;
        }

        @Override
        public RenderIndex<VoxelLocalAttributes> createRenderIndex(OpenGL gl, BakeStorage<VoxelLocalAttributes> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
            gl.checkSupported(this.requiredExtensions());
            return new CPUCulledBaseInstanceRenderIndex<>(gl, bakeStorage, alloc, globalRenderer.voxelInstancedAttributesFormat);
        }
    },

    CPU_CULLED_UNIFORM_SINGLE {
        @Override
        public boolean enabled(@NonNull FP2Config config) {
            return true;
        }

        @Override
        public GLExtensionSet requiredExtensions() {
            return CPUCulledUniformRenderIndex.REQUIRED_EXTENSIONS;
        }

        @Override
        public RenderIndex<VoxelLocalAttributes> createRenderIndex(OpenGL gl, BakeStorage<VoxelLocalAttributes> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer) {
            gl.checkSupported(this.requiredExtensions());
            return new CPUCulledUniformRenderIndex<>(gl, bakeStorage, alloc);
        }
    };

    /**
     * Checks if this render index implementation is enabled with the current configuration.
     *
     * @param config the current config
     * @return {@code true} if this render index implementation is enabled
     */
    public abstract boolean enabled(@NonNull FP2Config config);

    /**
     * @return a {@link GLExtensionSet} containing the OpenGL extensions required by this render index implementation
     */
    public abstract GLExtensionSet requiredExtensions();

    /**
     * Creates a new {@link RenderIndex} instance using this implementation.
     *
     * @param gl                        the OpenGL context
     * @param bakeStorage               the baked tile data storage
     * @param alloc                     an allocator for off-heap memory
     * @param globalRenderer            the {@link GlobalRenderer} instance
     * @param cameraStateUniformsBuffer the uniform buffer containing camera state uniforms
     * @return a new {@link RenderIndex} instance using this implementation
     * @throws UnsupportedOperationException if this render index implementation isn't supported
     */
    public abstract RenderIndex<VoxelLocalAttributes> createRenderIndex(OpenGL gl, BakeStorage<VoxelLocalAttributes> bakeStorage, DirectMemoryAllocator alloc, GlobalRenderer globalRenderer, UniformBuffer<CameraStateUniforms> cameraStateUniformsBuffer);
}
